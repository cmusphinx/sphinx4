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
 * Indicates events like beginning/end of audio utterance, data dropped,
 * quality changed, etc..
 */
public class Signal implements Serializable {

    private String name = null;

    /**
     * Signal to indicate data present.
     */
    public static final Signal CONTENT = new Signal("CONTENT");

    /**
     * Signal to indicate the end of a speech utterance.
     */
    public static final Signal UTTERANCE_END = new Signal("UTTERANCE_END");

    /**
     * Signal to indicate the start of a speech utterance.
     */
    public static final Signal UTTERANCE_START = new Signal("UTTERANCE_START");

    /**
     * Signal to indicate the start of speech.
     */
    public static final Signal SPEECH_START = new Signal("SPEECH_START");

    /**
     * Signal to indicate the end of speech.
     */
    public static final Signal SPEECH_END = new Signal("SPEECH_END");


    /**
     * Constructs a Signal with the given name.
     */
    protected Signal(String name) {
        this.name = name;
    }


    /**
     * Returns true if the given Signal is equal to this Signal.
     *
     * @param signal the Signal to compare
     *
     * @return true if they are the same, false otherwise
     */
    public boolean equals(Signal signal) {
        if (this == signal) {
            return true;
        } else if (signal != null) {
            return toString().equals(signal.toString());
        } else {
            return false;
        }
    }


    /**
     * Returns a hash code value for this Signal.
     *
     * @return a hash code value for this Signal.
     */
    public int hashCode() {
        return toString().hashCode();
    }


    /**
     * Returns the name of this Signal.
     *
     * @return the name of this Signal.
     */
    public String toString() {
        return name;
    }
}
