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

import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.knowledge.acoustic.Context;
import edu.cmu.sphinx.util.ExtendedStreamTokenizer;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StreamFactory;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.knowledge.dictionary.Dictionary;

import java.io.InputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * This is a simple dictionary that reads in the sphinx3 format
 * dictionary. In this dictionary there is only one possible
 * pronunciation per word.
 */
public class FullDictionary implements Dictionary {

    private Map dictionary = new HashMap();
    private Map unitCache = new HashMap();
    private boolean addSilEndingPronunciation;
    private String wordReplacement;
    private boolean allowMissingWords;


    /**
     * Constructs a FullDictionary using the given context.
     * The context will give you the SphinxProperties that tells you
     * where the word and filler dictionaries are by the following
     * properties: <pre>
     * edu.cmu.sphinx.knowledge.dictionary.Dictionary.dictionaryPath
     * edu.cmu.sphinx.knowledge.dictionary.Dictionary.fillerDictionaryPath </pre>
     *
     * @param context the context of this FullDictionary
     *
     * @throws java.lang.IllegalArgumentException if the given context
     *    does not contain a path to the dictionary
     *
     * @throws java.io.IOException if there is an error reading the dictionary
     */
    public FullDictionary(String context) throws IllegalArgumentException,
            IOException {

        SphinxProperties properties = SphinxProperties.getSphinxProperties
            (context);

        boolean useAMLocation = properties.getBoolean
            (Dictionary.PROP_USE_AM_LOCATION, true);

        String location = null;
        if (useAMLocation) {
            location = properties.getString(AcousticModel.PROP_LOCATION, null);
        }

        String wordDictionaryFile = properties.getString
            (Dictionary.PROP_DICTIONARY, null);

        String fillerDictionaryFile = properties.getString
            (Dictionary.PROP_FILLER_DICTIONARY, null);

        addSilEndingPronunciation = properties.getBoolean
            (Dictionary.PROP_ADD_SIL_ENDING_PRONUNCIATION, false);

        wordReplacement = properties.getString
            (Dictionary.PROP_WORD_REPLACEMENT, null);

        allowMissingWords = properties.getBoolean
            (Dictionary.PROP_ALLOW_MISSING_WORDS, false);

        if (wordDictionaryFile == null) {
            throw new IllegalArgumentException
                ("Context \"" + context + "\" does not contain " +
                 "the property: " + PROP_DICTIONARY);
        }
        if (fillerDictionaryFile == null) {
            throw new IllegalArgumentException
                ("Context \"" + context + "\" does not contain " +
                 "the property: " + PROP_FILLER_DICTIONARY);
        }

	Timer loadTimer = Timer.getTimer(context, "DictionaryLoad");

	loadTimer.start();

        // NOTE: "location" can be null here, in which case the 
        // "wordDictionaryFile" and "fillerDictionaryFile" should
        // contain the full path to the Dictionaries.

        loadDictionary(StreamFactory.getInputStream
                       (location, wordDictionaryFile), false);
        loadDictionary(StreamFactory.getInputStream
                       (location, fillerDictionaryFile), true);
	loadTimer.stop();

        unitCache.put(Unit.SILENCE.getName(), Unit.SILENCE);
    }
    

    /**
     * Loads the given sphinx3 style simple dictionary from 
     * the given InputStream. The InputStream is assumed to contain
     * ASCII data.
     *
     * @param inputStream the InputStream of the dictionary
     * @param path the path to load the dictionary from
     * @param isFillerDict true if this is a filler dictionary, 
     *    false otherwise
     *
     * @throws java.io.IOException if there is an error reading the dictionary
     */
    private void loadDictionary(InputStream inputStream,
                                boolean isFillerDict) throws
            IOException {

        ExtendedStreamTokenizer est = new ExtendedStreamTokenizer
            (inputStream, true);
        String word;

        while ((word = est.getString()) != null) {

            word = removeParensFromWord(word);
	    word = word.toLowerCase();

            List units = new ArrayList(20);
            String unitText;

            while ((unitText = est.getString()) != null) {
                units.add(getCIUnit(unitText, isFillerDict));
            }
            
            Unit[] unitsArray = (Unit[]) units.toArray
                (new Unit[units.size()]);
            Pronunciation pronunciation = new Pronunciation
                (word, unitsArray, null, null, 1.0f);

            List pronunciations = (List) dictionary.get(word);
            if (pronunciations == null) {
                pronunciations = new LinkedList();
            }
            pronunciations.add(pronunciation);
            
            // if we are adding a SIL ending duplicate
            if (!isFillerDict && addSilEndingPronunciation) {
                units.add(Unit.SILENCE);
                Unit[] unitsArray2 = (Unit[]) units.toArray
                    (new Unit[units.size()]);
                Pronunciation pronunciation2 = new Pronunciation
                    (word, unitsArray2, null, null, 1.0f);
                pronunciations.add(pronunciation2);
            }

            dictionary.put(word, pronunciations);
        }
        est.close();
    }

