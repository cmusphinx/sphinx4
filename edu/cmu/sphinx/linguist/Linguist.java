
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

package edu.cmu.sphinx.linguist;

import java.io.IOException;

import edu.cmu.sphinx.util.props.Configurable;



/**
 * Provides language model services. 
 *
 */
public interface Linguist extends Configurable {

    // TODO sort out all of these props. Are the all necessary?
    // should the all be here at this level?
    
    
    /**
      * Word insertion probability property
      */
    public final static String PROP_WORD_INSERTION_PROBABILITY
        = "wordInsertionProbability";


    /**
     * The default value for PROP_WORD_INSERTION_PROBABILITY
     */
    public final static double PROP_WORD_INSERTION_PROBABILITY_DEFAULT = 1.0;


    /**
      * Unit insertion probability property
      */
    public final static String PROP_UNIT_INSERTION_PROBABILITY
        =  "unitInsertionProbability";


    /**
     * The default value for PROP_UNIT_INSERTION_PROBABILITY.
     */
    public final static double PROP_UNIT_INSERTION_PROBABILITY_DEFAULT = 1.0;


    /**
      * Silence insertion probability property
      */
    public final static String PROP_SILENCE_INSERTION_PROBABILITY
        = "silenceInsertionProbability";


    /**
     * The default value for PROP_SILENCE_INSERTION_PROBABILITY.
     */
    public final static double PROP_SILENCE_INSERTION_PROBABILITY_DEFAULT 
        = 1.0;

    /**
      * Filler insertion probability property
      */
    public final static String PROP_FILLER_INSERTION_PROBABILITY
        =  "fillerInsertionProbability";


    /**
     * The default value for PROP_FILLER_INSERTION_PROBABILITY.
     */
    public final static double PROP_FILLER_INSERTION_PROBABILITY_DEFAULT = 1.0;


    /**
     * Sphinx property that defines the language weight for the search
     */
    public final static String PROP_LANGUAGE_WEIGHT  =
	 "languageWeight";


    /**
     * The default value for the PROP_LANGUAGE_WEIGHT property
     */
    public final static float PROP_LANGUAGE_WEIGHT_DEFAULT  = 1.0f;


    /**
     * Property to control the maximum number of right contexts to
     * consider before switching over to using composite hmms
     */
    public final static String PROP_COMPOSITE_THRESHOLD 
        =  "compositeThreshold";

    
    /**
     * The default value for PROP_COMPOSITE_THRESHOLD.
     */
    public final static int PROP_COMPOSITE_THRESHOLD_DEFAULT = 1000;


    /**
     * Property to control whether pronunciations subtrees are
     * re-joined to reduce fan-out
     */
    public final static String PROP_JOIN_PRONUNCIATIONS
        =  "joinPronunciations";


    /**
     * The default value for PROP_JOIN_PRONUNCIATIONS.
     */
    public final static boolean PROP_JOIN_PRONUNCIATIONS_DEFAULT = false;


    /**
     * Property that controls whether word probabilities are spread
     * across all pronunciations.
     */
    public final static 
        String PROP_SPREAD_WORD_PROBABILITIES_ACROSS_PRONUNCIATIONS =
         "spreadWordProbabilitiesAcrossPronunciations";


    /**
     * The default value for 
     * PROP_SPREAD_WORD_PROBABILTIES_ACROSS_PRONUNCIATIONS.
     */
    public final static boolean 
        PROP_SPREAD_WORD_PROBABILITIES_ACROSS_PRONUNCIATIONS_DEFAULT = false;

    /**
     * Property that controls whether filler words are automatically
     * added to the vocabulary
     */
    public final static String PROP_ADD_FILLER_WORDS =
             "addFillerWords";


    /**
     * The default value for PROP_ADD_FILLER_WORDS.
     */
    public final static boolean PROP_ADD_FILLER_WORDS_DEFAULT = false;


    /**
     * Property to control whether silence units are automatically
     * looped to allow for longer silences
     */
    public final static String PROP_AUTO_LOOP_SILENCES = 
	"autoLoopSilences";


    /**
     * The default value for PROP_AUTO_LOOP_SILENCES.
     */
    public final static boolean PROP_AUTO_LOOP_SILENCES_DEFAULT = false;


    /**
     * Property to control the the dumping of the search space
     */
    public final static String PROP_SHOW_SEARCH_SPACE 
        = "showSearchSpace";


    /**
     * The default value for PROP_SHOW_SEARCH_SPACE.
     */
    public final static boolean PROP_SHOW_SEARCH_SPACE_DEFAULT = false;


    /**
     * Property to control the the validating of the search space
     */
    public final static String PROP_VALIDATE_SEARCH_SPACE
        =  "validateSearchSpace";


    /**
     * The default value for PROP_VALIDATE_SEARCH_SPACE.
     */
    public final static boolean PROP_VALIDATE_SEARCH_SPACE_DEFAULT = false;


    /**
     * Property to control whether contexts are considered across
     * grammar node boundaries
     */
    public final static String PROP_EXPAND_INTER_NODE_CONTEXTS
        = "expandInterNodeContexts";


    /**
     * The default value for PROP_EXPAND_INTER_NODE_CONTEXTS.
     */
    public final static boolean PROP_EXPAND_INTER_NODE_CONTEXTS_DEFAULT
        = false;


    /**
     * Property to control whether compilation progress is displayed
     * on stdout. If this property is true, a 'dot' is displayed for
     * every 1000 search states added to the search space
     */
    public final static String PROP_SHOW_COMPILATION_PROGRESS
        =  "showCompilationProgress";


    /**
     * The default value for PROP_SHOW_COMPILATION_PROGRESS.
     */
    public final static boolean PROP_SHOW_COMPILATION_PROGRESS_DEFAULT = false;


    /**
     * Property to control whether or not the linguist will generate
     * unit states.   When this property is false the linguist may
     * omit UnitSearchState states.  For some search algorithms 
     * this will allow for a faster search with more compact results.
     */
    public final static String PROP_GENERATE_UNIT_STATES
        =  "generateUnitStates";

    /**
     * The default value for PROP_GENERATE_UNIT_STATES
     */
    public final static boolean PROP_GENERATE_UNIT_STATES_DEFAULT = false;

    /**
      * A sphinx property that determines whether or not unigram
      * probabilities are smeared through the lex tree
      */
    public final static String PROP_WANT_UNIGRAM_SMEAR
        = "wantUnigramSmear";

    /**
     * The default value for PROP_WANT_UNIGRAM_SMEAR
     */
    public final static boolean PROP_WANT_UNIGRAM_SMEAR_DEFAULT = false;


    /**
      * A sphinx property that determines the weight of the smear
      */
    public final static String PROP_UNIGRAM_SMEAR_WEIGHT
        = "unigramSmearWeight";

    /**
     * The default value for PROP_UNIGRAM_SMEAR_WEIGHT
     */
    public final static float PROP_UNIGRAM_SMEAR_WEIGHT_DEFAULT = 1.0f;



    /**
     * Retrieves search graph
     * 
     * @return the search graph
     */
    public SearchGraph getSearchGraph();



    /**
     * Called before a recognition
     */
    public void startRecognition();

    /**
     * Called after a recognition
     */
    public void stopRecognition();
    
    
    /**
     * Allocates the linguist
     * @throws IOException if an IO error occurs
     */
    public void allocate() throws IOException;
    
    
    /**
     * Deallocates the linguist
     *
     */
    public void deallocate();


}

