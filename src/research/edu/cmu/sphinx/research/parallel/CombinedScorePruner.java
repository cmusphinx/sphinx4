
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

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import java.util.Comparator;

/**
 * Prunes an ActiveList of ParallelTokens based on their CombinedScore.
 */
public class CombinedScorePruner extends TokenScorePruner {

    private static Comparator tokenComparator = null;


    /**
     * Returns the score that we use to compare this CombineToken with
     * other CombineTokens.
     *
     * @param token the CombineToken to compare
     *
     * @return the score we use for comparison
     */
    protected float getTokenScore(Token token) {
	CombineToken combineToken = (CombineToken) token;
        return combineToken.getCombinedScore();
    }
}


