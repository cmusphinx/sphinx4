
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

import edu.cmu.sphinx.decoder.linguist.simple.SentenceHMMState;
import edu.cmu.sphinx.decoder.search.Token;


/**
 * A Token for the within parallel stream sentence HMM states.
 * The <code>getScore()</code> method returns the parallel stream score.
 */
public class ParallelToken extends Token {

    private String modelName;       // the name of the acoustic model
    private float eta;              // feature stream eta factor
    private float combinedScore;    // the combined score
    private boolean pruned;         // is this token pruned?
    private int lastCombineTime;    // the last combination time
    private String lastCombineStamp;
    

    /**
     * Constructs a ParallelToken
     *
     * @param predecessor the predecessor for this token
     * @param state the SentenceHMMState associated with this token
     * @param eta the eta factor of this parallel branch
     * @param featureScore the score for this feature stream
     * @param combinedScore the combinedScore
     * @param frameNumber the frame number associated with this token
     *
     */
    public ParallelToken(ParallelToken predecessor, 
			 SentenceHMMState state,
                         float eta,
			 float featureScore,
			 float combinedScore,
			 int frameNumber) {
	super(predecessor, state, featureScore, 0.0f, 0.0f, frameNumber);
	if (predecessor != null) {
	    this.modelName = predecessor.getModelName();
	}
        this.eta = eta;
	this.combinedScore = combinedScore;
	this.pruned = false;
    }


    /**
     * Constructs a ParallelToken
     *
     * @param predecessor the predecessor for this token
     * @param state the SentenceHMMState associated with this token
     * @param eta the eta factor of this parallel branch
     * @param featureScore the score for this feature stream
     * @param combinedScore the combinedScore
     * @param frameNumber the frame number associated with this token
     * @param lastCombineFrame the frame number at which score combination
     *     last occurred
     *
     */
    public ParallelToken(ParallelToken predecessor,
			 SentenceHMMState state,
			 float eta,
			 float featureScore,
			 float combinedScore,
			 int frameNumber,
			 int lastCombineFrame) {
	this(predecessor, state, eta, featureScore, combinedScore,frameNumber);
	this.lastCombineTime = lastCombineFrame;
    }


    /**
     * Constructs a ParallelToken
     *
     * @param predecessor the predecessor for this token
     * @param state the SentenceHMMState associated with this token
     * @param eta the eta factor of this parallel branch
     * @param featureScore the score for this feature stream
     * @param combinedScore the combinedScore
     * @param frameNumber the frame number associated with this token
     * @param lastCombineStamp the stamp used to identify the last
     *    score combination event
     *
     */
    public ParallelToken(ParallelToken predecessor,
			 SentenceHMMState state,
			 float eta,
			 float featureScore,
			 float combinedScore,
			 int frameNumber,
                         String lastCombineStamp) {
	this(predecessor, state, eta, featureScore, combinedScore, -1);
        this.lastCombineStamp = lastCombineStamp;
    }

    
    /**
     * Constructs a ParallelToken with no predecessors, i.e.,
     * this is the ParallelToken at the first state of the SentenceHMM.
     *
     * @param modelName the name of the acoustic model
     * @param state the SentenceHMMState associated with this token
     * @param eta the eta factor of this parallel branch
     * @param frameNumber the frame number of this token
     */
    public ParallelToken(String modelName, SentenceHMMState state,
                         float eta, int frameNumber) {
        this(null, state, eta, 0.0f, 0.0f, frameNumber);
        this.modelName = modelName;
    }


    /**
     * Returns the eta factor for the feature stream of this ParallelToken.
     *
     * @return the eta factor
     */
    public float getEta() {
        return eta;
    }


    /**
     * Returns the combined score of this ParallelToken.
     * 
     * @return the combined score of this ParallelToken
     */
    public float getCombinedScore() {
	return combinedScore;
    }

    
    /**
     * Returns the feature score of this ParallelToken.
     *
     * @return the feature score of this ParallelToken
     */
    public float getFeatureScore() {
	return getScore();
    }


    /**
     * Returns the name of the acoustic model.
     *
     * @return the name of the acoustic model
     */
    public String getModelName() {
	return modelName;
	/*
        ParallelState parallelState = (ParallelState) getSentenceHMMState();
        return parallelState.getModelName();
	*/
    }


    /**
     * Returns the last frame score combination takes place.
     *
     * @return the last frame score combination takes place
     */
    public int getLastCombineTime() {
	return lastCombineTime; 
    }

    
    /**
     * Returns the stamp used to identify the last score combination event.
     *
     * @return the stamp used to identify the last score combination event
     */
    public String getLastCombineStamp() {
        return lastCombineStamp;
    }


    /**
     * Sets the combined score of this ParallelToken.
     *
     * @param combinedScore the new combined score
     */
    public void setCombinedScore(float combinedScore) {
        this.combinedScore = combinedScore;
    }


    /**
     * Sets the feature score of this ParallelToken.
     *
     * @param featureScore the new feature score
     */
    public void setFeatureScore(float featureScore) {
	setScore(featureScore);
    }


    /**
     * Sets the frame number at which score combination takes place.
     *
     * @param frameNumber the frame at which score combination last took
     *     place.
     */
    public void setLastCombineTime(int frameNumber) {
	this.lastCombineTime = frameNumber;
    }


    /**
     * Sets the stamp used to identify the last score combination event.
     *
     * @param stamp the stamp for identification
     */
    public void setLastCombineStamp(String stamp) {
	this.lastCombineStamp = stamp;
    }


    /**
     * Returns true if this ParallelToken has been pruned.
     *
     * @return true if this ParallelToken has been pruned.
     */
    public boolean isPruned() {
	return pruned;
    }


    /**
     * Sets this ParallelToken to be pruned.
     *
     * @param pruned sets this ParallelToken to be pruned or not
     */
    public void setPruned(boolean pruned) {
	this.pruned = pruned;
    }
}
