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

package edu.cmu.sphinx.decoder.linguist.cclextree;

import edu.cmu.sphinx.decoder.linguist.Linguist;
import edu.cmu.sphinx.decoder.linguist.Grammar;
import edu.cmu.sphinx.knowledge.dictionary.Dictionary;
import edu.cmu.sphinx.knowledge.dictionary.Word;
import edu.cmu.sphinx.decoder.linguist.SearchState;
import edu.cmu.sphinx.decoder.linguist.SearchStateArc;
import edu.cmu.sphinx.decoder.linguist.WordSearchState;
import edu.cmu.sphinx.decoder.linguist.UnitSearchState;
import edu.cmu.sphinx.decoder.linguist.HMMSearchState;
import edu.cmu.sphinx.decoder.linguist.util.LinguistTimer;
import edu.cmu.sphinx.decoder.linguist.util.HMMPool;

import edu.cmu.sphinx.knowledge.acoustic.HMM;
import edu.cmu.sphinx.knowledge.acoustic.HMMState;
import edu.cmu.sphinx.knowledge.acoustic.HMMStateArc;
import edu.cmu.sphinx.knowledge.acoustic.HMMPosition;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;
import edu.cmu.sphinx.knowledge.acoustic.Context;
import edu.cmu.sphinx.knowledge.acoustic.LeftRightContext;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.knowledge.language.WordSequence;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.knowledge.dictionary.Pronunciation;


import java.io.IOException;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashSet;
import java.util.Random;
import java.util.Comparator;

/**
 */
public class LexTreeLinguist implements  Linguist {

    /**
     * Prefix for search.Linguist.LexTreeLinguist  SphinxProperties.
     */
    private final static String PROP_PREFIX =
	"edu.cmu.sphinx.decoder.linguist.clextree.LexTreeLinguist.";


    /**
      * A sphinx property that determines whether or not full word
      * histories are used to determine when two states are equal.
      */
    public final static String PROP_FULL_WORD_HISTORIES
        = PROP_PREFIX + "fullWordHistories";

    /**
     * The default value for PROP_FULL_WORD_HISTORIES
     */
    public final static boolean PROP_FULL_WORD_HISTORIES_DEFAULT = true;


    /**
     * The default value for PROP_MAINTAIN_SEPARATE_WORD_RC
     */
    public final static boolean PROP_MAINTAIN_SEPARATE_WORD_RC_DEFAULT = true;


    /**
     * An array of classes that represents the order 
     * in which the states will be returned.
     */
    private final static Class[] searchStateOrder = {
        LexTreeNonEmittingHMMState.class,
        LexTreeWordState.class,
        LexTreeUnitState.class,
        LexTreeEndUnitState.class,
        LexTreeHMMState.class
    };

    private final static SearchStateArc[] EMPTY_ARC = new SearchStateArc[0];

    // just for detailed debugging
    private final boolean tracing = false;

    private SphinxProperties props;
    private LanguageModel languageModel;
    private AcousticModel acousticModel;
    private LogMath logMath;

    private float languageWeight;
    private float logWordInsertionProbability;
    private float logUnitInsertionProbability;
    private float logFillerInsertionProbability;
    private float logSilenceInsertionProbability;
    private float logOne;

    private HMMPool hmmPool;
    private HMMTree hmmTree;
    private Dictionary dictionary;
    
    private int silenceID;
    private boolean fullWordHistories = true;
    private boolean addFillerWords = false;
    private boolean generateUnitStates = false;
    private boolean wantUnigramSmear = true;
    private float unigramSmearWeight = 1.0f;
    private float unigramSmearOffset = .0f;

    private Word sentenceEndWord;
    private Word[] sentenceStartWordArray;

    /**
     * Returns an array of classes that represents the order 
     * in which the states will be returned.
     *
     * @return an array of classes that represents the order 
     *     in which the states will be returned
     */
    public Class[] getSearchStateOrder() {
        return searchStateOrder;
    }


