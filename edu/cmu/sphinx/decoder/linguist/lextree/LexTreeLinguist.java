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

        lexTree = new LexTree(hmmPool, dictionary, languageModel);



        hmmPool.dumpInfo();

        Timer.stop("compile");
        // Now that we are all done, dump out some interesting
        // information about the process

        Timer.dumpAll();

        return null;
    }

}



/**
 * Represents the vocabulary as a lex tree
 */
class LexTree {
    LexNode initialNode;
    HMMPool hmmPool;
    Dictionary dictionary;

    /**
     * Creates the lextree
     *
     * @param pool the pool of HMMs and units
     * @param dictionary the dictionary containing the pronunciations
     * @param languageModel contains the words
     */
    LexTree(HMMPool pool, Dictionary dictionary, LanguageModel languageModel) {
        hmmPool = pool;
        this.dictionary = dictionary;

        Collection  words = languageModel.getVocabulary();

        Utilities.dumpMemoryInfo("lextree before");
        Timer.start("Create lextree");
        initialNode = new LexNode();
        addWords(words);
        Timer.stop("Create lextree");
        Utilities.dumpMemoryInfo("lextree after");
        System.out.println("Added " + words.size() + " words");
        dumpStats();
    }

    /**
     * Adds the given collection of words to the lex tree
     *
     * @param words the collection of words
     */
    private void addWords(Collection words) {
        for (Iterator i = words.iterator(); i.hasNext(); ) {
            String word = (String) i.next();
            Pronunciation[] pronunciations =
                dictionary.getPronunciations(word, null);
            if (pronunciations == null) {
                System.out.println("Can't find pronunciation for '" +
                        word + "' .. skipping it.");
            } else {
                for (int j = 0; j < pronunciations.length; j++) {
                    addPronunciation(pronunciations[j]);
                }
            }
        }
    }

    /**
     * Adds the given pronunciation to the lex tree
     *
     * @param pronunciation the pronunciation
     */
    private void addPronunciation(Pronunciation pronunciation) {
        LexNode node = initialNode;
        Unit[] units = pronunciation.getUnits();
        for (int i = 0; i < units.length; i++) {
            node = node.addUnit(units[i]);
        }
        node.addWord(pronunciation);
    }

    /**
     * Dumps the words that are in the lex tree
     *
     * @node the root of the tree to dump
     */
    void dumpWords(LexNode node) {
        Collection nextNodes = node.getNext();
        for (Iterator i = nextNodes.iterator(); i.hasNext(); ) {
            LeafLexNode nextNode = (LeafLexNode) i.next();
            if (nextNode instanceof WordLexNode) {
                WordLexNode wln = (WordLexNode) nextNode;
                System.out.println("Word: " +
                        wln.getPronunciation().getWord());
            } else {
                dumpWords((LexNode) nextNode);
            }
        }
    }

    /**
     * Dumps the tree
     *
     * @node the root of the tree to dump
     */
    void dumpTree(int level, LexNode node) {
        Collection nextNodes = node.getNext();
        for (Iterator i = nextNodes.iterator(); i.hasNext(); ) {
            LeafLexNode nextNode = (LeafLexNode) i.next();
            if (nextNode instanceof WordLexNode) {
                WordLexNode wln = (WordLexNode) nextNode;
                System.out.println(Utilities.pad(level * 1) 
                   + "'" + wln.getPronunciation().getWord() + "'");
            } else if (nextNode instanceof UnitLexNode) {
                UnitLexNode uln = (UnitLexNode) nextNode;
                System.out.println(Utilities.pad(level * 1) +
                        uln.getUnit().getName());
                dumpTree(level + 1, (LexNode) nextNode);
            }
        }
    }

    /**
     * Collects stats for a subtree
     *
     * @node the root of the tree to collect the stats
     */
    void collectStats(int depth, TreeStats stats, LexNode node) {
        if (depth > stats.maxDepth) {
            stats.maxDepth = depth;
        }
        Collection nextNodes = node.getNext();
        for (Iterator i = nextNodes.iterator(); i.hasNext(); ) {
            stats.numBranches++;
            LeafLexNode nextNode = (LeafLexNode) i.next();
            if (nextNode instanceof WordLexNode) {
                stats.numWords++;
            } else if (nextNode instanceof UnitLexNode) {
                stats.numUnits++;
                collectStats(depth + 1, stats, (LexNode) nextNode);
            }
        }
    }

