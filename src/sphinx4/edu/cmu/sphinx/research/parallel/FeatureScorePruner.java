
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

import edu.cmu.sphinx.decoder.search.Token;

import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;

import java.util.Comparator;

/**
 * Prunes an ActiveList of ParallelTokens based on their FeatureScore.
 */
public class FeatureScorePruner extends TokenScorePruner {

    private static Comparator tokenComparator = null;

    /**
     * Returns the score that we use to compare this ParallelToken with
     * other ParallelTokens.
     *
     * @param token the ParallelToken to compare
     *
     * @return the score we use for comparison
     */
    protected float getTokenScore(Token token) {
	ParallelToken parallelToken = (ParallelToken) token;
        return parallelToken.getFeatureScore();
    }
}


