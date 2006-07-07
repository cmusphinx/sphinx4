
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

package edu.cmu.sphinx.decoder;

/**
 * Provides summary results for a batch run of the decoder
 *
 */
public class BatchResults {

    private int numWords;
    private int numSentences;
    private int subErrors;
    private int insErrors;
    private int delErrors;
    private int sentenceErrors;


    /**
     * Creates a new BatchResults with the given parameters
     *
     * @param numWords the total number of reference words
     * @param numSentences the total number of sentences recognized
     * @param subErrors the number of substitution errors
     * @param insErrors the number of insertion errors
     * @param delErrors the number of deletion errors
     * @param sentenceErrors the number of sentence errors
     */
    public BatchResults(int numWords,
                 int numSentences,
                 int subErrors,
                 int insErrors,
                 int delErrors,
                 int sentenceErrors) {
        this.numWords = numWords;
        this.numSentences = numSentences;
        this.subErrors = subErrors;
        this.insErrors = insErrors;
        this.delErrors = delErrors;
        this.sentenceErrors = sentenceErrors;
    }


    /**
     * Gets the word error rate
     *
     *  return the word error rate as a percentage
     */
    public float getWER() {
        if (getNumWords() == 0) {
            return 0.0f;
        } else {
            return (getTotalErrors() / getNumWords()) * 100.0f;
        }
    }


    /**
     * Gets the number of reference words
     *
     * @return the number of reference words
     */
    public int getNumWords() {
        return numWords;
    }

    /**
     * Gets the number of reference sentences
     *
     * @return the number of reference sentences
     */
    public int getNumSentences() {
        return numSentences;
    }

    /**
     * Gets the number of substitution errors
     *
     * @return the number of substitution errors
     */
    public int getSubErrors() {
        return subErrors;
    }

    /**
     * Gets the number of insertion errors
     *
     * @return the number of insertion errors
     */
    public int getInsErrors() {
        return insErrors;
    }

    /**
     * Gets the number of deletion errors
     *
     * @return the number of deletion errors
     */
    public int getDelErrors() {
        return delErrors;
    }

    /**
     * Gets the number of sentence errors
     *
     * @return the number of sentence errors
     */
    public int getSentenceErrors() {
        return sentenceErrors;
    }

    /**
     * Gets the total number of errors
     *
     * @return the total number of errors
     */
    public int getTotalErrors() {
        return getInsErrors() + getDelErrors() + getSubErrors();
    }

    /**
     * Outputs a string representation of this batch result
     */
    public String toString() {
        return numWords + " " +
            numSentences + " " +
            getWER() + " " +
            getTotalErrors() + " " +
            getSubErrors() + " " +
            getInsErrors() + " " +
            getDelErrors() + " " +
            getSentenceErrors();
    }
}

