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
 * A Data object that holds data of primitive type double.
 */
public class DoubleData implements Data, Cloneable {

    private double[] values;
    private int sampleRate;
    private long firstSampleNumber;
    private long collectTime;


    /**
     * Constructs a Data object with the given values, collect time,
     * and first sample number.
     *
     * @param values the data values
     * @param sampleRate the sample rate of the data
     * @param collectTime the time at which this data is collected
     * @param firstSampleNumber the position of the first sample in the
     *                          original data
     */
    public DoubleData(double[] values, int sampleRate,
                      long collectTime, long firstSampleNumber) {
        this.values = values;
        this.sampleRate = sampleRate;
        this.collectTime = collectTime;
        this.firstSampleNumber = firstSampleNumber;
    }

    /**
     * Returns a string that describes this DoubleData.
     *
     * @return a string that describes this DoubleData
     */
    public String toString() {
        return ("DoubleData: " + sampleRate + "Hz, first sample #: " +
                firstSampleNumber + ", collect time: " + collectTime);
    }
        

    /**
     * Returns the values of this DoubleData object.
     *
     * @return the values
     */
    public double[] getValues() {
        return values;
    }


    /**
     * Returns the sample rate of the data.
     *
     * @return the sample rate of the data
     */
    public int getSampleRate() {
        return sampleRate;
    }

    
    /**
     * Returns the position of the first sample in the original data.
     * The very first sample number is zero.
     *
     * @return the position of the first sample in the original data
     */
    public long getFirstSampleNumber() {
        return firstSampleNumber;
    }


    /**
     * Returns the time in milliseconds at which the audio data is collected.
     *
     * @return the difference, in milliseconds, between the time the
     *    audio data is collected and midnight, January 1, 1970
     */
    public long getCollectTime() {
        return collectTime;
    }


    /**
     * Returns a clone of this Data object.
     *
     * @return a clone of this data object
     */
    public Object clone() {
	try {
	    Data data = (Data) super.clone();
	    return data;
	} catch (CloneNotSupportedException e) {
	    throw new InternalError(e.toString());
	}
    }
}
