
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

package edu.cmu.sphinx.decoder.linguist;
import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;
import edu.cmu.sphinx.knowledge.dictionary.Dictionary;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.decoder.linguist.Grammar;



/**
 * Provides language model services. 
 *
 */
public interface Linguist {


    /**
     * Prefix for search.Linguist SphinxProperties.
     */
    public final static String PROP_PREFIX =
	"edu.cmu.sphinx.decoder.linguist.Linguist.";


    /**
      * Word insertion probability property
      */
    public final static String PROP_WORD_INSERTION_PROBABILITY
        = PROP_PREFIX + "wordInsertionProbability";


    /**
     * The default value for PROP_WORD_INSERTION_PROBABILITY
     */
    public final static double PROP_WORD_INSERTION_PROBABILITY_DEFAULT = 1.0;


    /**
      * Unit insertion probability property
      */
    public final static String PROP_UNIT_INSERTION_PROBABILITY
        = PROP_PREFIX + "unitInsertionProbability";


    /**
     * The default value for PROP_UNIT_INSERTION_PROBABILITY.
     */
    public final static double PROP_UNIT_INSERTION_PROBABILITY_DEFAULT = 1.0;


    /**
      * Silence insertion probability property
      */
    public final static String PROP_SILENCE_INSERTION_PROBABILITY
        = PROP_PREFIX + "silenceInsertionProbability";


    /**
     * The default value for PROP_SILENCE_INSERTION_PROBABILITY.
     */
    public final static double PROP_SILENCE_INSERTION_PROBABILITY_DEFAULT 
        = 1.0;

    /**
      * Filler insertion probability property
      */
    public final static String PROP_FILLER_INSERTION_PROBABILITY
        = PROP_PREFIX + "fillerInsertionProbability";


    /**
     * The default value for PROP_FILLER_INSERTION_PROBABILITY.
     */
    public final static double PROP_FILLER_INSERTION_PROBABILITY_DEFAULT = 1.0;


    /**
     * Sphinx property that defines the language weight for the search
     */
    public final static String PROP_LANGUAGE_WEIGHT  =
	PROP_PREFIX + "languageWeight";


    /**
     * The default value for the PROP_LANGUAGE_WEIGHT property
     */
    public final static float PROP_LANGUAGE_WEIGHT_DEFAULT  = 1.0f;


    /**
     * Property to control the maximum number of right contexts to
     * consider before switching over to using composite hmms
     */
    public final static String PROP_COMPOSITE_THRESHOLD 
        = PROP_PREFIX + "compositeThreshold";

    
    /**
     * The default value for PROP_COMPOSITE_THRESHOLD.
     */
    public final static int PROP_COMPOSITE_THRESHOLD_DEFAULT = 1000;


    /**
     * Property to control whether pronunciations subtrees are
     * re-joined to reduce fan-out
     */
    public final static String PROP_JOIN_PRONUNCIATIONS
        = PROP_PREFIX + "joinPronunciations";


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
        PROP_PREFIX + "spreadWordProbabilitiesAcrossPronunciations";


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
            PROP_PREFIX + "addFillerWords";


    /**
     * The default value for PROP_ADD_FILLER_WORDS.
     */
    public final static boolean PROP_ADD_FILLER_WORDS_DEFAULT = false;


    /**
     * Property to control whether silence units are automatically
     * looped to allow for longer silences
     */
    public final static String PROP_AUTO_LOOP_SILENCES = PROP_PREFIX +
	"autoLoopSilences";


    /**
     * The default value for PROP_AUTO_LOOP_SILENCES.
     */
    public final static boolean PROP_AUTO_LOOP_SILENCES_DEFAULT = false;


    /**
     * Property to control the the dumping of the search space
     */
    public final static String PROP_SHOW_SEARCH_SPACE 
        = PROP_PREFIX + "showSearchSpace";


    /**
     * The default value for PROP_SHOW_SEARCH_SPACE.
     */
    public final static boolean PROP_SHOW_SEARCH_SPACE_DEFAULT = false;


    /**
     * Property to control the the validating of the search space
     */
    public final static String PROP_VALIDATE_SEARCH_SPACE
        = PROP_PREFIX + "validateSearchSpace";


    /**
     * The default value for PROP_VALIDATE_SEARCH_SPACE.
     */
    public final static boolean PROP_VALIDATE_SEARCH_SPACE_DEFAULT = false;


    /**
     * Property to control whether contexts are considered across
     * grammar node boundaries
     */
    public final static String PROP_EXPAND_INTER_NODE_CONTEXTS
        = PROP_PREFIX + "expandInterNodeContexts";


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
        = PROP_PREFIX + "showCompilationProgress";


    /**
     * The default value for PROP_SHOW_COMPILATION_PROGRESS.
     */
    public final static boolean PROP_SHOW_COMPILATION_PROGRESS_DEFAULT = false;


    /**
     * Property to control whether or not the linguist may omit
     * unit states.   When this property is true the linguist may
     * omit UnitSearchState states.  For some search algorithms 
     * this will allow for a faster search with more compact results.
     */
    public final static String PROP_OMIT_UNIT_STATES
        = PROP_PREFIX + "generateUnitStates";

    /**
     * The default value for PROP_OMIT_UNIT_STATES
     */
    public final static boolean PROP_OMIT_UNIT_STATES_DEFAULT = true;


    /**
     * Initializes this linguist
     *
     * @param context the context to associate this linguist with
     * @param languageModel the language model
     * @param grammar the grammar for this linguist
     * @param models the acoustic model(s) used by this linguist,
     *    normally there is only one AcousticModel, but it is possible
     *    for the Linguist to use multiple AcousticModel(s)
     */
    public void initialize(String context,
			   LanguageModel languageModel,
                           Dictionary dictionary,
			   Grammar grammar,
			   AcousticModel[] models) ;


    /**
     * Retrieves initial search state
     * 
     * @return the set of initial search state
     */
    public SearchState getInitialSearchState();


    /**
     * Returns an array of classes that represents the order 
     * in which the states will be returned.
     *
     * @return an array of classes that represents the order 
     *     in which the states will be returned
     */
    public Class[] getSearchStateOrder();


    /**
     * Called before a recognitino
     */
    public void start();

    /**
     * Called after a recognition
     */
    public void stop();


    /**
     * Retrieves the language model for this linguist
     *
     * @return the language model (or null if there is none)
     */
    public LanguageModel getLanguageModel();
}

