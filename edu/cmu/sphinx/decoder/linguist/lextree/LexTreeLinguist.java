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

import edu.cmu.sphinx.decoder.linguist.Linguist;
import edu.cmu.sphinx.decoder.linguist.Grammar;
import edu.cmu.sphinx.knowledge.dictionary.Dictionary;
import edu.cmu.sphinx.knowledge.dictionary.FullDictionary;
import edu.cmu.sphinx.decoder.linguist.SentenceHMMState;

import edu.cmu.sphinx.knowledge.acoustic.HMM;
import edu.cmu.sphinx.knowledge.acoustic.HMMState;
import edu.cmu.sphinx.knowledge.acoustic.HMMStateArc;
import edu.cmu.sphinx.knowledge.acoustic.HMMPosition;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;
import edu.cmu.sphinx.knowledge.acoustic.Context;
import edu.cmu.sphinx.knowledge.acoustic.LeftRightContext;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashSet;

/**
 */
public class LexTreeLinguist implements  Linguist {

    /**
     * Sphinx property used to determine whether or not
     * the gstates are dumped.
     */
    public final static String PROP_DUMP_GSTATES =
        "edu.cmu.sphinx.decoder.linguist.SimpleLinguist.dumpGstates";
    /**
     * The default value for the PROP_DUMP_GSTATES property
     */
    public final static boolean PROP_DUMP_GSTATES_DEFAULT = false;

    private SphinxProperties props;

    private SentenceHMMState initialSentenceHMMState;
    private LanguageModel languageModel;
    private AcousticModel acousticModel;
    private LogMath logMath;


    private double logWordInsertionProbability;
    private double logSilenceInsertionProbability;
    private double logUnitInsertionProbability;

    private boolean showSentenceHMM;
    private boolean showCompilationProgress = true;

    private StatisticsVariable totalStates;
    private StatisticsVariable totalArcs;
    private StatisticsVariable actualArcs;


    private Map nodeStateMap = new HashMap();
    private Map arcPool = new HashMap();

    private transient int totalStateCounter = 0;
    private transient Collection stateSet = null;

    private boolean spreadWordProbabilitiesAcrossPronunciations;
    private double logOne;
    private boolean dumpGStates;

    // just for detailed debugging
    private final boolean tracing = false;


    private HMMPool hmmPool;
    private LexTree lexTree;
    private Dictionary dictionary;


    /**
     * Creates a LexTree linguist associated with the given context
     *
     * @param context the context to associate this linguist with
     * @param languageModel the language model
     * @param grammar the grammar for this linguist
     * @param models the acoustic model used by this linguist
     */
    public void initialize(String context, LanguageModel languageModel,
                        Grammar grammar, AcousticModel[] models) {
        assert models.length == 1;

        this.props = SphinxProperties.getSphinxProperties(context);
        this.acousticModel = models[0];
        this.logMath = LogMath.getLogMath(context);
        this.languageModel = languageModel;


        try {
            this.dictionary = new FullDictionary(context);
        } catch (IOException ioe) {
            throw new Error("LexTreeLinguist: Can't load dictionary", ioe);
        }

        logOne = logMath.getLogOne();


        logWordInsertionProbability = logMath.linearToLog(
                props.getDouble(Linguist.PROP_WORD_INSERTION_PROBABILITY, 1.0));

        logSilenceInsertionProbability = logMath.linearToLog(
            props.getDouble(Linguist.PROP_SILENCE_INSERTION_PROBABILITY, 1.0));

        logUnitInsertionProbability =  logMath.linearToLog(
            props.getDouble(Linguist.PROP_UNIT_INSERTION_PROBABILITY, 1.0));

        showCompilationProgress =
            props.getBoolean(Linguist.PROP_SHOW_COMPILATION_PROGRESS, false);


        totalStates = StatisticsVariable.getStatisticsVariable(
                props.getContext(), "totalStates");
        totalArcs = StatisticsVariable.getStatisticsVariable(
                props.getContext(), "totalArcs");
        actualArcs = StatisticsVariable.getStatisticsVariable(
                props.getContext(), "actualArcs");
        stateSet = compileGrammar();

        // totalStates.value = stateSet.size();

        // after we have compiled the grammar, we no longer need
        // these things, so release them so that resources can be
        // reclaimed.

        acousticModel = null;
        nodeStateMap = null;
        arcPool = null;

        StatisticsVariable.dumpAll();
    }

