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

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.Data;

import java.util.List;


/**
 * A Simple acoustic scorer which scores within the current thread.
 * <p/>
 * Note that all scores are maintained in LogMath log base.
 */
public class SimpleAcousticScorer extends AbstractScorer {


    protected Scoreable doScoring(List<Token> scoreableList, Data data) {
        Scoreable best;
        best = scoreableList.get(0);

        for (Token scoreable : scoreableList) {
            //TODO: programmable gain
            if (scoreable.calculateScore(data, keepData, 1.0f) >
                    best.getScore()) {
                best = scoreable;
            }
        }

        return best;
    }
}
