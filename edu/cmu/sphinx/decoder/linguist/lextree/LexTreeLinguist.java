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

package edu.cmu.sphinx.decoder.linguist.lextree;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.sphinx.decoder.linguist.Grammar;
import edu.cmu.sphinx.decoder.linguist.HMMSearchState;
import edu.cmu.sphinx.decoder.linguist.Linguist;
import edu.cmu.sphinx.decoder.linguist.SearchState;
import edu.cmu.sphinx.decoder.linguist.SearchStateArc;
import edu.cmu.sphinx.decoder.linguist.UnitSearchState;
import edu.cmu.sphinx.decoder.linguist.WordSearchState;
import edu.cmu.sphinx.decoder.linguist.util.HMMPool;
import edu.cmu.sphinx.decoder.linguist.util.LinguistTimer;
import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;
import edu.cmu.sphinx.knowledge.acoustic.HMM;
import edu.cmu.sphinx.knowledge.acoustic.HMMPosition;
import edu.cmu.sphinx.knowledge.acoustic.HMMState;
import edu.cmu.sphinx.knowledge.acoustic.HMMStateArc;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.knowledge.dictionary.Dictionary;
import edu.cmu.sphinx.knowledge.dictionary.Pronunciation;
import edu.cmu.sphinx.knowledge.dictionary.Word;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.knowledge.language.WordSequence;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

/**
 * A linguist that organizes the search space in a lex tree
 */
public class LexTreeLinguist implements  Linguist {

    /**
     * Prefix for search.Linguist.LexTreeLinguist  SphinxProperties.
     */
    private final static String PROP_PREFIX =
	"edu.cmu.sphinx.decoder.linguist.lextree.LexTreeLinguist.";


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
      * A sphinx property that determines whether or not the linguist
      * will maintain separate right context for word states. If this
      * property is set to true (the default), distinct word states
      * are maintained for all possible right contexts during the
      * search. This yields a correct search. When set to false, the
      * linguist will treat words with different right contexts as
      * equivalent.
      */
    public final static String PROP_MAINTAIN_SEPARATE_WORD_RC
        = PROP_PREFIX + "maintainSeparateWordRC";

    /**
     * The default value for PROP_MAINTAIN_SEPARATE_WORD_RC
     */
    public final static boolean PROP_MAINTAIN_SEPARATE_WORD_RC_DEFAULT = true;


    /**
     * An array of classes that represents the order 
     * in which the states will be returned.
     */
    private final static Class[] searchStateOrder = {
        LexTreeInitialState.class,
        LexTreeNonEmittingHMMState.class,
        LexTreeWordState.class,
        LexTreeEndWordState.class,
        LexTreeUnitState.class,
        LexTreeHMMState.class
    };


    private SphinxProperties props;

    private LanguageModel languageModel;
    private AcousticModel acousticModel;

    private LogMath logMath;
    private float languageWeight;
    private float logWordInsertionProbability;
    private float logSilenceInsertionProbability;
    private float logUnitInsertionProbability;

    // just for detailed debugging
    private final boolean tracing = false;


    private HMMPool hmmPool;
    private LexTree lexTree;
    private Dictionary dictionary;
    private final static SearchStateArc[] EMPTY_ARC = new SearchStateArc[0];
    
    private float logOne;
    private int silenceID;
    private LexTree.WordLexNode finalNode;
    private boolean fullWordHistories = true;
    private boolean maintainSeparateRightContextsForWords = false;

    private Word sentenceEndWord;
    private Word[] sentenceStartWordArray;
    private LexTreeEndWordState endWord;

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
     * @param dictionary the dictionary for this linguist
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

        this.maintainSeparateRightContextsForWords =
                    props.getBoolean(PROP_MAINTAIN_SEPARATE_WORD_RC,
                    PROP_MAINTAIN_SEPARATE_WORD_RC_DEFAULT);

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

        logUnitInsertionProbability = logMath.linearToLog
            (props.getDouble
             (Linguist.PROP_UNIT_INSERTION_PROBABILITY,
              Linguist.PROP_UNIT_INSERTION_PROBABILITY_DEFAULT));

