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

import edu.cmu.sphinx.frontend.CepstrumSource;

import java.io.IOException;


/**
 * Extracts Features from Cepstra.
 */
public interface FeatureExtractor extends FeatureSource {

    /**
     * Property prefix for the FeatureExtractor.
     */
    public static final String PROP_PREFIX = 
	FrontEnd.PROP_PREFIX + "featureExtractor.";
    
    /**
     * The SphinxProperty name for the length of a Feature.
     */
    public static final String PROP_FEATURE_LENGTH =
	PROP_PREFIX + "featureLength";

    /**
     * Initializes this FeatureExtractor with the appropriate name,
     * context and CepstrumSource.
     *
     * @throws java.io.IOException
     */
    public void initialize(String name, String context, 
			   CepstrumSource predecessor);
}
