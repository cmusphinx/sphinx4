
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

package edu.cmu.sphinx.research.parallel;

import edu.cmu.sphinx.decoder.search.ActiveList;

/**
 * Implements a FeatureStream used in parallel decoding.
 */
public class FeatureStream {

    /**
     * Name of the FeatureStream.
     */
    String name;

    /**
     * Eta value assigned to the FeatureStream.
     */
    float eta;

    /**
     * Token ActiveList for this FeatureStream.
     */
    ActiveList activeList;

    /**
     * Constructs a FeatureStream with the given name, eta value,
     * and ActiveList.
     *
     * @param name the name of the FeatureStream
     * @param eta the eta value of the FeatureStream
     */
    FeatureStream(String name, float eta) {
        this.name = name;
        this.eta = eta;
    }
}        
