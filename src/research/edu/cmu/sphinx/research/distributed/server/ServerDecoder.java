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

package edu.cmu.sphinx.research.distributed.server;

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * A server version of the decoder. After it is being started, it waits for a socket connection from a client. Once it
 * receives a socket connection request, it spawns a new thread to handle that request. The client will then send it
 * features, which the server then decodes and returns a result.
 */
public class ServerDecoder extends BaseServer {

    private Recognizer recognizer;
    private FrontEnd frontend;


    /**
     * Constructs a default ServerDecoder.
     * @param recognizer The recognizer that does the job
     * @param frontend Frontend for input pipeline;
     */
    public ServerDecoder(Recognizer recognizer, FrontEnd frontend) {
        this.recognizer = recognizer;
        this.frontend = frontend;
    }


    /**
     * This method is called after a connection request is made to this BaseServer. The <code>Socket</code> created as a
     * result of the connection request is passed to this method.
     *
     * @param socket the socket
     */
    protected void spawnProtocolHandler(Socket socket) {
        RecognitionHandler handler = new RecognitionHandler(socket);
        (new Thread(handler)).start();
    }


    /** Handles recognition requests from sockets. */
    class RecognitionHandler implements Runnable {

        private DataInputStream reader;
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
         * Sets the Socket to be used by this ProtocolHandler. It also sets the FrontEnd to read from this Socket.
         *
         * @param socket the Socket to be used
         */
        private void setSocket(Socket socket) {
            if (socket != null) {
                try {
                    reader = new DataInputStream(socket.getInputStream());
                    writer = new PrintWriter(socket.getOutputStream(), true);

                    frontend.setDataSource(new SocketDataSource(socket));

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


        /** Handles the recognition request. */
        public void run() {
            try {
                // "1" from the client signals a recognition request
                while (reader.readInt() == 1) {
                	Result result = recognizer.recognize();
                    String resultString = result.getBestResultNoFiller();
                    sendLine(resultString);
                }
            } catch (EOFException eofe) {
                System.out.println("RecognitionHandler: EOF reached");
                return;
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
                    ("Usage: ServerDecoder <configurationXML>");
            System.exit(1);
        }
        try {
            assert false : "not implemented yet";
        	ConfigurationManager cm = new ConfigurationManager (argv[0]);
        	Recognizer recognizer = (Recognizer)cm.lookup("recognizer");
        	FrontEnd frontend = (FrontEnd)cm.lookup("frontend");
        	
            ServerDecoder decoder = new ServerDecoder(recognizer, frontend);
            (new Thread(decoder)).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
