
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * A Token for the outside parallel stream sentence HMM states.
 * The <code>getScore()</code> method returns the combined score.
 */
public class CombineToken extends Token {

    private Map tokens;     // the list of predecessor tokens
    private boolean pruned;
    private int identity;

    private static int ID;
    static {
        ID = 0;
    }


    /**
     * Constructs a CombineToken
     *
     * @param predecessor the predecessor for this token
     * @param state the SentenceHMMState associated with this token
     * @param combinedScore the combinedScore
     * @param frameNumber the frame number associated with this token
     *
     */
    public CombineToken(Token predecessor,
			SentenceHMMState state,
			int frameNumber) {
	super(predecessor, state, 0.0f, 0.0f, 0.0f, frameNumber);
	this.tokens = new HashMap();
	this.pruned = false;
        this.identity = ID++;
    }


    /**
     * Returns the combined score of this CombineToken.
     *
     * @return the combined score
     */
    public float getCombinedScore() {
	return getScore();
    }


    /**
     * Sets the combined score.
     *
     * @param combinedScore the combined score
     */
    public void setCombinedScore(float combinedScore) {
	setScore(combinedScore);
    }


    /**
     * Adds the parallel score of the given parallel stream.
     *
     * @param parallelScore the parallel score
     * @param parallelStreamName the name of the parallel stream
     */
    public ParallelToken addParallelToken(Object key, 
                                          ParallelToken parallelToken) {
	return (ParallelToken) tokens.put(key, parallelToken);
    }


    /**
     * Adds all the ParallelTokens in the given list into this CombineToken.
     * The added tokens are keyed by their model name.
     *
     * @param tokenList the list of ParallelTokens
     */
    public void addAll(List tokenList) {
        for (Iterator i = tokenList.iterator(); i.hasNext(); ) {
            ParallelToken token = (ParallelToken) i.next();
            tokens.put(token.getModelName(), token);
        }
    }


    /**
     * Removes all the ParallelTokens from this CombineToken.
     */
    public void clear() {
        tokens.clear();
    }


    /**
     * Returns an Iterator for the ParallelToken(s).
     *
     * @return an Iterator for the ParallelToken(s)
     */
    public Iterator getTokenIterator() {
        return tokens.values().iterator();
    }


    /**
     * Returns a Collection of all the parallel tokens.
     *
     * @return a Collection of all the parallel tokens
     */
    public Collection getParallelTokens() {
        return tokens.values();
    }


    /**
     * Returns the parallel token of the given parallel stream.
     *
     * @return the parallel token of the given parallel stream
     */
    public ParallelToken getParallelToken(String parallelStreamName) {
        return (ParallelToken) tokens.get(parallelStreamName);
    }


    /**
     * Returns the ID of this CombineToken. They should all be unique
     * except when the wrap around of the integer primitive type happens.
     *
     * @return the ID of this CombineToken
     */
    public int getID() {
        return identity;
    }


    /**
     * Returns true if this CombineToken is pruned.
     *
     * @return true if this CombineToken is pruned
     */
    public boolean isPruned() {
	return pruned;
    }


    /**
     * Sets whether this CombineToken is pruned.
     *
     * @param pruned whether this CombineToken is pruned
     */
    public void setPruned(boolean pruned) {
	this.pruned = pruned;
    }


    /**
     * Sets the last combine time of all the ParallelTokens in this
     * CombineToken.
     *
     * @param frameNumber the last combine time
     */
    public void setLastCombineTime(int frameNumber) {
	for (Iterator i = getTokenIterator(); i.hasNext(); ) {
	    ParallelToken pToken = (ParallelToken) i.next();
	    pToken.setLastCombineTime(frameNumber);
	}
    }


    /**
     * Returns the string representation of this object.
     *
     * @return the string representation of this object
     */
    public String toString() {
        String parallelTokenScores = "";
        int t = 0;
        for (Iterator i = getTokenIterator(); i.hasNext(); t++) {
            ParallelToken token = (ParallelToken) i.next();
            parallelTokenScores += ("   ParallelToken " + t) + 
                ", " + token.getModelName() + ", feature: " +
                getScoreFormat().format(token.getFeatureScore()) +
                ", combined: " + 
                getScoreFormat().format(token.getCombinedScore()) + "\n";
        }
        return "CombinedToken: " +
            "Frame: " + getNumberFormat().format(getFrameNumber()) + 
            ", score: " + getScoreFormat().format(getScore()) + "\n" + 
            parallelTokenScores;
    }
}
