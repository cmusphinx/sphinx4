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

package edu.cmu.sphinx.result;

import static java.util.Collections.emptyList;
import static java.util.Collections.reverse;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.search.AlternateHypothesisManager;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.TimeFrame;

/**
 * Provides recognition results. Results can be partial or final. A result
 * should not be modified before it is a final result. Note that a result may
 * not contain all possible information.
 * <p>
 * The following methods are not yet defined but should be:
 * 
 * <pre>
 * public Result getDAG(int compressionLevel);
 * </pre>
 */
public class Result {

    private final ActiveList activeList;
    private final List<Token> resultList;
    private AlternateHypothesisManager alternateHypothesisManager;
    private boolean isFinal;
    private boolean wordTokenFirst;
    private final long currentCollectTime;
    private String reference;
    private final LogMath logMath;
    private final boolean toCreateLattice;

    /**
     * Creates a result
     * 
     * @param alternateHypothesisManager hypothesis manager
     * @param activeList
     *            the active list associated with this result
     * @param resultList
     *            the result list associated with this result
     * @param collectTime
     *            token time in a stream.
     * @param isFinal
     *            if true, the result is a final result
     * @param wordTokenFirst if word token goes first.
     * @param toCreateLattice create lattice or not.
     */
    public Result(AlternateHypothesisManager alternateHypothesisManager, ActiveList activeList, List<Token> resultList,
            long collectTime, boolean isFinal, boolean wordTokenFirst, boolean toCreateLattice) {
        this(activeList, resultList, collectTime, isFinal, wordTokenFirst, toCreateLattice);
        this.alternateHypothesisManager = alternateHypothesisManager;
    }

    /**
     * Creates a result
     * 
     * @param activeList
     *            the active list associated with this result
     * @param resultList
     *            the result list associated with this result
     * @param collectTime
     *            token collect time in a stream.
     * @param isFinal
     *            if true, the result is a final result. This means that the
     *            last frame in the speech segment has been decoded.
     * @param wordTokenFirst if word token goes first.
     * @param toCreateLattice create lattice or not.
     */
    public Result(ActiveList activeList, List<Token> resultList, long collectTime, boolean isFinal, boolean wordTokenFirst, boolean toCreateLattice) {
        this.activeList = activeList;
        this.resultList = resultList;
        this.currentCollectTime = collectTime;
        this.isFinal = isFinal;
        this.toCreateLattice = toCreateLattice;
        this.wordTokenFirst = wordTokenFirst;
        logMath = LogMath.getLogMath();
    }

    /**
     * Determines if the result is a final result. A final result is guaranteed
     * to no longer be modified by the SearchManager that generated it.
     * Non-final results can be modifed by a
     * <code>SearchManager.recognize</code> calls.
     * 
     * @return true if the result is a final result
     */
    public boolean isFinal() {
        return isFinal;
    }

    /**
     * Checks if it justified to build lattice for this result
     * 
     * @return true if lattice created from this result can provide confidence
     *         scores and n-best list
     */
    public boolean toCreateLattice() {
        return toCreateLattice;
    }

    /**
     * Returns the log math used for this Result.
     * 
     * @return the log math used
     */
    public LogMath getLogMath() {
        return logMath;
    }

    /**
     * Returns a list of active tokens for this result. The list contains zero
     * or active <code>Token</code> objects that represents the leaf nodes of
     * all active branches in the result (sometimes referred to as the
     * 'lattice').
     * <p>
     * The lattice is live and may be modified by a SearchManager during a
     * recognition. Once the Result is final, the lattice is fixed and will no
     * longer be modified by the SearchManager. Applications can modify the
     * lattice (to prepare for a re-recognition, for example) only after
     * <code>isFinal</code> returns <code>true</code>
     * 
     * @return a list containing the active tokens for this result
     * @see Token
     */
    public ActiveList getActiveTokens() {
        return activeList;
    }

    /**
     * Returns a list of result tokens for this result. The list contains zero
     * or more result <code>Token</code> objects that represents the leaf nodes
     * of all final branches in the result (sometimes referred to as the
     * 'lattice').
     * <p>
     * The lattice is live and may be modified by a SearchManager during a
     * recognition. Once the Result is final, the lattice is fixed and will no
     * longer be modified by the SearchManager. Applications can modify the
     * lattice (to prepare for a re-recognition, for example) only after
     * <code>isFinal</code> returns <code>true</code>
     * 
     * @return a list containing the final result tokens for this result
     * @see Token
     */
    public List<Token> getResultTokens() {
        return resultList;
    }

