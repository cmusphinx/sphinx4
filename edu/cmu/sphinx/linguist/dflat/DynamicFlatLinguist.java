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
package edu.cmu.sphinx.linguist.dflat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.Set;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.SearchGraph;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.UnitSearchState;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.util.HMMPool;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.HMMStateArc;
import edu.cmu.sphinx.linguist.acoustic.LeftRightContext;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.grammar.Grammar;
import edu.cmu.sphinx.linguist.language.grammar.GrammarArc;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;
/**
 * A simple form of the linguist. It makes the following simplifying
 * assumptions: 1) Zero or one word per grammar node 2) No fan-in allowed ever 3)
 * No composites (yet) 4) Only Unit, HMMState, and pronunciation states (and
 * the initial/final grammar state are in the graph (no word, alternative or
 * grammar states attached). 5) Only valid tranisitions (matching contexts) are
 * allowed 6) No tree organization of units 7) Branching grammar states are
 * allowed
 *
 * This is a dynamic version of the flat linguist that is more efficient in
 * terms of startup time and overall footprint
 * 
 * Note that all probabilties are maintained in the log math domain
 */
public class DynamicFlatLinguist implements Linguist, Configurable {
    /**
     * A sphinx property used to define the grammar to use when building the
     * search graph
     */
    public final static String PROP_GRAMMAR = "grammar";

    /**
     * A sphinx property used to define the unit manager to use 
     * when building the search graph
     */
    public final static String PROP_UNIT_MANAGER = "unitManager";

    /**
     * A sphinx property used to define the acoustic model to use when building
     * the search graph
     */
    public final static String PROP_ACOUSTIC_MODEL = "acousticModel";

    /**
     * Sphinx property that specifies whether to add a branch for detecting
     * out-of-grammar utterances.
     */
    public final static String PROP_ADD_OUT_OF_GRAMMAR_BRANCH
        = "addOutOfGrammarBranch";

    /**
     * Default value of PROP_ADD_OUT_OF_GRAMMAR_BRANCH.
     */
    public final static boolean PROP_ADD_OUT_OF_GRAMMAR_BRANCH_DEFAULT = false;

    
    /**
     * Sphinx property for the probability of entering the out-of-grammar
     * branch.
     */
    public final static String PROP_OUT_OF_GRAMMAR_PROBABILITY
        = "outOfGrammarProbability";

    /**
     * The default value for PROP_OUT_OF_GRAMMAR_PROBABILITY.
     */
    public final static double PROP_OUT_OF_GRAMMAR_PROBABILITY_DEFAULT
        = 1.0;
    

    /**
     * Sphinx property for the probability of inserting a CI phone in
     * the out-of-grammar ci phone loop
     */
    public static final String PROP_PHONE_INSERTION_PROBABILITY
        = "phoneInsertionProbability";

    /**
     * Default value for PROP_PHONE_INSERTION_PROBABILITY
     */
    public static final double PROP_PHONE_INSERTION_PROBABILITY_DEFAULT = 1.0;

    /**
     * Sphinx property for the acoustic model to use to build the phone loop
     * that detects out of grammar utterances.
     */
    public final static String PROP_PHONE_LOOP_ACOUSTIC_MODEL
        = "phoneLoopAcousticModel";

    /**
     * Sphinx property that defines the name of the logmath to be used by this
     * search manager.
     */
    public final static String PROP_LOG_MATH = "logMath";
    private final static float logOne = LogMath.getLogOne();

    // ----------------------------------
    // Subcomponents that are configured
    // by the property sheet
    // -----------------------------------
    private Grammar grammar;
    private AcousticModel acousticModel;
    private AcousticModel phoneLoopAcousticModel;
    private LogMath logMath;
    private UnitManager unitManager;
    // ------------------------------------
    // Data that is configured by the
    // property sheet
    // ------------------------------------
    private float logWordInsertionProbability;
    private float logSilenceInsertionProbability;
    private float logUnitInsertionProbability;
    private float logFillerInsertionProbability;
    private float languageWeight;
    private float logOutOfGrammarBranchProbability;
    private float logPhoneInsertionProbability;
    private boolean addOutOfGrammarBranch;

    // ------------------------------------
    // Data used for building and maintaining
    // the search graph
    // -------------------------------------
    private SearchGraph searchGraph;
    private String name;
    private Logger logger;
    private HMMPool hmmPool;
    SearchStateArc outOfGrammarGraph;

    // this map is used to manage the set of follow on units for a
    // particular grammar node. It is used to select the set of
    // possible right contexts as we leave a node

    private Map nodeToNextUnitArrayMap;


    // this map is used to manage the set of possible entry units for
    // a grammar node. It is used to filter paths so that we only
    // branch to grammar nodes that match the current right context.

