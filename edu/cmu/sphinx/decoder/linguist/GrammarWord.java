
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

package edu.cmu.sphinx.decoder.linguist;
import edu.cmu.sphinx.knowledge.dictionary.Pronunciation;
import edu.cmu.sphinx.knowledge.dictionary.Dictionary;

import java.io.Serializable;

/**
 * Represents a word in a grammar. 
 *
 * All word probabilities are in the LogMath log domain
 */
public class GrammarWord implements Serializable {
    private Pronunciation[] pronunciations;
    private String spelling;

    /**
     * Creates a GrammarWord with the given set of pronunciations
     *
     * @param spelling the spelling for this word
     * @param pronunciations the set of pronunciations associated with
     * this GrammarWord
     */
    public GrammarWord(String spelling, Pronunciation[] pronunciations) {
	this.spelling = spelling;
	this.pronunciations = pronunciations;
    }

    /**
     * Retrieves the pronunciations associated with this GrammarWord
     *
     * @return the pronunciations
     */

    public Pronunciation[] getPronunciations() {
	return pronunciations;
    }


    /**
     * Retrieves the spelling for this word
     *
     * @return the spelling for this word
     */
    public String getSpelling() {
	return spelling;
    }


    /**
     * Returns true if this word represents a sentence start
     *
     * @return <code>true</code> if the word represents a sentence
     * start
     */
    public boolean isSentenceStart() {
        return spelling.equals(Dictionary.SENTENCE_START_SPELLING);
    }

    /**
     * Returns true if this word represents a sentence End
     *
     * @return <code>true</code> if the word represents a sentence
     * start
     */
    public boolean isSentenceEnd() {
        return spelling.equals(Dictionary.SENTENCE_END_SPELLING);
    }


    /**
     * Returns the log probability for this word in the grammar.  Certain
     * grammars may allow various proabilities for different words.
     * This method will retrieve this probability.  Currently this
     * feature is not supported so the probability returned is always
     * 0.0.
     *
     * @return the log probability for this word
     */
    // TODO: fix me
    public float getProbability() {
	return 0.0f;
    }

    /**
     * Gets a string representation of this object
     *
     * @return the string representation
     */
    public String toString() {
	return spelling;
    }

    /**
     * Determines if this word is a 'silence' word
     *
     * @return true if the word is a silence
     */
    public boolean isSilence() {
	return spelling.equals(Dictionary.SILENCE_SPELLING) ||
            isSentenceEnd() || isSentenceEnd();
    }
}
