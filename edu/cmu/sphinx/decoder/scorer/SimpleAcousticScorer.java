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

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Signal;


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
     * @param scoreableList a list containing scoreable objects to
     * be scored
     *
     * @return true if there was a Data available to score
     *         false if there was no more Data available to score
     */
    public Scoreable calculateScores(List scoreableList) {

        if (scoreableList.size() <= 0) {
            return null;
        }

        Scoreable best = null;
	
	try {
	    Data data = frontEnd.getData();
            if (data == null) {
                System.out.println("SimpleAcousticScorer: Data is null");
                return best;
            }

	    if (data instanceof DataStartSignal) {
                data = frontEnd.getData();
                if (data == null) {
                    System.out.println("SimpleAcousticScorer: Data is null");
                    return best;
                }
	    }

	    if (data instanceof DataEndSignal) {
		return best;
	    }

            if (data instanceof Signal) {
                throw new Error("trying to score non-content data");
            }

            best = (Scoreable) scoreableList.get(0);

	    for (Iterator i = scoreableList.iterator(); i.hasNext(); ) {
                Scoreable scoreable = (Scoreable) i.next();
                /*
		if (scoreable.getFrameNumber() != data.getID()) {
		    throw new Error
			("Frame number mismatch: Token: " + 
			 scoreable.getFrameNumber() +
			 "  Data: " + data.getID());
		}
                */
                if (scoreable.calculateScore(data, false) > 
                    best.getScore()) {
                    best = scoreable;
                }
	    }

            if (normalizeScores) {
                for (Iterator i = scoreableList.iterator(); i.hasNext(); ) {
                    Scoreable scoreable = (Scoreable) i.next();
                    scoreable.normalizeScore(best.getScore());
                }
	    }
	} catch (DataProcessingException dpe) {
            dpe.printStackTrace();
	    return best;
	}
        
	return best;
    }

    /**
     * Performs post-recognition cleanup. 
     */
    public void stop() {
    }
}
