/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package edu.cmu.sphinx.research.distributed.client;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FrontEnd;

import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.util.SphinxProperties;



/**
 * An implementation of the ClientFrontEnd interface.
 */
public class ClientFrontEndImpl implements ClientFrontEnd {


    private BufferedReader reader;
    private DataOutputStream dataWriter;

    private String serverAddress;
    private int serverPort;
    private Socket socket;

    private StreamDataSource streamAudioSource;
    private FrontEnd frontend;

    private boolean inUtterance = false;


    /**
     * Constructs a default ClientFrontEndImpl.
     *
     * @param name the name of this ClientFrontEndImpl
     * @param context the context of this ClientFrontEndImpl
     *
     * @throws InstantiationException if there is an initialization error
     * @throws IOException if there is an I/O error
     */
    public void initialize(String name, String context)
        throws InstantiationException, IOException {
        SphinxProperties props = SphinxProperties.getSphinxProperties(context);

        streamAudioSource = new StreamDataSource();
        streamAudioSource.initialize("StreamDataSource", null, props, null);

        frontend = FrontEndFactory.getFrontEnd(props, "client");
        frontend.setDataSource(streamAudioSource);

        serverAddress = props.getString(PROP_SERVER, PROP_SERVER_DEFAULT);
        serverPort = props.getInt(PROP_PORT, PROP_PORT_DEFAULT);
    }


    /**
     * Connects this ClientFrontEndImpl to the back-end server.
     */
    public void connect() {
        try {
            socket = connectToBackEnd();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    /**
     * Closes the connection to the back-end server.
     */
    public void close() {
        try {
            closeConnection(socket);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    
    /**
     * Decodes the data in the given InputStream.
     *
     * @param is the InputStream to decode
     * @param streamName the name of the InputStream
     *
     * @return the result string
     */
    public String decode(InputStream is, String streamName) 
        throws DataProcessingException, IOException {
        sendRecognition();
        streamAudioSource.setInputStream(is, streamName);
        sendData();
        String result = readResult();
        return result;
    }


    /**
     * Connects this client to the server.
     */
    private Socket connectToBackEnd() throws IOException {
        Socket socket = new Socket(serverAddress, serverPort);
        reader = new BufferedReader
            (new InputStreamReader(socket.getInputStream()));
        dataWriter = new DataOutputStream
            (new BufferedOutputStream(socket.getOutputStream()));
        return socket;
    }


    /**
     * Sends a single int "1" to the back-end server to
     * signal the cepstra will be sent for recognition.
     */
    private void sendRecognition() throws IOException {
        dataWriter.writeInt(1);
        dataWriter.flush();
    }


    /**
     * Closes the connection to the back-end server.
     */
    private void closeConnection(Socket socket) throws IOException {
        reader.close();
        dataWriter.close();
        socket.close();
    }


    /**
     * Sends all the available cepstra to the back-end server.
     */
    private void sendData() throws DataProcessingException, IOException {
        Data cepstrum = null;
        do {
            cepstrum = frontend.getData();
            if (cepstrum != null) {
                if (!inUtterance) {
                    if (cepstrum instanceof DataStartSignal) {
                        inUtterance = true;
                        dataWriter.writeDouble(Double.MAX_VALUE);
                        dataWriter.flush();
                    } else {
                        throw new IllegalStateException
                            ("No DataStartSignal read");
                    }
                } else {
                    if (cepstrum instanceof DoubleData) {
                        // send the cepstrum data
                        double[] data = ((DoubleData) cepstrum).getValues();
                        for (int i = 0; i < data.length; i++) {
                            dataWriter.writeDouble(data[i]);
                        }
                    } else if (cepstrum instanceof DataEndSignal) {
                        // send a DataEndSignal
                        dataWriter.writeDouble(Double.MIN_VALUE);
                        inUtterance = false;
                    } else if (cepstrum instanceof DataStartSignal) {
                        throw new IllegalStateException
                            ("Too many DataStartSignals.");
                    }
                    dataWriter.flush();
                }
            }
        } while (cepstrum != null);
    }


    /**
     * Reads the result string from the back-end server.
     *
     * @return the result string
     */
    private String readResult() throws IOException {
        return reader.readLine();
    }

}

