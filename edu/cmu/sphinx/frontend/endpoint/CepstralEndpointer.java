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
 * An interface for Cepstrum based endpointers.
 */
public interface CepstralEndpointer extends CepstrumSource {

    /**
     * Initializes an CepstralEndpointer with the given name, context,
     * and CepstrumSource predecessor.
     *
     * @param name the name of this CepstralEndpointer
     * @param context the context of the SphinxProperties this
     *    CepstralEndpointer uses
     * @param props the SphinxProperties to read properties from
     * @param predecessor the CepstrumSource where this CepstralEndpointer
     *    gets Cepstrum from
     *
     * @throws java.io.IOException
     */
    public void initialize(String name, String context, 
                           SphinxProperties props,
                           CepstrumSource predecessor) throws IOException;
}