        languageWeight = props.getFloat(PROP_LANGUAGE_WEIGHT,
                                        PROP_LANGUAGE_WEIGHT_DEFAULT);


        compileGrammar();

        acousticModel = null;

        if (false) {
            LinguistTimer lt = new LinguistTimer(this, false);
            lt.timeLinguist(10, 500, 1000);
        }

        finalNode = lexTree.findWordNode(Dictionary.SENTENCE_END_SPELLING);

        this.dictionary = null;
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
        return new LexTreeInitialState(lexTree.getInitialNode());
    }


    /**
     * Compiles the n-gram into a lex tree that is used during the
     * search
     */
    protected void compileGrammar() {
        Timer.start("compile");

        hmmPool = new HMMPool(acousticModel);
        silenceID = hmmPool.getID(Unit.SILENCE);

        lexTree = new LexTree(hmmPool, dictionary, languageModel);

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
    abstract class LexTreeState implements SearchState, SearchStateArc {
        private int leftID;
        private LexTree.UnitLexNode central;
        private LexTree.UnitLexNode right;
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
         */
        LexTreeState(int leftID,
                     LexTree.UnitLexNode central,
                     LexTree.UnitLexNode right,
                     WordSequence wordSequence) {

            if (leftID == 0) {
                leftID = silenceID;
            }

            // if the central portion is silence then
            // we don't care about left and right contexts
            // TODO: This can be more generalized to deal with
            // variable sized contexts

            if (central != null && central.getID() == silenceID) {
                leftID = silenceID;
                right = null;
            }

            this.leftID = leftID;
            this.central = central;
            this.right = right;
            this.wordSequence = wordSequence;
        }


        /**
         * Gets the unique signature for this state. The signature
         * building code is slow and should only be used for
         * non-time-critical tasks such as plotting states.
         *
         * @return the ID
         */
        public String getSignature() {
            int c = 121;
            int r = 123;
            if (central != null) {
                c = central.hashCode();
            }
            if (right != null) {
                r = right.hashCode();
            }
            return "c:" + c + "-r:" + r + "-l;" + leftID + "-wh:" +
                wordSequence;
        }


        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        public int hashCode() {
            int c = 121;
            int l = leftID;
            int r = 127;

            if (central != null) {
                c = central.hashCode();
            }
            if (right != null) {
                r = right.hashCode();
            }
            int hashCode = fullWordHistories ? wordSequence.hashCode()*37 : 37;
            hashCode +=  (c << 22) | (l << 8) | (r);
            return hashCode;
        }

        /**
         * Returns the lextree state
         *
         * @return the lex tree state
         */
         public Object getLexState() {
             return central;
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
                return  central == other.central &&
                        leftID == other.leftID &&
                        right == other.right &&  wordSequenceMatch;
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
        public abstract float getProbability();

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
          * Gets the lex tree node representing the 
          * central portion of the triphone unit
          *
          * @return the central unit lex tree node
          */
         protected LexTree.UnitLexNode getCentral() {
             return central;
         }

         /**
          * Gets the lex tree node representing the 
          * right portion of the triphone unit
          *
          * @return the right unit lex tree node
          */
         protected LexTree.UnitLexNode getRight() {
             return right;
         }

         /**
          * Gets id of the left context
          *
          * @return the left unit ID (or 0 if there is no left
          * context)
          */
         protected int getLeftID() {
             return leftID;
         }


         /**
          * Returns the word sequence for this state
          *
          * @return the word sequence
          */
         public WordSequence getWordHistory() {
             return wordSequence;
         }


         /**
          * Returns the list of successors to this state
          *
          * @return a list of SearchState objects
          */
         abstract public SearchStateArc[] getSuccessors();

         /**
          * Returns the string representation of this object
          *
          * @return the string representation
          */
        public String toString() {
            String pos = central == null ? "" 
                : central.getPosition().toString();
            Unit left = hmmPool.getUnit(leftID);


            return central + "[" + left +
                   "," + right +"]@" + pos + " " + getProbability() +
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
     * An initial state in the search space. It is non-emitting
     * and most likely has UnitSearchStates as successors
     */
    public class LexTreeInitialState extends LexTreeState {
        private LexTree.NonLeafLexNode node;

        /**
         * Creates a LexTreeInitialState object
         */
        LexTreeInitialState(LexTree.NonLeafLexNode node) {
            super(silenceID, null, null, 
                  (WordSequence.getWordSequence(sentenceStartWordArray).trim
                   (languageModel.getMaxDepth() - 1)));
            this.node = node;
        }

        /**
         * Gets the composite probability of entering this state
         *
         * @return the log probability
         */
        public float getProbability() {
            return logOne;
        }

        /**
         * Gets the ID for this state
         *
         * @return the ID
         */
        public String getSignature() {
            return super.getSignature() + "-INIT";
        }

        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        public int hashCode() {
            return super.hashCode() * 17 + node.hashCode();
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
            } else if (o instanceof LexTreeInitialState) {
                LexTreeInitialState other = (LexTreeInitialState) o;
                return  super.equals(o) && node == other.node;
            } else {
                return false;
            }
        }

        /**
         * Gets a successor to this language state
         *
         * @return a successor
         */
        public SearchStateArc[] getSuccessors(){
            return getArcsToAllWords(this);
        }


        /**
         * Returns a string representation of this object
         *
         * @return a string representation 
         */
        public String toString() {
            return super.toString() + " initial";
        }
    }


    /**
     * Represents a unit in the search space
     */
    public class LexTreeUnitState extends LexTreeState 
    implements UnitSearchState {
        int unitID;
        float logInsertionProbability;

        /**
         * Constructs a LexTreeUnitState
         *
         * @param leftID the id of the unit forming the left context
         * (or 0 if there is no left context) of a triphone context
         *
         * @param central the unit forming the central portion of a
         * triphone context
         *
         * @param right the unit forming the right portion of a
         * triphone context
         *
         * @param wordSequence the history of words
         */
        LexTreeUnitState(int leftID,
               LexTree.UnitLexNode central, LexTree.UnitLexNode right,
               WordSequence wordSequence, float languageProbability) {

            super(leftID, central, right, wordSequence);

            logInsertionProbability = 
                calculateInsertionProbability(getCentral());

            int rightID;

            if (leftID == 0) {
                leftID = silenceID;
            } 


            if (right == null) {
                rightID = silenceID;
            } else {
                rightID = right.getID();
            }

            unitID = hmmPool.buildID(
                        central.getID(), leftID, rightID);

            if (hmmPool.getUnit(unitID) == null) {
                System.out.println("bad unit " + leftID + " " +
                        central + " right " + right);
                System.out.println("left " + hmmPool.getUnit(leftID));
                System.out.println("center " +
                        hmmPool.getUnit(central.getID()));
                System.out.println("right " + hmmPool.getUnit(rightID));
            }

            if (false) {
                System.out.println("Created LexTreeUnit state for " + this
                        + " prob is " + languageProbability);
            }
        }

         /**
          * Gets the composite probability of entering this state
          *
          * @return the log probability
          */
        public final float getProbability() {
            return logInsertionProbability;
        }

        /**
         * Returns the insertion probability of this state.
         *
         * @return the insertion probability
         */
        public float getInsertionProbability() {
            return logInsertionProbability;
        }

        /**
         * Gets the ID for this state
         *
         * @return the ID
         */
        public String getSignature() {
            return super.getSignature() + "-UNIT";
        }

        /**
         * Returns the triphone unit assciated with this state
         *
         * @return the triphone unit
         */

        public Unit getUnit() {
            return hmmPool.getUnit(unitID);
        }

        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        public int hashCode() {
            return super.hashCode() * 17 + unitID;
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
                return  super.equals(o) && unitID == other.unitID;
            } else {
                return false;
            }
        }

        /**
         * Returns the successors for this unit. The successors for a
         * unit are always the initial states(s) of the HMM associated
         * with the unit
         *
         * @return the list of successors for this state
         */
        public SearchStateArc[] getSuccessors() {
            SearchStateArc[] nextStates   = new SearchStateArc[1];
            HMMPosition position = getCentral().getPosition();

            HMM hmm = hmmPool.getHMM(unitID, position);

            if (hmm == null) {
                System.out.println(unitID + " pos " + position);
            }
            assert hmm != null;

            nextStates[0] = new LexTreeHMMState(getLeftID(), getCentral(),
                getRight(), getWordHistory(), hmm.getInitialState(), logOne);
            return nextStates;
        }


        /*
         * Returns a string representation of this object
         *
         * @return a string representation of this object
         */
        public String toString() {
            return super.toString() + " unit";
        }

        /*
         * Returns a pretty string representation
         *
         * @return a pretty string representation
         */
        public String toPrettyString() {
            HMMPosition position = getCentral().getPosition();
            return getUnit().toString() + "@" +  position;
        }
    }

    /**
     * Represents a HMM state in the search space
     */
    public class LexTreeHMMState extends LexTreeState 
    implements HMMSearchState {
    
        private HMMState hmmState;
        private int hashCode = -1;
        private float logAcousticProbability;

        /**
         * Constructs a LexTreeHMMState
         *
         * @param leftID the id of the unit forming the left context
         * (or 0 if there is no left context) of a triphone context
         *
         * @param central the unit forming the central portion of a
         * triphone context
         *
         * @param right the unit forming the right portion of a
         * triphone context
         * 
         * @param hmmState the hmm state associated with this unit
         *
         * @param wordSequence the word history 
         *
         * @param probability the probability of the transition
         * occuring
         */
        LexTreeHMMState(int leftID, LexTree.UnitLexNode central, 
               LexTree.UnitLexNode right, WordSequence wordSequence, 
               HMMState hmmState, float probability) {
            super(leftID, central, right, wordSequence);
            this.logAcousticProbability = probability;
            this.hmmState = hmmState;
        }

        /**
         * Gets the composite probability of entering this state
         *
          * @return the log probability
          */
        public final float getProbability() {
            return logAcousticProbability;
        }

        /**
         * Returns the acoustic probability of entering this state.
         *
         * @return the acoustic probability
         */
        public float getAcousticProbability() {
            return logAcousticProbability;
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
            if (hashCode == -1) {
                hashCode = super.hashCode() * 29 + hmmState.getState();
            }
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
            } else if (o instanceof LexTreeHMMState) {
                LexTreeHMMState other = (LexTreeHMMState) o;
                return super.equals(o) && hmmState == other.hmmState;
            } else {
                return false;
            }
        }

        /**
         * Retreives the set of successors for this state
         *
         * @return the list of sucessor states
         */
        public SearchStateArc[] getSuccessors() {
            SearchStateArc[] nextStates;

            // if this is an exit state, we are transitioning to a
            // new unit or to a word end.

            if (hmmState.isExitState()) {

                // if this hmm  state is the last state of the last
                // unit of a word, then all next nodes must be word
                // nodes.

                if (getCentral().isWordEnd()) {
                    LexTree.LexNode[] nodes = getCentral().getNextNodes();

                    // if the last unit was a silence unit
                    // then we add a self loop. This allows the
                    // decoder to 'hang-out' at a silence.
                    if (getCentral().getUnit().isSilence()) {
                        nextStates = new SearchStateArc[nodes.length + 1];
                        nextStates[nextStates.length - 1] = 
                            new LexTreeUnitState(getLeftID(),
                                    getCentral(), getRight(),
                                    getWordHistory(), logOne);
                    } else {
                        nextStates = new SearchStateArc[nodes.length];
                    }
                    for (int i = 0; i < nodes.length; i++) {
                        LexTree.LexNode node = nodes[i];

                        if (! (node instanceof LexTree.WordLexNode)) {
                            throw new Error("Corrupt lex tree (word)");
                        }
                        nextStates[i] = createNextWordState(this, 
                                (LexTree.WordLexNode) node);
                    }
                } else {

                    // its not the end of the word, so the next set of 
                    // states must be units, so we advance the left,
                    // and central units and iterate through the
                    // possible right contexts.

                    int nextLeftID = getCentral() == null ?  
                                        0 : getCentral().getID();
                    LexTree.UnitLexNode nextCentral = getRight();

                    if (nextCentral.getUnit().isSilence()) {

                    // for units with no right context (silence for
                    // instance), we just create a single state
                    // instead of one for each right context.  We mark
                    // this by making the right context be null

                        nextStates = new SearchStateArc[1];
                        nextStates[0] = new LexTreeUnitState(
                                nextLeftID, nextCentral,  null,
                                getWordHistory(), logOne);
                    } else {
                        LexTree.LexNode[] nodes = getNextUnits(nextCentral);

                        nextStates = new SearchStateArc[nodes.length];

                        float languageProbability =
                            nextCentral.getProbability() -
                            getCentral().getProbability();

                        assert languageProbability == 0.0f;

                        for (int i = 0; i < nodes.length; i++) {
                            LexTree.LexNode nextRight = nodes[i];

                            if (! (nextRight instanceof LexTree.UnitLexNode)) {
                                throw new Error("Corrupt lex tree (hmm) ");
                            }

                            nextStates[i] = new LexTreeUnitState(
                                    nextLeftID, nextCentral, 
                                    (LexTree.UnitLexNode) nextRight,
                                    getWordHistory(),
                                    languageProbability);
                        }
                    }
                }
            } else {
                // The current hmm state is not an exit state, so we
                // just go through the next set of successors

                HMMStateArc[] arcs = hmmState.getSuccessors();
                nextStates = new SearchStateArc[arcs.length];
                for (int i = 0; i < arcs.length; i++) {
                    HMMStateArc arc = arcs[i];
                    HMMState state = arc.getHMMState();
                    if (state.isEmitting()) {
                        if (state == hmmState) {
                            logAcousticProbability = arc.getLogProbability();
                            nextStates[i] = this;
                        } else {
                            nextStates[i] = new LexTreeHMMState
                                (getLeftID(), getCentral(), getRight(),
                                 getWordHistory(),
                                 state, arc.getLogProbability());
                        }
                    } else {
                        nextStates[i] = new LexTreeNonEmittingHMMState
                            (getLeftID(), getCentral(), getRight(),
                             getWordHistory(), state,
                             arc.getLogProbability());
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

    /**
     * Represents  non-emitting hmm state in the lex tree
     */
    public class LexTreeNonEmittingHMMState extends LexTreeHMMState {
        
        /**
         * Constructs a NonEmittingLexTreeHMMState
         *
         * @param leftID the id of the unit forming the left context
         * (or 0 if there is no left context) of a triphone context
         *
         * @param central the unit forming the central portion of a
         * triphone context
         *
         * @param right the unit forming the right portion of a
         * triphone context
         * 
         * @param hmmState the hmm state associated with this unit
         *
         * @param wordSequence the word history 
         *
         * @param probability the probability of the transition
         * occuring
         */
        LexTreeNonEmittingHMMState(int leftID, LexTree.UnitLexNode central, 
                                   LexTree.UnitLexNode right, 
                                   WordSequence wordSequence, 
                                   HMMState hmmState, float probability) {
            super(leftID, central, right, wordSequence, hmmState, probability);
        }
    }


    /**
     * Gets the arcs to all of the successor nodes
     *
     * @param state the current state
     *
     * @return an array of arcs to the first units of all the wods
     */

    SearchStateArc[] getArcsToAllWords(LexTreeState state) {
        WordSequence wordSequence = state.getWordHistory();
        int leftID = state.getCentral() == null ?  0 
                        : state.getCentral().getID();
        List list = new ArrayList();
        LexTree.LexNode[] nodes =lexTree.getInitialNode().getNextNodes();

        for (int i = 0; i < nodes.length; i++) {
            LexTree.LexNode node = nodes[i];

            if ((node instanceof LexTree.UnitLexNode)) {

                LexTree.UnitLexNode central = (LexTree.UnitLexNode) node;

                if (central.getUnit().isSilence()) {
                    list.add(new LexTreeUnitState(leftID, central, null,
                                wordSequence, logOne));
                } else {
                    LexTree.LexNode[] rightNodes = getNextUnits(central);

                    for (int j = 0; j < rightNodes.length; j++) {
                        list.add(new LexTreeUnitState(leftID, 
                                    central, (LexTree.UnitLexNode)
                                    rightNodes[j], wordSequence,
                                    central.getProbability()));
                    }
                }
            } else {
                throw new Error("Corrupt lex tree (initial state)");
            }
        }
        // list.add(createNextWordState(state, finalNode));
        list.add(getEndWord());
        return (SearchStateArc[]) 
            list.toArray(new SearchStateArc[list.size()]);
    }

    /**
     * Represents a word state in the search space
     */
    public class LexTreeWordState extends LexTreeState 
        implements WordSearchState {
        private LexTree.WordLexNode wordLexNode;
        private float logLanguageProbability;

        /**
         * Constructs a LexTreeWordState
         *
         * @param leftID the id of the unit forming the left context
         * (or 0 if there is no left context) of a triphone context
         *
         * @param central the unit forming the central portion of a
         * triphone context
         *
         * @param right the unit forming the right portion of a
         * triphone context
         *
         * @param wordSequence the sequence of words
         * triphone context
         * 
         * @param wordLexNode the lex tree node associated with this
         * word
         *
         * @param logProbability the probability of this word occuring
         *
         */
        LexTreeWordState(int leftID,
               LexTree.UnitLexNode central, 
               LexTree.UnitLexNode right, 
               WordSequence wordSequence,
               LexTree.WordLexNode wordLexNode,
               float logProbability) {

            super(leftID, central, right, wordSequence);
            this.wordLexNode = wordLexNode;
            if (wordLexNode == null) {
                throw new Error("Null WordLexNode while creating: " +
                                wordSequence.toString());
            }
            logLanguageProbability = logProbability * languageWeight;
        }

        /**
         * Gets the composite probability of entering this state
         *
         * @return the log probability
         */
        public float getProbability() {
            return logLanguageProbability;
        }

        /**
         * Returns the language probability of entering this state
         *
         * @return the language probability
         */
        public float getLanguageProbability() {
            return logLanguageProbability;
        }

        /**
         * Gets the ID for this state
         *
         * @return the ID
         */
        public String getSignature() {
            return super.getSignature() + "-WORD-" + wordLexNode.hashCode();
        }

        /**
         * Gets the word pronunciation for this state
         *
         * @return the pronunciation for this word
         */
        public Pronunciation getPronunciation() {
            return wordLexNode.getPronunciation();
        }

         /**
          * Determines if this is a final state
          *
          * @return <code>true</code> if this is an final state.
          */
         public boolean isFinal() {
             return wordLexNode.getPronunciation()
                 .getWord().equals(sentenceEndWord);
         }


        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        public int hashCode() {
            if (wordLexNode == null) {
                throw new Error("No wordLexNode");
            }
            return super.hashCode() * 31 + wordLexNode.hashCode();
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
                return super.equals(o) && wordLexNode == other.wordLexNode;
            } else {
                return false;
            }
        }

        /**
         * Retrieves the successors for this node
         *
         * @return the list of successor states
         */
        public SearchStateArc[] getSuccessors() {
            LexTree.UnitLexNode nextCentral = getRight();

            // if this is the final state, we do not have any
            // succesors

            if (isFinal()) {
                return EMPTY_ARC;
            }

            // if the new next central unit is null, this word will
            // connect to all next units ... that is, it is not
            // constrained at all. This typically occurs if the last
            // unit of a word has a zero context size (such as a
            // silence)

            else if (nextCentral == null) {
                return getArcsToAllWords(this);
            } else {
                int nextLeftID = getCentral().getID();
                LexTree.LexNode[] nextNodes = getNextUnits(nextCentral);
                SearchStateArc[] nextStates 
                    = new SearchStateArc[nextNodes.length + 1];

                for (int i = 0; i < nextNodes.length; i++) {
                    LexTree.LexNode nextRight = nextNodes[i];
                        nextStates[i] = new LexTreeUnitState(nextLeftID, 
                                nextCentral, (LexTree.UnitLexNode)
                                nextRight, getWordHistory(),
                                nextCentral.getProbability());
                }
                //nextStates[nextStates.length - 1] = 
                /*
                nextStates[nextStates.length -1] = 
                    createNextWordState(this, finalNode);
                */
                nextStates[nextStates.length -1] = getEndWord();
                return nextStates;
            }
        }

         public String toString() {
             return " Word:" + wordLexNode.getPronunciation() + " " + 
                 super.toString();
         }

         /**
          * Returns a pretty string version of this state
          *
          * @return the pretty string representation
          */
         public String toPrettyString() {
             String spelling =
                 wordLexNode.getPronunciation().getWord().getSpelling();
             if (isFinal()) {
                 return spelling;
             } else {
                 return spelling
                     + "[" + getCentral() + "," + getRight() + "]"; 
             }
         }
    }


    /**
     * Given a unit node, return the next set of units.  If the unit
     * node marks the end of the word, then the next set of unit nodes
     * are the set of beginning word units
     *
     * @param unitNode the unit node of interest
     *
     * @return an array of next unit nodes
     */
    private LexTree.LexNode[]  getNextUnits(LexTree.UnitLexNode unitNode) {
        if (unitNode.isWordEnd()) {
            return lexTree.getInitialNode().getNextNodes();
        } else {
            return unitNode.getNextNodes();
        }
    }

    /**
     * Determines the insertion probability for the given unit lex
     * node
     *
     * @param unitNode the unit lex node
     *
     * @return the insertion probability
     */
    private float calculateInsertionProbability(LexTree.UnitLexNode unitNode) {
        float logInsertionProbability = logUnitInsertionProbability;
        if (unitNode.getUnit().isSilence()) {
            logInsertionProbability = logSilenceInsertionProbability;
        } else if (unitNode.isWordBeginning()) {
            logInsertionProbability += logWordInsertionProbability;
        }
        return logInsertionProbability;
    }


    /**
     * Given a search state and the next lextree node representing a
     * word, create the new search state with the proper probability.
     *
     * @param curState the current search state
     * @param nextWord the next word in the lex tree
     *
     * @return the arc to the search state associated with the next word
     */
    private SearchStateArc createNextWordState(LexTreeState
            curState, LexTree.WordLexNode nextWord) {

        WordSequence wordSequence = curState.getWordHistory();
        float logProbability = logOne;
        if (!nextWord.isSilence()) {
            wordSequence = wordSequence.addWord
                (nextWord.getPronunciation().getWord(),
                 languageModel.getMaxDepth());
            logProbability = languageModel.getProbability(wordSequence);
            // remove the probability encountered so far
            //  logProbability -= curState.getProbability();

            if (false) {
                System.out.println(wordSequence + " " + logProbability);
            }
        }
        // The left unit of a word state doesn't matter to the world,
        // so we can reduce the number of word states (especially for
        // one unit words) by eliminating the lef it (by setting it to
        // 0).

        LexTree.UnitLexNode right = maintainSeparateRightContextsForWords
            ? curState.getRight() : null;

        return new LexTreeWordState(0,
            curState.getCentral(), right, 
            wordSequence.trim(languageModel.getMaxDepth() - 1), 
            nextWord, logProbability);
    }

    /**
     * returns the sole end word. The probability of transitioning to this 
     * word is fixed (which may be a bug).
     *
     * @return the end word.
     */
    private SearchStateArc getEndWord() {
        if (endWord == null) {
            endWord = new LexTreeEndWordState();
        }
        return endWord;
    }

    /**
     * Represents the end word state in the search space.
     */
    public class LexTreeEndWordState extends LexTreeWordState {
     
        /**
         * Constructs a default LexTreeEndWordState.
         */
        public LexTreeEndWordState() {
            super(0, null, null, WordSequence.EMPTY, finalNode, 0.0f);
        }
    }
}

