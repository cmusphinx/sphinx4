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
 * Implements the base class for all Data objects that go between
 * DataSources. Subclass of Data contain will contain the actual
 * data. A Data object can also contain a Signal.
 * 
 * <p>Data can be event signals or just data to be processed, and they go
 * through the processors in the front-end pipeline. Data can be
 * audio, preemphasized audio data, cepstra, etc.. Signals
 * can be used to indicate events like beginning/end of audio
 * segment, data dropped, quality changed, etc..
 *
 * <p><b>IMPORTANT:</b>
 * Subclass of Data that are assumed to contain actual data by default.
 * Therefore, calling <code>Data.getSignal()</code> will return
 * <code>Signal.CONTENT</code>. The programmer must override this behavior
 * with the constructor.
 *
 * @see Signal
 */
public class FloatData implements Data, Cloneable {

    private float[] values;
    private long firstSampleNumber;
    private long collectTime;


    /**
     * Constructs a Data object with the given values, collect time,
     * and first sample number.
     *
     * @param values the data values
     * @param collectTime the time at which this data is collected
     * @param firstSampleNumber the position of the first sample in the
     *                          original data
     */
    public FloatData(float[] values,
                     long collectTime, long firstSampleNumber) {
        this.values = values;
        this.collectTime = collectTime;
        this.firstSampleNumber = firstSampleNumber;
    }

    /**
     * Returns the values of this DoubleData object.
     *
     * @return the values
     */
    public float[] getValues() {
        return values;
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
