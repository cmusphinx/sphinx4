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

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.util.LogMath;


/**
 * Represents a word in a recognition result. This is designed specifically for obtaining confidence scores. All scores
 * are maintained in LogMath log base.
 */
public interface WordResult {

    /**
     * Gets the total score for this word.
     *
     * @return the score for the word (in LogMath log base)
     */
    public double getScore();


    /**
     * Returns a log confidence score for this WordResult. Use the getLogMath().logToLinear() method to convert the log
     * confidence score to linear. The linear value should be between 0.0 and 1.0 (inclusive) for this word.
     *
     * @return a log confidence score which linear value is between 0.0 and 1.0 (inclusive)
     */
    public double getConfidence();


    /**
     * Returns the log math of the scores.
     *
     * @return the log math of the scores
     */
    public LogMath getLogMath();


    /**
     * Gets the pronunciation for this word.
     *
     * @return the pronunciation for the word
     */
    public Pronunciation getPronunciation();


    /**
     * Gets the starting frame number for the word
     *
     * @return the starting frame number for the word
     */
    public int getStartFrame();


    /**
     * Gets the ending frame number for the word
     *
     * @return the ending frame number for the word
     */
    public int getEndFrame();


    /**
     * Gets the feature frames associated with this word
     *
     * @return the set of feature frames associated with this word, or null if the frames are not available.
     */
    public Data[] getDataFrames();


    /** Returns a string representation of this object */
    @Override
    public String toString();


    /**
     * Does this word result represent a filler token?
     *
     * @return true if this is a filler
     */
    public boolean isFiller();
}

