
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

import edu.cmu.sphinx.decoder.search.Token;

import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;

import java.util.Comparator;

/**
 * Prunes an ActiveList of ParallelTokens based on their FeatureScore.
 * Pruning by this Pruner simply means performing a
 * <code>Token.setPruned(true)</code>.
 */
public class FeatureScorePruner extends TokenScorePruner {

    private static final String PROP_PREFIX = 
    "edu.cmu.sphinx.research.parallel.FeatureScorePruner.";

    private static final String PROP_ABSOLUTE_BEAM_WIDTH =
    PROP_PREFIX + "absoluteBeamWidth";

    private static final int PROP_ABSOLUTE_BEAM_WIDTH_DEFAULT = 2000;

    private static final String PROP_RELATIVE_BEAM_WIDTH =
    PROP_PREFIX + "relativeBeamWidth";

    private static final double PROP_RELATIVE_BEAM_WIDTH_DEFAULT = 0;

    private static Comparator tokenComparator = null;

   
    /**
     * Initializes this Pruner with the given context.
     *
     * @param context the context to use
     */
    public void initialize(String context) {
        SphinxProperties props = SphinxProperties.getSphinxProperties(context);
        setAbsoluteBeamWidth(props.getInt(PROP_ABSOLUTE_BEAM_WIDTH,
                                          PROP_ABSOLUTE_BEAM_WIDTH_DEFAULT));
        LogMath logMath = LogMath.getLogMath(context);
        double linearRelativeBeamWidth =
            props.getDouble(PROP_RELATIVE_BEAM_WIDTH, 
                            PROP_RELATIVE_BEAM_WIDTH_DEFAULT);
        setRelativeBeamWidth
            ((float) logMath.linearToLog(linearRelativeBeamWidth));
    }


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


