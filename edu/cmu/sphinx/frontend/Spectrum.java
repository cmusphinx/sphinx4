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

import edu.cmu.sphinx.frontend.util.Util;

/**
 * Represents the power spectrum.
 */
public class Spectrum extends Data {

    private double[] spectraData;


    /**
     * Constructs a Spectrum with the given spectra data.
     *
     * @param spectraData a frame's spectral data
     * @param collectTime the time at which the original audio (from
     *    which this spectrum is obtained) is collected
     * @param firstSampleNumber the position of the first sample in the
     *    original data
     */
    public Spectrum(double[] spectraData, long collectTime, 
                    long firstSampleNumber) {
	super(collectTime, firstSampleNumber);
        this.spectraData = spectraData;
    }


    /**
     * Constructs a Spectrum with the given spectra data and utterance.
     *
     * @param spectraData a frame's spectral data
     * @param utterance the Utterance associated with this Spectrum
     * @param collectTime the time at which the original audio (from
     *    which this spectrum is obtained) is collected
     * @param firstSampleNumber the position of the first sample in the
     *    original data
     */
    public Spectrum(double[] spectraData, Utterance utterance, 
                    long collectTime, long firstSampleNumber) {
        super(utterance, collectTime, firstSampleNumber);
	this.spectraData = spectraData;
    }


    /**
     * Constructs a Spectrum with the given Signal.
     *
     * @param signal the Signal this Spectrum carries
     * @param collectTime the time of this Spectrum object
     * @param firstSampleNumber the position of the first sample in the
     *    original data
     */
    public Spectrum(Signal signal, long collectTime, long firstSampleNumber) {
        super(signal, collectTime, firstSampleNumber);
    }


    /**
     * Returns the spectrum data.
     *
     * @return the spectrum data
     */
    public double[] getSpectrumData() {
	return spectraData;
    }

    
    /**
     * Returns a string representation of this Spectrum.
     * The format of the string is:
     * <pre>spectrumLength data0 data1 ...</pre>
     *
     * @return a string representation of this Spectrum
     */ 
    public String toString() {
	if (spectraData != null) {
	    return ("Spectrum: " + Util.doubleArrayToString(spectraData));
	} else {
	    return ("Spectrum: " + getSignal().toString());
	}
    }                
}
