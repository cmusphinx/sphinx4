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
package edu.cmu.sphinx.linguist.language.ngram;

import java.io.IOException;
import java.util.Set;

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.util.props.Configurable;

/**
 * Represents the generic interface to an N-Gram language model.
 * <p>
 * Note that all probabilities are in LogMath log base, except as
 * otherwise noted.
 */
public interface LanguageModel extends Configurable {
    /**
     * The SphinxProperty specifying the format of the language model.
     */
    public final static String PROP_FORMAT = "format";
    /**
     * The default value of PROP_FORMAT.
     */
    public final static String PROP_FORMAT_DEFAULT = "arpa";
    /**
     * The Sphinx Property specifying the location of the language model.
     */
    public final static String PROP_LOCATION = "location";
    /**
     * The default value of PROP_LOCATION.
     */
    public final static String PROP_LOCATION_DEFAULT = ".";
    /**
     * The Sphinx Property specifying the unigram weight
     */
    public final static String PROP_UNIGRAM_WEIGHT = "unigramWeight";
    /**
     * The default value for PROP_UNIGRAM_WEIGHT
     */
    public final static float PROP_UNIGRAM_WEIGHT_DEFAULT = 1.0f;
    /**
     * The Sphinx Property specifying the maximum depth reported by the
     * language model (from a getMaxDepth()) call. If this property is set to
     * (-1) (the default) the language model reports the implicit depth of the
     * model. This property allows a deeper language model to be used. For
     * instance, a trigram language model could be used as a bigram model by
     * setting this property to 2. Note if this property is set to a value
     * greater than the implicit depth, the implicit depth is used. Legal
     * values for this property are 1..N and -1.
     */
    public final static String PROP_MAX_DEPTH = "maxDepth";
    /**
     * The default value for PROP_MAX_DEPTH.
     */
    public final static int PROP_MAX_DEPTH_DEFAULT = -1;

    
    /**
     * The Sphinx Property specifying the dictionary to use
     */
    public final static String PROP_DICTIONARY = "dictionary";
    
    /**
     * Create the language model
     */
    public void allocate() throws IOException;

    /**
     * Deallocate resources allocated to this language model
     */
    public void deallocate();

    /**
     * Called before a recognition
     */
    public void start();

    /**
     * Called after a recognition
     */
    public void stop();

    /**
     * Gets the ngram probability of the word sequence represented by the word
     * list
     * 
     * @param wordSequence
     *                the wordSequence
     * 
     * @return the probability of the word sequence in LogMath log base
     */
    public float getProbability(WordSequence wordSequence);

    /**
     * Gets the smear term for the given wordSequence
     * 
     * @param wordSequence
     *                the word sequence
     * @return the smear term associated with this word sequence
     */
    public float getSmear(WordSequence wordSequence);

    /**
     * Returns the set of words in the lanaguage model. The set is
     * unmodifiable.
     * 
     * @return the unmodifiable set of words
     */
    public Set getVocabulary();

    /**
     * Returns the maximum depth of the language model
     * 
     * @return the maximum depth of the language mdoel
     */
    public int getMaxDepth();
}
