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

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.FeatureExtractor;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.IDGenerator;

import java.io.IOException;

/**
 * Simply casts incoming Cepstrum objects into Feature objects,
 * without doing anything to the actual Cepstrum data.
 *
 * @see Cepstrum
 * @see Feature
 */
public class NullFeatureExtractor extends DataProcessor implements
FeatureExtractor {

    private CepstrumSource predecessor;
    private IDGenerator featureID;

    /**
     * Initializes this NullFeatureExtractor.
     *
     * @param name the name of this NullFeatureExtractor
     * @param context the context
     * @param props the SphinxProperties to use
     * @param predecessor the CepstrumSource to get Cepstrum from
     */
    public void initialize(String name, String context, SphinxProperties props,
			   CepstrumSource predecessor) {
        super.initialize(name, context, props);
        this.predecessor = predecessor;
	this.featureID = new IDGenerator();
    }

    /**
     * Returns the next Feature object produced by this NullFeatureExtractor.
     *
     * @return the next available Feature object, returns null if no
     *     Feature object is available
     *
     * @throws java.io.IOException if there is an error reading
     * the Feature objects
     *
     * @see Feature
     * @see FeatureFrame
     */
    public Feature getFeature() throws IOException {

	Cepstrum input = predecessor.getCepstrum();
	Feature output = null;

	if (input != null) {
	    if (input.hasContent()) {
		output = new Feature(input.getCepstrumData(),
				     featureID.getNextID(),
				     input.getUtterance(),
                                     input.getCollectTime());
	    } else if (input.getSignal() != null) {
		output = new Feature(input.getSignal(), featureID.NON_ID,
                                     input.getCollectTime());
	    }
	}

	return output;
    }
}

