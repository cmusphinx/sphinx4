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

import edu.cmu.sphinx.decoder.scorer.Scoreable;
import edu.cmu.sphinx.decoder.scorer.ScoreProvider;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.UnitSearchState;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Represents a single state in the recognition trellis. Subclasses of a token are used to represent the various
 * emitting state.
 * <p>
 * All scores are maintained in LogMath log base
 */
public class Token implements Scoreable {

    private static int curCount;
    private static int lastCount;
    private static final DecimalFormat scoreFmt = new DecimalFormat("0.0000000E00");
    private static final DecimalFormat numFmt = new DecimalFormat("0000");

    private Token predecessor;

    private float logLanguageScore;
    private float logTotalScore;
    private float logInsertionScore;
    private float logAcousticScore;
    
    private SearchState searchState;

    private long collectTime;
    private Data data;

    /**
     * Internal constructor for a token. Used by classes Token, CombineToken, ParallelToken
     *
     * @param predecessor             the predecessor for this token
     * @param state                   the SentenceHMMState associated with this token
     * @param logTotalScore           the total entry score for this token (in LogMath log base)
     * @param logInsertionScore       the insertion score associated with this token (in LogMath log base)
     * @param logLanguageScore        the language score associated with this token (in LogMath log base)
     * @param collectTime             the frame collection time
     */
    public Token(Token predecessor,
                 SearchState state,
                 float logTotalScore,
                 float logInsertionScore,
                 float logLanguageScore,                 
                 long collectTime) {
        this.predecessor = predecessor;
        this.searchState = state;
        this.logTotalScore = logTotalScore;
        this.logInsertionScore = logInsertionScore;
        this.logLanguageScore = logLanguageScore;
        this.collectTime = collectTime;
        curCount++;
    }


    /**
     * Creates the initial token with the given word history depth
     *
     * @param state       the SearchState associated with this token
     * @param collectTime collection time of this token
     */
    public Token(SearchState state, long collectTime) {
        this(null, state, 0.0f, 0.0f, 0.0f, collectTime);
    }


    /**
     * Creates a Token with the given acoustic and language scores and predecessor.
     *
     * @param predecessor previous token
     * @param logTotalScore total score
     * @param logAcousticScore the log acoustic score
     * @param logInsertionScore the log insertion score
     * @param logLanguageScore the log language score
     */
    public Token(Token predecessor,
                 float logTotalScore, 
                 float logAcousticScore,
                 float logInsertionScore,
                 float logLanguageScore) {
        this(predecessor, null, logTotalScore, logInsertionScore, logLanguageScore, 0);
        this.logAcousticScore = logAcousticScore;
    }


    /**
     * Returns the predecessor for this token, or null if this token has no predecessors
     *
     * @return the predecessor
     */
    public Token getPredecessor() {
        return predecessor;
    }


    /**
     * Collect time is different from frame number because some frames might be skipped in silence detector
     * 
     * @return collection time in milliseconds
     */
    public long getCollectTime() {
        return collectTime;
    }


    /** Sets the feature for this Token.
     * @param data features
     */
    public void setData(Data data) {
        this.data = data;
        if (data instanceof FloatData) {
            collectTime = ((FloatData)data).getCollectTime();
        }
    }


    /**
     * Returns the feature for this Token.
     *
     * @return the feature for this Token
     */
    public Data getData() {
        return data;
    }


    /**
     * Returns the score for the token. The score is a combination of language and acoustic scores
     *
     * @return the score of this frame (in logMath log base)
     */
    public float getScore() {
        return logTotalScore;
    }


    /**
     * Calculates a score against the given feature. The score can be retrieved 
     * with get score. The token will keep a reference to the scored feature-vector.
     *
     * @param feature the feature to be scored
     * @return the score for the feature
     */
    public float calculateScore(Data feature) {
        
        logAcousticScore = ((ScoreProvider) searchState).getScore(feature);

        logTotalScore += logAcousticScore;

        setData(feature);

        return logTotalScore;
    }
    
    public float[] calculateComponentScore(Data feature){
    	return ((ScoreProvider) searchState).getComponentScore(feature);
    }


    /**
     * Normalizes a previously calculated score
     *
     * @param maxLogScore the score to normalize this score with
     * @return the normalized score
     */
    public float normalizeScore(float maxLogScore) {
        logTotalScore -= maxLogScore;
        logAcousticScore -= maxLogScore;
        return logTotalScore;
    }

    /**
     * Sets the score for this token
     *
     * @param logScore the new score for the token (in logMath log base)
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
     * Returns the insertion score associated with this token.
     * Insertion score is the score of the transition between
     * states. It might be transition score from the acoustic model,
     * phone insertion score or word insertion probability from
     * the linguist.
     *
     * @return the language score (in logMath log base)
     */
    public float getInsertionScore() {
        return logInsertionScore;
    }