    /**
     * Creates a LexTree linguist associated with the given context
     *
     * @param context the context to associate this linguist with
     * @param languageModel the language model
     * @param grammar the grammar for this linguist
     * @param models the acoustic model used by this linguist
     */
    public void initialize(String context, 
                           LanguageModel languageModel,
                           Dictionary dictionary,
                           Grammar grammar, 
                           AcousticModel[] models) {
        assert models.length == 1;

        this.props = SphinxProperties.getSphinxProperties(context);
        this.acousticModel = models[0];
        this.logMath = LogMath.getLogMath(context);
        this.languageModel = languageModel;

        this.fullWordHistories =
            props.getBoolean(PROP_FULL_WORD_HISTORIES,
                    PROP_FULL_WORD_HISTORIES_DEFAULT);

        this.wantUnigramSmear  =
            props.getBoolean(PROP_WANT_UNIGRAM_SMEAR,
                    PROP_WANT_UNIGRAM_SMEAR_DEFAULT);


        // System.out.println("LM Max depth is " + languageModel.getMaxDepth());

        this.dictionary = dictionary;

        sentenceEndWord = dictionary.getSentenceEndWord();
        sentenceStartWordArray = new Word[1];
        sentenceStartWordArray[0] = dictionary.getSentenceStartWord();
        
        logOne = logMath.getLogOne();


        logWordInsertionProbability = logMath.linearToLog
            (props.getDouble
             (Linguist.PROP_WORD_INSERTION_PROBABILITY,
              Linguist.PROP_WORD_INSERTION_PROBABILITY_DEFAULT));

        logSilenceInsertionProbability = logMath.linearToLog
            (props.getDouble
             (Linguist.PROP_SILENCE_INSERTION_PROBABILITY,
              Linguist.PROP_SILENCE_INSERTION_PROBABILITY_DEFAULT));

        logFillerInsertionProbability = logMath.linearToLog
            (props.getDouble
             (Linguist.PROP_FILLER_INSERTION_PROBABILITY,
              Linguist.PROP_FILLER_INSERTION_PROBABILITY_DEFAULT));

        logUnitInsertionProbability = logMath.linearToLog
            (props.getDouble
             (Linguist.PROP_UNIT_INSERTION_PROBABILITY,
              Linguist.PROP_UNIT_INSERTION_PROBABILITY_DEFAULT));

        languageWeight = props.getFloat(PROP_LANGUAGE_WEIGHT,
                                        PROP_LANGUAGE_WEIGHT_DEFAULT);

        addFillerWords = (props.getBoolean (Linguist.PROP_ADD_FILLER_WORDS,
              Linguist.PROP_ADD_FILLER_WORDS_DEFAULT));

        generateUnitStates = 
            (props.getBoolean(Linguist.PROP_GENERATE_UNIT_STATES,
              Linguist.PROP_GENERATE_UNIT_STATES_DEFAULT));

        unigramSmearWeight = props.getFloat(PROP_UNIGRAM_SMEAR_WEIGHT,
             PROP_UNIGRAM_SMEAR_WEIGHT_DEFAULT);


        compileGrammar();

        acousticModel = null;

        if (false) {
            LinguistTimer lt = new LinguistTimer(this, false);
            lt.timeLinguist(10, 500, 1000);
        }
    }

    /**
     * 
     * Called before a recognition
     */
    public void start() {
        // property getters are here in 'start' to allow changing
        // of these properties on the fly
        logWordInsertionProbability = logMath.linearToLog
            (props.getDouble
             (Linguist.PROP_WORD_INSERTION_PROBABILITY,
              Linguist.PROP_WORD_INSERTION_PROBABILITY_DEFAULT));

        logSilenceInsertionProbability = logMath.linearToLog
            (props.getDouble
             (Linguist.PROP_SILENCE_INSERTION_PROBABILITY,
              Linguist.PROP_SILENCE_INSERTION_PROBABILITY_DEFAULT));

        logUnitInsertionProbability = logMath.linearToLog
            (props.getDouble
             (Linguist.PROP_UNIT_INSERTION_PROBABILITY,
              Linguist.PROP_UNIT_INSERTION_PROBABILITY_DEFAULT));

        languageWeight = props.getFloat(PROP_LANGUAGE_WEIGHT,
                                        PROP_LANGUAGE_WEIGHT_DEFAULT);

	languageModel.start();
    }