    /**
     * Returns the AlternateHypothesisManager Used to construct a Lattice
     * 
     * @return the AlternateHypothesisManager
     */
    public AlternateHypothesisManager getAlternateHypothesisManager() {
        return alternateHypothesisManager;
    }

    /**
     * Returns the current frame number
     * 
     * @return the frame number
     */
    public long getCollectTime() {
        return currentCollectTime;
    }

    /**
     * Returns the best scoring final token in the result. A final token is a
     * token that has reached a final state in the current frame.
     * 
     * @return the best scoring final token or null
     */
    public Token getBestFinalToken() {
        Token bestToken = null;
        for (Token token : resultList) {
            if (bestToken == null || token.getScore() > bestToken.getScore()) {
                bestToken = token;
            }
        }
        return bestToken;
    }

    /**
     * Returns the best scoring token in the result. First, the best final token
     * is retrieved. A final token is one that has reached the final state in
     * the search space. If no final tokens can be found, then the best,
     * non-final token is returned.
     * 
     * @return the best scoring token or null
     */
    public Token getBestToken() {
        Token bestToken = getBestFinalToken();

        if (bestToken == null) {
            bestToken = getBestActiveToken();
        }

        return bestToken;
    }

    /**
     * Returns the best scoring token in the active set
     * 
     * @return the best scoring token or null
     */
    public Token getBestActiveToken() {
        Token bestToken = null;
        if (activeList != null) {
            for (Token token : activeList) {
                if (bestToken == null || token.getScore() > bestToken.getScore()) {
                    bestToken = token;
                }
            }
        }
        return bestToken;
    }

    /**
     * Searches through the n-best list to find the the branch that matches the
     * given string
     * 
     * @param text
     *            the string to search for
     * @return the token at the head of the branch or null
     */
    public Token findToken(String text) {
        text = text.trim();
        for (Token token : resultList) {
            if (text.equals(token.getWordPathNoFiller())) {
                return token;
            }
        }
        return null;
    }

    /**
     * Searches through the n-best list to find the the branch that matches the
     * beginning of the given string
     * 
     * @param text
     *            the string to search for
     * @return the list token at the head of the branch
     */
    public List<Token> findPartialMatchingTokens(String text) {
        List<Token> list = new ArrayList<Token>();
        text = text.trim();
        for (Token token : activeList) {
            if (text.startsWith(token.getWordPathNoFiller())) {
                list.add(token);
            }
        }
        return list;
    }

    /**
     * Returns the best scoring token that matches the beginning of the given
     * text.
     * 
     * @param text
     *            the text to match
     * @return best token
     */
    public Token getBestActiveParitalMatchingToken(String text) {
        List<Token> matchingList = findPartialMatchingTokens(text);
        Token bestToken = null;
        for (Token token : matchingList) {
            if (bestToken == null || token.getScore() > bestToken.getScore()) {
                bestToken = token;
            }
        }
        return bestToken;
    }

    /**
     * Returns detailed frame statistics for this result
     * 
     * @return frame statistics for this result as an array, with one element
     *         per frame or <code>null</code> if no frame statistics are
     *         available.
     */
    public FrameStatistics[] getFrameStatistics() {
        return null; // [[[ TBD: write me ]]]
    }

    /**
     * Gets the starting frame number for the result. Note that this method is
     * currently not implemented, and always returns zero.
     * 
     * @return the starting frame number for the result
     */
    public int getStartFrame() {
        return 0;
    }

    /**
     * Gets the ending frame number for the result. Note that this method is
     * currently not implemented, and always returns zero.
     * 
     * @return the ending frame number for the result
     */
    public int getEndFrame() {
        return 0; // [[[ TBD: write me ]]]
    }

    /**
     * Gets the feature frames associated with this result
     * 
     * @return the set of feature frames associated with this result, or null if
     *         the frames are not available.
     */
    public List<Data> getDataFrames() {
        // find the best token, and then trace back for all the features
        Token token = getBestToken();

        if (token == null)
            return null;

        List<Data> featureList = new LinkedList<Data>();

        do {
            Data feature = token.getData();
            if (feature != null)
                featureList.add(0, feature);

            token = token.getPredecessor();
        } while (token != null);

        return featureList;
    }

    /**
     * Returns the string of the best result, removing any filler words. This
     * method first attempts to return the best final result, that is, the
     * result that has reached the final state of the search space. If there are
     * no best final results, then the best non-final result, that is, the one
     * that did not reach the final state, is returned.
     * 
     * @return the string of the best result, removing any filler words
     */
    public String getBestResultNoFiller() {
        Token token = getBestToken();
        if (token == null) {
            return "";
        } else {
            return token.getWordPathNoFiller();
        }
    }

