/*
 * Copyright 2013 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */

package edu.cmu.sphinx.api;

import java.io.IOException;
import java.io.InputStream;

import edu.cmu.sphinx.util.TimeFrame;

/**
 * Speech recognizer that works with audio resources.
 *
 * @see LiveSpeechRecognizer live speech recognizer
 */
public class StreamSpeechRecognizer extends AbstractSpeechRecognizer {

    /**
     * Constructs new stream recognizer.
     *
     * @param configuration configuration
     * @throws IOException error occured during model load
     */
    public StreamSpeechRecognizer(Configuration configuration)
        throws IOException
    {
        super(configuration);
    }

    public void startRecognition(InputStream stream) {
        startRecognition(stream, TimeFrame.INFINITE);
    }

    /**
     * Starts recognition process.
     *
     * Starts recognition process and optionally clears previous data.
     *
     * @param stream input stream to process
     * @param timeFrame time range of the stream to process
     * @see StreamSpeechRecognizer#stopRecognition()
     */
    public void startRecognition(InputStream stream, TimeFrame timeFrame) {
        recognizer.allocate();
        context.setSpeechSource(stream, timeFrame);
    }

    /**
     * Stops recognition process.
     *
     * Recognition process is paused until the next call to startRecognition.
     *
     * @see StreamSpeechRecognizer#startRecognition(InputStream, TimeFrame)
     */
    public void stopRecognition() {
        recognizer.deallocate();
    }
}
