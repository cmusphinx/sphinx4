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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Logger;

import edu.cmu.sphinx.linguist.acoustic.Context;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.util.StreamFactory;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * Creates a dictionary by quickly reading in an ASCII-based Sphinx-3 format
 * dictionary. It is called the FastDictionary because the loading is fast.
 * When loaded the dictionary just loads each line of the dictionary into the
 * hash table, assuming that most words are not going to be used. Only when a
 * word is actually used is its pronunciations massaged into an array of
 * pronunciations.
 * <p>
 * The format of the ASCII dictionary that it explains is the same as the
 * {@link FullDictionary FullDictionary}, i.e., the word, followed by spaces
 * or tab, followed by the pronunciation(s). For example, a digits dictionary
 * will look like:
 * 
 * <pre>
 *  ONE HH W AH N
 *  ONE(2) W AH N
 *  TWO T UW
 *  THREE TH R IY
 *  FOUR F AO R
 *  FIVE F AY V
 *  SIX S IH K S
 *  SEVEN S EH V AH N
 *  EIGHT EY T
 *  NINE N AY N
 *  ZERO Z IH R OW
 *  ZERO(2) Z IY R OW
 *  OH OW
 * </pre>
 * 
 * <p>
 * In the above example, the words "one" and "zero" have two pronunciations
 * each.
 */
public class FastDictionary implements Dictionary {


    // -------------------------------
    // Configuration data
    // --------------------------------
    private String name;
    private Logger logger;
    private boolean addSilEndingPronunciation;
    private boolean allowMissingWords;
    private boolean createMissingWords;
    private String wordReplacement;
    private URL wordDictionaryFile;
    private URL fillerDictionaryFile;

    private UnitManager unitManager;

    // -------------------------------
    // working data
    // -------------------------------
    private Map dictionary;

    private final static String FILLER_TAG = "-F-";
    private Set fillerWords;
    private boolean allocated;

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

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.linguist.dictionary.Dictionary#allocate()
     */
    public void allocate() throws IOException {
        if (!allocated) {
            dictionary = new HashMap();
            Timer loadTimer = Timer.getTimer("DictionaryLoad");
            fillerWords = new HashSet();

            loadTimer.start();

            logger.info("Loading dictionary from: " + wordDictionaryFile);

            loadDictionary(wordDictionaryFile.openStream(), false);

            logger.info("Loading filler dictionary from: " +
                        fillerDictionaryFile);

            loadDictionary(fillerDictionaryFile.openStream(), true);

            loadTimer.stop();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.linguist.dictionary.Dictionary#deallocate()
     */
public void deallocate() {
        if (allocated) {
            dictionary = null;
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
    private void loadDictionary(InputStream inputStream, boolean isFillerDict)
            throws IOException {
        InputStreamReader isr = new InputStreamReader(inputStream);
        BufferedReader br = new BufferedReader(isr);
        String line;

        while ((line = br.readLine()) != null) {
            if (line.length() > 0) {
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
                if (spaceIndex == -1) {
                    throw new Error("Error loading word: " + line);
                }
                String word = line.substring(0, spaceIndex);
                word = word.toLowerCase();
                if (isFillerDict) {
                    dictionary.put(word, (FILLER_TAG + line));
                    fillerWords.add(word);
                } else {
                    dictionary.put(word, line);
                }
            }
        }

        br.close();
        isr.close();
        inputStream.close();
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
     * Returns a Word object based on the spelling and its classification. The
     * behavior of this method is also affected by the properties
     * wordReplacement, allowMissingWords, and createMissingWords.
     * 
     * @param text
     *                the spelling of the word of interest.
     * 
     * @return a Word object
     * 
     * @see edu.cmu.sphinx.linguist.dictionary.Word
     */
    public Word getWord(String text) {
        Word word = null;
        text = text.toLowerCase();

        Object object = dictionary.get(text);

        if (object == null) { // deal with 'not found' case
            logger.warning("Missing word: " + text);
            if (wordReplacement != null) {
                word = getWord(wordReplacement);
            } else if (allowMissingWords) {
                if (createMissingWords) {
                    word = createWord(text, null, false);
                }
            }                
        } else if (object instanceof String) { // first lookup for this string
            word = processEntry(text);
        } else if (object instanceof Word) {
            word = (Word) object;
        }
        return word;
    }

    /**
     * Create a Word object with the given spelling and pronunciations, and
     * insert it into the dictionary.
     * 
     * @param text
     *                the spelling of the word
     * @param pronunciation
     *                the pronunciation of the word
     * @param isFiller
     *                if <code>true</code> this is a filler word
     * 
     * @return the word
     */
    private Word createWord(String text, Pronunciation[] pronunciation,
            boolean isFiller) {
        Word word = new Word(text, pronunciation, isFiller);
        dictionary.put(text, word);
        return word;
    }

    /**
     * Processes a dictionary entry. When loaded the dictionary just loads each
     * line of the dictionary into the hash table, assuming that most words are
     * not going to be used. Only when a word is actually used is its
     * pronunciations massaged into an array of pronunciations.
     */
    private Word processEntry(String word) {
        List pList = new LinkedList();
        String line = null;
        int count = 0;
        boolean isFiller = false;

        do {
            count++;
            String lookupWord = word;
            if (count > 1) {
                lookupWord = lookupWord + "(" + count + ")";
            }
            line = (String) dictionary.get(lookupWord);
            if (line != null) {
                StringTokenizer st = new StringTokenizer(line);

                String tag = st.nextToken();
                isFiller = tag.startsWith(FILLER_TAG);
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
                    silUnits[unitCount] = UnitManager.SILENCE;
                    units = silUnits;
                }
                pList.add(new Pronunciation(units, null, null, 1.f));
            }
        } while (line != null);

        Pronunciation[] pronunciations = new Pronunciation[pList.size()];
        pList.toArray(pronunciations);
        Word wordObject = createWord(word, pronunciations, isFiller);

        for (int i = 0; i < pronunciations.length; i++) {
            pronunciations[i].setWord(wordObject);
        }

        return wordObject;
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
     * Returns a string representation of this FastDictionary in alphabetical
     * order.
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
     * Gets the set of all filler words in the dictionary
     * 
     * @return an array (possibly empty) of all filler words
     */
    public Word[] getFillerWords() {
        Word[] fillerWordArray = new Word[fillerWords.size()];
        int index = 0;
        for (Iterator i = fillerWords.iterator(); i.hasNext();) {
            String spelling = (String) i.next();
            fillerWordArray[index++] = getWord(spelling);
        }
        return fillerWordArray;
    }

    /**
     * Dumps this FastDictionary to System.out.
     */
    public void dump() {
        System.out.print(toString());
    }

}
