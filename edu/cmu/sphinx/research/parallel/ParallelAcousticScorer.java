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

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FrontEndFactory;
import edu.cmu.sphinx.frontend.Signal;

import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.decoder.scorer.Scoreable;

import edu.cmu.sphinx.decoder.search.Token;

import edu.cmu.sphinx.util.SphinxProperties;

import java.util.Iterator;
import java.util.List;

import java.io.IOException;


/**
 * A parallel acoustic scorer that is capable of scoring multiple
 * feature streams.
 */
public class ParallelAcousticScorer implements AcousticScorer {

    private FrontEnd frontEnd;
    private SphinxProperties props;


    /**
     * Initializes this SimpleAcousticScorer with the given
     * context and FrontEnd.
     *
     * @param context the context to use
     * @param frontend the FrontEnd to use
     */
    public void initialize(String context, FrontEnd frontend) {
	this.frontEnd = frontend;
        this.props = SphinxProperties.getSphinxProperties(context);
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
        frontEnd = FrontEndFactory.getFrontEnd(modelName, props);
        
	Data data;

	try {
	    data = frontEnd.getData();
	 
	    if (data == null) {
                return false;
            }

	    if (data instanceof DataStartSignal) {
                data = frontEnd.getData();
                if (data == null) {
                    return false;
                }
	    }

	    if (data instanceof DataEndSignal) {
		return false;
	    }

            if (data instanceof Signal) {
                throw new Error("trying to score non-content feature");
            }

            float logMaxScore = -Float.MAX_VALUE;
	    for (Iterator i = scoreableList.iterator(); i.hasNext(); ) {
                Scoreable scoreable = (Scoreable) i.next();
                float logScore =  scoreable.calculateScore(data);
                if (logScore > logMaxScore) {
                    logMaxScore = logScore;
                }
	    }
	} catch (IOException ioe) {
	    System.out.println("IO Exception " + ioe);
	    return false;
	} catch (DataProcessingException dpe) {
            dpe.printStackTrace();
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
