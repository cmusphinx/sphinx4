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

import java.io.Serializable;

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
public class Data implements Serializable, Cloneable {

    private long firstSampleNumber;
    private long collectTime;
    private Signal signal = null;
    private Utterance utterance = null;


    /**
     * Constructs a default Data object. Calling <code>getSignal()</code>
     * on this Data will return <code>Signal.CONTENT</code>.
     *
     * @param collectTime the time at which this data is collected
     * @param firstSampleNumber the position of the first sample in the
     *    original data
     */
    protected Data(long collectTime, long firstSampleNumber) {
        this(Signal.CONTENT, collectTime, firstSampleNumber);
    }
    

    /**
     * Constructs a Data object with the given Signal.
     *
     * @param signal the Signal of this Data object
     * @param collectTime the time at which this data is collected
     * @param firstSampleNumber the position of the first sample in the
     *    original data
     */
    protected Data(Signal signal, long collectTime, long firstSampleNumber) {
        this.signal = signal;
        this.collectTime = collectTime;
        this.firstSampleNumber = firstSampleNumber;
    }


    /**
     * Constructs a Data object with the given Utterance.
     *
     * @param collectTime the time at which this data is collected
     * @param firstSampleNumber the position of the first sample in the
     *    original data
     */
    protected Data(Utterance utterance, long collectTime, 
                   long firstSampleNumber) {
        this(collectTime, firstSampleNumber);
        this.utterance = utterance;
    }


    /**
     * Returns the Signal of this Data. Returns null if no Signal present.
     *
     * @return the Signal of this Data, or null if no Signal present
     */
    public Signal getSignal() {
        return signal;
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
     * Returns the Utterance associated with this Data object.
     *
     * @return the Utterance associated with this Data object,
     *     or null if there is no Utterance associated
     */
    public Utterance getUtterance() {
        return utterance;
    }


    /**
     * Returns true if this Data has content in it. Effectively it
     * checks whether <code>getSignal()</code> returns
     * <code>Signal.CONTENT</code>.
     *
     * Subclass of Data that are assumed to contain actual data by default.
     * Therefore, this will return <code>Signal.CONTENT</code> unless the
     * Data subclass constructor calls the <code>Data(Signal)</code>
     * constructor.
     *
     * @return true if it has content, false otherwise
     */
    public boolean hasContent() {
        return (getSignal().equals(Signal.CONTENT));
    }


    /**
     * Returns true if this Data has the given Signal
     *
     * @return true if this Data object has the given Signal,
     *    false otherwise
     */
    public boolean hasSignal(Signal signal) {
        if (getSignal() != null) {
            return (getSignal().equals(signal));
        } else {
            return false;
        }
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