    /**
     * Called after a recognition
     */
    public void stop() {
	languageModel.stop();
    }

    /**
     * Retrieves the language model for this linguist
     *
     * @return the language model (or null if there is none)
     */
    public LanguageModel getLanguageModel() {
        return null;
    }


    /**
     * retrieves the initial language state
     *
     * @return the initial language state
     */
    public SearchState getInitialSearchState() {
        InitialWordNode node = hmmTree.getInitialNode();
        return new LexTreeWordState(node, node.getParent(),
              WordSequence.getWordSequence(sentenceStartWordArray).trim
               (languageModel.getMaxDepth() - 1), logOne);
    }


    /**
     * Compiles the n-gram into a lex tree that is used during the
     * search
     */
    protected void compileGrammar() {
        Timer.start("compile");

        hmmPool = new HMMPool(acousticModel);
        silenceID = hmmPool.getID(Unit.SILENCE);

        hmmTree = new HMMTree(hmmPool, dictionary, languageModel,
                addFillerWords, languageWeight);

        hmmPool.dumpInfo();

        Timer.stop("compile");
        // Now that we are all done, dump out some interesting
        // information about the process

        Timer.dumpAll();
    }



    /**
     * The LexTreeLinguist returns lanague states to the search
     * manager. This class forms the base implementation for all
     * language states returned.  This LexTreeState keeps track of the
     * probability of entering this state (a language+insertion
     * probability) as well as the unit history. The unit history
     * consists of the LexTree nodes that correspond to the left,
     * center and right contexts.
     *
     *  This is an abstract class, subclasses must implement the
     *  getSuccessorss method.
     */
    class LexTreeState implements SearchState, SearchStateArc {
        private Node node;
        private WordSequence wordSequence;

        /**
         * Creates a LexTreeState.
         *
         * @param leftID the id of the unit forming the left context
         * (or 0 if  there is no left context) of a triphone context
         *
         * @param central the unit forming the central portion of a
         * triphone context
         *
         * @param right the unit forming the right portion of a
         * triphone context
         *
         * @param wordSequence the history of words up until this point
         *
         * @param logProbability the probability (in log math domain)
         * of entering this state.  This is a combination of insertion
         * and language probability.
         */
        LexTreeState(Node node, WordSequence wordSequence) {

            this.node = node;
            this.wordSequence = wordSequence;
        }


        /**
         * Gets the unique signature for this state. The signature
         * building code is slow and should only be used for
         * non-time-critical tasks such as plotting states.
         *
         * @return the signature
         */
        public String getSignature() {
            return "lts-" + node.hashCode() + "-ws-" + wordSequence;
        }


        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        public int hashCode() {
            int hashCode = fullWordHistories ? wordSequence.hashCode() * 37 
                                                                       : 37;
            hashCode +=  node.hashCode();
            return hashCode;
        }

