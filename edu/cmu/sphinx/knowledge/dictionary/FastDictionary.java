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
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedReader;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.StringTokenizer;


/**
 * This is a simple dictionary that reads in the sphinx3 format
 * dictionary. In this dictionary there is only one possible
 * pronunciation per word. This is called the FastDictionary
 * because the loading is fast. When loaded the dictionary just
 * loads each line of the dictionary into the hash table, assuming
 * that most words are not going to be used. Only when a word is
 * actually used is its pronunciations massaged into an array of
 * pronunciations. 
 */
public class FastDictionary implements Dictionary {

    /**
     * The logger for this class
     */
    private static Logger logger =
        Logger.getLogger("edu.cmu.sphinx.knowledge.dictionary.FastDictionary");

    private Map dictionary = new HashMap();
    private boolean addSilEndingPronunciation;
    private boolean allowMissingWords;
    private String wordReplacement;
    private int wordID;
    private final static String FILLER_TAG=  "-F-";


    /**
     * Constructs a FastDictionary using the given context.
     * The context will give you the SphinxProperties that tells you
     * where the word and filler dictionaries are by the following
     * properties: <pre>
     * edu.cmu.sphinx.knowledge.dictionary.Dictionary.dictionaryPath
     * edu.cmu.sphinx.knowledge.dictionary.Dictionary.fillerDictionaryPath 
     * </pre>
     *
     * @param context the context of this FastDictionary
     *
     * @throws java.lang.IllegalArgumentException if the given context
     *    does not contain a path to the dictionary
     *
     * @throws java.io.IOException if there is an error reading the dictionary
     */
    public FastDictionary(String context) throws IllegalArgumentException,
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

	if (wordReplacement != null) {
	    wordReplacement = wordReplacement.toLowerCase();
	}

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

	logger.info("Loading dictionary from: ");
        logger.info(location + "/" + wordDictionaryFile);
        
        loadDictionary
            (StreamFactory.getInputStream(location, wordDictionaryFile), 
             false);
	
        logger.info("Loading filler dictionary from: ");
        logger.info(location + "/" + fillerDictionaryFile);
        
        loadDictionary
            (StreamFactory.getInputStream(location, fillerDictionaryFile), 
             true);
	
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
	InputStreamReader isr = new InputStreamReader(inputStream);
	BufferedReader br = new BufferedReader(isr);
	String line;

	while ((line = br.readLine()) != null) {
	    int spaceIndex = line.indexOf(' ');
	    int spaceIndexTab = line.indexOf('\t');
	    if (spaceIndex == -1) {
		// Case where there's no blank character
		spaceIndex = spaceIndexTab;
	    } else if ((spaceIndexTab >= 0) && (spaceIndexTab < spaceIndex)) {
		// Case where there's a blank and a tab, but the tab
		// precedes the blank
		spaceIndex = spaceIndexTab;
	    }
	    // TODO: throw an exception if spaceIndex == -1 ?
	    String word = line.substring(0, spaceIndex);
	    word = word.toLowerCase();
            int wordID = getNextWordID(word);
	    if (isFillerDict) {
		dictionary.put(word, (wordID + " " + FILLER_TAG + line));
	    } else {
		dictionary.put(word, (wordID + " " + line));
	    }
	}

	br.close();
	isr.close();
    }

    /**
     * Returns a word ID given a word.
     *
     * @return a suitable ID for the given word
     */
    private int getNextWordID(String word) {
        String line = (String)dictionary.get(word);
        if (line == null) {
            return wordID++;
        } else {
            int spaceIndex = line.indexOf(" ");
            String idString = line.substring(0, spaceIndex);
            return Integer.parseInt(idString);
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
     * Returns the ID of the given word.
     *
     * @return the ID of the given word, or UNKNOWN_WORD_ID if the
     *    word is not in the dictionary
     */
    public int getWordID(String word) {
        if (word.equals(SENTENCE_START_SPELLING)) {
            return SENTENCE_START_ID;
        } else if (word.equals(SENTENCE_END_SPELLING)) {
            return SENTENCE_END_ID;
        } else if (word.equals(SILENCE_SPELLING)) {
            return SILENCE_ID;
        }
        Pronunciation[] prons = getPronunciations(word, null);
        if (prons == null) {
            return UNKNOWN_WORD_ID;
        } else {
            return prons[0].getWordID();
        }
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
        
	Pronunciation[] pronunciations = null;
	text = text.toLowerCase();

	Object object = dictionary.get(text);

	if (object == null) {  // deal with 'not found' case
	    if (wordReplacement != null) {
		return getPronunciations(wordReplacement,
			wordClassification, tag);
	    } else if (allowMissingWords) {
		System.out.println("Missing word: " + text);
		return null;
	    }
	} else if (object instanceof String) { // first lookup for this string
	    pronunciations = processEntry(text);
	} else if (object instanceof Pronunciation[]) {
	    pronunciations = (Pronunciation[]) object;
	}
	return pronunciations;
    }


    /**
     * Processes a dictionary entry. When loaded the dictionary just
     * loads each line of the dictionary into the hash table, assuming
     * that most words are not going to be used. Only when a word is
     * actually used is its pronunciations massaged into an array of
     * pronunciations.
     */
    private Pronunciation[] processEntry(String word) {
	List pList = new LinkedList();
	String line = null;
	int count = 0;

	do {
	    count++;
	    String lookupWord = word;
	    if (count > 1) {
		lookupWord = lookupWord + "(" + count + ")";
	    } 
	    line = (String) dictionary.get(lookupWord);
	    if (line != null) {
		StringTokenizer st = new StringTokenizer(line);
                int wordID = Integer.parseInt(st.nextToken());
		String tag = st.nextToken();
		boolean isFiller = tag.startsWith(FILLER_TAG);
		int unitCount = st.countTokens();
		dictionary.remove(lookupWord);

		Unit[] units = new Unit[unitCount];
		for (int i = 0; i < units.length; i++) {
		    String unitName = st.nextToken();
		    units[i] = getCIUnit(unitName, isFiller);
		}

		if (!isFiller && addSilEndingPronunciation) {
		    Unit[] silUnits = new Unit[unitCount + 1];
		    System.arraycopy(units, 0, silUnits, 0, unitCount);
		    silUnits[unitCount] = Unit.SILENCE;
                    units = silUnits;
		}
                pList.add
                    (new Pronunciation(word, wordID, units, null, null, 1.f));
            }
	} while (line != null);

        Pronunciation[] pronunciations = new Pronunciation[pList.size()];
        pList.toArray(pronunciations);

	dictionary.put(word, pronunciations);
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
     * Returns a string representation of this FastDictionary
     * in alphabetical order.
     *
     * @return a string representation of this FastDictionary
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
     * Dumps this FastDictionary to System.out.
     */
    public void dump() {
        System.out.print(toString());
    }

}

