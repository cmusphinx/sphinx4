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

import java.io.IOException;


/**
 * A bank of spectral filters.
 */
public interface Filterbank extends SpectrumSource {

    /**
     * Initializes this Filterbank.
     *
     * @param context the context to use
     */
    public void initialize(String name, String context, 
			   SpectrumSource predecessor) throws IOException;
}
