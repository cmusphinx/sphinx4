
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

/**
 * Used to transfer data from the trainer to the acoustic model
 */
public class TrainerScore {
    private Feature feature;
    private float probability;
    private int senoneID;

    /**
     * Creates a new buffer
     *
     * @param feature the current feature
     * @param probability the score for the current frame
     * @param senone the id for the current senone
     */
    public TrainerScore(Feature feature, float probability, int senone) {
	this.feature = feature;
	this.probability = probability;
	this.senoneID = senone;
    }

    /**
     * Retrieves the Feature.
     *
     * @return the Feature
     */
    public Feature getFeature() {
	return feature;
    }

    /**
     * Retrieves the probability.
     *
     * @return the probability
     */
    public float getScore() {
	return probability;
    }

    /**
     * Retrieves the senone ID.
     *
     * @return the senone ID
     */
    public int getSenoneID() {
	return senoneID;
    }
}

