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

package edu.cmu.sphinx.knowledge.language.large;


/**
 * Represents a probability, a backoff probability, and the location
 * of the first bigram entry.
 */
class UnigramProbability {

    private float logProbability;
    private float logBackoff;
    private int firstBigramEntry;

    /**
     * Constructs a UnigramProbability
     *
     * @param probability the probability
     * @param backoff the backoff probability
     * @param firstBigramEntry the first bigram entry
     */
    public UnigramProbability(float logProbability, float logBackoff, 
                       int firstBigramEntry) {
        this.logProbability = logProbability;
        this.logBackoff = logBackoff;
        this.firstBigramEntry = firstBigramEntry;
    }

    /**
     * Returns a string representation of this object
     *
     * @return the string form of this object
     */
    public String toString() {
        return "Prob: " + logProbability + " " + logBackoff;
    }


    /**
     * Returns the log probability of this unigram.
     *
     * @return the log probability of this unigram
     */
    public float getLogProbability() {
        return logProbability;
    }


    /**
     * Returns the log backoff weight of this unigram
     *
     * @return the log backoff weight of this unigram
     */
    public float getLogBackoff() {
        return logBackoff;
    }


    /**
     * Returns the index of the first bigram entry of this unigram.
     *
     * @return the index of the first bigram entry of this unigram
     */
    public int getFirstBigramEntry() {
        return firstBigramEntry;
    }


    /**
     * Sets the log probability of this unigram
     *
     * @param probability  the new log probability of this unigram
     */
    public void setLogProbability(float probability) {
        logProbability = probability;
    }
}



