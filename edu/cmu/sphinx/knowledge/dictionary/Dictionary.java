
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


/**
 * Provides a generic interface to a dictionary. The dictionary is
 * responsibile for determining how a word is pronounced.
 */
public interface Dictionary {

    /**
     * Spelling of the sentence start word.
     */
    public static final String SENTENCE_START_SPELLING = "<s>";

    /**
     * Spelling of the sentence end word.
     */
    public static final String SENTENCE_END_SPELLING = "</s>";

    /**
     * Spelling of the 'word' that marks a silence
     */
    public static final String SILENCE_SPELLING = "<sil>";


    /**
     * Prefix string for the Sphinx properties of this Dictionary.
     */
    public static final String PROP_PREFIX =
        "edu.cmu.sphinx.knowledge.dictionary.Dictionary.";


    /**
     * The name of the SphinxProperty that specifies whether to look
     * for the Dictionaries at the Acoustic Model location.
     */
    public static final String PROP_USE_AM_LOCATION =
        PROP_PREFIX + "useAcousticModelLocation";


    /**
     * The default value of PROP_USE_AM_LOCATION.
     */
    public static final boolean PROP_USE_AM_LOCATION_DEFAULT = true;


    /**
     * The name of the SphinxProperty for the dictionary file path.
     */
    public static final String PROP_DICTIONARY = PROP_PREFIX +"dictionaryPath";


    /**
     * The default value of PROP_DICTIONARY.
     */
    public static final String PROP_DICTIONARY_DEFAULT = null;


    /**
     * The name of the SphinxProperty for the filler dictionary file path.
     */
    public static final String PROP_FILLER_DICTIONARY =
        PROP_PREFIX + "fillerPath";


    /**
     * The default value of PROP_FILLER_DICTIONARY.
     */
    public static final String PROP_FILLER_DICTIONARY_DEFAULT = null;


    /**
     * The name of the SphinxProperty that specifies whether to add
     * a duplicate SIL-ending pronunication.
     */
    public static final String PROP_ADD_SIL_ENDING_PRONUNCIATION =
        PROP_PREFIX + "addSilEndingPronunciation";


    /**
     * The default value of PROP_ADD_SIL_ENDING_PRONUNCIATION.
     */
    public static final boolean PROP_ADD_SIL_ENDING_PRONUNCIATION_DEFAULT
	= false;


    /**
     * The name of the SphinxProperty that specifies the word to
     * substitute when a lookup fails to find the word in the
     * dictionary.  If this is not set, no substitute is performed.
     */
    public static final String PROP_WORD_REPLACEMENT =
        PROP_PREFIX + "wordReplacement";
   

    /**
     * The default value of PROP_WORD_REPLACEMENT.
     */
    public static final String PROP_WORD_REPLACEMENT_DEFAULT = null;

 
    /**
     * The name of the SphinxProperty that specifies whether the
     * dictionary should return null if a word is not found in the
     * dictionary, or whether it should throw an error.  If true, a
     * null is returned for words that are not found in the dictionary
     * (and the 'PROP_WORD_REPLACEMENT' property is not set).
     */
    public static final String PROP_ALLOW_MISSING_WORDS =
        PROP_PREFIX + "allowMissingWords";


    /**
     * The default value of PROP_ALLOW_MISSING_WORDS.
     */
    public static final boolean PROP_ALLOW_MISSING_WORDS_DEFAULT = false;


    /**
     * The SphinxProperty that specifies whether the Dictionary.getWord()
     * method should return a Word object even if the word does not exist
     * in the dictionary. If this property is true, and property
     * allowMissingWords is also true, the method will return a Word, 
     * but the Word will have null Pronunciations. Otherwise, the method
     * will return null. This property is usually only used for
     * testing purposes.
     */
    public static final String PROP_CREATE_MISSING_WORDS =
        PROP_PREFIX + "createMissingWords";


    /**
     * The default value of PROP_CREATE_MISSING_WORD.
     */
    public static final boolean PROP_CREATE_MISSING_WORDS_DEFAULT = false;
    

    /**
     * Returns a Word object based on the spelling and its classification.
     * The behavior of this method is also affected by the properties
     * wordReplacement, allowMissingWords, and createMissingWords.
     *
     * @param text the spelling of the word of interest.
     *
     * @return a Word object
     *
     * @see edu.cmu.sphinx.knowledge.dictionary.Pronunciation
     */
    public Word getWord(String text);

    /**
     * Returns the sentence start word.
     *
     * @return the sentence start word
     */
    public Word getSentenceStartWord();

    /**
     * Returns the sentence end word.
     *
     * @return the sentence end word
     */
    public Word getSentenceEndWord();

    /**
     * Returns the silence word.
     *
     * @return the silence word
     */
    public Word getSilenceWord();
    
    /**
     * Returns the set of all possible word classifications for this
     * dictionary.
     *
     * @return the set of all possible word classifications
     */
    public WordClassification[] getPossibleWordClassifications();

    /**
     * Dumps out a dictionary
     *
     */
    public void dump();

    /**
     * Gets the set of all filler words in the dictionary
     *
     * @return an array (possibly empty) of all filler words
     */
    public Word[] getFillerWords();
}

