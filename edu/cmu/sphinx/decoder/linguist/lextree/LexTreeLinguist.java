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

    abstract class LexTreeState implements LanguageState {
        private float logProbability;
        private int thisUnitID;
        private LexTree.UnitLexNode left, central, right;


        LexTreeState(
                LexTree.UnitLexNode left,
                LexTree.UnitLexNode central,
                LexTree.UnitLexNode right,
                float logProbability) {
            this.left = left;
            this.central = central;
            this.right = right;
            this.logProbability = logProbability;
        }

         public  float getProbability() {
            return logProbability;
         }

         /**
          * Determines if this is an emitting state
          */
         public boolean isEmitting() {
             return false;
         }

         public LexTree.UnitLexNode getCentral() {
             return central;
         }

         public LexTree.UnitLexNode getRight() {
             return right;
         }

         public LexTree.UnitLexNode getLeft() {
             return left;
         }


         abstract public List getSuccessors() ;
    }

    /**
     * An initial state in the search space. It is non-emitting
     * and most likely has UnitLanguageStates as successors
     */
    class LexTreeInitialState extends LexTreeState {
        private LexTree.NonLeafLexNode node;


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

            for (int i = 0; i < nodes.length; i++) {
                LexTree.LexNode node = nodes[i];

                if (! (node instanceof LexTree.UnitLexNode)) {
                    throw new Error("Corrupt lex tree");
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


    class LexTreeUnitState extends LexTreeState {
        int unitID;
        LexTreeUnitState( LexTree.UnitLexNode left,
               LexTree.UnitLexNode central, LexTree.UnitLexNode right) {
            super(left, central, right, 0.0f);
            int leftID;

            if (left == null) {
                leftID = hmmPool.getID(Unit.SILENCE);
            } else {
                leftID = left.getID();
            }

            unitID = hmmPool.buildID(
                        central.getID(), leftID, right.getID());
        }


        Unit getUnit() {
            return hmmPool.getUnit(unitID);
        }

        public List getSuccessors() {
            List list = new ArrayList(1);
            HMMPosition position = getCentral().getPosition();
            HMM hmm = hmmPool.getHMM(unitID, position);

            list.add(new LexTreeHMMState(getLeft(), getCentral(),
                    getRight(), hmm.getInitialState(), (float) logOne));
            return null;
        }
    }

    class LexTreeHMMState extends LexTreeState implements HMMLanguageState {
        private HMMState hmmState;

        LexTreeHMMState( LexTree.UnitLexNode left,
               LexTree.UnitLexNode central, 
               LexTree.UnitLexNode right, HMMState hmmState, 
               float probability) {
            super(left, central, right, probability);
            this.hmmState = hmmState;
        }


        public HMMState getHMMState() {
            return hmmState;
        }

        public List getSuccessors() {
            List list = null;
            if (hmmState.isExitState()) {

                // if we are the last unit of a word
                // add a word state

                list = new ArrayList();

                LexTree.UnitLexNode nextCentral = getRight();

                LexTree.LexNode[] nodes = nextCentral.getNextNodes();

                for (int i = 0; i < nodes.length; i++) {
                    LexTree.LexNode node = nodes[i];

                    if (! (node instanceof LexTree.UnitLexNode)) {
                        throw new Error("Corrupt lex tree");
                    }

                    LexTree.UnitLexNode leftNode = getCentral();
                    LexTree.UnitLexNode central = (LexTree.UnitLexNode) node;
                    LexTree.LexNode[] rightNodes = getNextUnits(central);

                    for (int j = 0; j < rightNodes.length; j++) {
                        list.add(new LexTreeUnitState(leftNode, central, 
                                    (LexTree.UnitLexNode) rightNodes[j]));
                    }
                }
            } else {
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