    /**
     * 
     * Called before a recognition
     */
    public void start() {
    }

    /**
     * Called after a recognition
     */
    public void stop() {
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
     * Retrieves initial SentenceHMMState
     * 
     * @return the set of initial SentenceHMMState
     */
    public SentenceHMMState getInitialState() {
        return initialSentenceHMMState;
    }


    public LanguageState getInitialLanguageState() {
        return new LexTreeInitialState(lexTree.getInitialNode());
    }


    /**
     * Compiles the grammar into a sentence hmm.  A GrammarJob is
     * created for the initial grammar node and added to the
     * GrammarJob queue. While there are jobs left on the grammar job
     * queue, a job is removed from the queue and the associated
     * grammar node is expanded and attached to the tails. GrammarJobs
     * for the successors are added to the grammar job queue.
     */
    protected Collection compileGrammar() {
        List gstateList = new ArrayList();

        Timer.start("compile");

        Timer.start("buildHmmPool");
        hmmPool = new HMMPool(acousticModel);
        Timer.stop("buildHmmPool");

        lexTree = new LexTree(hmmPool, dictionary,
                languageModel.getVocabulary());

        hmmPool.dumpInfo();

        Timer.stop("compile");
        // Now that we are all done, dump out some interesting
        // information about the process

        Timer.dumpAll();

        return null;
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
    abstract class LexTreeState implements LanguageState {
        private double logProbability;
        private int thisUnitID;
        private LexTree.UnitLexNode left;
        private LexTree.UnitLexNode central;
        private LexTree.UnitLexNode right;


        /**
         * Creates a LexTreeState.
         *
         * @param left the unit forming the left context (or null if
         * there is no left context) of a triphone context
         *
         * @param central the unit forming the central portion of a
         * triphone context
         *
         * @param right the unit forming the right portion of a
         * triphone context
         *
         * @param logProbability the probability (in log math domain)
         * of entering this state.  This is a combination of insertion
         * and language probability.
         */
        LexTreeState(
                LexTree.UnitLexNode left,
                LexTree.UnitLexNode central,
                LexTree.UnitLexNode right,
                double logProbability) {
            this.left = left;
            this.central = central;
            this.right = right;
            this.logProbability = logProbability;
        }

        /**
         * Gets the probability associated with entering this
         * state.
         *
         * @return the log math base probability of entering this
         * state
         */
         public  double getProbability() {
            return logProbability;
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
         public LexTree.UnitLexNode getCentral() {
             return central;
         }

         /**
          * Gets the lex tree node representing the 
          * right portion of the triphone unit
          *
          * @return the right unit lex tree node
          */
         public LexTree.UnitLexNode getRight() {
             return right;
         }

         /**
          * Gets the lex tree node representing the 
          * left portion of the triphone unit
          *
          * @return the left unit lex tree node (or null if there is
          * no left context)
          */
         public LexTree.UnitLexNode getLeft() {
             return left;
         }


         /**
          * Returns the list of successors to this state
          *
          * @return a list of LanguageState objects
          */
         abstract public List getSuccessors() ;
    }

    /**
     * An initial state in the search space. It is non-emitting
     * and most likely has UnitLanguageStates as successors
     */
    class LexTreeInitialState extends LexTreeState {
        private LexTree.NonLeafLexNode node;


        /**
         * Creates a LexTreeInitialState object
         */
        LexTreeInitialState(LexTree.NonLeafLexNode node) {
            super(null, null, null, 0.0f);
            this.node = node;
        }

        /**
         * Gets a successor to this language state
         *
         * @param the successor index
         *
         * @return a successor
         */
        public List getSuccessors(){
            List list = new ArrayList();

            LexTree.LexNode[] nodes = node.getNextNodes();

            // the set of successors for the initial state is a set
            // of LexTreeUnitStates that correspond to the lex tree
            // successors.  We have to create a new state for each
            // possible right context. The set of next lex nodes for
            // this initial state must all be Unit nodes (that is,
            // there can be no word nodes at this point).

            for (int i = 0; i < nodes.length; i++) {
                LexTree.LexNode node = nodes[i];

                if (! (node instanceof LexTree.UnitLexNode)) {
                    throw new Error("Corrupt lex tree (initial state)");
                }

                LexTree.UnitLexNode leftNode = getCentral();
                LexTree.UnitLexNode central = (LexTree.UnitLexNode) node;
                LexTree.LexNode[] rightNodes = getNextUnits(central);

                for (int j = 0; j < rightNodes.length; j++) {
                    list.add(new LexTreeUnitState(leftNode, central, 
                                (LexTree.UnitLexNode) rightNodes[j]));
                }
            }
            return list;
        }
    }


    /**
     * Represents a unit in the search space
     */
    class LexTreeUnitState extends LexTreeState {
        int unitID;


        /**
         * Constructs a LexTreeUnitState
         *
         * @param left the unit forming the left context (or null if
         * there is no left context) of a triphone context
         *
         * @param central the unit forming the central portion of a
         * triphone context
         *
         * @param right the unit forming the right portion of a
         * triphone context
         */
        LexTreeUnitState( LexTree.UnitLexNode left,
               LexTree.UnitLexNode central, LexTree.UnitLexNode right) {
            super(left, central, right, logUnitInsertionProbability);
            int leftID;

            if (left == null) {
                leftID = hmmPool.getID(Unit.SILENCE);
            } else {
                leftID = left.getID();
            }

            unitID = hmmPool.buildID(
                        central.getID(), leftID, right.getID());
        }

        /**
         * Returns the triphone unit assciated with this state
         *
         * @return the triphone unit
         */

        Unit getUnit() {
            return hmmPool.getUnit(unitID);
        }

        /**
         * Returns the successors for this unit. The successors for a
         * unit are always the initial states(s) of the HMM associated
         * with the unit
         *
         * @return the list of successors for this state
         */
        public List getSuccessors() {
            List list = new ArrayList(1);
            HMMPosition position = getCentral().getPosition();
            HMM hmm = hmmPool.getHMM(unitID, position);

            list.add(new LexTreeHMMState(getLeft(), getCentral(),
                    getRight(), hmm.getInitialState(), logOne));
            return null;
        }

        /**
         * Determines the insertion probability for the given unit lex
         * node
         *
         * @param unitNode the unit lex node
         *
         * @return the insertion probability
         */
        private double getInsertionProbability(LexTree.UnitLexNode unitNode) {
            double logInsertionProbability = logUnitInsertionProbability;
            if (unitNode.getUnit().isSilence()) {
                logUnitInsertionProbability = logSilenceInsertionProbability;
            } 
            if (unitNode.isWordBeginning()) {
                logInsertionProbability += logWordInsertionProbability;
            }
            return logInsertionProbability;
        }
    }


    /**
     * Represents a HMM state in the search space
     */
    class LexTreeHMMState extends LexTreeState implements HMMLanguageState {
        private HMMState hmmState;

        /**
         * Constructs a LexTreeHMMState
         *
         * @param left the unit forming the left context (or null if
         * there is no left context) of a triphone context
         *
         * @param central the unit forming the central portion of a
         * triphone context
         *
         * @param right the unit forming the right portion of a
         * triphone context
         * 
         * @param hmmState the hmm state associated with this unit
         *
         * @param probability the probability of the transition
         * occuring
         */
        LexTreeHMMState( LexTree.UnitLexNode left,
               LexTree.UnitLexNode central, 
               LexTree.UnitLexNode right, HMMState hmmState, 
               double probability) {
            super(left, central, right, probability);
            this.hmmState = hmmState;
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
         * Retreives the set of successors for this state
         *
         * @return the list of sucessor states
         */
        public List getSuccessors() {
            List list = null;

            // if this is an exit state, we are transitioning to a
            // new unit or to a word end.

            if (hmmState.isExitState()) {
                list = new ArrayList();

                // if this hmm  state is the last state of the last
                // unit of a word, then all next nodes must be word
                // nodes.

                if (getCentral().isWordEnd()) {
                    LexTree.LexNode[] nodes = getCentral().getNextNodes();
                    for (int i = 0; i < nodes.length; i++) {
                        LexTree.LexNode node = nodes[i];

                        if (! (node instanceof LexTree.WordLexNode)) {
                            throw new Error("Corrupt lex tree (word)");
                        }
                // BUG: we are not incorporating language
                // probabilities into the search yet.
                        list.add(new LexTreeWordState(getLeft(), getCentral(), 
                                    getRight(), (LexTree.WordLexNode) node,
                                    0.0f));

                    }
                } else {

                    // its not the end of the word, so the next set of 
                    // states must be units, so we advance the left,
                    // and central units and iterate through the
                    // possible right contexts.

                    LexTree.UnitLexNode nextLeft = getCentral();
                    LexTree.UnitLexNode nextCentral = getRight();

                    LexTree.LexNode[] nodes = nextCentral.getNextNodes();

                    for (int i = 0; i < nodes.length; i++) {
                        LexTree.LexNode nextRight = nodes[i];

                        if (! (nextRight instanceof LexTree.UnitLexNode)) {
                            throw new Error("Corrupt lex tree (unit) ");
                        }

                        list.add(new LexTreeUnitState(nextLeft, nextCentral, 
                                    (LexTree.UnitLexNode) nextRight));
                    }
                }
            } else {
                // The current hmm state is not an exit state, so we
                // just go through the next set of successors

                HMMStateArc[] arcs = hmmState.getSuccessors();
                list = new ArrayList(arcs.length);
                for (int i = 0; i < arcs.length; i++) {
                    HMMStateArc arc = arcs[i];
                    list.add(new LexTreeHMMState(getLeft(),
                                getCentral(), getRight(),
                                arc.getHMMState(),
                                arc.getProbability()));
                }
            }
            return list;
        }

         /**
          * Determines if this is an emitting state
          */
         public boolean isEmitting() {
             return hmmState.isEmitting();
         }
    }

    /**
     * Represents a word state in the search space
     */
    class LexTreeWordState extends LexTreeState implements WordLanguageState {
        private LexTree.WordLexNode wordLexNode;

        /**
         * Constructs a LexTreeWordState
         *
         * @param left the unit forming the left context (or null if
         * there is no left context) of a triphone context
         *
         * @param central the unit forming the central portion of a
         * triphone context
         *
         * @param right the unit forming the right portion of a
         * triphone context
         * 
         * @param wordLexNode the lex tree node associated with this
         * word
         *
         * @param probability the probability of the transition
         * occuring
         */
        LexTreeWordState( LexTree.UnitLexNode left,
               LexTree.UnitLexNode central, 
               LexTree.UnitLexNode right, 
               LexTree.WordLexNode wordLexNode, 
               double probability) {

            super(left, central, right, probability);
            this.wordLexNode = wordLexNode;
        }


        /**
         * Gets the word pronunciation for this state
         *
         * @return the pronunciation for this word
         */
        public Pronunciation getWord() {
            return wordLexNode.getPronunciation();
        }

         /**
          * Determines if this is a final state. A LexTreeWordState is
          * considered final if the right unit context is null or silence
          *
          * @return <code>true</code> if this is an final state.
          */
         public boolean isFinal() {
             return getRight() == null || getRight().getUnit().isSilence();
         }

        /**
         * Retrieves the successors for this node
         *
         * @return the list of successor states
         */
        public List getSuccessors() {
            List list = null;

            LexTree.UnitLexNode nextLeft = getCentral();
            LexTree.UnitLexNode nextCentral = getRight();

            LexTree.LexNode[] nodes = nextCentral.getNextNodes();

            for (int i = 0; i < nodes.length; i++) {
                LexTree.LexNode nextRight = nodes[i];

                if (! (nextRight instanceof LexTree.UnitLexNode)) {
                    throw new Error("Corrupt lex tree (unit) ");
                }

                list.add(new LexTreeUnitState(nextLeft, nextCentral, 
                            (LexTree.UnitLexNode) nextRight));
            }
            return list;
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
}

