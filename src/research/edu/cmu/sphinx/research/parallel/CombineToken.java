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

package edu.cmu.sphinx.research.parallel;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.linguist.flat.SentenceHMMState;

import java.util.*;


/**
 * A token for the sentence HMM states that are not feature stream-specific. The <code>getScore()</code> method returns
 * the combined score. A combined token carries a parallel token for each feature stream, so that scores pertaining to
 * each stream can be propagated.
 */
public class CombineToken extends Token implements Iterable<ParallelToken> {

    private Map<Object, ParallelToken> tokens;     // the list of parallel tokens


    /**
     * Constructs a CombineToken
     *
     * @param predecessor the predecessor for this token
     * @param state       the SentenceHMMState associated with this token
     * @param frameNumber the frame number associated with this token
     */
    public CombineToken(Token predecessor,
                        SentenceHMMState state,
                        int frameNumber) {
        super(predecessor, state, 0.0f, 0.0f, 0.0f, frameNumber);
        this.tokens = new HashMap<Object, ParallelToken>();
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
     * @param key           the stream name
     * @param parallelToken the the parallel token
     */
    public ParallelToken addParallelToken(Object key,
                                          ParallelToken parallelToken) {
        return tokens.put(key, parallelToken);
    }


    /**
     * Adds all the ParallelTokens in the given list into this CombineToken. The added tokens are keyed by their model
     * name.
     *
     * @param tokenList the list of ParallelTokens
     */
    public void addAll(List<ParallelToken> tokenList) {
        for (ParallelToken token : tokenList)
            tokens.put(token.getFeatureStream(), token);
    }


    /** Removes all the ParallelTokens from this CombineToken. */
    public void clear() {
        tokens.clear();
    }


    /**
     * Returns an Iterator for the ParallelToken(s).
     *
     * @return an Iterator for the ParallelToken(s)
     */
    @Override
    public Iterator<ParallelToken> iterator() {
        return tokens.values().iterator();
    }


    /**
     * Returns a Collection of all the parallel tokens.
     *
     * @return a Collection of all the parallel tokens
     */
    public Collection<ParallelToken> getParallelTokens() {
        return tokens.values();
    }


    /**
     * Returns the parallel token of the given parallel stream.
     *
     * @return the parallel token of the given parallel stream
     */
    public ParallelToken getParallelToken(FeatureStream stream) {
        return tokens.get(stream);
    }


    /**
     * Sets the last combine time of all the ParallelTokens in this CombineToken.
     *
     * @param frameNumber the last combine time
     */
    public void setLastCombineTime(int frameNumber) {
        for (ParallelToken pToken : this)
            pToken.setLastCombineTime(frameNumber);
    }


    /**
     * Returns the string representation of this object.
     *
     * @return the string representation of this object
     */
    public String toString() {
        StringBuilder parallelTokenScores = new StringBuilder("CombinedToken: ")
            .append("Frame: ").append(getNumberFormat().format(getFrameNumber()))
            .append(", score: ").append(getScoreFormat().format(getScore())).append('\n');
        int t = 0;
        for (ParallelToken token : this) {
            parallelTokenScores.append("   ParallelToken ").append(t).append(", ").append(token.getModelName())
                .append(", feature: ").append(getScoreFormat().format(token.getFeatureScore()))
                .append(", combined: ").append(getScoreFormat().format(token.getCombinedScore())).append('\n');
        }
        return parallelTokenScores.toString();
    }
}
