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
 * Represents a word ID (second word of a bigram), a bigram probability ID,
 * a backoff probability ID, and the location of the first trigram entry.
 */
class BigramProbability {

    private int wordID;
    private int probabilityID;
    private int backoffID;
    private int firstTrigramEntry;
    

    /**
     * Constructs a BigramProbability
     *
     * @param wordID the ID of the second word in a bigram
     * @param probabilityID the index into the probability array
     * @param backoffID the index into the backoff probability array
     * @param firstTrigramEntry the first trigram entry
     */
    public BigramProbability(int wordID, int probabilityID, int backoffID,
                             int firstTrigramEntry) {
        this.wordID = wordID;
        this.probabilityID = probabilityID;
        this.backoffID = backoffID;
        this.firstTrigramEntry = firstTrigramEntry;
    }


    public int getWordID() {
        return wordID;
    }


    public int getProbabilityID() {
        return probabilityID;
    }


    public int getBackoffID() {
        return backoffID;
    }


    public int getFirstTrigramEntry() {
        return firstTrigramEntry;
    }
}
