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
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * This is a simple dictionary that reads in the sphinx3 format
 * dictionary. In this dictionary there is only one possible
 * pronunciation per word.
 */
public class FullDictionary implements Dictionary {

    private Map dictionary = new HashMap();
    private boolean addSilEndingPronunciation;
    private boolean allowMissingWords;
    private String wordReplacement;

    /**
     * Constructs a FullDictionary using the given context.
     * The context will give you the SphinxProperties that tells you
     * where the word and filler dictionaries are by the following
     * properties: <pre>
     * edu.cmu.sphinx.knowledge.dictionary.Dictionary.dictionaryPath
     * edu.cmu.sphinx.knowledge.dictionary.Dictionary.fillerDictionaryPath
     * </pre>
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
            (Dictionary.PROP_USE_AM_LOCATION,
	     Dictionary.PROP_USE_AM_LOCATION_DEFAULT);

        String location = null;
        if (useAMLocation) {
            location = properties.getString(AcousticModel.PROP_LOCATION, null);
        }

        String wordDictionaryFile = properties.getString
            (Dictionary.PROP_DICTIONARY, Dictionary.PROP_DICTIONARY_DEFAULT);

        String fillerDictionaryFile = properties.getString
            (Dictionary.PROP_FILLER_DICTIONARY,
	     Dictionary.PROP_FILLER_DICTIONARY_DEFAULT);

        addSilEndingPronunciation = properties.getBoolean
            (Dictionary.PROP_ADD_SIL_ENDING_PRONUNCIATION,
	     Dictionary.PROP_ADD_SIL_ENDING_PRONUNCIATION_DEFAULT);

        wordReplacement = properties.getString
            (Dictionary.PROP_WORD_REPLACEMENT,
	     Dictionary.PROP_WORD_REPLACEMENT_DEFAULT);

        allowMissingWords = properties.getBoolean
            (Dictionary.PROP_ALLOW_MISSING_WORDS,
	     Dictionary.PROP_ALLOW_MISSING_WORDS_DEFAULT);

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
        createWords();
	loadTimer.stop();
    }
    

    /**
     * Loads the given sphinx3 style simple dictionary from 
     * the given InputStream. The InputStream is assumed to contain
     * ASCII data.
     *
     * @param inputStream the InputStream of the dictionary
     * @param isFillerDict true if this is a filler dictionary, 
     *    false otherwise
     *
     * @throws java.io.IOException if there is an error reading the dictionary
     */
    private void loadDictionary(InputStream inputStream,
                                boolean isFillerDict) throws IOException {

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
            
            List pronunciations = (List) dictionary.get(word);

            if (pronunciations == null) {
                pronunciations = new LinkedList();
            }

            Pronunciation pronunciation = new Pronunciation
                (unitsArray, null, null, 1.0f);

            pronunciations.add(pronunciation);
            
            // if we are adding a SIL ending duplicate
            if (!isFillerDict && addSilEndingPronunciation) {
                units.add(Unit.SILENCE);
                Unit[] unitsArray2 = (Unit[]) units.toArray
                    (new Unit[units.size()]);
                Pronunciation pronunciation2 = new Pronunciation
                    (unitsArray2, null, null, 1.0f);
                pronunciations.add(pronunciation2);
            }

            dictionary.put(word, pronunciations);
        }
        inputStream.close();
        est.close();
    }


    /**
     * Converts the spelling/Pronunciations mappings in the dictionary
     * into spelling/Word mappings.
     */
    private void createWords() {
        Set spellings = dictionary.keySet();
        for (Iterator s = spellings.iterator(); s.hasNext();) {
            String spelling = (String) s.next();
            List pronunciations = (List) dictionary.get(spelling);
            Pronunciation[] pros = new Pronunciation[pronunciations.size()];
            for (int i = 0; i < pros.length; i++) {
                pros[i] = (Pronunciation) pronunciations.get(i);
            }
            Word word = new Word(spelling, pros);
            for (int i = 0; i < pros.length; i++) {
                pros[i].setWord(word);
            }
            dictionary.put(spelling, word);
        }
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
        return Unit.getUnit(name, isFiller, Context.EMPTY_CONTEXT);
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
     * Returns a Word object based on the spelling and its classification.
     *
     * @param text the spelling of the word of interest.
     *
     * @return a Word object
     *
     * @see edu.cmu.sphinx.knowledge.dictionary.Word
     */
    public Word getWord(String text) {
        
        text = text.toLowerCase();
        Word word = (Word) dictionary.get(text);
        
        if (word == null) {
            if (wordReplacement != null) {
                word = (Word) dictionary.get(wordReplacement);
                System.out.println("Replacing " + text + " with " +
                                   wordReplacement);
                if (word == null) {
                    System.out.println("Replacement word " +
                                       wordReplacement + " not found!");
                }
            } else if (allowMissingWords) {
                System.out.println("FullDictionary: Missing word: " + text);
                return null;
            }
        }
        return word;
    }

    /**
     * Returns the sentence start word.
     *
     * @return the sentence start word
     */
    public Word getSentenceStartWord() {
        return getWord(SENTENCE_START_SPELLING);
    }

    /**
     * Returns the sentence end word.
     *
     * @return the sentence end word
     */
    public Word getSentenceEndWord() {
        return getWord(SENTENCE_END_SPELLING);
    }

    /**
     * Returns the silence word.
     *
     * @return the silence word
     */
    public Word getSilenceWord() {
        return getWord(SILENCE_SPELLING);
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
            Word word = (Word) i.next();
            Pronunciation[] pronunciations = word.getPronunciations(null);
            result += (word + "\n");
            for (int p = 0; p < pronunciations.length; p++) {
                result += ("   " + pronunciations[p].toString() + "\n");
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

