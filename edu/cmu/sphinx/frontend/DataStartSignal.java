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


package edu.cmu.sphinx.frontend;

/**
 * A signal that indicates the start of data.
 *
 * @see Data
 * @see DataProcessor
 * @see Signal
 */
public class DataStartSignal extends Signal {

    String name;
    String transcript;

    /**
     * Constructs a DataStartSignal.
     */
    public DataStartSignal() {
        this(System.currentTimeMillis());
    }

    /**
     * Constructs a DataStartSignal at the given time.
     *
     * @param time the time this DataStartSignal is created
     */
    public DataStartSignal(long time) {
        super(time);
    }

    /**
     * Constructs a DataStartSignal with the given data name and transcript.
     *
     * @param name   the name of the data stream, can be the name of the 
     *               file from which the data stream is created
     * @param transcript   the transcript text of the data stream
     */
    public DataStartSignal(String name, String transcript) {
        this();
        this.name = name;
        this.transcript = transcript;
    }

    /**
     * Returns the name of this data stream.
     *
     * @return the name of this data stream
     */
    public String getStreamName() {
        return name;
    }

    /**
     * Returns the transcript text.
     *
     * @return the transcript text
     */
    public String getTranscript() {
        return transcript;
    }
}
