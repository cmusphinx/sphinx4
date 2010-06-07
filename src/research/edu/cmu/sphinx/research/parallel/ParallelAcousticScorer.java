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

import edu.cmu.sphinx.decoder.scorer.SimpleAcousticScorer;
import edu.cmu.sphinx.decoder.scorer.Scoreable;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

import java.util.List;

/** A parallel acoustic scorer that is capable of scoring multiple feature streams. */
public class ParallelAcousticScorer extends SimpleAcousticScorer {

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
    }

    /**
     * Scores the given set of Tokens. All Tokens in the given list are assumed to belong to the same acoustic model.
     *
     * @param scoreableList a list containing StateToken objects to be scored
     * @return the best scoring scoreable, or null if there are no more frames to score
     */
    @Override
    public Data calculateScores(List<? extends Scoreable> scoreableList) {
        frontEnd = getFrontEnd(scoreableList);
        return super.calculateScores(scoreableList);
    }

    /**
     * Returns the acoustic model name of the Tokens in the given list .
     *
     * @return the acoustic model name of the Tokens
     */
    private FrontEnd getFrontEnd(List<? extends Scoreable> activeList) {
       Scoreable scoreable =  activeList.isEmpty() ? null : activeList.get(0);
       if (scoreable == null || !(scoreable instanceof ParallelToken))
    	   throw new RuntimeException("Scorer doesn't support anything except list of ParallelTokens");
       return ((ParallelToken)scoreable).getFeatureStream().getFrontEnd();
    }
}
