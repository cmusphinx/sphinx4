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
package edu.cmu.sphinx.linguist.dictionary;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import edu.cmu.sphinx.linguist.acoustic.Context;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.util.ExtendedStreamTokenizer;
import edu.cmu.sphinx.util.StreamFactory;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * Creates a dictionary by reading in an ASCII-based Sphinx-3
 * format dictionary. Each line of the dictionary specifies the
 * word, followed by spaces or tab, followed by the pronuncation
 * (by way of the list of phones) of the word. Each word can have
 * more than one pronunciations.
 * For example, a digits dictionary will look like:
 *
 * <pre>
 * ONE                  HH W AH N
 * ONE(2)               W AH N
 * TWO                  T UW
 * THREE                TH R IY
 * FOUR                 F AO R
 * FIVE                 F AY V
 * SIX                  S IH K S
 * SEVEN                S EH V AH N
 * EIGHT                EY T
 * NINE                 N AY N
 * ZERO                 Z IH R OW
 * ZERO(2)              Z IY R OW
 * OH                   OW
 * </pre>
 * <p>
 * In the above example, the words "one" and "zero" have two pronunciations
 * each.
 * <p>
 * This dictionary will read in all the words and its pronunciation(s)
 * at startup. Therefore, if the dictionary is big, it will take longer
 * to load and will consume more memory.
 */
