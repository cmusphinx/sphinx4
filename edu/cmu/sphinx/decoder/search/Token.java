/*
 * Copyright 1999-2002 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.decoder.search;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import edu.cmu.sphinx.decoder.scorer.Scoreable;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.dictionary.Word;

/**
 * Represents a single state in the recognition trellis. Subclasses of
 * a token are used to represent the various emitting state.
 *
 * All scores are maintained in LogMath log base
 *
 */
public class Token implements Scoreable {

    /**
     * a token comparator that is used to order tokens in
     * descending order
     *
     */
    public final static Comparator COMPARATOR = new Comparator() {
	    public int compare(Object o1, Object o2) {
		Token t1 = (Token) o1;
		Token t2 = (Token) o2;

		if (t1.getScore() > t2.getScore()) {
		    return -1;
		} else if (t1.getScore() ==  t2.getScore()) {
		    return 0;
		} else {
		    return 1;
		}
	    }
	};

    private final static boolean COMBINE_BRANCHES = true;

    private static int curCount;
    private static int lastCount;
    private static DecimalFormat scoreFmt = new DecimalFormat("0.0000000E00");
    private static DecimalFormat numFmt = new DecimalFormat("0000");

    private Token predecessor;
    private int frameNumber;
    private float logTotalScore;
    private float logLanguageScore;
    private float logInsertionProbability;
    private float logAcousticScore;
    private float logWorkingScore;
    private SearchState searchState;

    private int location;
    private Object appObject;

    private static Set predecessorClasses = null;

    /**
     * Set the predecessor class. Used to modify the behavior of child() 
     * so that the predecessor backpointer will skip internal states.  
     * For example, when retaining tokens to form a word lattice, 
     * it would be inefficient to
     * keep any states but WordStates-- other types of states are not used
     * and the memory should be saved.  On the other hand, a phoneme recognizer
     * would require PronunciationStates to create a suitable result.
     *
     * @param bpClasses
     */
    public static void setPredecessorClass(Set bpClasses) {
        predecessorClasses = bpClasses;
    }

    /**
     * Constructs a new token that continues the search from the current token.
     * If predessorClasses is null or if the class of the state is a member of
     * predecessorClasses, the predecessor of the new token is set to the
     * current token.  Otherwise it is set to the predecessor of the current
     * token.  This behavior is used to save memory when building lattices.
     *
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
     */
    public Token child(SearchState state,
                       float logTotalScore,
                       float logLanguageScore,
                       float logInsertionProbability,
                       int frameNumber) {
       if ((predecessorClasses == null) || 
	   predecessorClasses.contains(state.getClass())) {
            return new Token(this, state, 
			     logTotalScore, logLanguageScore, 
			     logInsertionProbability, frameNumber);
        } else {
            return new Token(predecessor, state, 
			     logTotalScore, logLanguageScore, 
			     logInsertionProbability, frameNumber);
        }
    }

    /**
     * Internal constructor for a token.  
     * Used by classes Token, CombineToken, ParallelToken
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
    protected Token(Token predecessor,
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
     *
     * @return the feature for this Token
     */
    public Data getData() {
        if (appObject != null && appObject instanceof Data) {
            return (Data) appObject;
        } else {
            return null;
        }
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
     * @param keepData whether this Scoreable should keep a reference
     *    to the given feature
     * @param gain gain to apply to the score;
     *
     * @return the score for the feature
     */
    public float calculateScore(Data feature, boolean keepData, float gain) {

        //assert searchState.isEmitting() 
        //   : "Attempting to score non-scoreable token: " + searchState;
        HMMSearchState hmmSearchState = (HMMSearchState) searchState;
        HMMState hmmState = hmmSearchState.getHMMState();
        logAcousticScore = hmmState.getScore(feature) * gain;
        logTotalScore += logAcousticScore;

        if (keepData) {
            setAppObject(feature);
        }

        return logTotalScore;
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
    public float getWorkingScore() {
	    return logWorkingScore;
    }


    /**
     * Sets the working score for this token
     *
     * @param logScore the working score (in logMath log base)
     */
    public void setWorkingScore(float logScore) {
	    logWorkingScore = logScore;
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
     * Determines if this token marks the end of a word
     *
     * @return <code>true</code> if this token marks the end of a word
     */
    public boolean isWord() {
	return searchState instanceof WordSearchState;
    }

    /**
     * Retrieves the string representation of this object
     *
     * @return the string representation of this object
     */
    public String toString() {
        String appString = "";

        if (appObject != null) {
            appString = " " + appObject.toString();
        }
        return
                numFmt.format(getFrameNumber()) + " " +
                scoreFmt.format(getScore()) + " " +
                scoreFmt.format(getAcousticScore()) + " " +
                scoreFmt.format(getLanguageScore()) + " " +
                scoreFmt.format(getInsertionProbability())
                + " " + getSearchState() + appString;
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
                    (!(token.getSearchState() instanceof HMMSearchState))) {
                System.out.println("  " + token);
            }
        }
        System.out.println();
    }

    /**
     * Returns the string of words leading up to this token.
     *
     * @param wantFiller if true, filler words are added
     *
     * @return the word path
     */
    public String getWordPath(boolean wantFiller) {
        StringBuffer sb = new StringBuffer();
        Token token = this;

        while (token != null) {
            if (token.isWord()) {
                WordSearchState wordState =
                    (WordSearchState) token.getSearchState();
                Word word = wordState.getPronunciation().getWord();
                if (wantFiller || !word.isFiller()) {
                    sb.insert(0, word.getSpelling());
                    sb.insert(0, " ");
                }
            }
            token = token.getPredecessor();
        }
        return sb.toString().trim();
    }

    /**
     * Returns the string of words for this token, with no embedded
     * filler words
     *
     * @return the string of words
     */
    public String getWordPathNoFiller() {
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
     * Returns the word of this Token, the search state is a WordSearchState.
     * If the search state is not a WordSearchState, return null.
     *
     * @return the word of this Token, or null if this is not a word token
     */
    public Word getWord() {
        if (isWord()) {
            WordSearchState wordState = (WordSearchState) searchState;
            return wordState.getPronunciation().getWord();
        } else {
            return null;
        }
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
    public boolean validate() {
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

    /**
     * Returns the application object
     *
     * @return the application object
     */
    public Object getAppObject() {
        return appObject;
    }

    /**
     * Sets the application object
     *
     * @param obj the application object
     */
    public void setAppObject(Object obj) {
        appObject = obj;
    }
}
