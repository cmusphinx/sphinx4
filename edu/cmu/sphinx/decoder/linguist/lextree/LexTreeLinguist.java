/*
   LanguageState initialState = getInitialLanguageState();
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

        testLinguist(5000, 5000);
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


    /**
     * retrieves the initial language state
     *
     * @return the initial language state
     */
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
         abstract public LanguageState[] getSuccessors() ;

         /**
          * Returns the string representation of this object
          *
          * @return the string representation
          */
        public String toString() {
            String pos = central == null ? "" 
                : central.getPosition().toString();
            return central + "[" + left +
                   "," + right +"]@" + pos + " " + getProbability();
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
     * and most likely has UnitLanguageStates as successors
     */
    class LexTreeInitialState extends LexTreeState {
        private LexTree.NonLeafLexNode node;


        /**
         * Creates a LexTreeInitialState object
         */
        LexTreeInitialState(LexTree.NonLeafLexNode node) {
            super(null, null, null, 0.0);
            this.node = node;
        }

        /**
         * Gets a successor to this language state
         *
         * @param the successor index
         *
         * @return a successor
         */
        public LanguageState[] getSuccessors(){
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
            System.out.println("LTIS: return " + list.size() + " nodes.");
            // since this only called once, it doesn't have to be too
            // fast, so we can create the array of successors from the
            // list
            return (LanguageState[]) 
                list.toArray(new LanguageState[list.size()]);
        }


        public String toString() {
            return super.toString() + " initial";
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
            super(left, central, right, getInsertionProbability(central));
            int leftID;

            if (left == null) {
                leftID = hmmPool.getID(Unit.SILENCE);
            } else {
                leftID = left.getID();
            }

            unitID = hmmPool.buildID(
                        central.getID(), leftID, right.getID());

            if (false) {
                System.out.println("Left id " + leftID + " node " + left);
                System.out.println("central id " + central + " node " +
                        central.getID());
                System.out.println("right id " + right + " node " +
                        right.getID());
            }
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
        public LanguageState[] getSuccessors() {
            LanguageState[] nextStates   = new LanguageState[1];
            HMMPosition position = getCentral().getPosition();

            HMM hmm = hmmPool.getHMM(unitID, position);

            nextStates[0] = new LexTreeHMMState(getLeft(), getCentral(),
                    getRight(), hmm.getInitialState(), logOne);
            return nextStates;
        }


        public String toString() {
            return super.toString() + " unit";
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
        public LanguageState[] getSuccessors() {
            LanguageState[] nextStates;

            // if this is an exit state, we are transitioning to a
            // new unit or to a word end.

            if (hmmState.isExitState()) {

                // if this hmm  state is the last state of the last
                // unit of a word, then all next nodes must be word
                // nodes.

                if (getCentral().isWordEnd()) {
                    LexTree.LexNode[] nodes = getCentral().getNextNodes();
                    nextStates = new LanguageState[nodes.length];
                    for (int i = 0; i < nodes.length; i++) {
                        LexTree.LexNode node = nodes[i];

                        if (! (node instanceof LexTree.WordLexNode)) {
                            throw new Error("Corrupt lex tree (word)");
                        }
                // BUG: we are not incorporating language
                // probabilities into the search yet.
                        nextStates[i] = new LexTreeWordState(getLeft(), 
                                getCentral(), getRight(), 
                                (LexTree.WordLexNode) node, 0.0);
                    }
                } else {

                    // its not the end of the word, so the next set of 
                    // states must be units, so we advance the left,
                    // and central units and iterate through the
                    // possible right contexts.

                    LexTree.UnitLexNode nextLeft = getCentral();
                    LexTree.UnitLexNode nextCentral = getRight();

                    LexTree.LexNode[] nodes = getNextUnits(nextCentral);

                    nextStates = new LanguageState[nodes.length];

                    for (int i = 0; i < nodes.length; i++) {
                        LexTree.LexNode nextRight = nodes[i];

                        if (! (nextRight instanceof LexTree.UnitLexNode)) {
                            System.out.println("next right " + nextRight);
                            System.out.println("central is " +
                                    getCentral());

                            throw new Error("Corrupt lex tree (hmm) ");
                        }

                        nextStates[i] = new LexTreeUnitState(nextLeft, 
                                nextCentral, (LexTree.UnitLexNode) nextRight);
                    }
                }
            } else {
                // The current hmm state is not an exit state, so we
                // just go through the next set of successors

                HMMStateArc[] arcs = hmmState.getSuccessors();
                nextStates = new LanguageState[arcs.length];
                for (int i = 0; i < arcs.length; i++) {
                    HMMStateArc arc = arcs[i];
                    nextStates[i] = new LexTreeHMMState(getLeft(),
                                getCentral(), getRight(),
                                arc.getHMMState(),
                                arc.getProbability());
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
        public LanguageState[] getSuccessors() {

            LexTree.UnitLexNode nextLeft = getCentral();
            LexTree.UnitLexNode nextCentral = getRight();

            LexTree.LexNode[] nextNodes = getNextUnits(nextCentral);
            LanguageState[] nextStates = new LanguageState[nextNodes.length];

            for (int i = 0; i < nextNodes.length; i++) {
                LexTree.LexNode nextRight = nextNodes[i];

                if (! (nextRight instanceof LexTree.UnitLexNode)) {
                    System.out.println("nextRight " + nextRight);
                    throw new Error("Corrupt lex tree (unit) ");
                }
                nextStates[i] = new LexTreeUnitState(nextLeft, nextCentral, 
                            (LexTree.UnitLexNode) nextRight);
            }
            return nextStates;
        }

         public String toString() {
             return super.toString() + " word:" +
                 wordLexNode.getPronunciation();
         }

         public String toPrettyString() {
             return wordLexNode.getPronunciation().getWord();
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


    /**
     *  tests the linguist
     */

    int nonEmittingStates;
    int maxSuccessors;
    boolean testTracing = true;

    public void testLinguist(int numFrames, int maxBeam) {
        // this test invokes the linguist using access patterns that
        // are similar to a real search. It allows for timing and
        // profiling of the linguist, independent of the search
        // or scoring
        Random random = new Random(1000);
        Timer frameTimer = Timer.getTimer(props.getContext(),
                "frameTimer");
        Timer totalTimer = Timer.getTimer(props.getContext(),
                "totalTimer");
        List activeList = new ArrayList();
        int level = 0;
        LanguageState initialState = getInitialLanguageState();

        // Note: this comparator imposes orderings that are
        // inconsistent with equals.

        Comparator stateComparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                LanguageState ls1 = (LanguageState) o1;
                LanguageState ls2 = (LanguageState) o2;


                if (ls1.getProbability() > ls2.getProbability()) {
                    return 1;
                } else if (ls1.getProbability() < ls2.getProbability()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        };

        activeList.add(initialState);

        totalTimer.start();
        start();

        for (int i = 0; i < numFrames; i++) {
            List oldList = activeList;
            activeList = new ArrayList(maxBeam * 10);
            nonEmittingStates = 0;

            frameTimer.start();
            for (int j = 0; j < oldList.size(); j++) {
                LanguageState nextStates = (LanguageState) oldList.get(j);
                testExpandState(level, activeList, nextStates);
            }
            frameTimer.stop();

            if (testTracing) {
                System.out.println(" === frame " + i + " of " 
                        + numFrames + " ====");
                System.out.println(" Active size   : " + activeList.size());
                System.out.println(" NonEmitting   : " + nonEmittingStates);
            }

            Collections.shuffle(activeList, random);

            Collections.sort(activeList, stateComparator);
            if (activeList.size() > maxBeam) {
                activeList = activeList.subList(0, maxBeam);
            }
        }
        stop();
        totalTimer.stop();
        System.out.println(" MaxSuccessors : " + maxSuccessors);
        Timer.dumpAll();
    }

    private void testExpandState(int level, List activeList, LanguageState ls) {
        LanguageState[] newStates = ls.getSuccessors();

        System.out.println(Utilities.pad(level * 2) + ls);
        if (newStates.length > maxSuccessors) {
            maxSuccessors = newStates.length;
        }

        for (int i = 0; i < newStates.length; i++) {
            LanguageState ns = newStates[i];
            if (ns.isEmitting()) {
                activeList.add(ns);
            } else {
                nonEmittingStates ++;
                if (testTracing && ns.isFinal()) {
                    System.out.println("result " + ns.toPrettyString());
                }
                testExpandState(level + 1, activeList, ns);
            }
        }
    }
}

