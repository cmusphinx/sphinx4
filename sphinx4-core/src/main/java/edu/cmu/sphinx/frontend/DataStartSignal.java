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


package edu.cmu.sphinx.frontend;

/**
 * A signal that indicates the start of data.
 *
 * @see Data
 * @see DataProcessor
 * @see Signal
 */
public class DataStartSignal extends Signal {

    private final int sampleRate;

    /**
     * Constructs a DataStartSignal at the given time.
     *
     * @param sampleRate the sampling rate of the started data stream.
     * @param time       the time this DataStartSignal is created
     */
    public DataStartSignal(int sampleRate, long time) {
        super(time);
        this.sampleRate = sampleRate;
    }

    /**
     * Constructs a DataStartSignal at the given time.
     *
     * @param sampleRate  the sampling rate of the started data stream.
     */
    public DataStartSignal(int sampleRate) {
        this(sampleRate, System.currentTimeMillis());
    }

    /**
     * Returns the string "DataStartSignal".
     *
     * @return the string "DataStartSignal"
     */
    @Override
    public String toString() {
        return "DataStartSignal: creation time: " + getTime();
    }


    /** @return the sampling rate of the started data stream. */
    public int getSampleRate() {
        return sampleRate;
    }
}
