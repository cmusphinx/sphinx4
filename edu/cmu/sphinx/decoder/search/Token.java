
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

package edu.cmu.sphinx.decoder.search;

import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.knowledge.acoustic.HMMState;
import edu.cmu.sphinx.knowledge.acoustic.Context;
import edu.cmu.sphinx.knowledge.acoustic.LeftRightContext;
import edu.cmu.sphinx.knowledge.dictionary.Dictionary;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.decoder.linguist.SearchState;
import edu.cmu.sphinx.decoder.linguist.WordSearchState;
import edu.cmu.sphinx.decoder.linguist.UnitSearchState;
import edu.cmu.sphinx.decoder.linguist.HMMSearchState;
import edu.cmu.sphinx.decoder.scorer.Scoreable;

import java.util.List;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * Represents a single state in the recognition trellis. Subclasses of
 * a token are used to represent the various emitting state.
 *
 * All scores are maintained in LogMath log base
 *
 */
public class Token implements Scoreable {

    private final static boolean COMBINE_BRANCHES = true;

    private static int curCount;
    private static int lastCount;
    private static Set silenceSet;
    private static DecimalFormat scoreFmt = new DecimalFormat("0.0000000E00");
    private static DecimalFormat numFmt = new DecimalFormat("0000");

    static {
        silenceSet = new HashSet();
        silenceSet.add(Dictionary.SILENCE_SPELLING);
        silenceSet.add(Dictionary.SENTENCE_START_SPELLING);
        silenceSet.add(Dictionary.SENTENCE_END_SPELLING);
    }

    private Token predecessor;
    private Feature feature;
    private int frameNumber;		
    private float logTotalScore;			
    private float logLanguageScore;
    private float logInsertionProbability;
    private float logAcousticScore;
    private double logWorkingScore;
    private SearchState searchState;

    private int location;

    /**
     * Constructs a token
     *
     * @param predecessor the predecessor for this token
     * @param state the SentenceHMMState associated with this token
     *
     * @param logTotalScore the total entry score for this token (in
     * LogMath log base)
     *
     * @param logLanguageScore the language score associated with this
     * token (in LogMath log base)
     *
     * @param logInsertionProbability the insertion probabilty  associated with
     * this token (in LogMath log base)
     *
     * @param frameNumber the frame number associated with this token
     *
     */
    public Token(Token predecessor,
                 SearchState state,
                 float logTotalScore,
                 float logLanguageScore,
                 float logInsertionProbability,
                 int frameNumber) {
	this.predecessor = predecessor;
	this.searchState = state;
	this.logTotalScore = logTotalScore;
	this.logLanguageScore = logLanguageScore;
	this.logInsertionProbability = logInsertionProbability;
	this.frameNumber = frameNumber;
        this.location = -1;
	curCount++;

    }

    /**
     * Creates the initial token with the given word history depth
     *
     * @param state the SearchState associated with this token
     *
     * @param the depth of the word history to keep track of in this
     * lattice. If depth is 0, no word history will be maintained.
     *
     * @param frameNumber the frame number for this token
     */
    public Token(SearchState state, int frameNumber) {
	this(null, state, 0.0f, 0.0f, 0.0f, frameNumber);
    }


    /**
     * Returns the predecessor for this token, or null if this token
     * has no predecessors
     *
     * @return the predecessor
     */
    public Token getPredecessor() {
	return predecessor; 
    }


    /**
     * Returns the frame number for this token. Note that for tokens that
     * are associated with non-emitting states, the frame number
     * represents the next frame number.  For emitting states, the
     * frame number represents the current frame number.
     * 
     * @return the frame number for this token
     */
    public int getFrameNumber() { 
	return frameNumber; 
    }

    /**
     * Returns the feature for this Token.
     */
    public Feature getFeature() {
        return feature;
    }

    /**
     * Returns the score for the token. The score is a combination of
     * language and acoustic scores
     *
     * @return the score of this frame (in logMath log base)
     */
    public float getScore() { 
	return logTotalScore;
    }

    /**
     * Calculates a score against the given feature. The score can be
     * retreived with get score
     *
     * @param feature the feature to be scored
     *
     * @return the score for the feature
     */
    public float calculateScore(Feature feature) {
        if (searchState instanceof HMMSearchState) {
            HMMSearchState hmmSearchState = (HMMSearchState) searchState;
            HMMState hmmState = hmmSearchState.getHMMState();
            float logScore = hmmState.getScore(feature);
            logTotalScore += logScore;
            logAcousticScore = logScore;
            return logScore;
        } else {
            System.out.println("SS: " + searchState);
            throw new Error ("Attempting to score non-scoreable token");
        }
    }


    /**
     * Normalizes a previously calculated score
     *
     * @param maxLogScore the score to normalize this score with
     *
     * @return the normalized score
     */
    public float normalizeScore(float maxLogScore) {
        logTotalScore -= maxLogScore;
        logAcousticScore -= maxLogScore;
        return logTotalScore;
    }


    /**
     * Gets the working score. The working score is used to maintain
     * non-final scores during the search. Some search algorithms such
     * as bushderby use the working score
     *
     * @return the working score (in logMath log base)
     */
    public double getWorkingScore() {
	return logWorkingScore;
    }


