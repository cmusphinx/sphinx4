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
 * Represents the power spectrum.
 */
public class Spectrum extends Data {

    private double[] spectraData;


    /**
     * Constructs a Spectrum with the given spectra data.
     *
     * @param spectraData a frame's spectral data
     */
    public Spectrum(double[] spectraData) {
	this.spectraData = spectraData;
    }


    /**
     * Constructs a Spectrum with the given spectra data and utterance.
     *
     * @param spectraData a frame's spectral data
     * @param utterance the Utterance associated with this Spectrum
     */
    public Spectrum(double[] spectraData, Utterance utterance) {
        super(utterance);
	this.spectraData = spectraData;
    }


    /**
     * Constructs a Spectrum with the given Signal.
     *
     * @param signal the Signal this Spectrum carries
     */
    public Spectrum(Signal signal) {
        super(signal);
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
