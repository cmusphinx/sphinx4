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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;

import edu.cmu.sphinx.util.SphinxProperties;


/**
 * Reads cepstral data from a Socket, and returns them as Data
 * objects.
 */
public class SocketDataSource extends BaseDataProcessor {

    private DataInputStream dataReader;

    private static final double DATA_START = Float.MAX_VALUE;
    private static final double DATA_END = Float.MIN_VALUE;
    
    private int cepstrumLength = 13;
    
    // BUG: should use window size and shift to calculate sample number
    private long firstSampleNumber = -1;
    private int sampleRate = 16;

    private boolean inUtterance;


    /**
     * Constructs a default SocketDataSource from a Socket.
     *
     * @param socket the Socket from which cepstra data is read
     *
     * @throws IOException if an I/O error occurs
     */
    public SocketDataSource(Socket socket) throws IOException {
        inUtterance = false;
        this.dataReader = new DataInputStream
            (new BufferedInputStream(socket.getInputStream()));
    }

    /**
     * Initializes this DataProcessor.
     *
     * @param name         the name of this DataProcessor
     * @param frontEndName the name of the front-end pipeline this
     *                     DataProcessor is in
     * @param props        the SphinxProperties to use
     * @param predecessor  the predecessor of this DataProcessor
     */
    public void initialize(String name, String frontEndName,
                           SphinxProperties props,
                           DataProcessor predecessor) {
    }

    /**
     * Returns the next Data object produced by this SocketDataSource.
     * The Cepstra objects of an Utterance should be preceded by
     * a Data object with Signal.DATA_START and ended by
     * a Data object with Signal.DATA_END.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     *
     * @throws DataProcessingException if a data processing error occurs
     */
    public Data getData() throws DataProcessingException {
        try {
            double firstValue = dataReader.readDouble();
            if (!inUtterance) {
                if (firstValue == DATA_START) {
                    inUtterance = true;
                    return (new DataStartSignal());
                } else {
                    throw new IllegalStateException
                        ("No DATA_START read from socket: " + firstValue +
                         ", while DATA_START is " + DATA_START);
                }
            } else {
                if (firstValue == DATA_END) {
                    inUtterance = false;

                    // BUG : should calculate the duration using frame size
                    //       and frame shift
                    return (new DataEndSignal(-1));

                } else if (firstValue == DATA_START) {
                    throw new IllegalStateException("Too many DATA_STARTs.");
                } else {
                    double[] data = new double[cepstrumLength];
                    data[0] = firstValue;
                    long timeStamp = System.currentTimeMillis();
                    
                    for (int i = 1; i < cepstrumLength; i++) {
                        data[i] = dataReader.readDouble();
                    }
                    return (new DoubleData
                            (data, sampleRate, timeStamp, firstSampleNumber));
                }
            }
        } catch (IOException ioe) {
            throw new DataProcessingException
                ("Error reading from the network.");
        }
    }

}
