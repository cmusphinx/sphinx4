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

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;


/**
 * A bank of spectral filters.
 */
public interface Filterbank extends SpectrumSource {

    /**
     * Initializes this Filterbank.
     *
     * @param name the name of this Filterbank
     * @param context the context
     * @param props the SphinxProperties to read properties from
     * @param predecessor the SpectrumSource from which Spectra to filter
     *    are obtained
     *
     * @throws IOException if an I/O error occurs
     */
    public void initialize(String name, String context,
			   SphinxProperties props,
			   SpectrumSource predecessor) throws IOException;
}
