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

package edu.cmu.sphinx.knowledge.acoustic;

import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.util.LogMath;
import java.io.Serializable;

/**
 * 
 * Represents a concrete implementation of a simple senone. A simple
 * senone is a set of probability density functions implemented  as a
 * gaussian mixture.
 *
 * All scores and weights are maintained in LogMath log base
 */
class GaussianMixture implements Senone, Serializable {
    // these data element in a senone may be shared with other senones
    // and therefore should not be written to.
    private float[] logMixtureWeights;			
    private MixtureComponent[] mixtureComponents;	

    transient volatile private Feature logLastFeatureScored;
    transient volatile private float logLastScore;

    private LogMath logMath;

    /**
     * Creates a new senone from the given components.
     *
     * @param logMath the log math 
     * @param logMixtureWeights the mixture weights for this senone in
     * LogMath log base
     * @param mixtureComponents the mixture components for this
     * senone
     */
    public GaussianMixture(LogMath logMath, float[] logMixtureWeights, 
    		MixtureComponent[] mixtureComponents) {

	assert mixtureComponents.length == logMixtureWeights.length;

	this.logMath = logMath;
	this.mixtureComponents = mixtureComponents;
	this.logMixtureWeights = logMixtureWeights;
    }


    /**
     * Dumps this senone.
     *
     * @param msg annotation message
     */
    public void dump(String msg) {
	System.out.println(msg + " GaussianMixture: " + logLastScore);
    }

    
    /**
     * Returns a score for the given feature based upon this senone,
     * and calculates it if not already calculated. Note that this
     * method is not thread safe and should be externally synchronized
     * if it could be potentially called from multiple threads.
     *
     * @param feature the feature to score
     *
     * @return the score, in logMath log base, for the feature
     */
    public float getScore(Feature feature)  {
	float logScore;
	
	if (feature == logLastFeatureScored) {
	    logScore = logLastScore;
	} else {
	    logScore = calculateScore(feature);
	    logLastScore = logScore;
	    logLastFeatureScored = feature;
	}
	return logScore;
    }

    /**
     * Calculates the score for the senone.
     *
     * @param feature the feature to score
     *
     * @return the score, in logMath log base, for the feature
     */

    public float calculateScore(Feature feature) {
	float logTotal = logMath.getLogZero();
	for (int i = 0; i < mixtureComponents.length; i++) {
	    // In linear form, this would be:
	    //
	    // Total += Mixture[i].score * MixtureWeight[i]
	    logTotal = logMath.addAsLinear(logTotal,
		 mixtureComponents[i].getScore(feature)+
		 logMixtureWeights[i]);
	}

	return logTotal;
    }

    /**
     * Returns the mixture components associated with this Gaussian
     *
     * @return the array of mixture components
     */
    public MixtureComponent[] getMixtureComponents() {
	return mixtureComponents;
    }
}
