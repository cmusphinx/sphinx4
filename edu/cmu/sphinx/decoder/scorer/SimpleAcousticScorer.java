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
import java.util.List;
import java.io.IOException;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.decoder.scorer.AcousticScorer;


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
     * Checks to see if a FeatureFrame is null or if there are Features in it.
     *
     * @param ff the FeatureFrame to check
     *
     * @return false if the given FeatureFrame is null or if there
     * are no Features in the FeatureFrame; true otherwise.
     */
    private boolean hasFeatures(FeatureFrame ff) {
        if (ff == null) {
            System.out.println("SimpleAcousticScorer: FeatureFrame is null");
            return false;
        }
        if (ff.getFeatures() == null) {
            System.out.println
                ("SimpleAcousticScorer: no features in FeatureFrame");
            return false;
        }
        return true;
    }


    /**
     * Scores the given set of states
     *
     * @param scoreableList a list containing scoreable objects to
     * be scored
     *
     * @return true if there was a Feature available to score
     *         false if there was no more Feature available to score
     */
    public boolean calculateScores(List scoreableList) {

	FeatureFrame ff;

	try {
	    ff = frontEnd.getFeatureFrame(1, null);
	    Feature feature;

	    if (!hasFeatures(ff)) {
                return false;
            }

	    feature = ff.getFeatures()[0];

	    if (feature.getSignal() == Signal.UTTERANCE_START) {
                ff = frontEnd.getFeatureFrame(1, null);
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

            float logMaxScore = - Float.MAX_VALUE;
	    for (Iterator i = scoreableList.iterator(); i.hasNext(); ) {
                Scoreable scoreable = (Scoreable) i.next();
		if (scoreable.getFrameNumber() != curFeature.getID()) {
		    throw new Error
			("Frame number mismatch: Token: " + 
			 scoreable.getFrameNumber() +
			 "  Feature: " + curFeature.getID());
		}
                float logScore =  scoreable.calculateScore(feature);
                if (logScore > logMaxScore) {
                    logMaxScore = logScore;
                }
	    }

            if (normalizeScores) {
                for (Iterator i = scoreableList.iterator(); i.hasNext(); ) {
                    Scoreable scoreable = (Scoreable) i.next();
                    scoreable.normalizeScore(logMaxScore);
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
