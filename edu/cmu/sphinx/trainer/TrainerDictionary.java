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

package edu.cmu.sphinx.trainer;

import edu.cmu.sphinx.knowledge.dictionary.*;
import java.io.IOException;

/**
 * Dummy trainer dictionary.
 */
public class TrainerDictionary implements Dictionary {

    static private Dictionary dictionary;
    static String context = "nada";

    static final String UTTERANCE_BEGIN_SYMBOL = "<s>";
    static final String UTTERANCE_END_SYMBOL = "</s>";
    static final String SILENCE_SYMBOL = "SIL";

    static public Dictionary getDictionary() {
	try {
	    dictionary = new FullDictionary(context);
	} catch (IllegalArgumentException iae) {
	    System.out.println("IAE " + iae);
	} catch (IOException ie) {
	    System.out.println("IE " + ie);
	}
	return dictionary;
    }

    // Below here, dummy implementations.
    // We want it to compile :-).
    /**
     * Retrieves the pronunciations for a particular word based upon
     * its classification.
     *
     * @param text the spelling of the word of interest.
     *
     * @param wordClassification the classification of the word
     * (typically part of speech classification) or null if all word
     * classifications are acceptable. The word classification must be
     * one of the set returned by <code>getPossibleWordClassifications</code>
     *
     * @return an array of zero or more
     * <code>Pronunciation</code> objects.
     *
     * @see edu.cmu.sphinx.knowledge.dictionary.Pronunciation
     */
    public Pronunciation[] getPronunciations(String text,
				     WordClassification wordClassification){
	return null;
    }

    /**
     * Retrieves the pronunciations for a particular word based upon
     * its classification and tag.
     *
     * @param text the spelling of the word of interest.
     * @param wordClassification the classification of the word
     * (typically part of speech classification)  or null if all word
     * classifications are acceptable. The word classification must be
     * one of the set returned by <code>getPossibleWordClassifications</code>
     * @param tag a tag used to distinguish one homograph from
     * another.
     *
     * @return an array of zero or more
     * <code>Pronunciation</code> objects.
     *
     * @see edu.cmu.sphinx.knowledge.dictionary.Pronunciation
     */
    public Pronunciation[] getPronunciations(String text,
		     WordClassification wordClassification, String tag) {
	return null;
    }

    /**
     * Returns the set of all possible word classifications for this
     * dictionary.
     *
     * @return the set of all possible word classifications
     */
    public WordClassification[] getPossibleWordClassifications() {
	return null;
    }



    /**
     * Dumps out a dictionary
     *
     */
    public void dump() {
    }

    /**
     * Prints out dictionary as a string.
     */
    public String toString() {
	return "DEFAULT";
    }
}
