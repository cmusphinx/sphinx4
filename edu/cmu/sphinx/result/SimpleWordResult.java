/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 *
 * Created on Aug 31, 2004
 */
 
package edu.cmu.sphinx.result;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.util.LogMath;

/**
 * Represents a single word result with associated scoring and 
 * timing information.
 * 
 * @author pgorniak
 */
public class SimpleWordResult implements WordResult {
    Word word;
    int startFrame;
    int endFrame;
    double score;
    double confidence;
    LogMath logMath;
    
    /**
     * Construct a word result from a string and a confidence score.
     * Note that
     * @param w the word
     * @param confidence the confidence for this word
     */
    public SimpleWordResult(String w, double confidence, LogMath logMath) {
        Pronunciation[] pros = { Pronunciation.UNKNOWN };
        this.word = new Word(w,pros,false);
        this.confidence = confidence;
        this.score = logMath.getLogZero();
        this.logMath = logMath;
    }
    
    /**
     * Construct a word result with full information.
     * 
     * @param w the word object to store
     * @param sf word start time
     * @param ef word end time
     * @param score score of the word
     * @param confidence confidence (posterior) of the word
     */
    public SimpleWordResult(Word w, int sf, int ef, double score, 
                            double confidence, LogMath logMath) {
        this.word = w;
        this.startFrame = sf;
        this.endFrame = ef;
        this.score = score;
        this.confidence = confidence;
        this.logMath = logMath;
    }
    
    /**
     * Construct a WordResult using a Node object and a confidence (posterior).
     * This does not use the posterior stored in the Node object, just its word,
     * start and end.
     * TODO: score is currently set to zero
     * 
     * @param node the node to extract information from
     * @param confidence the confidence (posterior) to assign
     */
    public SimpleWordResult(Node node, double confidence, LogMath logMath) {
        this(node.getWord(), node.getBeginTime(), node.getEndTime(), 
             LogMath.getLogZero(), confidence, logMath);
    }
    
    /**
     * @see edu.cmu.sphinx.result.WordResult#getScore()
     */
    public double getScore() {
        return score;
    }

    /**
     * @see edu.cmu.sphinx.result.WordResult#getConfidence()
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * @see edu.cmu.sphinx.result.WordResult#getLogMath()
     */
    public LogMath getLogMath() {
        return logMath;
    }

    /**
     * @see edu.cmu.sphinx.result.WordResult#getPronunciation()
     */
    public Pronunciation getPronunciation() {
        return word.getMostLikelyPronunciation();
    }

    /**
     * @see edu.cmu.sphinx.result.WordResult#getStartFrame()
     */
    public int getStartFrame() {
        return startFrame;
    }

    /**
     * @see edu.cmu.sphinx.result.WordResult#getEndFrame()
     */
    public int getEndFrame() {
        return endFrame;
    }

    /**
     * Return this WordResult as a string.
     * @return the word stored here as a string
     */
    public String toString() {
        return word.toString();
    }
    
    /**
     * @see edu.cmu.sphinx.result.WordResult#getDataFrames()
     */
    public Data[] getDataFrames() {
        // TODO not yet implemented
        return null;
    }

}
