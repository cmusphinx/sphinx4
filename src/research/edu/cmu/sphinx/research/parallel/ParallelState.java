
/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.research.parallel;

import edu.cmu.sphinx.decoder.search.TokenStack;


/**
 * A SentenceHMMState in a parallel branch.
 */
public interface ParallelState {

    /**
     * Returns the FeatureStream of the acoustic model behind this 
     * ParallelHMMStateState.
     *
     * @return the FeatureStream of the acoustic model
     */
    public FeatureStream getFeatureStream();

    /**
     * Returns the token stack of this ParallelUnitState.
     *
     * @return the token stack
     */
    public TokenStack getTokenStack();
}


