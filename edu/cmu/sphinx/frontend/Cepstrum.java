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
 * Represents a Cepstrum.
 */
public class Cepstrum extends Data {

    private float[] cepstrumData;


    /**
     * Constructs a Cepstrum with the given cepstrum data.
     *
     * @param cepstrumData the cepstrum data
     */
    public Cepstrum(float[] cepstrumData) {
        super(Signal.CONTENT);
	this.cepstrumData = cepstrumData;
    }


    /**
     * Constructs a Cepstrum with the given cepstrum data and Utterance.
     *
     * @param cepstrumData the cepstrum data
     * @param utterance the Utterance associated with this Cepstrum
     */
    public Cepstrum(float[] cepstrumData, Utterance utterance) {
        super(utterance);
        this.cepstrumData = cepstrumData;
    }


    /**
     * Constructs a Cepstrum with the given Signal.
     *
     * @param signal the Signal this Cepstrum carries
     */
    public Cepstrum(Signal signal) {
        super(signal);
        cepstrumData = null;
    }


    /**
     * Returns the cepstrum data.
     *
     * @return the cepstrum data
     */
    public float[] getCepstrumData() {
	return cepstrumData;
    }


    /**
     * Returns the energy value of this Cepstrum.
     *
     * @return the energy value of this Cepstrum or zero if
     *    this Cepstrum has no data
     */
    public float getEnergy() {
        if (cepstrumData != null && cepstrumData.length > 0) {
            return cepstrumData[0];
        } else {
            return 0.0f;
        }
    }


    /**
     * Returns a string representation of this Cepstrum.
     * The format of the string is:
     * <pre>cepstrumLength data0 data1 ...</pre>
     *
     * @return a string representation of this Cepstrum
     */
    public String toString() {
        return (Util.floatArrayToString(cepstrumData));
    }
}
