/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.research.distributed.server;

import edu.cmu.sphinx.decoder.Decoder;

import edu.cmu.sphinx.result.Result;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.net.Socket;
import java.net.URL;


public class ServerDecoder extends BaseServer {

    private Decoder decoder;

    
    /**
     * Constructs a default ServerDecoder.
     *
     * @param context the context of this ServerDecoder
     */
    public ServerDecoder(String context) throws IOException {
        initDecoder(context);
    }


    /**
     * Initialize the decoder with the given context.
     *
     * @param context the context to use
     */
    public void initDecoder(String context) throws IOException {
        // using null as the DataSource will still initialize the decoder
        decoder = new Decoder(context, null);
    }


    /**
     * This method is called after a connection request is made to this
     * BaseServer. The <code>Socket</code> created as a result of the
     * connection request is passed to this method.
     *
     * @param socket the socket
     */
    protected void spawnProtocolHandler(Socket socket) {
        RecognitionHandler handler = new RecognitionHandler(socket);
        (new Thread(handler)).start();
    }


    /**
     * Handles recognition requests from sockets.
     */
    class RecognitionHandler implements Runnable {
        
        // the Socket to communicate with
        private Socket socket;

        private BufferedReader reader;
        private PrintWriter writer;
        

        /**
         * Constructs a default RecognitionHandler with the given Socket.
         *
         * @param socket the Socket from which recognition data comes in
         */
        public RecognitionHandler(Socket socket) {
            setSocket(socket);
        }


        /**
         * Sets the Socket to be used by this ProtocolHandler.
         * It also sets the FrontEnd to read from this Socket.
         *
         * @param socket the Socket to be used
         */
        private void setSocket(Socket socket) {
            this.socket = socket;
            if (socket != null) {
                try {
                    reader = new BufferedReader
                        (new InputStreamReader(socket.getInputStream()));
                    writer = new PrintWriter(socket.getOutputStream(), true);

                    decoder.getRecognizer().getFrontEnd().setDataSource
                        (new SocketCepstrumSource(socket));

                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    System.out.println
                        ("Socket reader/writer not instantiated");
                    throw new Error();
                }
            }
        }


        /**
         * Sends the given line of text over the Socket.
         *
         * @param line the line of text to send
         */
        private void sendLine(String line) {
            writer.print(line);
            writer.print('\n');
            writer.flush();
        }
        

        /**
         * Handles the recognition request.
         */
        public void run() {
            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    if (line.equals("RECOGNITION")) {
                        Result result = decoder.decode();
                        String resultString = result.getBestResultNoSilences();
                        sendLine(resultString);
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }


    /**
     * Starts this ServerDecoder.
     *
     * @param argv argv[0] : SphinxProperties file
     */
    public static void main(String[] argv) {

        if (argv.length < 1) {
            System.out.println
                ("Usage: ServerDecoder propertiesFile");
            System.exit(1);
        }

        String context = "ServerDecoder";
        String propertiesFile = argv[0];
        String pwd = System.getProperty("user.dir");

        try {
            SphinxProperties.initContext
                (context, new URL("file://" + pwd +  "/" + propertiesFile));
            ServerDecoder decoder = new ServerDecoder(context);
            (new Thread(decoder)).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
