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

import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.search.Token;

import java.util.Iterator;

import java.io.IOException;

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.Signal;


/**
 * A parallel acoustic scorer that is capable of scoring multiple
 * feature streams.
 */
public class ParallelAcousticScorer implements AcousticScorer {

    private FrontEnd frontEnd;


    /**
     * Initializes this ParallelAcousticScorer with the given
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
     * Scores the given set of Tokens. All Tokens in the given
     * ActiveList are assumed to belong to the same acoustic model.
     *
     * @param stateTokenList a list containing StateToken objects to
     * be scored
     *
     * @return true if there are more features available
     */
    public boolean calculateScores(ActiveList stateTokenList) {

	FeatureFrame ff;

	try {
	    assert stateTokenList.size() > 0;

	    String modelName = getModelName(stateTokenList);
	    if (modelName == null) {
		System.out.println
		    ("ParallelAcousticScorer: modelName is null");
	    }
	    assert modelName != null;

	    ff = frontEnd.getFeatureFrame(1, modelName);
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
		return true;
	    }
	    if (feature.getSignal() == Signal.UTTERANCE_END) {
		return false;
	    }

	    for (Iterator i = stateTokenList.iterator(); i.hasNext(); ) {
		ParallelToken token = (ParallelToken) i.next();
                if (!token.isPruned()) {
                    ParallelHMMStateState hmmStateState = 
                        (ParallelHMMStateState)token.getSearchState();
                    if (feature.hasContent()) {
                        float score = hmmStateState.getScore(feature);
                        token.applyScore(score, feature);
                    } else {
                        System.out.println("non-content feature " +
                                           feature + " id " + feature.getID());
                    }
                }
	    }
	} catch (IOException ioe) {
	    System.out.println("IO Exception " + ioe);
	    return false;
	}
        
	return true;
    }


    /**
     * Returns the acoustic model name of the Tokens in the given
     * ActiveList.
     *
     * @return the acoustic model name of the Tokens
     */
    private String getModelName(ActiveList activeList) {
	String modelName = null;
	if (activeList.size() > 0) {
	    Iterator i = activeList.iterator();
	    if (i.hasNext()) {
		ParallelToken token = (ParallelToken) i.next();
		/*
		ParallelHMMStateState state = (ParallelHMMStateState)
		    token.getSentenceHMMState();
		modelName = state.getModelName();
		*/
		modelName = token.getModelName();
	    }
	}
	return modelName;
    }


    /**
     * Performs post-recognition cleanup. 
     */
    public void stop() {
    }
}
