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
 * Represents an array of Features.
 */
public class FeatureFrame {

    private Feature[] features = null;

    /**
     * Constructs a FeatureFrame with the given array of Features.
     *
     * @param features the Feature array
     */
    public FeatureFrame(Feature[] features) {
	this.features = features;
    }


    /**
     * Returns the array of Features.
     *
     * @return the array of Features
     */
    public Feature[] getFeatures() {
	return features;
    }


    /**
     * Returns a String representation of this FeatureFrame.
     *
     * @return the String representation
     */
    public String toString() {
        String result = "FEATURE_FRAME ";
        if (features != null) {
            result += features.length;
            for (int i = 0; i < features.length; i++) {
                result += ("\nFEATURE " + features[i].toString());
            }
        } else {
            result += "0";
        }
        return result;
    }
}
