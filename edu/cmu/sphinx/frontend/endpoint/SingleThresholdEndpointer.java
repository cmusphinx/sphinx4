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


package edu.cmu.sphinx.frontend.endpoint;

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumSource;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.Signal;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;

import java.util.*;


/**
 * Implements an simple single energy threshold endpointer.
 *
 */
public class SingleThresholdEndpointer extends DataProcessor implements Endpointer {


    private static final String PROP_PREFIX = 
    "edu.cmu.sphinx.frontend.endpoint.SingleThresholdEndpointer.";


    /**
     * The SphinxProperty for the energy level which is a lower bound
     * threshold at the energy is considered as speech.
     */
    public static final String PROP_ENERGY_THRESHOLD = 
        PROP_PREFIX + "energyThreshold";


    /**
     * The default value for PROP_START_LOW.
     */
    public static final float PROP_ENERGY_THRESHOLD_DEFAULT = 0.0f;
        

    private CepstrumSource predecessor;  // where to pull Cepstra from
    private float energyThreshold;


    /**
     * Constructs an SingleThresholdEndpointer with the given name, context,
     * and CepstrumSource predecessor.
     *
     * @param name the name of this SingleThresholdEndpointer
     * @param context the context of the SphinxProperties this
     *    SingleThresholdEndpointer uses
     * @param props the SphinxProperties to read properties from
     * @param predecessor the CepstrumSource where this 
     *    SingleThresholdEndpointer gets Cepstrum from
     *
     * @throws java.io.IOException
     */
    public void initialize(String name, String context, 
                           SphinxProperties props,
                           CepstrumSource predecessor) throws IOException {
        super.initialize(name, context, props);
        setProperties();
        this.predecessor = predecessor;
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void setProperties() {
        SphinxProperties properties = getSphinxProperties();
        energyThreshold
            = properties.getFloat(PROP_ENERGY_THRESHOLD,
                                  PROP_ENERGY_THRESHOLD_DEFAULT);
    }


    /**
     * Returns the next Cepstrum, which can be either Cepstrum with
     * data, or Cepstrum with a SPEECH_START or SPEECH_END signal.
     *
     * @return the next Cepstrum, or null if no Cepstrum is available
     *
     * @throws java.io.IOException if there is error reading the
     *    Cepstrum object
     *
     * @see Cepstrum
     */
    public Cepstrum getCepstrum() throws IOException {
       
        Cepstrum cepstrum = predecessor.getCepstrum();
        
        getTimer().start();
        
        while (cepstrum != null && cepstrum.hasContent() &&
               cepstrum.getEnergy() < energyThreshold) {
            cepstrum = predecessor.getCepstrum();
        }

        getTimer().stop();

        return cepstrum;
    }
}