    /** 
     * Returns the acoustic score for this token (in logMath log base).
     * Acoustic score is a sum of frame GMM.
     *
     * @return score
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
     * Determines if this token is associated with an emitting state. An emitting state is a state that can be scored
     * acoustically.
     *
     * @return <code>true</code> if this token is associated with an emitting state
     */
    public boolean isEmitting() {
        return searchState.isEmitting();
    }


    /**
     * Determines if this token is associated with a final SentenceHMM state.
     *
     * @return <code>true</code> if this token is associated with a final state
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
    @Override
    public String toString() {
        return
            numFmt.format(getCollectTime()) + ' ' +
            scoreFmt.format(getScore()) + ' ' +
            scoreFmt.format(getAcousticScore()) + ' ' +
            scoreFmt.format(getLanguageScore()) + ' ' +
            getSearchState();
    }


    /** dumps a branch of tokens */
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
        List<Token> list = new ArrayList<Token>();

        while (token != null) {
            list.add(token);
            token = token.getPredecessor();
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            token = list.get(i);
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
     * @param wantFiller         if true, filler words are added
     * @param wantPronunciations if true append [ phoneme phoneme ... ] after each word
     * @return the word path
     */
    public String getWordPath(boolean wantFiller, boolean wantPronunciations) {
        StringBuilder sb = new StringBuilder();
        Token token = this;

        while (token != null) {
            if (token.isWord()) {
                WordSearchState wordState =
                        (WordSearchState) token.getSearchState();
                Pronunciation pron = wordState.getPronunciation();
                Word word = wordState.getPronunciation().getWord();

//                System.out.println(token.getFrameNumber() + " " + word + " " + token.logLanguageScore + " " + token.logAcousticScore);

                if (wantFiller || !word.isFiller()) {
                    if (wantPronunciations) {
                        sb.insert(0, ']');
                        Unit[] u = pron.getUnits();
                        for (int i = u.length - 1; i >= 0; i--) {
                            if (i < u.length - 1) sb.insert(0, ',');
                            sb.insert(0, u[i].getName());
                        }
                        sb.insert(0, '[');
                    }
                    sb.insert(0, word.getSpelling());
                    sb.insert(0, ' ');
                }
            }
            token = token.getPredecessor();
        }
        return sb.toString().trim();
    }


    /**
     * Returns the string of words for this token, with no embedded filler words
     *
     * @return the string of words
     */
    public String getWordPathNoFiller() {
        return getWordPath(false, false);
    }


    /**
     * Returns the string of words for this token, with embedded silences
     *
     * @return the string of words
     */
    public String getWordPath() {
        return getWordPath(true, false);
    }


    /**
     * Returns the string of words and units for this token, with embedded silences.
     *
     * @return the string of words and units
     */
    public String getWordUnitPath() {
        StringBuilder sb = new StringBuilder();
        Token token = this;

        while (token != null) {
            SearchState searchState = token.getSearchState();
            if (searchState instanceof WordSearchState) {
                WordSearchState wordState = (WordSearchState) searchState;
                Word word = wordState.getPronunciation().getWord();
                sb.insert(0, ' ' + word.getSpelling());
            } else if (searchState instanceof UnitSearchState) {
                UnitSearchState unitState = (UnitSearchState) searchState;
                Unit unit = unitState.getUnit();
                sb.insert(0, ' ' + unit.getName());
            }
            token = token.getPredecessor();
        }
        return sb.toString().trim();
    }


    /**
     * Returns the word of this Token, the search state is a WordSearchState. If the search state is not a
     * WordSearchState, return null.
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


    /** Shows the token count */
    public static void showCount() {
        System.out.println("Cur count: " + curCount + " new " +
                (curCount - lastCount));
        lastCount = curCount;
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
     * Return the DecimalFormat object for formatting the print out of scores.
     *
     * @return the DecimalFormat object for formatting score print outs
     */
    protected static DecimalFormat getScoreFormat() {
        return scoreFmt;
    }


    /**
     * Return the DecimalFormat object for formatting the print out of numbers
     *
     * @return the DecimalFormat object for formatting number print outs
     */
    protected static DecimalFormat getNumberFormat() {
        return numFmt;
    }

    public void update(Token predecessor, SearchState nextState,
            float logEntryScore, float insertionProbability,
            float languageProbability, long collectTime) {
        this.predecessor = predecessor;
        this.searchState = nextState;
        this.logTotalScore = logEntryScore;
        this.logInsertionScore = insertionProbability;
        this.logLanguageScore = languageProbability;
        this.collectTime = collectTime;
    }
}
