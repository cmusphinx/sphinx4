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

package edu.cmu.sphinx.decoder;

import edu.cmu.sphinx.frontend.Feature;

/**
 *  The listener interface for being informed when a non-content
 *  feature is generated
 */
public interface FeatureListener {
    /**
     * Method called when a non-content feature is detected
     *
     * @param feature the non-content feature
     *
     */
     public void featureOccurred(Feature feature);
}

