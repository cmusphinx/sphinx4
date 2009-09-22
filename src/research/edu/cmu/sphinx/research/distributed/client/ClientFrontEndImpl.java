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

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.util.props.*;

import java.io.*;
import java.net.Socket;

/**
 * A FrontEnd that runs on client applications. The main interface between the application and this ClientFrontEnd is
 * the <b>decode(InputStream inputStream, String streamName)</b> method, which returns the decoded result as a String.
 * <p/>
 * <p>The method <b>connect()</b> should be called before <b>decode()</b>, and the method <b>close()</b> should be
 * called after all decoding is done. Therefore, the correct sequence of calls is: <code> connect();
 * decode(inputstream1, name1); ... decode(inputstreamN, nameN); close(); </code>
 */
public class ClientFrontEndImpl implements Configurable{


    private BufferedReader reader;
    private DataOutputStream dataWriter;

    @S4String(defaultValue = "localhost")
    public static final String SERVER_ADRESS = "serverAdress";
    private String serverAddress;


    @S4Integer(defaultValue = 52703)
    public static final String SERVER_PORT = "serverPort";
    private int serverPort;

    private Socket socket;

    private StreamDataSource streamAudioSource;
    private FrontEnd frontend;

    private boolean inUtterance;



    public void newProperties(PropertySheet ps) throws PropertyException {
          streamAudioSource = new StreamDataSource();
        streamAudioSource.initialize();
//        streamAudioSource.initialize("StreamDataSource", null, props, null);

//        frontend = FrontEndFactory.getFrontEnd(props, "client");
        frontend.setDataSource(streamAudioSource);

        serverAddress = ps.getString(SERVER_ADRESS);
        serverPort = ps.getInt(SERVER_PORT);
    }


    /** Connects this ClientFrontEndImpl to the back-end server. */
    public void connect() {
        try {
            socket = connectToBackEnd();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    /** Closes the connection to the back-end server. */
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
     * @param is         the InputStream to decode
     * @param streamName the name of the InputStream
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


    /** Connects this client to the server. */
    private Socket connectToBackEnd() throws IOException {
        Socket socket = new Socket(serverAddress, serverPort);
        reader = new BufferedReader
                (new InputStreamReader(socket.getInputStream()));
        dataWriter = new DataOutputStream
                (new BufferedOutputStream(socket.getOutputStream()));
        return socket;
    }


    /** Sends a single int "1" to the back-end server to signal the cepstra will be sent for recognition. */
    private void sendRecognition() throws IOException {
        dataWriter.writeInt(1);
        dataWriter.flush();
    }


    /** Closes the connection to the back-end server. */
    private void closeConnection(Socket socket) throws IOException {
        reader.close();
        dataWriter.close();
        socket.close();
    }


    /** Sends all the available cepstra to the back-end server. */
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
                        for (double val : data) {
                            dataWriter.writeDouble(val);
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