public class FullDictionary implements Dictionary {
    // ----------------------------------
    // configuration variables
    // ----------------------------------
    private String name;
    private  Logger logger;
    private boolean addSilEndingPronunciation;
    private boolean allowMissingWords;
    private boolean createMissingWords;
    private String wordReplacement;
    private URL wordDictionaryFile;
    private URL fillerDictionaryFile;
    private boolean allocated = false;
    private UnitManager unitManager;
    
    
    private Map wordDictionary;
    private Map fillerDictionary;
    private Timer loadTimer;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        this.name = name;
        registry.register(PROP_DICTIONARY, PropertyType.RESOURCE);
        registry.register(PROP_FILLER_DICTIONARY, PropertyType.RESOURCE);
        registry.register(PROP_ADD_SIL_ENDING_PRONUNCIATION, 
                          PropertyType.BOOLEAN);
        registry.register(PROP_WORD_REPLACEMENT, PropertyType.STRING);
        registry.register(PROP_ALLOW_MISSING_WORDS, PropertyType.BOOLEAN);
        registry.register(PROP_CREATE_MISSING_WORDS, PropertyType.BOOLEAN);
        registry.register(PROP_UNIT_MANAGER, PropertyType.COMPONENT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        logger = ps.getLogger();
        wordDictionaryFile = ps.getResource(PROP_DICTIONARY);
        fillerDictionaryFile = ps.getResource(PROP_FILLER_DICTIONARY);
        addSilEndingPronunciation = ps.getBoolean(
                PROP_ADD_SIL_ENDING_PRONUNCIATION,
                PROP_ADD_SIL_ENDING_PRONUNCIATION_DEFAULT);
        wordReplacement = ps.getString(Dictionary.PROP_WORD_REPLACEMENT,
                PROP_WORD_REPLACEMENT_DEFAULT);
        allowMissingWords = ps.getBoolean(Dictionary.PROP_ALLOW_MISSING_WORDS,
                PROP_ALLOW_MISSING_WORDS_DEFAULT);
        createMissingWords = ps.getBoolean(PROP_CREATE_MISSING_WORDS,
                PROP_CREATE_MISSING_WORDS_DEFAULT);
        unitManager = (UnitManager) ps.getComponent(PROP_UNIT_MANAGER,
                UnitManager.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.linguist.dictionary.Dictionary#allocate()
     */
    public void allocate() throws IOException {
        
        if (!allocated) {
            loadTimer = Timer.getTimer("DictionaryLoad");
            loadTimer.start();
            // NOTE: "location" can be null here, in which case the
            // "wordDictionaryFile" and "fillerDictionaryFile" should
            // contain the full path to the Dictionaries.
            logger.info("Loading dictionary from: " + wordDictionaryFile);
            wordDictionary = 
                loadDictionary(wordDictionaryFile.openStream(), false);
            logger.info("Loading filler dictionary from: " + 
                        fillerDictionaryFile);
            fillerDictionary = 
                loadDictionary(fillerDictionaryFile.openStream(), true);
            loadTimer.stop();
            allocated = true;
        }
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.linguist.dictionary.Dictionary#deallocate()
     */
    public void deallocate() {
        if (allocated) {
            fillerDictionary = null;
            wordDictionary = null;
            loadTimer = null;
            allocated = false;
        }
    }
        

    
    /**
     * Loads the given sphinx3 style simple dictionary from the given
     * InputStream. The InputStream is assumed to contain ASCII data.
     * 
     * @param inputStream
     *                the InputStream of the dictionary
     * @param isFillerDict
     *                true if this is a filler dictionary, false otherwise
     * 
     * @throws java.io.IOException
     *                 if there is an error reading the dictionary
     */
    private Map loadDictionary(InputStream inputStream, boolean isFillerDict)
            throws IOException {
        Map dictionary = new HashMap();
        ExtendedStreamTokenizer est = new ExtendedStreamTokenizer(inputStream,
                true);
        String word;
        while ((word = est.getString()) != null) {
            word = removeParensFromWord(word);
            word = word.toLowerCase();
            List units = new ArrayList(20);
            String unitText;
            while ((unitText = est.getString()) != null) {
                units.add(getCIUnit(unitText, isFillerDict));
            }
            Unit[] unitsArray = (Unit[]) units.toArray(new Unit[units.size()]);
            List pronunciations = (List) dictionary.get(word);
            if (pronunciations == null) {
                pronunciations = new LinkedList();
            }
            Pronunciation pronunciation = new Pronunciation(unitsArray, null,
                    null, 1.0f);
            pronunciations.add(pronunciation);
            // if we are adding a SIL ending duplicate
            if (!isFillerDict && addSilEndingPronunciation) {
                units.add(UnitManager.SILENCE);
                Unit[] unitsArray2 = (Unit[]) units.toArray(new Unit[units
                        .size()]);
                Pronunciation pronunciation2 = new Pronunciation(unitsArray2,
                        null, null, 1.0f);
                pronunciations.add(pronunciation2);
            }
            dictionary.put(word, pronunciations);
        }
        inputStream.close();
        est.close();
        createWords(dictionary, isFillerDict);
        return dictionary;
    }

    /**
     * Converts the spelling/Pronunciations mappings in the dictionary into
     * spelling/Word mappings.
     * 
     * @param isFillerDict
     *                if true this is a filler dictionary
     *  
     */
    private void createWords(Map dictionary, boolean isFillerDict) {
        Set spellings = dictionary.keySet();
        for (Iterator s = spellings.iterator(); s.hasNext();) {
            String spelling = (String) s.next();
            List pronunciations = (List) dictionary.get(spelling);
            Pronunciation[] pros = new Pronunciation[pronunciations.size()];
            for (int i = 0; i < pros.length; i++) {
                pros[i] = (Pronunciation) pronunciations.get(i);
            }
            Word word = new Word(spelling, pros, isFillerDict);
            for (int i = 0; i < pros.length; i++) {
                pros[i].setWord(word);
            }
            dictionary.put(spelling, word);
        }
    }

    /**
     * Gets a context independent unit. There should only be one instance of
     * any CI unit
     * 
     * @param name
     *                the name of the unit
     * @param isFiller
     *                if true, the unit is a filler unit
     * 
     * @return the unit
     *  
     */
    private Unit getCIUnit(String name, boolean isFiller) {
        return unitManager.getUnit(name, isFiller, Context.EMPTY_CONTEXT);
    }

    /**
     * Returns a new string that is the given word but with the ending
     * parenthesis removed.
     * <p>
     * Example:
     * 
     * <pre>
     *  "LEAD(2)" returns "LEAD"
     *  "LEAD" returns "LEAD"
     *  @param word
     *  the word to be stripped
     *  
     *  @return the given word but with all characters from the first
     *  open parentheses removed
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
     * Returns a Word object based on the spelling and its classification. The
     * behavior of this method is affected by the properties wordReplacement,
     * allowMissingWords, and createMissingWords.
     * 
     * @param text
     *                the spelling of the word of interest.
     * 
     * @return a Word object
     * 
     * @see edu.cmu.sphinx.linguist.dictionary.Word
     */
    public Word getWord(String text) {
        text = text.toLowerCase();
        Word word = lookupWord(text);
        if (word == null) {
            logger.warning("Missing word: " + text);
            if (wordReplacement != null) {
                word = lookupWord(wordReplacement);
                logger.warning("Replacing " + text + " with " + 
                               wordReplacement);
                if (word == null) {
                    logger.severe("Replacement word " + wordReplacement
                                  + " not found!");
                }
            } else if (allowMissingWords) {
                if (createMissingWords) {
                    word = new Word(text, null, false);
                    wordDictionary.put(text, word);
                }
                return null;
            }
        }
        return word;
    }

    /**
     * Lookups up a word
     * 
     * @param spelling
     *                the spellling of the word
     * 
     * @return the word or null
     */
    private Word lookupWord(String spelling) {
        Word word = (Word) wordDictionary.get(spelling);
        if (word == null) {
            word = (Word) fillerDictionary.get(spelling);
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
     * Returns a string representation of this FullDictionary in alphabetical
     * order.
     * 
     * @return a string representation of this FullDictionary
     */
    public String toString() {
        SortedMap sorted = new TreeMap(wordDictionary);
        String result = "";
        sorted.putAll(fillerDictionary);
        for (Iterator i = sorted.keySet().iterator(); i.hasNext();) {
            String text = (String) i.next();
            Word word = getWord(text);
            Pronunciation[] pronunciations = word.getPronunciations(null);
            result += (word + "\n");
            for (int p = 0; p < pronunciations.length; p++) {
                result += ("   " + pronunciations[p].toString() + "\n");
            }
        }
        return result;
    }

    /**
     * Gets the set of all filler words in the dictionary
     * 
     * @return an array (possibly empty) of all filler words
     */
    public Word[] getFillerWords() {
        return (Word[]) fillerDictionary.values().toArray(
                new Word[fillerDictionary.values().size()]);
    }

    /**
     * Dumps this FullDictionary to System.out.
     */
    public void dump() {
        System.out.println(wordDictionary.size() + " words");
        System.out.print(toString());
    }
}