    /**
     * Returns the string of the best final result, removing any filler words. A
     * final result is a path that has reached the final state. A Result object
     * can also contain paths that did not reach the final state, and those
     * paths are not returned by this method.
     * 
     * @return the string of the best result, removing any filler words, or null
     *         if there are no best results
     */
    public String getBestFinalResultNoFiller() {
        Token token = getBestFinalToken();
        if (token == null) {
            return "";
        } else {
            return token.getWordPathNoFiller();
        }
    }

    /**
     * The method is used when the application wants the phonemes on the best
     * final path. Note that words may have more than one pronunciation, so this
     * is not equivalent to the word path e.g. one[HH,W,AH,N] to[T,UW]
     * three[TH,R,IY]
     * 
     * @return the String of words and associated phonemes on the best path
     */
    public String getBestPronunciationResult() {
        Token token = getBestFinalToken();
        if (token == null) {
            return "";
        } else {
            return token.getWordPath(false, true);
        }
    }

    /**
     * Returns the string of words (with timestamp) for this token.
     * 
     * @param withFillers
     *            true if we want filler words included, false otherwise
     * @return the string of words
     */
    public List<WordResult> getTimedBestResult(boolean withFillers) {
        Token token = getBestToken();
        if (token == null) {
            return emptyList();
        } else {
            if (wordTokenFirst) {
                return getTimedWordPath(token, withFillers);
            } else {
                return getTimedWordTokenLastPath(token, withFillers);
            }
        }
    }

    /**
     * Returns the string of words (with timestamp) for this token. This method
     * assumes that the word tokens come before other types of token.
     * 
     * @param withFillers
     *            true if we want filler words, false otherwise
     * @return list of word with timestamps
     */
    private List<WordResult> getTimedWordPath(Token token, boolean withFillers) {
        long prevWordEnd = -1;
        List<WordResult> result = new ArrayList<WordResult>();

        while (token != null) {

            if (prevWordEnd < 0)
                prevWordEnd = token.getCollectTime();

            if (token.isWord()) {
                Word word = token.getWord();
                if (withFillers || !word.isFiller()) {
                    TimeFrame timeFrame = new TimeFrame(token.getCollectTime(), prevWordEnd);
                    result.add(new WordResult(word, timeFrame, token.getScore(), 1.));
                }
                prevWordEnd = token.getCollectTime();
            }
            token = token.getPredecessor();
        }

        reverse(result);
        return result;
    }

    /**
     * Returns the string of words for this token, each with the starting sample
     * number as the timestamp. This method assumes that the word tokens come
     * after the unit and HMM tokens.
     * 
     * @return the string of words, each with the starting sample number
     */
    private List<WordResult> getTimedWordTokenLastPath(Token token, boolean withFillers) {
        long lastWordEnd = -1;
        long lastWordStart = -1;
        Word word = null;

        List<WordResult> result = new ArrayList<WordResult>();
        while (token != null) {
            if (token.isWord()) {
                if (word != null && lastWordEnd >= 0) {
                    if (withFillers || !word.isFiller()) {
                        TimeFrame timeFrame = new TimeFrame(lastWordStart, lastWordEnd);
                        result.add(new WordResult(word, timeFrame, token.getScore(), 1.));
                    }
                }
                lastWordEnd = token.getCollectTime();
                word = token.getWord();
            }
            lastWordStart = token.getCollectTime();
            token = token.getPredecessor();
        }

        reverse(result);
        return result;
    }

    /** Returns a string representation of this object */
    @Override
    public String toString() {
        Token token = getBestToken();
        if (token == null) {
            return "";
        } else {
            return token.getWordPath();
        }
    }

    /**
     * Sets the results as a final result
     * 
     * @param finalResult
     *            if true, the result should be made final
     */
    void setFinal(boolean finalResult) {
        this.isFinal = finalResult;
    }

    /**
     * Determines if the Result is valid. This is used for testing and debugging
     * 
     * @return true if the result is properly formed.
     */
    public boolean validate() {
        boolean valid = true;
        for (Token token : activeList) {
            if (!token.validate()) {
                valid = false;
                token.dumpTokenPath();
            }
        }
        return valid;
    }

    /**
     * Sets the reference text
     * 
     * @param ref
     *            the reference text
     */
    public void setReferenceText(String ref) {
        reference = ref;
    }

    /**
     * Retrieves the reference text. The reference text is a transcript of the
     * text that was spoken.
     * 
     * @return the reference text or null if no reference text exists.
     */
    public String getReferenceText() {
        return reference;
    }

    /**
     * Getter for wordTokenFirst flag
     * 
     * @return true if word tokens goes first, before data tokens
     */
    public boolean getWordTokenFirst() {
        return wordTokenFirst;
    }

}