        /**
         * Determines if the given object is equal to this object
         * 
         * @param o the object to test
         * @return <code>true</code> if the object is equal to this
         */
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof LexTreeState) {
                LexTreeState other = (LexTreeState) o;
                boolean wordSequenceMatch = fullWordHistories ?
                     wordSequence.equals(other.wordSequence) : true;
                return  node == other.node && wordSequenceMatch;
            } else {
                return false;
            }
        }

        /**
         * Gets a successor to this search state
         *
         * @return the sucessor state
         */
         public SearchState  getState() {
             return this;
         }

         /**
          * Gets the composite probability of entering this state
          *
          * @return the log probability
          */
         public float getProbability() {
             return getLanguageProbability() +
                 getAcousticProbability() + getInsertionProbability();
         }

         /**
          * Gets the language probability of entering this state
          *
          * @return the log probability
          */
         public float getLanguageProbability() {
             return logOne;
          }

         /**
          * Gets the language probability of entering this state
          *
          * @return the log probability
          */
         public float getAcousticProbability() {
             return logOne;
         }

         /**
          * Gets the insertion probability of entering this state
          *
          * @return the log probability
          */
         public float getInsertionProbability() {
             return logOne;
         }



         /**
          * Determines if this is an emitting state
          *
          * @return <code>true</code> if this is an emitting state.
          */
         public boolean isEmitting() {
             return false;
         }

         /**
          * Determines if this is a final state
          *
          * @return <code>true</code> if this is an final state.
          */
         public boolean isFinal() {
             return false;
         }

         /**
          * Gets the hmm tree node representing the unit
          *
          * @return the unit lex node
          */
         protected Node getNode() {
             return node;
         }

         /**
          * Returns the word sequence for this state
          *
          * @return the word sequence
          */
         public WordSequence getWordHistory() {
             return wordSequence;
         }

         public Object getLexState() {
             return node;
         }

         /**
          * Returns the list of successors to this state
          *
          * @return a list of SearchState objects
          */
         public SearchStateArc[] getSuccessors() {
             return getSuccessors(node);
         }

         /**
          * Returns the list of successors to this state
          *
          * @return a list of SearchState objects
          */
         protected SearchStateArc[] getSuccessors(Node theNode) {
	     Collection nodes = theNode.getSuccessors();
             SearchStateArc[] arcs = new SearchStateArc[nodes.size()];
	     Iterator iter = nodes.iterator();
             // System.out.println("Arc: "+ this);
	     for (int i = 0; i < arcs.length; i++) {
		 Node nextNode = (Node) iter.next();
               //  System.out.println("           " + nextNode);
                 if (nextNode instanceof WordNode) {
                     arcs[i] = createWordStateArc((WordNode) nextNode, 
                             (HMMNode) getNode(), getUnigramSmear(theNode));
                 } else if (nextNode instanceof EndNode) {
                     arcs[i] = createEndUnitArc((EndNode) nextNode, 
                             getUnigramSmear(theNode));
                 } else {
                     arcs[i] = createUnitStateArc((HMMNode)nextNode, 
                             getUnigramSmear(theNode));
                 }
	     }
             return arcs;
         }


         /**
          * Creates a word search state for the given word node
          *
          * @param wordNode the wordNode
          *
          * @return the search state for the wordNode
          */
         private SearchStateArc createWordStateArc(WordNode wordNode,
                 HMMNode lastUnit, float fixupProb) {
	      // System.out.println("CWSA " + wordNode);
            float logProbability = logOne;
            Word nextWord = wordNode.getWord();

            // the last node has the unigram probability that has been
            // applied up until this point (the unigram smear).  Since
            // we are now going to apply the actual probability, we
            // will subtract out this previously applied probablilty


            WordSequence nextWordSequence = wordSequence;

            if (nextWord.isFiller()) {
                if (nextWord != dictionary.getSilenceWord()) {
                    logProbability = logFillerInsertionProbability;
                    logProbability -= fixupProb;
                }
            } else {
                nextWordSequence  = wordSequence.addWord(nextWord, 
                        languageModel.getMaxDepth());
                logProbability = languageModel.getProbability(nextWordSequence);
                logProbability *= languageWeight;
                // subtract off the previously applied unigram  probability
                logProbability -= unigramSmearOffset;
                logProbability -= fixupProb;
            }

            return new LexTreeWordState(wordNode, lastUnit,
                nextWordSequence.trim(languageModel.getMaxDepth() - 1), 
                logProbability);
         }



         /**
          * Creates a unit search state for the given unit node
          *
          * @param hmmNode the unit node
          *
          * @return the search state
          */
         SearchStateArc createUnitStateArc(HMMNode hmmNode, float lastProb) {
             SearchStateArc arc;
	     // System.out.println("CUSA " + hmmNode);
             float insertionProbability =
                 calculateInsertionProbability(hmmNode);
             float languageProbability = getUnigramSmear(hmmNode) - lastProb;

             // if we want a unit state create it, otherwise
             // get the first hmm state of the unit

             if (generateUnitStates) {
                 arc = new LexTreeUnitState(hmmNode,
                         getWordHistory(), languageProbability, 
                         insertionProbability);
            } else {
                HMM hmm = hmmNode.getHMM();
                arc = new LexTreeHMMState(hmmNode,
                     getWordHistory(), hmm.getInitialState(),
                     languageProbability,
                     insertionProbability, logOne, null);
            }
            return arc;
         }

         /**
          * Creates a unit search state for the given unit node
          *
          * @param hmmNode the unit node
          *
          * @return the search state
          */
         SearchStateArc createEndUnitArc(EndNode endNode, float lastProb) {
             float languageProbability = getUnigramSmear(endNode) - lastProb;
             float insertionProbability =
                 calculateInsertionProbability(endNode);
             return  new LexTreeEndUnitState(endNode, getWordHistory(), 
                     languageProbability, insertionProbability);
         }

         /**
          * Returns the string representation of this object
          *
          * @return the string representation
          */
        public String toString() {
            return "lt-" + node + " " + getProbability() +
                   "{" + wordSequence + "}" ;
        }

       /**
        * Returns a pretty version of the string representation 
        * for this object
        *
        * @return a pretty string
        */
       public String toPrettyString() {
           return toString();
       }
    }

    /**
     * Represents a unit in the search space
     */
    public class LexTreeEndUnitState extends LexTreeState 
                implements UnitSearchState {

        float logLanguageProbability;
        float logInsertionProbability;
        /**
         * Constructs a LexTreeUnitState
         *
         *
         * @param wordSequence the history of words
         */
        LexTreeEndUnitState(EndNode endNode, WordSequence
                wordSequence, float languageProbability, 
                float insertionProbability) {
            super(endNode, wordSequence);
            logLanguageProbability = languageProbability;
            logInsertionProbability = insertionProbability;
            // System.out.println("LTEUS " + logLanguageProbability + " " + logInsertionProbability);
        }

        /**
         * Returns the base unit assciated with this state
         *
         * @return the base unit
         */
        public Unit getUnit() {
            return getEndNode().getBaseUnit();
        }

        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        public int hashCode() {
            return super.hashCode() * 17 + 423;
        }

        /**
         * Determines if the given object is equal to this object
         * 
         * @param o the object to test
         * @return <code>true</code> if the object is equal to this
         */
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof LexTreeEndUnitState) {
                LexTreeEndUnitState other = (LexTreeEndUnitState) o;
                return  super.equals(o);
            } else {
                return false;
            }
        }

        /**
         * Returns the unit node for this state
         *
         * @return the unit node
         */
        private EndNode getEndNode() {
            return (EndNode) getNode();
        }


       /**
        * Returns the list of successors to this state
        *
        * @return a list of SearchState objects
        */
       public SearchStateArc[] getSuccessors() {
          HMMNode[] nodes = getHMMNodes(getEndNode());
          SearchStateArc[] arcs = new SearchStateArc[nodes.length];

          for (int i = 0; i < nodes.length; i++) {
              arcs[i] = new LexTreeUnitState(nodes[i], getWordHistory(), 
                  logLanguageProbability, logInsertionProbability, 
                  this.getNode());
          }
          return arcs;
       }

       public String toString() {
           return super.toString() + " EndUnit";
       }
    }

    /**
     * Represents a unit in the search space
     */
    public class LexTreeUnitState extends LexTreeState 
                implements UnitSearchState {

        private float logInsertionProbability;
        private float logLanguageProbability;
        private Node parentNode = null;

        /**
         * Constructs a LexTreeUnitState
         *
         *
         * @param wordSequence the history of words
         */
        LexTreeUnitState(HMMNode hmmNode, 
               WordSequence wordSequence, 
               float languageProbability, float insertionProbability) {
            this(hmmNode, wordSequence, languageProbability,
                    insertionProbability, null);
        }

        /**
         * Constructs a LexTreeUnitState
         *
         *
         * @param wordSequence the history of words
         */
        LexTreeUnitState(HMMNode hmmNode, 
               WordSequence wordSequence, 
               float languageProbability, float insertionProbability,
               Node parentNode) {
            super(hmmNode, wordSequence);
            this.logInsertionProbability = insertionProbability;
            this.logLanguageProbability = languageProbability;
            this.parentNode = parentNode;
        }

        /**
         * Returns the base unit assciated with this state
         *
         * @return the base unit
         */
        public Unit getUnit() {
            return getHMMNode().getBaseUnit();
        }

        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        public int hashCode() {
            int hc = super.hashCode() * 17 + 421;
            if (parentNode != null) {
                hc *=432;
                hc += parentNode.hashCode();
            }
            return hc;
        }

        /**
         * Determines if the given object is equal to this object
         * 
         * @param o the object to test
         * @return <code>true</code> if the object is equal to this
         */
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof LexTreeUnitState) {
                LexTreeUnitState other = (LexTreeUnitState) o;
                return  super.equals(o) && parentNode == other.parentNode;
            } else {
                return false;
            }
        }

        /**
         * Returns the unit node for this state
         *
         * @return the unit node
         */
        private HMMNode getHMMNode() {
            return (HMMNode) getNode();
        }

         /**
          * Returns the list of successors to this state
          *
          * @return a list of SearchState objects
          */
         public SearchStateArc[] getSuccessors() {
            SearchStateArc[] arcs = new SearchStateArc[1];
            HMM hmm = getHMMNode().getHMM();
            arcs[0] = new LexTreeHMMState(getHMMNode(),
                 getWordHistory(), hmm.getInitialState(),
                 logOne, logOne, logOne, parentNode);
            return arcs;
         }

         public String toString() {
             return super.toString() + " unit";
         }

         /**
          * Gets the acoustic  probability of entering this state
          *
          * @return the log probability
          */
         public float getInsertionProbability() {
             return logInsertionProbability;
         }

         /**
          * Gets the language probability of entering this state
          *
          * @return the log probability
          */
         public float getLanguageProbability() {
             return logLanguageProbability;
         }
    }

    /**
     * Represents a HMM state in the search space
     */
    public class LexTreeHMMState extends LexTreeState 
            implements HMMSearchState {
    
        private HMMState hmmState;
        private float logLanguageProbability;
        private float logInsertionProbability;
        private float logAcousticProbability;
        private Node parentNode;

        /**
         * Constructs a LexTreeHMMState
         *
         * 
         * @param hmmNode the hmm state associated with this unit
         *
         * @param wordSequence the word history 
         *
         * @param languageProbability the probability of the transition
         * @param insertionProbability the probability of the transition
         * @param acousticProbability the probability of the transition
         * occuring
         */
        LexTreeHMMState(HMMNode hmmNode, WordSequence wordSequence, 
               HMMState hmmState, float languageProbability,
	       float insertionProbability,
               float acousticProbability, Node parentNode) {
            super(hmmNode, wordSequence);
            this.hmmState = hmmState;
            this.parentNode = parentNode;
            this.logLanguageProbability = languageProbability;
            this.logAcousticProbability = acousticProbability;
            this.logInsertionProbability = insertionProbability;
        }

        /**
         * Gets the ID for this state
         *
         * @return the ID
         */
        public String getSignature() {
            return super.getSignature() + "-HMM-" + hmmState.getState();
        }

        /**
         * returns the hmm state associated with this state
         *
         * @return the hmm state
         */
        public HMMState getHMMState() {
            return hmmState;
        }

        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        public int hashCode() {
            int hc =  super.hashCode() * 29 + (hmmState.getState() + 1);
            if (parentNode != null) {
                hc *=377;
                hc += parentNode.hashCode();
            }
            return hc;
        }

        /**
         * Determines if the given object is equal to this object
         * 
         * @param o the object to test
         * @return <code>true</code> if the object is equal to this
         */
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof LexTreeHMMState) {
                LexTreeHMMState other = (LexTreeHMMState) o;
                return super.equals(o) && hmmState == other.hmmState
                    && parentNode == other.parentNode ;
            } else {
                return false;
            }
        }

         /**
          * Gets the acoustic  probability of entering this state
          *
          * @return the log probability
          */
         public float getAcousticProbability() {
             return logAcousticProbability;
         }

         /**
          * Gets the language probability of entering this state
          *
          * @return the log probability
          */
         public float getLanguageProbability() {
             return logLanguageProbability;
         }

         /**
          * Gets the language probability of entering this state
          *
          * @return the log probability
          */
         public float getInsertionProbability() {
             return logInsertionProbability;
         }

        /**
         * Retreives the set of successors for this state
         *
         * @return the list of sucessor states
         */
        public SearchStateArc[] getSuccessors() {
            SearchStateArc[] nextStates = null;

            // if this is an exit state, we are transitioning to a
            // new unit or to a word end.

            if (hmmState.isExitState()) {
                if (parentNode == null) {
                    nextStates =  super.getSuccessors();
                } else {
                    nextStates = super.getSuccessors(parentNode);
                }
            } else {
                // The current hmm state is not an exit state, so we
                // just go through the next set of successors

                HMMStateArc[] arcs = hmmState.getSuccessors();
                nextStates = new SearchStateArc[arcs.length];
                for (int i = 0; i < arcs.length; i++) {
                    HMMStateArc arc = arcs[i];
                    if (arc.getHMMState().isEmitting()) {
                        // if its a self loop and the prob. matches
                        // reuse the state
                        if (false && arc.getHMMState()  == hmmState &&
                            logAcousticProbability == arc.getLogProbability()) {
                            nextStates[i] = this;
                        } else {
                            nextStates[i] = new LexTreeHMMState(
                                (HMMNode) getNode(), getWordHistory(),
                                arc.getHMMState(), logOne, logOne,
                                arc.getLogProbability(), parentNode);
                        }
                    } else {
                        nextStates[i] = new LexTreeNonEmittingHMMState(
                            (HMMNode) getNode(), getWordHistory(),
                            arc.getHMMState(),
                            arc.getLogProbability(), parentNode);
                    }
                }
            }
            return nextStates;
        }

         /**
          * Determines if this is an emitting state
          */
         public boolean isEmitting() {
             return hmmState.isEmitting();
         }

         public String toString() {
             return super.toString() + " hmm:" +  hmmState;
         }
    }

    public class LexTreeNonEmittingHMMState extends LexTreeHMMState {
        
        /**
         * Constructs a NonEmittingLexTreeHMMState
         * 
         * @param hmmState the hmm state associated with this unit
         *
         * @param wordSequence the word history 
         *
         * @param probability the probability of the transition
         * occuring
         */
        LexTreeNonEmittingHMMState(HMMNode hmmNode, 
               WordSequence wordSequence, 
               HMMState hmmState, float probability, Node parentNode) {
            super(hmmNode, wordSequence, hmmState, logOne, logOne, 
                    probability, parentNode);
        }
    }




    /**
     * Represents a word state in the search space
     */
    public class LexTreeWordState extends LexTreeState 
        implements WordSearchState {

        private HMMNode lastNode;
        private float logLanguageProbability;

        /**
         * Constructs a LexTreeWordState
         *
         * @param wordNode the word node
         *
         * @param wordSequence the sequence of words
         * triphone context
         * 
         * @param logProbability the probability of this word occuring
         *
         */
        LexTreeWordState(WordNode wordNode, HMMNode lastNode,
               WordSequence wordSequence, float logProability) {

            super(wordNode, wordSequence);
            this.lastNode = lastNode;
            this.logLanguageProbability = logProability;
        }

        /**
         * Gets the word pronunciation for this state
         *
         * @return the pronunciation for this word
         */
        public Pronunciation getPronunciation() {
            return ((WordNode) getNode()).getPronunciation();
        }

         /**
          * Determines if this is a final state
          *
          * @return <code>true</code> if this is an final state.
          */
         public boolean isFinal() {
             return getPronunciation().getWord().equals(sentenceEndWord);
         }

        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        public int hashCode() {
            return super.hashCode() * 41 + lastNode.hashCode();
        }

        /**
         * Gets the unique signature for this state. The signature
         * building code is slow and should only be used for
         * non-time-critical tasks such as plotting states.
         *
         * @return the signature
         */
        public String getSignature() {
            return super.getSignature() + "-ln-" + lastNode.hashCode();
        }

        /**
         * Determines if the given object is equal to this object
         * 
         * @param o the object to test
         * @return <code>true</code> if the object is equal to this
         */
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof LexTreeWordState) {
                LexTreeWordState other = (LexTreeWordState) o;
                return  super.equals(o) && lastNode == other.lastNode;
            } else {
                return false;
            }
        }

         /**
          * Gets the language probability of entering this state
          *
          * @return the log probability
          */
         public float getLanguageProbability() {
             return logLanguageProbability;
          }

         /**
          * Returns the list of successors to this state
          *
          * @return a list of SearchState objects
          */
         public SearchStateArc[] getSuccessors() {
             SearchStateArc[] arcs = EMPTY_ARC;
             WordNode wordNode = (WordNode) getNode();

             if (wordNode.getWord() != sentenceEndWord) {
                 int index = 0;
                 List list = new ArrayList();
                 Unit[] rc  = lastNode.getRC();
                 Unit left = wordNode.getLastUnit();

                 for (int i = 0; i < rc.length; i++) {
                     Collection epList = hmmTree.getEntryPoint(left, rc[i]);
                     list.addAll(epList);
                 }

                 arcs = new SearchStateArc[list.size()];
                 for (Iterator i = list.iterator(); i.hasNext(); ) {
                     HMMNode node = (HMMNode) i.next();
                     arcs[index++] = createUnitStateArc(node,
                             LogMath.getLogOne());
                 }
             }
             return arcs;
         }
    }


    /**
     * Determines the insertion probability for the given unit lex
     * node
     *
     * @param hmmNode the unit lex node
     *
     * @return the insertion probability
     */
    private float calculateInsertionProbability(UnitNode unitNode) {
        float logInsertionProbability = logUnitInsertionProbability;
        if (unitNode.getBaseUnit().isSilence()) {
            logInsertionProbability = logSilenceInsertionProbability;
        } else if (unitNode.getPosition().isWordBeginning()) {
            logInsertionProbability += logWordInsertionProbability;
        }
        return logInsertionProbability;
    }

    /**
     * Retrieves the unigram smear from the given node
     *
     * @return the unigram smear
     */
    private float getUnigramSmear(Node node) {
        float prob;
        if (wantUnigramSmear) {
            prob = node.getUnigramProbability() * unigramSmearWeight;
        } else {
            prob = logOne;
        }
        return prob;
    }


    /**
     * Gets the  set of hmm nodes associated with the given end node
     *
     * @param endNode the end node
     *
     * @return an array of associated hmm nodes
     */
    private HMMNode[] getHMMNodes(EndNode endNode) {
        return hmmTree.getHMMNodes(endNode);
    }
}

