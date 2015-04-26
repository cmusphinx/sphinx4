/*
* Copyright 1999-2015 Carnegie Mellon University.
* All Rights Reserved.  Use is subject to license terms.
*
* See the file "license.terms" for information on usage and
* redistribution of this file, and for a DISCLAIMER OF ALL
* WARRANTIES.
*/

package edu.cmu.sphinx.result;

import java.util.Locale;

import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.TimeFrame;

/**
 * Represents a word in a recognition result.
 *
 * This is designed specifically for obtaining confidence scores.
 * All scores are maintained in LogMath log base.
 */
public class WordResult {

    private final Word word;
    private final TimeFrame timeFrame;
    private final double score;
    private final double confidence;

    /**
     * Construct a word result with full information.
     *
     * @param w the word object to store
     * @param timeFrame time frame
     * @param score acoustic score of the word
     * @param posterior of the word
     */
    public WordResult(Word w, TimeFrame timeFrame, double score, double posterior)
    {
        this.word = w;
        this.timeFrame = timeFrame;
        this.score = score;
        this.confidence = posterior;
    }

    /**
     * Construct a WordResult using a Node object and a confidence (posterior).
     *
     * This does not use the posterior stored in the Node object, just its
     * word, start and end.
     *
     * @param node the node to extract information from
     */
    public WordResult(Node node) {
        this(node.getWord(),
             new TimeFrame(node.getBeginTime(), node.getEndTime()), 
             node.getViterbiScore(), node.getPosterior());
    }

    /**
     * @return total score for this WirdResult in log domain
     */
    public double getScore() {
        return score;
    }

    /**
     * Returns a log confidence score for this WordResult.
     *
     * Use the getLogMath().logToLinear() method to convert the log confidence
     * score to linear. The linear value should be between 0.0 and 1.0
     * (inclusive) for this word.
     *
     * @return a log confidence score which linear value is in [0, 1]
     */
    public double getConfidence() {
        // TODO: can confidence really be greater than 1?
        return Math.min(confidence, LogMath.LOG_ONE);
    }

    /**
     * Gets the pronunciation for this word.
     *
     * @return the pronunciation for the word
     */
    public Pronunciation getPronunciation() {
        return word.getMostLikelyPronunciation();
    }

    /**
     * Gets the word object associated with the given result.
     *
     * @return the word object
     */
    public Word getWord() {
        return word;
    }

    /**
     * @return time frame for the word
     */
    public TimeFrame getTimeFrame() {
        return timeFrame;
    }

    /**
     * Does this word result represent a filler token?
     *
     * @return true if this is a filler
     */
    public boolean isFiller() {
        return word.isFiller() || word.toString().equals("<skip>");
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "{%s, %.3f, [%s]}", word, LogMath.getLogMath().logToLinear((float)getConfidence()), timeFrame);
    }
}
