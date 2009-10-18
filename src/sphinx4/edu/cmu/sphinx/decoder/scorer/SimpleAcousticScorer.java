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

package edu.cmu.sphinx.decoder.scorer;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.BaseDataProcessor;

import java.util.List;
import java.util.logging.Logger;


/**
 * A Simple acoustic scorer which scores within the current thread.
 * <p/>
 * Note that all scores are maintained in LogMath log base.
 */
public class SimpleAcousticScorer extends AbstractScorer {

    /**
     * @param frontEnd the frontend to retrieve features from for scoring
     * @param scoreNormalizer optional post-processor for computed scores that will normalize scores. If not set, no normalization will
     * applied and the token scores will be returned unchanged.
     */
    public SimpleAcousticScorer(BaseDataProcessor frontEnd, ScoreNormalizer scoreNormalizer) {
        super(frontEnd, scoreNormalizer);
    }

    public SimpleAcousticScorer() {       
    }

    @Override
    protected Data doScoring(List<? extends Scoreable> scoreableList, Data data) {
        Scoreable best;
        best = scoreableList.get(0);

        for (Scoreable scoreable : scoreableList) {
            if (scoreable.calculateScore(data) >
                    best.getScore()) {
                best = scoreable;
            }
        }

        return best;
    }
}