    private Map nodeToUnitSetMap;

    // an empty arc (just waiting for Noah, I guess)
    private final SearchStateArc[] EMPTY_ARCS = new SearchStateArc[0];

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        this.name = name;
        registry.register(PROP_GRAMMAR, PropertyType.COMPONENT);
        registry.register(PROP_ACOUSTIC_MODEL, PropertyType.COMPONENT);
        registry.register(PROP_LOG_MATH, PropertyType.COMPONENT);
        registry.register(PROP_WORD_INSERTION_PROBABILITY, PropertyType.DOUBLE);
        registry.register(PROP_SILENCE_INSERTION_PROBABILITY,
                PropertyType.DOUBLE);
        registry.register(PROP_UNIT_INSERTION_PROBABILITY, PropertyType.DOUBLE);
        registry.register(PROP_LANGUAGE_WEIGHT, PropertyType.FLOAT);
        registry.register(PROP_UNIT_MANAGER, PropertyType.COMPONENT);
        registry.register(PROP_FILLER_INSERTION_PROBABILITY,
                PropertyType.DOUBLE);
        registry.register(PROP_ADD_OUT_OF_GRAMMAR_BRANCH, PropertyType.BOOLEAN);
        registry.register(PROP_OUT_OF_GRAMMAR_PROBABILITY, PropertyType.DOUBLE);
        registry.register(PROP_PHONE_INSERTION_PROBABILITY, PropertyType.DOUBLE);
        registry.register(PROP_PHONE_LOOP_ACOUSTIC_MODEL, PropertyType.COMPONENT);
    }
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        // hookup to all of the components
        logger = ps.getLogger();
        setupAcousticModel(ps);
        logMath = (LogMath) ps.getComponent(PROP_LOG_MATH, LogMath.class);
        grammar = (Grammar) ps.getComponent(PROP_GRAMMAR, Grammar.class);
        unitManager = (UnitManager) ps.getComponent(PROP_UNIT_MANAGER,
                UnitManager.class);
        
        // get the rest of the configuration data
        logWordInsertionProbability = logMath.linearToLog(ps.getDouble(
                PROP_WORD_INSERTION_PROBABILITY,
                PROP_WORD_INSERTION_PROBABILITY_DEFAULT));
        logSilenceInsertionProbability = logMath.linearToLog(ps.getDouble(
                PROP_SILENCE_INSERTION_PROBABILITY,
                PROP_SILENCE_INSERTION_PROBABILITY_DEFAULT));
        logUnitInsertionProbability = logMath.linearToLog(ps.getDouble(
                PROP_UNIT_INSERTION_PROBABILITY,
                PROP_UNIT_INSERTION_PROBABILITY_DEFAULT));
        logFillerInsertionProbability = logMath.linearToLog(ps.getDouble(
                PROP_FILLER_INSERTION_PROBABILITY,
                PROP_FILLER_INSERTION_PROBABILITY_DEFAULT));
        languageWeight = ps.getFloat(Linguist.PROP_LANGUAGE_WEIGHT,
                PROP_LANGUAGE_WEIGHT_DEFAULT);
        addOutOfGrammarBranch = ps.getBoolean
            (PROP_ADD_OUT_OF_GRAMMAR_BRANCH,
             PROP_ADD_OUT_OF_GRAMMAR_BRANCH_DEFAULT);
        logOutOfGrammarBranchProbability = logMath.linearToLog
            (ps.getDouble(PROP_OUT_OF_GRAMMAR_PROBABILITY,
                          PROP_OUT_OF_GRAMMAR_PROBABILITY_DEFAULT));
        logPhoneInsertionProbability = logMath.linearToLog
            (ps.getDouble(PROP_PHONE_INSERTION_PROBABILITY,
                          PROP_PHONE_INSERTION_PROBABILITY_DEFAULT));
        if (addOutOfGrammarBranch) {
            phoneLoopAcousticModel = (AcousticModel)
                ps.getComponent(PROP_PHONE_LOOP_ACOUSTIC_MODEL,
                                AcousticModel.class);
        }
    }

    /**
     * Returns the search graph
     * 
     * @return the search graph
     */
    public SearchGraph getSearchGraph() {
        return searchGraph;
    }

    /**
     * Sets up the acoustic model.
     *
     * @param ps the PropertySheet from which to obtain the acoustic model
     */
    protected void setupAcousticModel(PropertySheet ps)
        throws PropertyException {
        acousticModel = (AcousticModel) ps.getComponent(PROP_ACOUSTIC_MODEL,
                                                        AcousticModel.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.linguist.Linguist#allocate()
     */
    public void allocate() throws IOException {
        logger.info("Allocating DFLAT");
        allocateAcousticModel();
        grammar.allocate();
        hmmPool = new HMMPool(acousticModel, logger, unitManager);
        nodeToNextUnitArrayMap = new HashMap();
        nodeToUnitSetMap = new HashMap();
        Timer timer = Timer.getTimer("compileGrammar");
        timer.start();
        compileGrammar();
        timer.stop();
        logger.info("Done allocating  DFLAT");
    }

    /**
     * Allocates the acoustic model.
     */
    protected void allocateAcousticModel() throws IOException {
        acousticModel.allocate();
        if (addOutOfGrammarBranch) {
            phoneLoopAcousticModel.allocate();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.linguist.Linguist#deallocate()
     */
    public void deallocate() {
        if (acousticModel != null) {
            acousticModel.deallocate();
        }
        grammar.deallocate();
    }
    /**
     * 
     * Called before a recognition
     */
    public void startRecognition() {
    }
    /**
     * Called after a recognition
     */
    public void stopRecognition() {
    }

    /**
     * Returns the LogMath used.
     *
     * @return the logMath used
     */
    public LogMath getLogMath() {
        return logMath;
    }

    /**
     * Returns the log silence insertion probability.
     *
     * @return the log silence insertion probability.
     */
    public float getLogSilenceInsertionProbability() {
        return logSilenceInsertionProbability;
    }

    /**
     * Compiles the grammar 
     */
    private void compileGrammar() {
        // iterate through the grammar nodes

        Set nodeSet = grammar.getGrammarNodes();

        for (Iterator i = nodeSet.iterator(); i.hasNext(); ) {
            GrammarNode node = (GrammarNode) i.next();
            initUnitMaps(node);
        }
        searchGraph = new DynamicFlatSearchGraph();
    }

    /**
     * Initializes the unit maps for this linguist. There are two unit maps:
     * (a) nodeToNextUnitArrayMap contains an array of unit ids for all possible
     * units that immediately follow the given grammar node. This is used to
     * determine the set of exit contexts for words within a grammar node. 
     * (b) nodeToUnitSetMap contains the set of possible entry units for a
     * given grammar node. This is typically used to determine if a path with
     * a given right context should branch into a particular grammar node
     *
     * @param node the units maps will be created for this node.
     */
    private void initUnitMaps(GrammarNode node) {

        // collect the set of next units for this node

        if (nodeToNextUnitArrayMap.get(node) == null) {
            Set vistedNodes = new HashSet();
            Set unitSet = new HashSet();

            GrammarArc[] arcs = node.getSuccessors();
            for (int i = 0; i < arcs.length; i++) {
                GrammarNode nextNode = arcs[i].getGrammarNode();
                collectNextUnits(nextNode, vistedNodes, unitSet);
            }
            int[] nextUnits = new int[unitSet.size()];
            int index = 0;
            for (Iterator i = unitSet.iterator(); i.hasNext(); ) {
                Unit unit = (Unit) i.next();
                nextUnits[index++] = unit.getBaseID();
            }
            nodeToNextUnitArrayMap.put(node, nextUnits);
        }

        // collect the set of entry units for this node

        if (nodeToUnitSetMap.get(node) == null) {
            Set vistedNodes = new HashSet();
            Set unitSet = new HashSet();
            collectNextUnits(node, vistedNodes, unitSet);
            nodeToUnitSetMap.put(node, unitSet);
        }
    }

    /**
     * For the given grammar node, collect the set of possible next units.
     *
     * @param thisNode the grammar node
     * @param vistedNodes the set of visited grammar nodes, used to ensure
     *          that we don't attempt to expand a particular grammar node more than
     *          once (which could lead to a death spiral)
     * @param unitSet the entry units are collected here.
     */
    private void collectNextUnits(GrammarNode thisNode, 
                Set vistedNodes, Set unitSet) {
        if (vistedNodes.contains(thisNode)) {
            return;
        }

        vistedNodes.add(thisNode);
        if (thisNode.isFinalNode()) {
            unitSet.add(UnitManager.SILENCE);
        } else if (!thisNode.isEmpty()) {
            Word word = thisNode.getWord();
            Pronunciation[] pronunciations = word.getPronunciations();
            for (int j = 0; j < pronunciations.length; j++) {
                unitSet.add(pronunciations[j].getUnits()[0]);
            }
        } else {
            GrammarArc[] arcs = thisNode.getSuccessors();
            for (int i = 0; i < arcs.length; i++) {
                GrammarNode nextNode = arcs[i].getGrammarNode();
                collectNextUnits(nextNode, vistedNodes, unitSet);
            }
        } 
    }


    /**
     * The base search state for this dynamic flat linguist.
     */
    abstract class FlatSearchState implements SearchState , SearchStateArc {
        final static int ANY = 0;

        /**
         * Gets the set of successors for this state
         *
         * @return the set of successors
         */
        public abstract SearchStateArc[] getSuccessors();

        /**
         * Returns a unique string representation of the state. This string is
         * suitable (and typically used) for a label for a GDL node
         *
         * @return the signature
         */
        public abstract String getSignature();

        /**
         * Returns the order of this state type among all of the search states
         *
         * @return the order
         */
        public abstract int getOrder(); 


        /**
         * Determines if this state is an emitting state
         *
         * @return true if this is an emitting state
         */
        public boolean isEmitting() {
            return false;
        }

        /**
         * Determines if this is a final state
         *
         * @return true if this is a final state
         */
        public boolean isFinal() {
            return false;
        }


        /**
         * Returns a lex state associated with the searc state (not applicable
         * to this linguist)
         *
         * @return the lex state (null for this linguist)
         */
        public Object getLexState() {
            return null;
        }

        /**
         * Returns a well formatted string representation of this state
         *
         * @return the formatted string
         */
        public String toPrettyString() {
            return toString();
        }

        /**
         * Returns a string representation of this object
         *
         * @return a string representation
         */
        public String toString() {
            return getSignature();
        }

        /**
         * Returns the word history for this state (not applicable to this
         * linguist)
         * @return the word history (null for this linguist)
         */
        public WordSequence getWordHistory() {
            return null;
        }

        /**
         * Gets a successor to this search state
         * 
         * @return the sucessor state
         */
        public SearchState getState() {
            return this;
        }

        /**
         * Gets the composite probability of entering this state
         * 
         * @return the log probability
         */
        public float getProbability() {
            return getLanguageProbability() + getAcousticProbability()
                    + getInsertionProbability();
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
         * Gets the acoustic probability of entering this state
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
         * Simple debugging output
         *
         * @param msg the debug message
         */
        void debug(String msg) {
            if (false) {
                System.out.println(msg);
            }
        }

    }

    /**
     * Represents a grammar node in the search graph. A grammar state needs to
     * keep track of the associated grammar node as well as the left context
     * and next base unit.
     */
    class GrammarState extends FlatSearchState {
        private GrammarNode node;
        private int lc;
        private int nextBaseID;
        private float languageProbability;


        /**
         * Creates a grammar state for the given node with a silence Lc
         *
         * @param node the grammar node
         */
        GrammarState(GrammarNode node) {
            this(node,  logOne, UnitManager.SILENCE.getBaseID());
        }


        /**
         * Creates a grammar state for the given node and left context. The
         * path will connect to any possible next base
         *
         * @param node the grammar node
         * @param languageProbability the probability of transistioning to
         *      this word
         * @param lc the left context for this path
         */
        GrammarState(GrammarNode node, float languageProbability, int lc) {
            this(node, languageProbability, lc, ANY);
        }

        /**
         * Creates a grammar state for the given node and left context and
         * next base ID. 
         *
         * @param node the grammar node
         * @param languageProbability the probability of transistioning to
         *      this word
         * @param lc the left context for this path
         * @param nextBaseID the next base ID
         */
        GrammarState(GrammarNode node, float languageProbability, 
                int lc, int nextBaseID) {
            this.lc = lc;
            this.nextBaseID = nextBaseID;
            this.node = node;
            this.languageProbability = languageProbability;
            debug("GS " + node + " next " + nextBaseID + " "
                    + hmmPool.getUnit(nextBaseID));
        }


        /**
         * Generate a hashcode for an object. Equality for a  grammar state
         * includes the grammar node, the lc and the next base ID
         * 
         * @return the hashcode
         */
        public int hashCode() {
            return node.hashCode() * 17 + lc * 7 + nextBaseID;
        }

        /**
         * Determines if the given object is equal to this object
         * 
         * @param o
         *                the object to test
         * @return <code>true</code> if the object is equal to this
         */
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof GrammarState) {
                GrammarState other = (GrammarState) o;
                return other.node == node && lc == other.lc 
                    && nextBaseID == other.nextBaseID;
            } else {
                return false;
            }
        }

        /**
         * Determines if this is a final state in the search graph
         *
         * @return true if this is a final state in the search graph
         */
        public boolean isFinal() {
            return node.isFinalNode();
        }

        /**
         * Gets the set of successors for this state
         *
         * @return the set of successors
         */
        public SearchStateArc[] getSuccessors() {
            if (isFinal()) {
                return EMPTY_ARCS;
            } else if (node.isEmpty()) {
                return getNextGrammarStates(lc, nextBaseID);
            } else {
                Word word = node.getWord();
                Pronunciation[] pronunciations = word.getPronunciations();
                pronunciations = filter(pronunciations, nextBaseID);
                SearchStateArc[] nextArcs = 
                    new SearchStateArc[pronunciations.length];

                for (int i = 0; i < pronunciations.length; i++) {
                    nextArcs[i] = 
                        new PronunciationState(this, pronunciations[i]);
                }
                return nextArcs;
            }
        }


        /**
         * Gets the set of arcs to the next set of grammar states that match
         * the given nextBaseID
         *
         * @param lc the current left context
         * @param nextBaseID the desired next base ID
         */
        SearchStateArc[] getNextGrammarStates(int lc, int nextBaseID) {
            GrammarArc[] nextNodes = node.getSuccessors();
            nextNodes = filter(nextNodes, nextBaseID);
            SearchStateArc[] nextArcs = new SearchStateArc[nextNodes.length];

            for (int i = 0; i < nextNodes.length; i++) {
                GrammarArc arc = nextNodes[i];
                nextArcs[i] = new GrammarState(arc.getGrammarNode(), 
                        arc.getProbability(), lc, nextBaseID);
            }
            return nextArcs;
        }



        /**
         * Returns a unique string representation of the state. This string is
         * suitable (and typically used) for a label for a GDL node
         *
         * @return the signature
         */
        public String getSignature() {
            return "GS " + node + "-lc-" + hmmPool.getUnit(lc) + "-rc-" +
                hmmPool.getUnit(nextBaseID);
        }


        /**
         * Returns the order of this state type among all of the search states
         *
         * @return the order
         */
        public int getOrder() {
            return 1;
        }


        /**
         * Given a set of arcs and the ID of the desired next unit, return the
         * set of arcs containing only those that transition to the next unit
         *
         * @param arcs the set of arcs to filter
         * @param nextBase the ID of the desired next unit
         */
        GrammarArc[] filter(GrammarArc[] arcs, int nextBase) {
            if (nextBase != ANY) {
                List list = new ArrayList();
                for (int i = 0; i < arcs.length; i++) {
                    GrammarNode node = arcs[i].getGrammarNode();
                    if (hasEntryContext(node, nextBase)) {
                        list.add(arcs[i]);
                    }
                }
                arcs = (GrammarArc[]) list.toArray(new GrammarArc[list.size()]);
            }
            return arcs;
        }


        /**
         * Determines if the given node starts with the specified unit
         *
         * @param node the grammar node
         * @param unitID the id of the unit
         */
        private boolean hasEntryContext(GrammarNode node, int unitID) {
            Set unitSet = (Set) nodeToUnitSetMap.get(node);
            return unitSet.contains(hmmPool.getUnit(unitID));
        }

        /**
         * Retain only the pronunciations that start with the unit indicated
         * by nextBase
         *
         * @param p the set of pronunciations to filter
         * @param nextBase the ID of the desired initial unit
         */
        Pronunciation[] filter(Pronunciation[] p, int nextBase) {
            if (true) {
                return p;
            }
            if (nextBase == ANY) {
                return p;
            } else {
                int count = 0;
                for (int i = 0; i < p.length; i++) {
                    Unit[] units = p[i].getUnits();
                    if (units[0].getBaseID() == nextBase) {
                        count++;
                    }
                }

                if (count == p.length) {
                    return p;
                } else {
                    Pronunciation[] filteredP = new Pronunciation[count];
                    int index = 0;
                    for (int i = 0; i < p.length; i++) {
                        Unit[] units = p[i].getUnits();
                        if (units[0].getBaseID() == nextBase) {
                            filteredP[index++] = p[i];
                        }
                    }
                    return filteredP;
                }
            }
        }


        /**
         * Gets the language probability of entering this state
         * 
         * @return the log probability
         */
        public float getLanguageProbability() {
            return languageProbability * languageWeight;
        }

        /**
         * Gets the ID of the left context unit for this path
         *
         * @return the left context ID
         */
        int getLC() {
            return lc;
        }

        /**
         * Gets the ID of the desired next unit
         *
         * @return the ID of the next unit
         */
        int getNextBaseID() {
            return nextBaseID;
        }
        
        /**
         * Returns the set of IDs for all possible next units for this grammar
         * node
         *
         * @return the set of IDs of all possible next units
         */
        int[] getNextUnits() {
            return  (int[]) nodeToNextUnitArrayMap.get(node);
        }


        /**
         * Returns a string representation of this object
         *
         * @return a string representation
         */
        public String toString() {
            return node + "[" + hmmPool.getUnit(lc) + "," +
                hmmPool.getUnit(nextBaseID) + "]";
        }

        /**
          * Returns the grammar node associated with this grammar state
          *
          * @return the grammar node
          */
        GrammarNode getGrammarNode() {
            return node;
        }

    }

    class InitialState extends FlatSearchState {
        private List nextArcs  = new ArrayList();


        /**
         * Gets the set of successors for this state
         *
         * @return the set of successors
         */
        public SearchStateArc[] getSuccessors() {
            return (SearchStateArc[]) nextArcs .toArray(new
                    SearchStateArc[nextArcs .size()]);
        }

        public void addArc(SearchStateArc arc) {
            nextArcs.add(arc);
        }

        /**
         * Returns a unique string representation of the state. This string is
         * suitable (and typically used) for a label for a GDL node
         *
         * @return the signature
         */
        public String getSignature() {
            return "initialState";
        }


        /**
         * Returns the order of this state type among all of the search states
         *
         * @return the order
         */
        public int getOrder() {
            return 1;
        }


        /**
         * Returns a string representation of this object
         *
         * @return a string representation
         */
        public String toString() {
            return getSignature();
        }
    }

    /**
     * This class representations a word punctuation in the search graph
     */
    class PronunciationState extends FlatSearchState implements
                WordSearchState {
        private GrammarState gs;
        private Pronunciation pronunciation;

        /**
         * Creates a PronunciationState
         *
         * @param gs the associated grammar state
         * @param p the pronunciation
         */
        PronunciationState(GrammarState gs, Pronunciation p) {
            this.gs = gs;
            this.pronunciation = p;
            debug("PS " + p);
        }

        /**
         * Generate a hashcode for an object
         * 
         * @return the hashcode
         */
        public int hashCode() {
            return 13 * gs.hashCode() + pronunciation.hashCode();
        }

        /**
         * Determines if the given object is equal to this object
         * 
         * @param o
         *                the object to test
         * @return <code>true</code> if the object is equal to this
         */
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof PronunciationState) {
                PronunciationState other = (PronunciationState) o;
                return other.gs.equals(gs) &&
                        other.pronunciation.equals(pronunciation);
            } else {
                return false;
            }
        }


        /**
         * Gets the successor states for this search graph
         * 
         * @return the successor states
         */
        public SearchStateArc[] getSuccessors() {
            return getSuccessors(gs.getLC(), 0);
        }

        /**
         * Gets the successor states for the unit and the given position and
         * left context
         *
         * @param lc the ID of the left context
         * @param index the position of the unit within the pronunciation
         *
         * @return the set of sucessor arcs
         */
        SearchStateArc[] getSuccessors(int lc, int index) {
            SearchStateArc[] arcs;
            if (index == pronunciation.getUnits().length -1) {
                if (isContextIndependentUnit(pronunciation.getUnits()[index])) {
                    arcs = new SearchStateArc[1];
                    arcs[0] = new FullHMMSearchState(this, index, lc, ANY);
                } else {
                    int[] nextUnits = gs.getNextUnits();
                    arcs = new SearchStateArc[nextUnits.length];
                    for (int i = 0; i < arcs.length; i++) {
                        arcs[i] = new 
                            FullHMMSearchState(this, index, lc, nextUnits[i]);
                    }
                }
            } else {
                arcs = new SearchStateArc[1];
                arcs[0] = new FullHMMSearchState(this, index, lc);
            }
            return arcs;
        }

        /**
         * Gets the pronunciation assocated with this state
         *
         * @return the pronunciation
         */
        public Pronunciation getPronunciation() {
            return pronunciation;
        }


        /**
         * Determines if the given unit is a CI unit
         *
         * @param unit the unit to test
         *
         * @return true if the unit is a context independent unit
         */
        private boolean isContextIndependentUnit(Unit unit) {
            return unit.isFiller();
        }

        /**
         * Gets the insertion probability of entering this state
         * 
         * @return the log probability
         */
        public float getInsertionProbability() {
            if (pronunciation.getWord().isFiller()) {
                return logOne;
            } else {
                return logWordInsertionProbability;
            }
        }


        /**
         * Returns a unique string representation of the state. This string is
         * suitable (and typically used) for a label for a GDL node
         *
         * @return the signature
         */
        public String getSignature() {
            return "PS " + gs.getSignature() + "-" + pronunciation;
        }

        /**
         * Returns a string representation of this object
         *
         * @return a string representation
         */
        public String toString() {
            return pronunciation.getWord().getSpelling();
        }


        /**
         * Returns the order of this state type among all of the search states
         *
         * @return the order
         */
        public int getOrder() {
            return 2;
        }


        /**
         * Returns the grammar state associated with this state
         *
         * @return the grammar state
         */
        GrammarState getGrammarState() {
            return gs;
        }
    }


    /**
     * Represents a unit (as an HMM) in the search graph
     */
    class FullHMMSearchState  extends FlatSearchState implements
        UnitSearchState {
        private PronunciationState pState;
        private int index;
        private int lc;
        private int rc;
        private HMM hmm;
        private boolean isLastUnitOfWord;

        /**
         * Creates a FullHMMSearchState 
         *
         * @param p the parent PronunciationState
         * @param which the index of the unit within the pronunciation
         * @param lc the ID of the left context
         */
        FullHMMSearchState(PronunciationState p, int which, int lc) {
            this(p, which, lc, 
                    p.getPronunciation().getUnits()[which + 1].getBaseID());
        }

        /**
         * Creates a FullHMMSearchState 
         *
         * @param p the parent PronunciationState
         * @param which the index of the unit within the pronunciation
         * @param lc the ID of the left context
         * @param rc the ID of the right context
         */
        FullHMMSearchState(PronunciationState p, int which, int lc, int rc) {
            this.pState = p;
            this.index = which;
            this.lc = lc;
            this.rc = rc;
            int base =
                p.getPronunciation().getUnits()[which].getBaseID();
            int id = hmmPool.buildID(base, lc, rc);
            hmm = hmmPool.getHMM(id, getPosition());
            isLastUnitOfWord = 
                    which == p.getPronunciation().getUnits().length - 1;

            debug("HMM " + hmm.getUnit() + " rc " + rc);
        }


        /**
         * Returns a string representation of this object
         *
         * @return a string representation
         */
        public String toString() {
            return hmm.getUnit().toString();
        }

        /**
         * Generate a hashcode for an object
         * 
         * @return the hashcode
         */
        public int hashCode() {
            return pState.getGrammarState().getGrammarNode().hashCode() * 29 +
                pState.getPronunciation().hashCode() * 19 +
                index * 7 + 43 * lc + rc;
        }

        /**
         * Determines if the given object is equal to this object
         * 
         * @param o
         *                the object to test
         * @return <code>true</code> if the object is equal to this
         */
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof FullHMMSearchState) {
                FullHMMSearchState other = (FullHMMSearchState) o;
                // the definition for equal for a FullHMMState:
                // Grammar Node equal
                // Pronunciation equal
                // index equal
                // rc equal

                return pState.getGrammarState().getGrammarNode() ==
                      other.pState.getGrammarState().getGrammarNode() &&
                      pState.getPronunciation() == other.pState.getPronunciation() &&
                      index == other.index && lc == other.lc && rc == other.rc;
            } else {
                return false;
            }
        }

        /**
         * Returns the unit assoicated with this state
         *
         * @return the unit
         */
        public Unit getUnit() {
            return hmm.getBaseUnit();
        }

        /**
         * Gets the set of successors for this state
         *
         * @return the set of successors
         */
        public SearchStateArc[] getSuccessors() {
            SearchStateArc[] arcs = new SearchStateArc[1];
            arcs[0] = new HMMStateSearchState(this, hmm.getInitialState());
            return arcs;
        }


        /**
         * Determines if this unit is the last unit of a word
         *
         * @return true if this unit is the last unit of a word
         */
        boolean isLastUnitOfWord() {
            return isLastUnitOfWord;
        }


        /**
         * Determines the position of the unit within the word
         *
         * @return the position of the unit within the word
         */
        HMMPosition getPosition() {
            int len = pState.getPronunciation().getUnits().length;
            if (len == 1) {
                return HMMPosition.SINGLE;
            } else if (index == 0) {
                return HMMPosition.BEGIN;
            } else if (index == len -1) {
                return HMMPosition.END;
            } else {
                return HMMPosition.INTERNAL;
            }
        }

        /**
         * Returns the HMM for this state
         *
         * @return the HMM
         */
        HMM getHMM() {
            return hmm;
        }

        /**
         * Returns the order of this state type among all of the search states
         *
         * @return the order
         */
        public int getOrder() {
            return 3;
        }

        /**
         * Determines the insertion probability based upon the type of unit
         *
         * @return the insertion probability
         */
        public float getInsertionProbability() {
            Unit unit = hmm.getBaseUnit();

            if (unit.isSilence()) {
                return logSilenceInsertionProbability;
            } else if (unit.isFiller()) {
                return logFillerInsertionProbability;
            } else {
                return logUnitInsertionProbability;
            }
        }

        /**
         * Returns a unique string representation of the state. This string is
         * suitable (and typically used) for a label for a GDL node
         *
         * @return the signature
         */
        public String getSignature() {
            return "HSS " + pState.getGrammarState().getGrammarNode() +
                pState.getPronunciation() + index + "-" +  rc + "-" + lc;
        }

        /**
         * Returns the ID of the right context for this state
         *
         * @return the right context unit ID
         */
        int getRC() {
            return rc;
        }


        /**
         * Returns the next set of arcs after this state and all
         * substates have been processed
         *
         * @return the next set of arcs
         */
        SearchStateArc[] getNextArcs() {
            SearchStateArc[] arcs;
            // this is the last state of the hmm
            // so check to see if we are at the end
            // of a word, if not get the next full hmm in the word
            // otherwise generate arcs to the next set of words

            Pronunciation pronunciation = pState.getPronunciation();
            int nextLC = getHMM().getBaseUnit().getBaseID(); 

            if (!isLastUnitOfWord()) {
                arcs = pState.getSuccessors(nextLC, index + 1);
            } else {
                // we are at the end of the word, so we transit to the
                // next grammar nodes
                GrammarState gs = pState.getGrammarState();
                debug("HSSS next base " + getRC());
                arcs = gs.getNextGrammarStates(nextLC, getRC());
            }
            return arcs;
        }
    }

    /**
     * Represents a single hmm state in the search graph
     */
    class HMMStateSearchState  extends FlatSearchState implements
                HMMSearchState {
        private FullHMMSearchState fullHMMSearchState;
        private HMMState hmmState;
        private float probability;
        

        /**
         * Creates an HMMStateSearchState
         *
         * @param hss the parent hmm state
         * @param hmmState which hmm state
         */
        HMMStateSearchState(FullHMMSearchState hss, HMMState hmmState) {
            this(hss, hmmState, logOne);
        }

        /**
         * Creates an HMMStateSearchState
         *
         * @param hss the parent hmm state
         * @param hmmState which hmm state
         * @param prob the transition probability
         */
        HMMStateSearchState(FullHMMSearchState hss, HMMState hmmState, 
                        float prob) {
            this.probability = prob;
            fullHMMSearchState = hss;
            this.hmmState = hmmState;
            debug("HSS " + hmmState);
        }

        /**
         * Generate a hashcode for an object
         * 
         * @return the hashcode
         */
        public int hashCode() {
            return 7 * fullHMMSearchState.hashCode() + hmmState.hashCode();
        }

        /**
         * Determines if the given object is equal to this object
         * 
         * @param o
         *                the object to test
         * @return <code>true</code> if the object is equal to this
         */
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof HMMStateSearchState) {
                HMMStateSearchState other = (HMMStateSearchState) o;
                return other.fullHMMSearchState.equals(fullHMMSearchState) 
                    && other.hmmState.equals(hmmState);
            } else {
                return false;
            }
        }

        /**
         * Determines if this state is an emitting state
         *
         * @return true if this is an emitting state
         */
        public boolean isEmitting() {
            return hmmState.isEmitting();
        }

        /**
         * Returns the acoustic probability for this state
         *
         * @return the probability
         */
        public float getAcousticProbability() {
            return probability;
        }

        /**
         * Gets the set of successors for this state
         *
         * @return the set of successors
         */
        public SearchStateArc[] getSuccessors() {
            SearchStateArc[] arcs;
            if (hmmState.isExitState()) {
                arcs = fullHMMSearchState.getNextArcs();
            } else {
                HMMStateArc[] next = hmmState.getSuccessors();
                arcs = new SearchStateArc[next.length];
                for (int i = 0; i < arcs.length; i++) {
                    arcs[i] = new
                        HMMStateSearchState(fullHMMSearchState,
                                next[i].getHMMState(), 
                                next[i].getLogProbability());
                }
            }
            return arcs;
        }

        /**
         * Returns the order of this state type among all of the search states
         *
         * @return the order
         */
        public int getOrder() {
            return isEmitting() ? 4 : 0;
        }

        /**
         * Returns a unique string representation of the state. This string is
         * suitable (and typically used) for a label for a GDL node
         *
         * @return the signature
         */
        public String getSignature() {
            return "HSSS " + fullHMMSearchState.getSignature() + "-" + hmmState;
        }

        /**
         * Returns the hmm state for this search state
         *
         * @return the hmm state
         */
        public HMMState getHMMState() {
            return hmmState;
        }
    }


    /**
     * The search graph that is produced by the flat linguist.
     */
    class DynamicFlatSearchGraph implements SearchGraph {
        /*
         * (non-Javadoc)
         * 
         * @see edu.cmu.sphinx.linguist.SearchGraph#getInitialState()
         */
        public SearchState getInitialState() {
            InitialState initialState = new InitialState();
            initialState.addArc(new GrammarState(grammar.getInitialNode()));
            // add an out-of-grammar branch if configured to do so
            if (addOutOfGrammarBranch) {
                OutOfGrammarGraph oogg = new OutOfGrammarGraph
                    (phoneLoopAcousticModel,
                     logOutOfGrammarBranchProbability,
                     logPhoneInsertionProbability);

                initialState.addArc(oogg.getOutOfGrammarGraph());
            }
            return initialState;
        }


        /*
         * (non-Javadoc)
         * 
         * @see edu.cmu.sphinx.linguist.SearchGraph#getNumStateOrder()
         */
        public int getNumStateOrder() {
            return 5;   
        }
    }
}