    /**
     * Sets the working score for this token
     *
     * @param logScore the working score (in logMath log base)
     */
    public void setWorkingScore(double logScore) {
	logWorkingScore = logScore;
    }

    /**
     * Applies the given score and the Feature that produced this
     * score for this token.
     *
     * @param logScore the score to apply to this token (in logMath
     * log base)
     * @param feature the Feature that generated the given score
     */
    public void applyScore(float logScore, Feature feature) {
	if (false) {
	    System.out.println("Applying score " + logScore  + " to " + this);
	} else if (false) {
	    assert(logScore <= 0.0);
	}
	this.logTotalScore += logScore;
        this.logAcousticScore = logScore;
        // this.feature = feature;
    }

    /**
     * Sets the score for this token
     *
     * @param logScore the new score for the token (in logMath log
     * base)
     */
    public void setScore(float logScore) {
	this.logTotalScore = logScore;
    }


    /**
     * Returns the language score associated with this token
     *
     * @return the language score (in logMath log base)
     */
    public float getLanguageScore() {
	return logLanguageScore;
    }

    /**
     * Returns the insertionPenalty associated with this token
     *
     * @return the insertion probability  (in logMath log base)
     */
    public float getInsertionProbability() {
	return logInsertionProbability;
    }

    /**
     * Returns the acoustic score for this token (in logMath log base)
     */
    public float getAcousticScore() {
        return logAcousticScore;
    }

    /**
     * Returns the SearchState associated with this token
     *
     * @return the searchState 
     */
    public SearchState getSearchState() {
	return searchState;
    }

    /**
     * Determines if this token is associated with an emitting state.
     * An emitting state is a state that can be scored acoustically.
     *
     * @return <code>true</code> if this token is associated with an
     * emitting state
     */
    public boolean isEmitting() {
	return searchState.isEmitting();
    }

    /**
     * Determines if this token is associated with a final SentenceHMM state.
     *
     * @return <code>true</code> if this token is associated with a
     * final state
     */
    public boolean isFinal() {
	return searchState.isFinal();
    }

    /**
     * Retrieves the string representation of this object
     *
     * @return the string representation of this object
     */
    public String toString() {
        return 
            numFmt.format(getFrameNumber()) + " " +
            scoreFmt.format(getScore()) + " " +
            scoreFmt.format(getAcousticScore())
            + " " + getSearchState();
    }


    /**
     * dumps a branch of tokens
     */
    public void dumpTokenPath() {
        dumpTokenPath(true);
    }

    /**
     * dumps a branch of tokens
     *
     * @param includeHMMStates if true include all sentence hmm states
     */
    public void dumpTokenPath(boolean includeHMMStates) {
	Token token = this;
	List list = new ArrayList();

	while (token != null) {
	    list.add(token);
	    token = token.getPredecessor();
	}
	for (int i = list.size() - 1; i >= 0; i--) {
	    token = (Token) list.get(i);
            if (includeHMMStates || 
                (! (token.getSearchState() instanceof HMMSearchState))) {
                System.out.println("  " + token);
            }
	}
	System.out.println();
    }

    /**
     * returns the string of words leading up to this token
     *
     * @param wantSilences if true, silences are added
     *
     * @return the word path
     */
    public String getWordPath(boolean wantSilences) {
	StringBuffer sb = new StringBuffer();
	Token token = this;

	while (token != null) {
	    if (token.getSearchState() instanceof WordSearchState) {
		WordSearchState wordState  =
		    (WordSearchState) token.getSearchState();
                String word = wordState.getPronunciation().getWord();
                if (wantSilences || !silenceSet.contains(word)) {
                    sb.insert(0, word);
                    sb.insert(0, " ");
                }
	    }
	    token = token.getPredecessor();
	}
	return sb.toString().trim();
    }

    /**
     * Returns the string of words for this token, with no embedded
     * silences
     *
     * @return the string of words
     */
    public String getWordPathNoSilences() {
        return getWordPath(false);
    }

    /**
     * Returns the string of words for this token, with embedded
     * silences
     *
     * @return the string of words
     */
    public String getWordPath() {
        return getWordPath(true);
    }


    /**
     * Shows the token count
     */
    public static void showCount() {
	System.out.println("Cur count: " + curCount + " new " +
		(curCount - lastCount));
	lastCount = curCount;
    }

    /**
     * Returns the location of this Token in the ActiveList.
     * In the HeapActiveList implementation, it is the index of the
     * Token in the array backing the heap.
     *
     * @return the location of this Token in the ActiveList
     */
    public final int getLocation() {
        return location;
    }

    /**
     * Sets the location of this Token in the ActiveList.
     *
     * @param location the location of this Token
     */
    public final void setLocation(int location) {
        this.location = location;
    }

    /**
     * Determines if this branch is valid
     *
     * @return true if the token and its predecessors are valid
     */
    public boolean validate()  {
        return true;
    }



    /**
     * Return the DecimalFormat object for formatting the print out
     * of scores.
     *
     * @return the DecimalFormat object for formatting score print outs
     */
    protected static DecimalFormat getScoreFormat() {
        return scoreFmt;
    }


    /**
     * Return the DecimalFormat object for formatting the print out
     * of numbers
     *
     * @return the DecimalFormat object for formatting number print outs
     */
    protected static DecimalFormat getNumberFormat() {
        return numFmt;
    }
}