    /**
     * Dumps the stats for this tree
     */
    void dumpStats() {
        TreeStats stats = new TreeStats();
        collectStats(0, stats, initialNode);
        stats.dump();

    }

    /**
     * Little class to track tree statistics
     */
    class TreeStats {
        int numBranches = 0;
        int numWords = 0;
        int numUnits = 0;
        int maxDepth = 0;

        /**
         * Dumps the tree stats
         */
        void dump() {
            System.out.println(" =========== lex tree stats ======= ");
            System.out.println(" Units       : " + numUnits);
            System.out.println(" Words       : " + numWords);
            System.out.println(" MaxDepth    : " + maxDepth);
            System.out.println(" NumBranches : " + numBranches);
            System.out.println(" AvgBranches : " + numBranches / numUnits);
        }
    }


    /**
     * A LexTree is made up of lex nodes. The simplest kind of lex
     * node is a leaf lex node.  A leaf lex node has no successors
     */
    class LeafLexNode {
    }


    /**
     * A lexNode is a non-leaf lex node. It has successors.
     */
    class LexNode extends LeafLexNode {
        private Map nextNodes = new HashMap();

        /**
         * Get the set of successor lex nodes
         *
         * @return the collection of successors
         */
        Collection getNext() {
            return nextNodes.values();
        }

        /**
         * Adds a unit to the set of succesors to this node. If a
         * child representing this unit already exists then that child
         * is returned, otherwise a new child node is created,
         * added and returned.
         *
         * @param unit the unit to add
         *
         * @return the node representing the unit added
         */
        LexNode addUnit(Unit unit) {
            assert !unit.isContextDependent();
            UnitLexNode next = (UnitLexNode) nextNodes.get(unit);
            if (next != null) {
                assert next.getID() == hmmPool.getID(unit);
                return next;
            } else {
                next = new UnitLexNode(unit);
                nextNodes.put(unit, next);
            }
            return next;
        }

        /**
         * Adds a word (represented as a pronunciation) to this node
         *
         * @param p the pronunciation to add
         *
         * @return the node representing the word
         */
        void addWord(Pronunciation p) {
            LexNode next = (LexNode) nextNodes.get(p);
            if (next != null) {
                throw new Error("Duplicate pronunciation " + p);
            } else {
                nextNodes.put(p, new WordLexNode(p));
            }
        }
    }


    /**
     * Represents a unit in a lex tree
     */
    class UnitLexNode extends LexNode {
        int id;

        /**
         * Creates a UnitLexNode for the given unit
         *
         * @param unit the unit held by this node
         */
        UnitLexNode(Unit unit) {
            id = hmmPool.getID(unit);
        }

        /**
         * Gets the unit for this node
         *
         * @return the unit
         */
        Unit getUnit() {
            return hmmPool.getUnit(id);
        }

        /**
         * Gets the unit ID 
         *
         * @return the unit id
         */
        int getID() {
            return id;
        }
    }



    /**
     * An initial node (the head of the tree)
     */
    class InitialLexNode extends LexNode {
    }



    /**
     * Represents a word in a lex tree. Words are always leaf nodes
     */
    class WordLexNode extends LeafLexNode {
        private Pronunciation pronunciation;

        /**
         * Creates a node with the given pronunciation
         *
         * @param p the pronunciation
         */
        WordLexNode(Pronunciation p) {
            this.pronunciation = p;
        }


        /**
         * Returns the pronunciation for this node
         *
         * @param the pronunciation
         */
        Pronunciation getPronunciation() {
            return pronunciation;
        }

        /**
         * Returns a string representation of this object
         *
         * @param the string representation
         */
        public String toString() {
            return pronunciation.toString();
        }
    }

}


/**
 * The HMMPool provides the ability to manage units via small integer
 * IDs.  Context Independent units and context dependent units can
 * be converted to an ID. IDs can be used to quickly retrieve a unit
 * or an hmm associated with the unit.  This class operates under the
 * constraint that context sizes are exactly one, which is generally
 * only valid for large vocabulary tasks.
 */
class HMMPool {
    private AcousticModel model;
    private int maxCIUnits = 0;
    private Unit[] unitTable;
    private Map unitMap;

