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


package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumSource;

import java.io.IOException;


/**
 * An CepstrumSource object that acts as a stub between the real
 * CepstrumSource and the first processor in the front end. It is 
 * there so that changing the real CepstrumSource will not require
 * resetting the CepstrumSource of the first processor.
 *
 * A StubAudioSource is constructed using the real AudioSource, and
 * calling <code>StubAudioSource.getAudio()</code> simply returns
 * <code>realAudioSource.getAudio()</code>.
 * The real AudioSource can be changed by the method 
 * <code>setAudioSource()</code>.
 */
public class StubCepstrumSource implements CepstrumSource {
    
    private CepstrumSource realSource;

    /**
     * Constructs a StubCepstrumSource with no real CepstrumSource.
     */
    public StubCepstrumSource() {};
    
    /**
     * Constructs a StubCepstrumSource with the given CepstrumSource.
     *
     * @param cepstrumSource the real CepstrumSource
     */
    public StubCepstrumSource(CepstrumSource cepstrumSource) {
        this.realSource = cepstrumSource;
    }
    
    /**
     * Returns the next Cepstrum object produced by this CepstrumSource.
     *
     * @return the next available Cepstrum object, returns null if no
     *     Cepstrum object is available
     *
     * @throws java.io.IOException
     */
    public Cepstrum getCepstrum() throws IOException {
        return realSource.getCepstrum();
    }
    
    /**
     * Sets the real CepstrumSource.
     *
     * @return the real CepstrumSource
     */
    public void setCepstrumSource(CepstrumSource newCepstrumSource) {
            this.realSource = newCepstrumSource;
    }

    /**
     * Returns a string of the class name for the real cepstrum source.
     *
     * @return a string of the class name for the real cepstrum source.
     */
    public String toString() {
        return "Stub for: " + realSource.getClass().getName();
    }
}



