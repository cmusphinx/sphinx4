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

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.Signal;

import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.decoder.scorer.Scoreable;

import edu.cmu.sphinx.decoder.search.Token;

import java.util.Iterator;
import java.util.List;

import java.io.IOException;


/**
 * A parallel acoustic scorer that is capable of scoring multiple
 * feature streams.
 */
public class ParallelAcousticScorer implements AcousticScorer {

    private FrontEnd frontEnd;


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
     * Performs post-recognition cleanup. 
     */
    public void stop() {
    }


    /**
     * Checks to see if a FeatureFrame is null or if there are Features in it.
     *
     * @param ff the FeatureFrame to check
     *
     * @return false if the given FeatureFrame is null or if there
     * are no Features in the FeatureFrame; true otherwise.
     */
    private boolean hasFeatures(FeatureFrame ff) {
        if (ff == null) {
            System.out.println("ParallelAcousticScorer: FeatureFrame is null");
            return false;
        }
        if (ff.getFeatures() == null) {
            System.out.println
                ("ParallelAcousticScorer: no features in FeatureFrame");
            return false;
        }
        return true;
    }



    /**
     * Scores the given set of Tokens. All Tokens in the given
     * list are assumed to belong to the same acoustic model.
     *
     * @param scoreableList a list containing StateToken objects to
     * be scored
     *
     * @return true if there are more features available
     */
    public boolean calculateScores(List scoreableList) {

        assert scoreableList.size() > 0;
        
        String modelName = getModelName(scoreableList);
        if (modelName == null) {
            System.out.println
                ("ParallelAcousticScorer: modelName is null");
        }
        assert modelName != null;
        
	FeatureFrame ff;

	try {
	    ff = frontEnd.getFeatureFrame(1, modelName);
	    Feature feature;

	    if (!hasFeatures(ff)) {
                return false;
            }

	    feature = ff.getFeatures()[0];

	    if (feature.getSignal() == Signal.UTTERANCE_START) {
                ff = frontEnd.getFeatureFrame(1, modelName);
                if (!hasFeatures(ff)) {
                    return false;
                }
                feature = ff.getFeatures()[0];
	    }

	    if (feature.getSignal() == Signal.UTTERANCE_END) {
		return false;
	    }

            if (!feature.hasContent()) {
                throw new Error("trying to score non-content feature");
            }

            float logMaxScore = -Float.MAX_VALUE;
	    for (Iterator i = scoreableList.iterator(); i.hasNext(); ) {
                Scoreable scoreable = (Scoreable) i.next();
		if (scoreable.getFrameNumber() != feature.getID()) {
		    throw new Error
			("Frame number mismatch: Token: " + 
			 scoreable.getFrameNumber() +
			 "  Feature: " + feature.getID());
		}
                float logScore =  scoreable.calculateScore(feature);
                if (logScore > logMaxScore) {
                    logMaxScore = logScore;
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
     * list .
     *
     * @return the acoustic model name of the Tokens
     */
    private String getModelName(List activeList) {
	String modelName = null;
	if (activeList.size() > 0) {
	    Iterator i = activeList.iterator();
	    if (i.hasNext()) {
		ParallelToken token = (ParallelToken) i.next();
                modelName = token.getModelName();
	    }
	}
	return modelName;
    }
}
