
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

package edu.cmu.sphinx.knowledge.dictionary;

import java.io.Serializable;

/**
 *  Provides pronunciation information for a word.
 *
 */
public class Word implements Serializable {

    /**
     * The Word representing the unknown word.
     */
    public static final Word UNKNOWN
        = new Word("<unk>", new Pronunciation[0]);

    private String spelling;               // the spelling of the word
    private Pronunciation[] pronunciations; // pronunciations of this word

    /**
     * Creates a Word
     *
     * @param spelling the spelling of this word
     * @param pronunciations the pronunciations of this word
     */
    Word(String spelling, Pronunciation[] pronunciations) {
	this.spelling = spelling;
        this.pronunciations = pronunciations;
    }


    /**
     * Returns the spelling of the word.
     * 
     * @return the spelling of the word
     */
    public String getSpelling() {
	return spelling;
    }

    /**
     * Retrieves the pronunciations of this word
     * 
     * @param wordClassification the classification of the word
     * (typically part of speech classification) or null if all word
     * classifications are acceptable. The word classification must be
     * one of the set returned by 
     * <code>Dictionary.getPossibleWordClassifications</code>
     *
     * @return the pronunciations of this word
     */
    public Pronunciation[] getPronunciations
        (WordClassification wordClassification) {
	return pronunciations;
    }

    /**
     * Retrieves the pronunciations of this word
     * 
     * @return the pronunciations of this word
     */
    public Pronunciation[] getPronunciations() {
	return pronunciations;
    }

    /**
     * Returns a string representation of this word, which is the spelling
     *
     * @return the spelling of this word
     */
    public String toString() {
        return spelling;
    }
}

