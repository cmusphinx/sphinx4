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

package edu.cmu.sphinx.knowledge.language;

import edu.cmu.sphinx.util.LogMath;
import java.util.List;
import java.util.Set;
import java.io.IOException;


/**
 * Represents the generic interface to an N-Gram language model
 *
 * Note that all probabilities are in LogMath log base, except as
 * otherwise noted.
 */
public interface LanguageModel {

    /**
     * Property prefix string.
     */
    public final static String PROP_PREFIX =
        "edu.cmu.sphinx.knowledge.language.LanguageModel";

    /**
     * The SphinxProperty specifying the format of the language model.
     */
    public final static String PROP_FORMAT = PROP_PREFIX + ".format";


    /**
     * The default value of PROP_FORMAT.
     */
    public final static String PROP_FORMAT_DEFAULT = "arpa";


    /**
     * The Sphinx Property specifying the location of the language model.
     */
    public final static String PROP_LOCATION = PROP_PREFIX + ".location";


    /**
     * The default value of PROP_LOCATION.
     */
    public final static String PROP_LOCATION_DEFAULT = ".";


    /**
     * The Sphinx Property specifying the unigram weight
     */
    public final static String PROP_UNIGRAM_WEIGHT = PROP_PREFIX + ".unigramWeight";

    /**
     * The default value for PROP_UNIGRAM_WEIGHT
     */
    public final static float PROP_UNIGRAM_WEIGHT_DEFAULT = 1.0f;


    /**
     * The Sphinx property specify the maximum depth of the language model.
     */
    public final static String PROP_MAX_DEPTH = PROP_PREFIX + ".maxDepth";


    /**
     * The default value for PROP_MAX_DEPTH.
     */
    public final static int PROP_MAX_DEPTH_DEFAULT = -1;


    /**
     * Initializes this LanguageModel
     *
     * @param context the context to associate this linguist with
     * 
     *
     * @throws IOException if an error occurs while loading the model
     */
    public void initialize(String context) throws IOException;
	    	

    /**
     * Called before a recognitino
     */
    public void start();

    /**
     * Called after a recognition
     */
    public void stop();

     /**
      * Gets the ngram probability of the word sequence represented by
      * the word list 
      *
      * @param wordList a list of strings representing the word
      * sequence of interest.
      *
      * @return the probability of the word sequence in LogMath log
      * base
      */
     // public float getProbability(List wordList);

     /**
      * Gets the ngram probability of the word sequence represented by
      * the word list 
      *
      * @param wordSequence the wordSequence
      *
      * @return the probability of the word sequence in LogMath log
      * base
      */
     public float getProbability(WordSequence wordSequence);


     /**
      * Returns the set of words in the lanaguage model. The set is
      * unmodifiable.
      *
      * @return the unmodifiable set of words
      */
     public Set getVocabulary();


     /**
      * Provides the log base that controls the range of probabilities
      * returned by this N-Gram
      */
     public void setLogMath(LogMath logMath);

     /**
      * Returns the log base that controls the range of probabilities
      * used by this N-gram
      */
     public LogMath getLogMath();



     /**
      * Returns the maximum depth of the language model
      *
      * @return the maximum depth of the language mdoel
      */
     public int getMaxDepth();
}

