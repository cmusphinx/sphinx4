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

package edu.cmu.sphinx.decoder.scorer;

import java.util.Iterator;
import java.io.IOException;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.linguist.HMMStateState;


/**
 * A Simple acoustic scorer.
 * a certain number of frames have been processed
 *
 * Note that all scores are maintained in LogMath log base.
 */
public class SimpleAcousticScorer implements AcousticScorer {

    private FrontEnd frontEnd;

    // TODO: Make this set with a SphinxProperty

    private boolean normalizeScores = false;


    /**
     * Initializes this SimpleAcousticScorer with the given
     * context and FrontEnd.
     *
     * @param context the context to use
     * @param frontend the FrontEnd to use
     */
    public void initialize(String context, FrontEnd frontend) {
	this.frontEnd = frontend;
    }


    /**
     * Starts the scorer
     */
    public void start() {
    }

    /**
     * Scores the given set of states
     *
     * @param stateTokenList a list containing StateToken objects to
     * be scored
     *
     * @return true if there are more features available
     */


    public boolean calculateScores(ActiveList stateTokenList) {

	FeatureFrame ff;

	try {
	    // TODO: fix the 'modelName', set to null
	    ff = frontEnd.getFeatureFrame(1, null);
	    Feature feature;
            

	    if (ff == null) {
		System.out.println("FeatureFrame is null");
		return false;
	    }


	    if (ff.getFeatures() == null) {
		System.out.println("features array is null ");
		return false;
	    }

	    feature = ff.getFeatures()[0];

	    if (feature.getSignal() == Signal.UTTERANCE_START) {
		return true; //calculateScores(stateTokenList);
	    }
	    if (feature.getSignal() == Signal.UTTERANCE_END) {
		return false;
	    }

            float[] logScores = new float[stateTokenList.size()];
            int which= 0;
            float logMaxScore = - Float.MAX_VALUE;
	    for (Iterator i = stateTokenList.iterator(); i.hasNext(); ) {
		Token token = (Token) i.next();
		edu.cmu.sphinx.decoder.linguist.HMMStateState hmmStateState = (edu.cmu.sphinx.decoder.linguist.HMMStateState)
		    token.getSentenceHMMState();
		if (feature.hasContent()) {
                    float logScore =  hmmStateState.getScore(feature);
                    if (logScore > logMaxScore) {
                        logMaxScore = logScore;
                    }
		    logScores[which++] = logScore;
		} else {
		    System.out.println("non-content feature " +
			feature + " id " + feature.getID());
		}
	    }
            which = 0;
	    for (Iterator i = stateTokenList.iterator(); i.hasNext(); ) {
		Token token = (Token) i.next();
                if (normalizeScores) {
                    token.applyScore(logScores[which++] - logMaxScore, feature);
                } else {
                    token.applyScore(logScores[which++], feature);
                }
	    }
	} catch (IOException ioe) {
	    System.out.println("IO Exception " + ioe);
	    return false;
	}
        
	return true;
    }

    /**
     * Performs post-recognition cleanup. 
     */
    public void stop() {
    }
}
