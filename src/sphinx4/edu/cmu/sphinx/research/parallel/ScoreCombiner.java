
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


/**
 * Combines the various feature stream scores in a CombineToken.
 */
public interface ScoreCombiner {

    /**
     * Combines the scores from all the feature stream in the given
     * CombineToken. The feature stream(s) are represented in the form
     * of ParallelTokens in the given CombineToken.
     *
     * @param token the CombineToken on which to combine the feature
     *   stream scores
     */
    public void combineScore(CombineToken token);
}