    HMMPool(AcousticModel model) {
        this.model = model;

        if (model.getLeftContextSize() != 1) {
            throw new Error("LexTreeLinguist: Unsupported left context size");
        }

        if (model.getRightContextSize() != 1) {
            throw new Error("LexTreeLinguist: Unsupported right context size");
        }

        // count CI units:

        unitMap = new HashMap();
        int curID = 1;
        for (Iterator i = model.getContextIndependentUnitIterator();
                i.hasNext();) {
            Unit unit = (Unit) i.next();
            unitMap.put(unit.getName(), new Integer(curID++));
        }

        maxCIUnits = curID;

        unitTable = new Unit[maxCIUnits * maxCIUnits * maxCIUnits];

        for (Iterator i = model.getHMMIterator(); i.hasNext(); ) {
            HMM hmm = (HMM) i.next();
            Unit unit = hmm.getUnit();
            int id = getID(unit);
            unitTable[id] = unit;
            // System.out.println("Unit " + unit + " id " + id);
        }
    }


    /**
     * Gets the unit for the given id
     *
     * @param unitID the id for the unit
     *
     * @returns the unit associated with the ID
     */
    Unit getUnit(int unitID) {
        return unitTable[unitID];
    }


    /**
     * Given a unit id and a position, return the HMM
     * associated with the unit/position
     *
     * @param unitID the id of the unit
     * @param position the position within the word
     *
     * @return the hmm associated with the unit/position
     */
    HMM getHMM(int unitID, HMMPosition position) {
        Unit unit = getUnit(unitID);
        assert unit != null;
        return model.lookupNearestHMM(unit, position);
    }

    /**
     * given a unit return its ID
     *
     * @param unit the unit
     *
     * @return an ID
     */
    int getID(Unit unit) {
        if (unit.isContextDependent()) {
            LeftRightContext context = (LeftRightContext) unit.getContext();
            assert context.getLeftContext().length == 1;
            assert context.getRightContext().length == 1;
            return buildID(
                getSimpleUnitID(unit),
                getSimpleUnitID(context.getLeftContext()[0]),
                getSimpleUnitID(context.getRightContext()[0]));
        } else {
            return getSimpleUnitID(unit);
        }
    }

    /**
     * Returns a context independent ID
     *
     * @param unit the unit of interest
     *
     * @return the ID of the central unit (ignoring any context)
     */
    int getSimpleUnitID(Unit unit) {
        Integer id = (Integer) unitMap.get(unit.getName());
        if (id == null) {
            throw new Error("Can't find " + unit + " in unitMap");
        }
        return  id.intValue();
    }

    /**
     * Builds an id from the given unit and its left and right unit
     * ids
     *
     * @param unitID the id of the central unit
     * @param leftID the id of the left context unit
     * @param rightID the id of the right context unit
     *
     * @return the id for the context dependent unit
     */
    int buildID(int unitID, int leftID, int rightID) {
        return unitID * (maxCIUnits * maxCIUnits)
              + (leftID * maxCIUnits) 
              + rightID ;
    }

    /**
     * Dumps out info about this pool
     */
    void dumpInfo() {
        System.out.println("Max CI Units " + maxCIUnits);
        System.out.println("Unit table size " + unitTable.length);
        System.out.println("Unit map size " + unitMap.size());

        if (false) {
            for (int i = 0; i < unitTable.length; i++) {
                System.out.println("" + i + " " + unitTable[i]);
            }
        }
    }




    /**
     * A quick and dirty benchmark to get an idea how long
     * the HMM lookups will take.  This experiment shows
     * that on a 1GHZ sparc system, the lookup takes a little
     * less than 1uSec.  This is probably fast enough.
     */

    static HMMPosition pos[] = {
        HMMPosition.BEGIN, HMMPosition.END, HMMPosition.SINGLE,
        HMMPosition.INTERNAL};

   static int ids[] = { 9206, 9320, 9620, 9865, 14831, 15836 };

    void benchmark() {
        int nullCount = 0;
        System.out.println("benchmarking ...");
        Timer.start("hmmPoolBenchmark");

        for (int i = 0; i < 1000000; i++) {
            int id = ids[i % ids.length];
            HMMPosition position = pos[i % pos.length];
            HMM hmm = getHMM(id, position);
            if (hmm == null) {
                nullCount++;
            }
        }
        Timer.stop("hmmPoolBenchmark");
        System.out.println("null count " + nullCount);
    }
}