    /**
     * Gets  a context independent unit.  There should only be one
     * instance of any CI unit
     *
     * @param name the name of the unit
     * @param isFiller if true, the unit is a filler unit
     *
     * @return the unit
     *
     */
    private Unit getCIUnit(String name, boolean isFiller) {
	Unit unit = (Unit) unitCache.get(name);
	if (unit == null) {
	    unit = new Unit(name, isFiller, Context.EMPTY_CONTEXT);
	    unitCache.put(name, unit);
	}
	return unit;
    }


    /**
     * Returns a new string that is the given word but with the ending
     * parenthesis removed.
     * <p>Example:
     * <pre>
     *       "LEAD(2)" returns "LEAD"
     *       "LEAD" returns "LEAD"
     *
     * @param word the word to be stripped
     *
     * @return the given word but with all characters from the first
     *     open parentheses removed
     */
    private String removeParensFromWord(String word) {
        if (word.charAt(word.length() - 1) == ')') {
            int index = word.lastIndexOf('(');
            if (index > 0) {
                word = word.substring(0, index);
            }
        }
        return word;
    }


    /**
     * Retrieves the pronunciations for a particular word based upon
     * its classification.
     *
     * @param text the spelling of the word of interest.
     * @param wordClassification the classification of the word
     * (typically part of speech classification)  or null if all word
     * classifications are acceptable. The word classification must be
     * one of the set returned by <code>
     * getPossibleWordClassifications </code>
     *
     * @return an array of zero or more
     * <code>Pronunciation</code> objects.
     *
     * @see edu.cmu.sphinx.knowledge.dictionary.Pronunciation
     */
    public Pronunciation[] getPronunciations(String text,
            WordClassification wordClassification) {
        return getPronunciations(text, wordClassification, null);
    }


    /**
     * Retrieves the pronunciations for a particular word based upon
     * its classification and tag.
     *
     * @param text the spelling of the word of interest.
     * @param wordClassification the classification of the word
     * (typically part of speech classification)  or null if all word
     * classifications are acceptable. The word classification must be
     * one of the set returned by <code>
     * getPossibleWordClassifications </code>
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
        
	text = text.toLowerCase();
        List pronounceList = (List) dictionary.get(text);
        
	// not found in the dictionary, return an empty list

	// BUG: need to deal with providing pronunciations for words
	// that are not in the dictionary. For now, we will just
	// return null, which will almost certainly cause a NPE at a
	// higher level.


	if (pronounceList == null && wordReplacement != null) {
	    pronounceList = (List) dictionary.get(wordReplacement);
	    System.out.println("Replacing " + text + " with " +
		    wordReplacement);
	    if (pronounceList == null) {
		System.out.println("Replacement word " +
			wordReplacement + " not found!");
	    }
	} else if (pronounceList == null && allowMissingWords) {
	    System.out.println("Missing word: " + text);
	    return null;
	} 
	
	if (pronounceList == null) {
	    throw new Error("Can't find pronunciation for '" + text
		    + "' in dictionary "  );
	}

        Pronunciation[] pronunciations =
            new Pronunciation[pronounceList.size()];

        // populate the Pronunications array
        int p = 0;
        for (Iterator i = pronounceList.iterator(); i.hasNext(); p++) {
            pronunciations[p] = (Pronunciation) i.next();
        }

        return pronunciations;
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
     * Returns a string representation of this FullDictionary
     * in alphabetical order.
     *
     * @return a string representation of this FullDictionary
     */
    public String toString() {

        SortedMap sorted = new TreeMap(dictionary);
        String result = "";

        for (Iterator i = sorted.keySet().iterator(); i.hasNext();) {
            String word = (String) i.next();
            List pronunciations = (List) sorted.get(word);
            result += (word + "\n");
            for (Iterator p = pronunciations.iterator(); p.hasNext();) {
                Pronunciation pronunciation = (Pronunciation) p.next();
                result += ("   " + pronunciation.toString() + "\n");
            }
        }

        return result;
    }


    /**
     * Dumps this FullDictionary to System.out.
     */
    public void dump() {
        System.out.print(toString());
    }
}

