
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

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import java.util.Iterator;


/**
 * Combines the various unit feature stream scores in a CombineToken.
 * Each feature stream is represented as a 
 * {@link ParallelToken ParallelToken} in a CombineToken. Suppose
 * that the CombineToken has two ParallelTokens, P1 and P2, then the
 * combined score is given by:
 * <pre>
 * P1.getFeatureScore() * P1.getEta() + P2.getFeatureScore() * P2.getEta()
 * </pre>
 * where the feature scores are in LogMath logbase, and the method
 * <code>getEta()</code> returns the eta value of that particular
 * feature stream.
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
