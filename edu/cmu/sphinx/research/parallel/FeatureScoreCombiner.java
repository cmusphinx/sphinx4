
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

import java.util.Iterator;


/**
 * Combines the various unit feature stream scores in a CombineToken.
 *
 * All scores are maintained internally in the LogMath logbase
 */
public class FeatureScoreCombiner implements ScoreCombiner {

    /**
     * Combines the scores from all the feature stream in the given
     * CombineToken. The feature stream(s) are represented in the form
     * of ParallelTokens in the given CombineToken.
     *
     * @param token the CombineToken on which to combine the feature
     *   stream scores
     */
    public void combineScore(CombineToken token) {
        double logTotalScore = 0.0f;

        for (Iterator i = token.getTokenIterator(); i.hasNext(); ) {
            ParallelToken pToken = (ParallelToken) i.next();

            // in linear domain, the following expression is:
            // score = pToken.getFeatureScore()^pToken.getEta()
            
            double logScore = pToken.getFeatureScore() * pToken.getEta();
	    
            // in linear domain, the following expression is:
            // totalScore *= score
            
            logTotalScore += logScore;
        }

        token.setCombinedScore((float) logTotalScore);

	// set the combined score of all ParallelTokens in this CombineToken
        for (Iterator i = token.getTokenIterator(); i.hasNext();) {
            ParallelToken parallelToken = (ParallelToken) i.next();
            parallelToken.setCombinedScore((float) logTotalScore);
        }
    }
}
