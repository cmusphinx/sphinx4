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

package edu.cmu.sphinx.linguist.language.ngram.large;


/**
 * Represents a word ID (second word of a bigram), a bigram probability ID,
 * a backoff probability ID, and the location of the first trigram entry.
 */
class BigramProbability {

    private int which;
    private int wordID;
    private int probabilityID;
    private int backoffID;
    private int firstTrigramEntry;


    /**
     * Constructs a BigramProbability
     *
     * @param which which follower of the first word is this bigram
     * @param wordID the ID of the second word in a bigram
     * @param probabilityID the index into the probability array
     * @param backoffID the index into the backoff probability array
     * @param firstTrigramEntry the first trigram entry
     */
    public BigramProbability(int which, int wordID, 
			     int probabilityID, int backoffID,
                             int firstTrigramEntry) {
        this.which = which;
	this.wordID = wordID;
        this.probabilityID = probabilityID;
        this.backoffID = backoffID;
        this.firstTrigramEntry = firstTrigramEntry;
    }

    
    /**
     * Returns which follower of the first word is this bigram
     *
     * @return which follower of the first word is this bigram
     */
    public int getWhichFollower() {
	return which;
    }


    /**
     * Returns the second word ID of this bigram
     *
     * @return the second word ID
     */ 
    public int getWordID() {
        return wordID;
    }


    /**
     * Returns the bigram probability ID.
     *
     * @return the bigram probability ID
     */
    public int getProbabilityID() {
        return probabilityID;
    }


    /**
     * Returns the backoff weight ID.
     *
     * @return the backoff weight ID
     */
    public int getBackoffID() {
        return backoffID;
    }


    /**
     * Returns the index of the first trigram entry.
     *
     * @return the index of the first trigram entry
     */
    public int getFirstTrigramEntry() {
        return firstTrigramEntry;
    }
}
