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

    private Signal signal = null;
    private Utterance utterance = null;


    /**
     * Constructs a default Data object. Calling <code>getSignal()</code>
     * on this Data will return <code>Signal.CONTENT</code>.
     *
     * @param signal the Signal of this Data object
     */
    protected Data() {
        signal = Signal.CONTENT;
    }
    

    /**
     * Constructs a Data object with the given Signal.
     *
     * @param signal the Signal of this Data object
     */
    protected Data(Signal signal) {
        this.signal = signal;
    }


    /**
     * Constructs a Data object with the given Utterance.
     */
    protected Data(Utterance utterance) {
        this.utterance = utterance;
        this.signal = Signal.CONTENT;
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
        if (getSignal() == null) {
            return false;
        } else {
            return (getSignal().equals(signal));
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
