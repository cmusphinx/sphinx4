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
package edu.cmu.sphinx.linguist.flat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.SearchGraph;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
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
 * Note that all probabilties are maintained in the log math domain
 */
public class FlatLinguist implements Linguist, Configurable {
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
     * Sphinx property that defines the name of the logmath to be used by this
     * search manager.
     */
    public final static String PROP_LOG_MATH = "logMath";
    /**
     * Sphinx property used to determine whether or not the gstates are dumped.
     */
    public final static String PROP_DUMP_GSTATES = "dumpGstates";
    /**
     * The default value for the PROP_DUMP_GSTATES property
     */
    public final static boolean PROP_DUMP_GSTATES_DEFAULT = false;
    private final static float logOne = LogMath.getLogOne();
    // ----------------------------------
    // Subcomponents that are configured
    // by the property sheet
    // -----------------------------------
    private Grammar grammar;
    private AcousticModel acousticModel;
    private LogMath logMath;
    private UnitManager unitManager;
    // ------------------------------------
    // Data that is configured by the
    // property sheet
    // ------------------------------------
    private float logWordInsertionProbability;
    private float logSilenceInsertionProbability;
    private float logUnitInsertionProbability;
    private boolean showSentenceHMM;
    private boolean showCompilationProgress = true;
    private boolean spreadWordProbabilitiesAcrossPronunciations;
    private boolean dumpGStates;
    private float languageWeight;
    // -----------------------------------
    // Data for monitoring performance
    // ------------------------------------
    private StatisticsVariable totalStates;
    private StatisticsVariable totalArcs;
    private StatisticsVariable actualArcs;
    private SearchGraph searchGraph;
    private transient int totalStateCounter = 0;
    private final boolean tracing = false;
    // ------------------------------------
    // Data used for building and maintaining
    // the search graph
    // -------------------------------------
    private Map nodeStateMap;
    private Map arcPool;
    private transient Collection stateSet = null;
    private String name;
    /**
     * Returns the search graph
     * 
     * @return the search graph
     */
    public SearchGraph getSearchGraph() {
        return searchGraph;
    }
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
        registry.register(PROP_SHOW_SEARCH_SPACE, PropertyType.BOOLEAN);
        registry.register(PROP_DUMP_GSTATES, PropertyType.BOOLEAN);
        registry.register(PROP_SHOW_COMPILATION_PROGRESS, PropertyType.BOOLEAN);
        registry.register(PROP_SPREAD_WORD_PROBABILITIES_ACROSS_PRONUNCIATIONS,
                PropertyType.BOOLEAN);
        registry.register(PROP_UNIT_MANAGER, PropertyType.COMPONENT);
    }
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        // hookup to all of the components
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
        languageWeight = ps.getFloat(Linguist.PROP_LANGUAGE_WEIGHT,
                PROP_LANGUAGE_WEIGHT_DEFAULT);
        showSentenceHMM = ps.getBoolean(Linguist.PROP_SHOW_SEARCH_SPACE,
                PROP_SHOW_SEARCH_SPACE_DEFAULT);
        dumpGStates = ps.getBoolean(PROP_DUMP_GSTATES,
                PROP_DUMP_GSTATES_DEFAULT);
        showCompilationProgress = ps.getBoolean(
                PROP_SHOW_COMPILATION_PROGRESS,
                PROP_SHOW_COMPILATION_PROGRESS_DEFAULT);
        spreadWordProbabilitiesAcrossPronunciations = ps.getBoolean(
                PROP_SPREAD_WORD_PROBABILITIES_ACROSS_PRONUNCIATIONS,
                PROP_SPREAD_WORD_PROBABILITIES_ACROSS_PRONUNCIATIONS_DEFAULT);
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
        nodeStateMap = new HashMap();
        arcPool = new HashMap();
        allocateAcousticModel();
        grammar.allocate();
        totalStates = StatisticsVariable.getStatisticsVariable(getName(),
                "totalStates");
        totalArcs = StatisticsVariable.getStatisticsVariable(getName(),
                "totalArcs");
        actualArcs = StatisticsVariable.getStatisticsVariable(getName(),
                "actualArcs");
        stateSet = compileGrammar();
        totalStates.value = stateSet.size();
        nodeStateMap = null;
        arcPool = null;
    }

    /**
     * Allocates the acoustic model.
     */
    protected void allocateAcousticModel() throws IOException {
        acousticModel.allocate();
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
     * Compiles the grammar into a sentence hmm. A GrammarJob is created for
     * the initial grammar node and added to the GrammarJob queue. While there
     * are jobs left on the grammar job queue, a job is removed from the queue
     * and the associated grammar node is expanded and attached to the tails.
     * GrammarJobs for the successors are added to the grammar job queue.
     */
    protected Collection compileGrammar() {
        List gstateList = new ArrayList();
        Timer.start("compile");
        // get the nodes from the grammar and create states
        // for them. Add the non-empty gstates to the gstate list.
        Timer.start("  createGStates");
        for (Iterator i = grammar.getGrammarNodes().iterator(); i.hasNext();) {
            GState gstate = createGState((GrammarNode) i.next());
            gstateList.add(gstate);
        }
        Timer.stop("  createGStates");
        addStartingPath(); // ensures an initial path to the start state
        // Prep all the gstates, by gathering all of the contexts up
        // this allows each gstate to know about its surrounding
        // contexts
        Timer.start("  collectContexts");
        for (Iterator i = gstateList.iterator(); i.hasNext();) {
            GState gstate = (GState) i.next();
            gstate.collectContexts();
        }
        Timer.stop("  collectContexts");
        // now all gstates know all about their contexts, we can
        // expand them fully
        Timer.start("  expandStates");
        for (Iterator i = gstateList.iterator(); i.hasNext();) {
            GState gstate = (GState) i.next();
            gstate.expand();
        }
        Timer.stop("  expandStates");
        // now that all states are expanded fully, we can connect all
        // the states up
        Timer.start("  connectNodes");
        for (Iterator i = gstateList.iterator(); i.hasNext();) {
            GState gstate = (GState) i.next();
            gstate.connect();
        }
        Timer.stop("  connectNodes");
        SentenceHMMState initialState = findStartingState();
        searchGraph = new FlatSearchGraph(initialState);
        Timer.stop("compile");
        // Now that we are all done, dump out some interesting
        // information about the process
        if (dumpGStates) {
            for (Iterator i = grammar.getGrammarNodes().iterator(); i.hasNext();) {
                GState gstate = getGState((GrammarNode) i.next());
                gstate.dumpInfo();
            }
        }
        return SentenceHMMState.collectStates(initialState);
    }
    /**
     * Returns a new GState for the given GrammarNode.
     * 
     * @return a new GState for the given GrammarNode
     */
    protected GState createGState(GrammarNode grammarNode) {
        return (new GState(grammarNode));
    }
    /**
     * Ensures that there is a starting path by adding an empty left context to
     * the strating gstate
     */
    // TODO: Currently the FlatLinguist requires that the initial
    // grammar node returned by the Grammar contains a "sil" word
    private void addStartingPath() {
        // guarantees a starting path into the initial node by
        // adding an empty left context to the starting gstate
        GrammarNode node = grammar.getInitialNode();
        GState gstate = getGState(node);
        gstate.addLeftContext(UnitContext.SILENCE);
    }
    /**
     * Finds the starting state
     * 
     * @return the starting state
     */
    private SentenceHMMState findStartingState() {
        GrammarNode node = grammar.getInitialNode();
        GState gstate = getGState(node);
        return (SentenceHMMState) gstate.getEntryPoint();
    }
    /**
     * Gets a SentenceHMMStateArc. The arc is drawn from a pool of arcs.
     * 
     * @param nextState
     *                the next state
     * @param logAcousticProbability
     *                the log acoustic probability
     * @param logLanguageProbability
     *                the log language probability
     * @param logInsertionProbability
     *                the log insertion probability
     *  
     */
    private SentenceHMMStateArc getArc(SentenceHMMState nextState,
            float logAcousticProbability, float logLanguageProbability,
            float logInsertionProbability) {
        SentenceHMMStateArc arc = new SentenceHMMStateArc(nextState,
                logAcousticProbability,
                logLanguageProbability * languageWeight,
                logInsertionProbability);
        SentenceHMMStateArc pooledArc = (SentenceHMMStateArc) arcPool.get(arc);
        if (pooledArc == null) {
            arcPool.put(arc, arc);
            pooledArc = arc;
            actualArcs.value++;
        }
        totalArcs.value++;
        return pooledArc;
    }
    /**
     * Given a grammar node, retrieve the grammar state
     * 
     * @param node
     *                the grammar node
     * 
     * @return the grammar state associated with the node
     *  
     */
    private GState getGState(GrammarNode node) {
        return (GState) nodeStateMap.get(node);
    }
    /**
     * The search graph that is produced by the flat linguist.
     */
    private final static Class[] searchStateOrder = {NonEmittingHMMState.class,
            BranchState.class, GrammarState.class, PronunciationState.class,
            ExtendedUnitState.class, HMMStateState.class};
    class FlatSearchGraph implements SearchGraph {
        /**
         * An array of classes that represents the order in which the states
         * will be returned.
         */
        private SearchState initialState;
        /**
         * Constructs a flast search graph with the given initial state
         * 
         * @param initialState
         *                the initial state
         */
        FlatSearchGraph(SearchState initialState) {
            this.initialState = initialState;
        }
        /*
         * (non-Javadoc)
         * 
         * @see edu.cmu.sphinx.linguist.SearchGraph#getInitialState()
         */
        public SearchState getInitialState() {
            return initialState;
        }
        /*
         * (non-Javadoc)
         * 
         * @see edu.cmu.sphinx.linguist.SearchGraph#getSearchStateOrder()
         */
        public Class[] getSearchStateOrder() {
            return searchStateOrder;
        }
    }
    /**
     * This is a nested class that is used to manage the construction of the
     * states in a grammar node. There is one GState created for each grammar
     * node. The GState is used to collect the entry and exit points for the
     * grammar node and for connecting up the grammar nodes to each other.
     */
    protected class GState {
        private GrammarNode node;
        private Set rightContexts = new HashSet();
        private Set leftContexts = new HashSet();
        private Set startingContexts;
        private Map entryPoints = new HashMap();
        private Map exitPoints = new HashMap();
        private Map existingStates = new HashMap();
        private int exitConnections = 0;
        private GrammarArc[] successors = null;
        /**
         * Creates a GState for a grammar ndoe
         * 
         * @param node
         *                the grammar node
         */
        protected GState(GrammarNode node) {
            this.node = node;
            nodeStateMap.put(node, this);
        }
        /**
         * Retrieves the set of starting contexts for this node. The starting
         * contexts are the set of Unit[] with a size equal to the maximum
         * right context size.
         * 
         * @return the set of starting contexts acrosss nodes.
         */
        private Set getStartingContexts() {
            if (startingContexts == null) {
                startingContexts = new HashSet();
                // if this is an empty node, the starting context is
                // the set of starting contexts for all successor
                // nodes, otherwise, it is built up from each
                // pronunciation of this word
                if (node.isEmpty()) {
                    GrammarArc[] arcs = getSuccessors();
                    for (int i = 0; i < arcs.length; i++) {
                        GState gstate = getGState((GrammarNode) arcs[i]
                                .getGrammarNode());
                        startingContexts.addAll(gstate.getStartingContexts());
                    }
                } else {
                    int maxSize = getRightContextSize();
                    Word word = node.getWord();
                    Pronunciation[] prons = word.getPronunciations(null);
                    for (int i = 0; i < prons.length; i++) {
                        UnitContext startingContext = getStartingContext(prons[i]);
                        startingContexts.add(startingContext);
                    }
                }
            }
            return startingContexts;
        }
        /**
         * Retrieves the starting UnitContext for the given pronunciation
         * 
         * @param pronunciation
         *                the pronunciation
         * 
         * @return a UnitContext representing the starting context of the
         *         pronunciation
         */
        private UnitContext getStartingContext(Pronunciation pronunciation) {
            int maxSize = getRightContextSize();
            Unit[] units = pronunciation.getUnits();
            int actualSize = Math.min(units.length, maxSize);
            Unit[] context = new Unit[actualSize];
            for (int j = 0; j < context.length; j++) {
                context[j] = units[j];
            }
            return UnitContext.get(context);
        }
        /**
         * Retrieves the set of trailing contexts for this node. the trailing
         * contexts are the set of Unit[] with a size equal to the maximum left
         * context size that align with the end of the node
         *  
         */
        Collection getEndingContexts() {
            Collection endingContexts = new HashSet();
            if (!node.isEmpty()) {
                int maxSize = getLeftContextSize();
                Word word = node.getWord();
                Pronunciation[] prons = word.getPronunciations(null);
                for (int i = 0; i < prons.length; i++) {
                    Unit[] units = prons[i].getUnits();
                    int actualSize = Math.min(units.length, maxSize);
                    Unit[] context = new Unit[actualSize];
                    int unitIndex = units.length - actualSize;
                    for (int j = 0; j < context.length; j++) {
                        context[j] = units[unitIndex++];
                    }
                    endingContexts.add(UnitContext.get(context));
                }
                // add a silence to the ending context since we want to
                // include an optional transition to a silence unit at
                // the end of all words
                endingContexts.add(UnitContext.SILENCE);
            }
            return endingContexts;
        }
        /**
         * Visit all of the successor states, and gather their starting
         * contexts into this gstates right context
         */
        private void pullRightContexts() {
            GrammarArc[] arcs = getSuccessors();
            for (int i = 0; i < arcs.length; i++) {
                GState gstate = getGState((GrammarNode) arcs[i]
                        .getGrammarNode());
                rightContexts.addAll(gstate.getStartingContexts());
            }
        }
        /**
         * Returns the set of succesor arcs for this grammar node. If a
         * successor grammar node has no words we'll substitute the successors
         * for that node (avoiding loops of course)
         * 
         * @return an array of successors for this GState
         */
        private GrammarArc[] getSuccessors() {
            return node.getSuccessors();
        }
        /**
         * Visit all of the successor states, and push our ending context into
         * the successors left context
         */
        void pushLeftContexts() {
            Collection endingContext = getEndingContexts();
            Set visitedSet = new HashSet();
            pushLeftContexts(visitedSet, endingContext);
        }
        /**
         * Pushes the given left context into the successor states. If a
         * successor state is empty, continue to push into this empty states
         * successors
         * 
         * @param leftContext
         *                the context to push
         */
        void pushLeftContexts(Set visitedSet, Collection leftContext) {
            if (visitedSet.contains(getNode())) {
                return;
            } else {
                visitedSet.add(getNode());
            }
            GrammarArc[] arcs = getSuccessors();
            for (int i = 0; i < arcs.length; i++) {
                GState gstate = getGState((GrammarNode) arcs[i]
                        .getGrammarNode());
                gstate.addLeftContext(leftContext);
                // if our successor state is empty, also push our
                // ending context into the empty nodes successors
                if (gstate.getNode().isEmpty()) {
                    gstate.pushLeftContexts(visitedSet, leftContext);
                }
            }
        }
        /**
         * Add the given left contexts to the set of left contexts for this
         * state
         * 
         * @param context
         *                the set of contexts to add
         */
        private void addLeftContext(Collection context) {
            leftContexts.addAll(context);
        }
        /**
         * Adds the given context to the set of left contexts for this state
         * 
         * @param context
         *                the context to add
         */
        private void addLeftContext(UnitContext context) {
            leftContexts.add(context);
        }
        /**
         * Returns the entry points for a given context pair
         */
        private List getEntryPoints(ContextPair contextPair) {
            return (List) entryPoints.get(contextPair);
        }
        /**
         * Gets the context-free entry point to this state
         * 
         * @return the entry point to the state
         */
        // TODO: ideally we'll look for entry points with no left
        // context, but those don't exist yet so we just take
        // the first entry point with an SILENCE left context
        // note that this assumes that the first node in a grammar has a
        // word and that word is a SIL. Not always a valid assumption.
        public SentenceHMMState getEntryPoint() {
            ContextPair cp = ContextPair.get(UnitContext.SILENCE,
                    UnitContext.SILENCE);
            List list = getEntryPoints(cp);
            if (list != null && list.size() > 0) {
                return (SentenceHMMState) list.get(0);
            } else {
                return null;
            }
        }
        /**
         * Returns the exit points for a given context pair
         * 
         * @param contextPair
         *                the context pair of interest
         * 
         * @return the list of exit points
         */
        private List getExitPoints(ContextPair contextPair) {
            return (List) exitPoints.get(contextPair);
        }
        /**
         * Add the items on the newContexts list to the dest list. Duplicate
         * items are not added.
         * 
         * @param dest
         *                where the contexts are added
         * @param newContexts
         *                the list of new contexts
         */
        private void addWithNoDuplicates(List dest, List newContexts) {
            // this could potentially be a bottleneck, but the contexts
            // lists should be fairly small (<100) items, so this approach
            // should be fast enough.
            for (Iterator i = newContexts.iterator(); i.hasNext();) {
                Unit[] context = (Unit[]) i.next();
                if (!listContains(dest, context)) {
                    dest.add(context);
                }
            }
        }
        /**
         * Deterimes if the give list contains the given context
         * 
         * @param list
         *                the list of contexts
         * 
         * @param context
         *                the context to check
         */
        private boolean listContains(List list, Unit[] context) {
            for (Iterator i = list.iterator(); i.hasNext();) {
                Unit[] item = (Unit[]) i.next();
                if (Unit.isContextMatch(item, context)) {
                    return true;
                }
            }
            return false;
        }
        /**
         * Collects the right contexts for this node and pushes this nodes
         * ending context into the next next set of nodes.
         */
        void collectContexts() {
            pullRightContexts();
            pushLeftContexts();
        }
        /**
         * Expands each GState into the sentence HMM States
         */
        void expand() {
            // for each left context/starting context pair create a list
            // of starting states.
            for (Iterator i = leftContexts.iterator(); i.hasNext();) {
                UnitContext leftContext = (UnitContext) i.next();
                for (Iterator j = getStartingContexts().iterator(); j.hasNext();) {
                    UnitContext startingContext = (UnitContext) j.next();
                    ContextPair contextPair = ContextPair.get(leftContext,
                            startingContext);
                    entryPoints.put(contextPair, new ArrayList());
                }
            }
            // if this is a final node don't expand it, just create a
            // state and add it to all entry points
            if (node.isFinalNode()) {
                GrammarState gs = new GrammarState(node);
                for (Iterator i = entryPoints.values().iterator(); i.hasNext();) {
                    List epList = (List) i.next();
                    epList.add(gs);
                }
            } else if (!node.isEmpty()) {
                // its a full fledged node with a word
                // so expand it. Nodes without words don't need
                // to be expanded.
                for (Iterator i = leftContexts.iterator(); i.hasNext();) {
                    UnitContext leftContext = (UnitContext) i.next();
                    expandWord(leftContext);
                }
            } else {
                //if the node is empty, populate the set of entry and exit
                //points with a branch state. The branch state
                // branches to the succesor entry points for this
                // state
                // the exit point should consist of the set of
                // incoming left contexts and outgoing right contexts
                // the 'entryPoint' table already consists of such
                // pairs so we can use that
                for (Iterator i = entryPoints.keySet().iterator(); i.hasNext();) {
                    ContextPair cp = (ContextPair) i.next();
                    List epList = (List) entryPoints.get(cp);
                    SentenceHMMState bs = new BranchState(cp.getLeftContext()
                            .toString(), cp.getRightContext().toString(), node
                            .getID());
                    epList.add(bs);
                    addExitPoint(cp, bs);
                }
            }
            addEmptyEntryPoints();
        }
        /**
         * Adds the set of empty entry points. The list of entry points are
         * tagged with a context pair. The context pair represent the left
         * context for the state and the starting contesxt for the state, this
         * allows states to be hooked up properly. However, we may be
         * transitioning from states that have no right hand context (CI units
         * such as SIL fall into this category). In this case we'd normally
         * have no place to transition to since we add entry points for each
         * starting context. To make sure that there are entry points for empty
         * contexts if necesary, we go through the list of entry points and
         * find all left contexts that have a right hand context size of zero.
         * These entry points will need an entry point with an empty starting
         * context. These entries are synthesized and added to the the list of
         * entry points.
         */
        private void addEmptyEntryPoints() {
            Map emptyEntryPoints = new HashMap();
            for (Iterator i = entryPoints.keySet().iterator(); i.hasNext();) {
                ContextPair cp = (ContextPair) i.next();
                if (needsEmptyVersion(cp)) {
                    ContextPair emptyContextPair = ContextPair.get(cp
                            .getLeftContext(), UnitContext.EMPTY);
                    List epList = (List) emptyEntryPoints.get(emptyContextPair);
                    if (epList == null) {
                        epList = new ArrayList();
                        emptyEntryPoints.put(emptyContextPair, epList);
                    }
                    epList.addAll((List) entryPoints.get(cp));
                }
            }
            entryPoints.putAll(emptyEntryPoints);
        }
        /**
         * Determines if the context pair needs an empty version. A context
         * pair needs an empty version if the left context has a max size of
         * zero.
         * 
         * @param cp
         *                the contex pair to check
         * 
         * @return <code>true</code> if the pair needs an empt version
         */
        private boolean needsEmptyVersion(ContextPair cp) {
            UnitContext left = cp.getLeftContext();
            Unit[] units = left.getUnits();
            if (units.length > 0) {
                return (getRightContextSize(units[0]) < getRightContextSize());
            }
            return false;
        }
        /**
         * Returns the grammar node of the gstate
         * 
         * @return the grammar node
         */
        private final GrammarNode getNode() {
            return node;
        }
        /**
         * Expand the the word given the left context
         * 
         * @param leftContext
         *                the left context
         */
        private void expandWord(UnitContext leftContext) {
            Word word = node.getWord();
            T("  Expanding word " + word + " for lc " + leftContext);
            Pronunciation[] pronunciations = word.getPronunciations(null);
            for (int i = 0; i < pronunciations.length; i++) {
                expandPronunciation(leftContext, pronunciations[i], i);
            }
        }
        /**
         * Expand the pronunciation given the left context
         * 
         * @param leftContext
         *                the left context
         * @param pronunciation
         *                the pronunciation to expand
         * @param which
         *                unique ID for this pronunciation
         */
        // Each GState maintains a list of entry points. This list of
        // entry points is used when connecting up the end states of
        // one GState to the beginning states in another GState. The
        // entry points are tagged by a ContextPair which represents
        // the left context upon entering the state (the left context
        // of the initial units of the state), and the right context
        // of the previous states (corresponding to the starting
        // contexts for this state).
        //
        // When expanding a proununciation, the following steps are
        // taken:
        //      1) Get the starting context for the pronunciation.
        //      This is the set of units that correspond to the start
        //      of the pronunciation.
        //
        //      2) Create a new PronunciationState for the
        //      pronunciation.
        //
        //      3) Add the PronunciationState to the entry point table
        //      (a hash table keyed by the ContextPair(LeftContext,
        //      StartingContext).
        //
        //      4) Generate the set of context dependent units, using
        //      the left and right context of the GState as necessary.
        //      Note that there will be fan out at the end of the
        //      pronunciation to allow for units with all of the
        //      various right contexts. The point where the fan-out
        //      occurs is the (length of the pronunciation - the max
        //      right context size).
        //
        //      5) Attach each cd unit to the tree
        //
        //      6) Expand each cd unit into the set of HMM states
        //
        //      7) Attach the optional and looping back silence cd
        //      unit
        //
        //      8) Collect the leaf states of the tree and add them to
        //      the exitStates list.
        private void expandPronunciation(UnitContext leftContext,
                Pronunciation pronunciation, int which) {
            UnitContext startingContext = getStartingContext(pronunciation);
            // Add the pronunciation state to the entry point list
            // (based upon its left and right context)
            String pname = "P(" + pronunciation.getWord() + "[" + leftContext
                    + "," + startingContext + "])-G" + getNode().getID();
            PronunciationState ps = new PronunciationState(pname,
                    pronunciation, which);
            T("     Expanding " + ps.getPronunciation() + " for lc "
                    + leftContext);
            ContextPair cp = ContextPair.get(leftContext, startingContext);
            List epList = (List) entryPoints.get(cp);
            if (epList == null) {
                throw new Error("No EP list for context pair " + cp);
            } else {
                epList.add(ps);
            }
            Unit[] units = pronunciation.getUnits();
            int fanOutPoint = units.length - getRightContextSize();
            if (fanOutPoint < 0) {
                fanOutPoint = 0;
            }
            SentenceHMMState tail = ps;
            for (int i = 0; tail != null && i < fanOutPoint; i++) {
                tail = attachUnit(ps, tail, units, i, leftContext,
                        UnitContext.EMPTY);
            }
            SentenceHMMState branchTail = tail;
            for (Iterator iter = rightContexts.iterator(); iter.hasNext();) {
                UnitContext finalRightContext = (UnitContext) iter.next();
                tail = branchTail;
                for (int i = fanOutPoint; tail != null && i < units.length; i++) {
                    tail = attachUnit(ps, tail, units, i, leftContext,
                            finalRightContext);
                }
            }
        }
        /**
         * Attaches the given unit to the given tail, expanding the unit if
         * necessary. If an identical unit is already attached, then this path
         * is folded into the existing path.
         * 
         * @param parent
         *                the parent state
         * @param tail
         *                the place to attach the unit to
         * @param units
         *                the set of units
         * @param which
         *                the index into the set of units
         * @param leftContext
         *                the left context for the unit
         * @param rightContext
         *                the right context for the unit
         * 
         * @return the tail of the added unit (or null if the path was folded
         *         onto an already expanded path.
         */
        private SentenceHMMState attachUnit(PronunciationState parent,
                SentenceHMMState tail, Unit[] units, int which,
                UnitContext leftContext, UnitContext rightContext) {
            Unit[] lc = getLC(leftContext, units, which);
            Unit[] rc = getRC(units, which, rightContext);
            UnitContext actualRightContext = UnitContext.get(rc);
            LeftRightContext context = LeftRightContext.get(lc, rc);
            Unit cdUnit = unitManager.getUnit(units[which].getName(), units[which]
                    .isFiller(), context);
            UnitState unitState = new ExtendedUnitState(parent, which, cdUnit);
            float logInsertionProbability;
            if (unitState.getUnit().isFiller()) {
                logInsertionProbability = logSilenceInsertionProbability;
            } else if (unitState.getWhich() == 0) {
                logInsertionProbability = logWordInsertionProbability;
            } else {
                logInsertionProbability = logUnitInsertionProbability;
            }
            // check to see if this state already exists, if so
            // branch to it and we are done, otherwise, branch to
            // the new state and expand it.
            SentenceHMMState existingState = getExistingState(unitState);
            if (existingState != null) {
                attachState(tail, existingState, logOne, logOne,
                        logInsertionProbability);
                // T(" Folding " + existingState);
                return null;
            } else {
                attachState(tail, unitState, logOne, logOne,
                        logInsertionProbability);
                addStateToCache(unitState);
                // T(" Attaching " + unitState);
                tail = expandUnit(unitState);
                // if we are attaching the last state of a word, then
                // we add it to the exitPoints table. the exit points
                // table is indexed by a ContextPair, consisting of
                // the exiting left context and the right context.
                if (unitState.isLast()) {
                    UnitContext nextLeftContext = generateNextLeftContext(
                            leftContext, units[which]);
                    ContextPair cp = ContextPair.get(nextLeftContext,
                            actualRightContext);
                    // T(" Adding to exitPoints " + cp);
                    addExitPoint(cp, tail);
                    // if we have encountered a last unit with a right
                    // context of silence, then we add a silence unit
                    // to this unit. the silence unit has a self
                    // loopback.
                    if (actualRightContext == UnitContext.SILENCE) {
                        SentenceHMMState silTail;
                        UnitState silUnit = new ExtendedUnitState(parent,
                                which + 1, UnitManager.SILENCE);
                        SentenceHMMState silExistingState = getExistingState(silUnit);
                        if (silExistingState != null) {
                            attachState(tail, silExistingState, logOne, logOne,
                                    logSilenceInsertionProbability);
                        } else {
                            attachState(tail, silUnit, logOne, logOne,
                                    logSilenceInsertionProbability);
                            addStateToCache(silUnit);
                            silTail = expandUnit(silUnit);
                            ContextPair silCP = ContextPair.get(
                                    UnitContext.SILENCE, UnitContext.EMPTY);
                            addExitPoint(silCP, silTail);
                        }
                    }
                }
                return tail;
            }
        }
        /**
         * Adds an exit point to this gstate
         * 
         * @param cp
         *                the context tag for the state
         * @param state
         *                the state associated with the tag
         */
        private void addExitPoint(ContextPair cp, SentenceHMMState state) {
            List list = (List) exitPoints.get(cp);
            if (list == null) {
                list = new ArrayList();
                exitPoints.put(cp, list);
            }
            list.add(state);
        }
        /**
         * Get the left context for a unit based upon the left context size,
         * the entry left context and the current unit.
         * 
         * @param left
         *                the entry left context
         * @param units
         *                the set of units
         * @param index
         *                the index of the current unit
         */
        private Unit[] getLC(UnitContext left, Unit[] units, int index) {
            Unit[] leftUnits = left.getUnits();
            int maxSize = getLeftContextSize(units[index]);
            int curSize = index + leftUnits.length;
            int actSize = Math.min(curSize, maxSize);
            Unit[] lc = new Unit[actSize];
            for (int i = 0; i < lc.length; i++) {
                int lcIndex = (index - lc.length) + i;
                if (lcIndex < 0) {
                    lc[i] = leftUnits[leftUnits.length + lcIndex];
                } else {
                    lc[i] = units[lcIndex];
                }
            }
            return lc;
        }
        /**
         * Get the right context for a unit based upon the right context size,
         * the exit right context and the current unit.
         * 
         * @param units
         *                the set of units
         * @param index
         *                the index of the current unit
         * @param right
         *                the exiting right context
         */
        private Unit[] getRC(Unit[] units, int index, UnitContext right) {
            Unit[] rightUnits = right.getUnits();
            int maxSize = getRightContextSize(units[index]);
            int curSize = (units.length - (index + 1)) + rightUnits.length;
            int actSize = Math.min(curSize, maxSize);
            Unit[] rc = new Unit[actSize];
            for (int i = 0; i < rc.length; i++) {
                int rcIndex = index + i + 1;
                if (rcIndex >= units.length) {
                    rc[i] = rightUnits[rcIndex - units.length];
                } else {
                    rc[i] = units[rcIndex];
                }
            }
            return rc;
        }
        /**
         * Gets the maximum context size for the given unit
         * 
         * @param unit
         *                the unit of interest
         * 
         * @return the maximum left context size for the unit
         */
        private int getLeftContextSize(Unit unit) {
            if (true && unit.isFiller()) {
                return 0;
            } else {
                return getLeftContextSize();
            }
        }
        /**
         * Gets the maximum context size for the given unit
         * 
         * @param unit
         *                the unit of interest
         * 
         * @return the maximum right context size for the unit
         */
        private int getRightContextSize(Unit unit) {
            if (true && unit.isFiller()) {
                return 0;
            } else {
                return getRightContextSize();
            }
        }
        /**
         * Returns the size of the left context.
         * 
         * @return the size of the left context
         */
        protected int getLeftContextSize() {
            return acousticModel.getLeftContextSize();
        }
        /**
         * Returns the size of the right context.
         * 
         * @return the size of the right context
         */
        protected int getRightContextSize() {
            return acousticModel.getRightContextSize();
        }
        /**
         * Generates the next left context based upon a previous context and a
         * unit
         * 
         * @param prevLeftContext
         *                the previous left context
         * @param unit
         *                the current unit
         */
        UnitContext generateNextLeftContext(UnitContext prevLeftContext,
                Unit unit) {
            Unit[] prevUnits = prevLeftContext.getUnits();
            int maxSize = getLeftContextSize();
            int curSize = prevUnits.length;
            int actSize = Math.min(maxSize, curSize);
            Unit[] leftUnits = new Unit[actSize];
            for (int i = 0; i < leftUnits.length - 1; i++) {
                leftUnits[i] = prevUnits[i + 1];
            }
            if (leftUnits.length > 0) {
                leftUnits[leftUnits.length - 1] = unit;
            }
            return UnitContext.get(leftUnits);
        }
        /**
         * Expands the unit into a set of HMMStates. If the unit is a silence
         * unit add an optional loopback to the tail.
         * 
         * @param unit
         *                the unit to expand
         * 
         * @return the head of the hmm tree
         */
        protected SentenceHMMState expandUnit(UnitState unit) {
            SentenceHMMState tail = getHMMStates(unit);
            // if the unit is a silence unit add a loop back from the
            // tail silence unit
            if (unit.getUnit().isSilence()) {
                // add the loopback, but don't expand it // anymore
                attachState(tail, unit, logOne, logOne,
                        logSilenceInsertionProbability);
            }
            return tail;
        }
        /**
         * Given a unit state, return the set of sentence hmm states associated
         * with the unit
         * 
         * @param unitState
         *                the unit state of intereset
         * 
         * @return the hmm tree for the unit
         */
        private HMMStateState getHMMStates(UnitState unitState) {
            HMMStateState hmmTree = null;
            HMMStateState finalState = null;
            Unit unit = unitState.getUnit();
            HMMPosition position = unitState.getPosition();
            HMM hmm = acousticModel.lookupNearestHMM(unit, position, false);
            HMMState initialState = hmm.getInitialState();
            hmmTree = new HMMStateState(unitState, initialState);
            attachState(unitState, hmmTree, logOne, logOne, logOne);
            addStateToCache(hmmTree);
            finalState = expandHMMTree(unitState, hmmTree);
            return finalState;
        }
        /**
         * Expands the given hmm state tree
         * 
         * @param parent
         *                the parent of the tree
         * @param tree
         *                the tree to expand
         * 
         * @return the final state in the tree
         */
        private HMMStateState expandHMMTree(UnitState parent, HMMStateState tree) {
            HMMStateState retState = tree;
            HMMStateArc[] arcs = tree.getHMMState().getSuccessors();
            for (int i = 0; i < arcs.length; i++) {
                HMMStateState newState;
                if (arcs[i].getHMMState().isEmitting()) {
                    newState = new HMMStateState(parent, arcs[i].getHMMState());
                } else {
                    newState = new NonEmittingHMMState(parent, arcs[i]
                            .getHMMState());
                }
                SentenceHMMState existingState = getExistingState(newState);
                float logProb = arcs[i].getLogProbability();
                if (existingState != null) {
                    attachState(tree, existingState, logProb, logOne, logOne);
                } else {
                    attachState(tree, newState, logProb, logOne, logOne);
                    addStateToCache(newState);
                    retState = expandHMMTree(parent, newState);
                }
            }
            return retState;
        }
        /**
         * Connect up all of the GStates. Each state now has a table of exit
         * points. These exit points represent tail states for the node. Each
         * of these tail states is tagged with a ContextPair, that indicates
         * what the left context is (the exiting context) and the right context
         * (the entering context) for the transition. To connect up a state,
         * the connect does the following: 1) Iterate through all of the
         * grammar successors for this state 2) Get the 'entry points' for the
         * successor that match the exit points. 3) Hook them up.
         * 
         * Note that for a task with 1000 words this will involve checking on
         * the order of 35,000,000 connections and making about 2,000,000
         * connections
         *  
         */
        void connect() {
            GrammarArc[] arcs = getSuccessors();
            // T("Connecting " + node.getWord());
            for (int i = 0; i < arcs.length; i++) {
                GState gstate = getGState(arcs[i].getGrammarNode());
                if (!gstate.getNode().isEmpty()
                        && gstate.getNode().getWord().getSpelling().equals(
                                Dictionary.SENTENCE_START_SPELLING)) {
                    continue;
                }
                float probability = arcs[i].getProbability();
                // adjust the language probability by the number of
                // pronunciations. If there are 3 ways to say the
                // word, then each pronunciation gets 1/3 of the total
                // probability.
                if (spreadWordProbabilitiesAcrossPronunciations
                        && !gstate.getNode().isEmpty()) {
                    int numPronunciations = gstate.getNode().getWord()
                            .getPronunciations(null).length;
                    probability -= logMath.linearToLog(numPronunciations);
                }
                float fprob = probability;
                for (Iterator keys = exitPoints.keySet().iterator(); keys
                        .hasNext();) {
                    ContextPair contextPair = (ContextPair) keys.next();
                    List destEntryPoints = gstate.getEntryPoints(contextPair);
                    if (destEntryPoints != null) {
                        List srcExitPoints = getExitPoints(contextPair);
                        connect(srcExitPoints, destEntryPoints, fprob);
                    }
                }
            }
        }
        /**
         * connect all the states in the source list to the states in the
         * destination list
         * 
         * @param sourceList
         *                the set of source states
         * @param destList
         *                the set of destinatin states.
         */
        private void connect(List sourceList, List destList, float logLangProb) {
            for (Iterator i = sourceList.iterator(); i.hasNext();) {
                SentenceHMMState sourceState = (SentenceHMMState) i.next();
                for (Iterator j = destList.iterator(); j.hasNext();) {
                    SentenceHMMState destState = (SentenceHMMState) j.next();
                    sourceState.connect(getArc(destState, logOne, logLangProb,
                            logOne));
                    exitConnections++;
                }
            }
        }
        /**
         * Attaches one SentenceHMMState as a child to another, the transition
         * has the given probability
         * 
         * @param prevState
         *                the parent state
         * @param nextState
         *                the child state
         * @param logAcousticProbability
         *                the acoustic probability of transition in the LogMath
         *                log domain
         * 
         * @param logLanguageProbablity
         *                the language probability of transition in the LogMath
         *                log domain
         * 
         * @param logInsertionProbablity
         *                insertion probability of transition in the LogMath
         *                log domain
         * 
         * 
         * @return the state that was attached
         */
        protected void attachState(SentenceHMMState prevState,
                SentenceHMMState nextState, float logAcousticProbability,
                float logLanguageProbablity, float logInsertionProbablity) {
            prevState.connect(getArc(nextState, logAcousticProbability,
                    logLanguageProbablity, logInsertionProbablity));
            if (showCompilationProgress && totalStateCounter++ % 1000 == 0) {
                System.out.print(".");
            }
        }
        /**
         * Returns all of the states maintained by this gstate
         * 
         * @return the set of all states
         */
        public Collection getStates() {
            // since pstates are not placed in the cache we have to
            // gather those states. All other states are found in the
            // existingStates cache.
            List allStates = new ArrayList();
            allStates.addAll(existingStates.values());
            for (Iterator i = entryPoints.values().iterator(); i.hasNext();) {
                allStates.addAll((List) i.next());
            }
            return allStates;
        }
        /**
         * Checks to see if a state that matches the given state already exists
         * 
         * @param state
         *                the state to check
         * 
         * @return true if a state with an identical signature already exists.
         */
        private SentenceHMMState getExistingState(SentenceHMMState state) {
            return (SentenceHMMState) existingStates.get(state.getSignature());
        }
        /**
         * Adds the given state to the cache of states
         * 
         * @param state
         *                the state to add
         */
        private void addStateToCache(SentenceHMMState state) {
            existingStates.put(state.getSignature(), state);
        }
        /**
         * Prints info about this GState
         */
        void dumpInfo() {
            System.out.println(" ==== " + this + " ========");
            System.out.print("Node: " + node);
            if (node.isEmpty()) {
                System.out.print("  (Empty)");
            } else {
                System.out.print(" " + node.getWord());
            }
            System.out.print(" ep: " + entryPoints.size());
            System.out.print(" exit: " + exitPoints.size());
            System.out.print(" cons: " + exitConnections);
            System.out.print(" tot: " + getStates().size());
            System.out.print(" sc: " + getStartingContexts().size());
            System.out.print(" rc: " + leftContexts.size());
            System.out.println(" lc: " + rightContexts.size());
            dumpDetails();
        }
        /**
         * Dumps the details for a gstate
         */
        void dumpDetails() {
            dumpCollection(" entryPoints", entryPoints.keySet());
            dumpCollection(" entryPoints states", entryPoints.values());
            dumpCollection(" exitPoints", exitPoints.keySet());
            dumpCollection(" exitPoints states", exitPoints.values());
            dumpNextNodes();
            dumpExitPoints(exitPoints.values());
            dumpCollection(" startingContexts", getStartingContexts());
            dumpCollection(" branchingInFrom", leftContexts);
            dumpCollection(" branchingOutTo", rightContexts);
            dumpCollection(" existingStates", existingStates.keySet());
        }
        /**
         * Dumps out the names of the next set of grammar nodes
         */
        private void dumpNextNodes() {
            System.out.println("     Next Grammar Nodes: ");
            GrammarArc[] arcs = node.getSuccessors();
            for (int i = 0; i < arcs.length; i++) {
                System.out.println("          " + arcs[i].getGrammarNode());
            }
        }
        /**
         * Dumps the exit points and their destination states
         * 
         * @param eps
         *                the collection of exit points
         */
        private void dumpExitPoints(Collection eps) {
            for (Iterator i = eps.iterator(); i.hasNext();) {
                List epList = (List) i.next();
                for (Iterator j = epList.iterator(); j.hasNext();) {
                    SentenceHMMState state = (SentenceHMMState) j.next();
                    System.out.println("      Arcs from: " + state);
                    SearchStateArc[] arcs = state.getSuccessors();
                    for (int k = 0; k < arcs.length; k++) {
                        System.out.println("          " + arcs[k].getState());
                    }
                }
            }
        }
        /**
         * Dumps the given collection
         * 
         * @param name
         *                the name of the collection
         * @param collection
         *                the collection to dump
         */
        private void dumpCollection(String name, Collection collection) {
            System.out.println("     " + name);
            for (Iterator i = collection.iterator(); i.hasNext();) {
                System.out.println("         " + i.next().toString());
            }
        }
        /**
         * Dumps the given map
         * 
         * @param name
         *                the name of the map
         * @param map
         *                the map to dump
         */
        private void dumpMap(String name, Map map) {
            System.out.println("     " + name);
            for (Iterator i = map.keySet().iterator(); i.hasNext();) {
                Object key = i.next();
                Object value = map.get(key);
                System.out.println("         key:" + key + "  val: " + value);
            }
        }
        /**
         * Returns the string representation of the object
         * 
         * @return the string representation of the object
         */
        public String toString() {
            if (node.isEmpty()) {
                return "GState " + node + "(empty)";
            } else {
                return "GState " + node + " word " + node.getWord();
            }
        }
    }
    /**
     * Quick and dirty tracing. Traces the string if 'tracing' is true
     * 
     * @param s
     *                the string to trace.
     */
    private void T(String s) {
        if (tracing) {
            System.out.println(s);
        }
    }
}
/**
 * A class that represents a set of units used as a context
 */
class UnitContext {
    private static Map unitContextMap = new HashMap();
    private Unit[] context;
    private int hashCode = 12;
    private static int foldedCount = 0;
    public final static UnitContext EMPTY = new UnitContext(new Unit[0]);
    public final static UnitContext SILENCE;
    static {
        Unit[] silenceUnit = new Unit[1];
        silenceUnit[0] = UnitManager.SILENCE;
        SILENCE = new UnitContext(silenceUnit);
        unitContextMap.put(EMPTY, EMPTY);
        unitContextMap.put(SILENCE, SILENCE);
    }
    /**
     * Creates a UnitContext for the given context. This constructor is not
     * directly accessible, use the factory method instead.
     * 
     * @param context
     *                the context to wrap with this UnitContext
     */
    private UnitContext(Unit[] context) {
        this.context = context;
        hashCode = 12;
        for (int i = 0; i < context.length; i++) {
            hashCode += context[i].getName().hashCode() * ((i + 1) * 34);
        }
    }
    /**
     * Gets the unit context for the given units. There is a single unit
     * context for each unit combination.
     * 
     * @param units
     *                the units of interest
     * 
     * @return the unit context.
     */
    static UnitContext get(Unit[] units) {
        UnitContext newUC = new UnitContext(units);
        UnitContext oldUC = (UnitContext) unitContextMap.get(newUC);
        if (oldUC == null) {
            unitContextMap.put(newUC, newUC);
        } else {
            foldedCount++;
            newUC = oldUC;
        }
        return newUC;
    }
    /**
     * Retrieves the units for this context
     * 
     * @return the units associated with this context
     */
    public Unit[] getUnits() {
        return context;
    }
    /**
     * Determines if the given object is equal to this UnitContext
     * 
     * @param o
     *                the object to compare to
     * 
     * @return <code>true</code> if the objects are equal
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof UnitContext) {
            UnitContext other = (UnitContext) o;
            if (this.context.length != other.context.length) {
                return false;
            } else {
                for (int i = 0; i < this.context.length; i++) {
                    if (this.context[i] != other.context[i]) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            return false;
        }
    }
    /**
     * Returns a hashcode for this object
     * 
     * @return the hashCode
     */
    public int hashCode() {
        return hashCode;
    }
    /**
     * Dumps information about the total number of UnitContext objects
     */
    public static void dumpInfo() {
        System.out.println("Total number of UnitContexts : "
                + unitContextMap.size() + " folded: " + foldedCount);
    }
    /**
     * Returns a string representation of this object
     * 
     * @return a string representation
     */
    public String toString() {
        return LeftRightContext.getContextName(context);
    }
}
/**
 * A context pair hold a left and starting context. It is used as a hash into
 * the set of starting points for a particular gstate
 */
class ContextPair {
    static Map contextPairMap = new HashMap();
    private UnitContext left;
    private UnitContext right;
    private int hashCode;
    /**
     * Creates a UnitContext for the given context. This constructor is not
     * directly accessible, use the factory method instead.
     * 
     * @param left
     *                the left context
     * @param right
     *                the right context
     */
    private ContextPair(UnitContext left, UnitContext right) {
        this.left = left;
        this.right = right;
        hashCode = 99 + left.hashCode() * 113 + right.hashCode();
    }
    /**
     * Gets the ContextPair for the given set of contexts. This is a factory
     * method. If the ContextPair already exists, return that one, othewise,
     * create it and store it so it can be reused.
     * 
     * @param left
     *                the left context
     * @param right
     *                the right context
     * 
     * @return the unit context.
     */
    static ContextPair get(UnitContext left, UnitContext right) {
        ContextPair newCP = new ContextPair(left, right);
        ContextPair oldCP = (ContextPair) contextPairMap.get(newCP);
        if (oldCP == null) {
            contextPairMap.put(newCP, newCP);
        } else {
            newCP = oldCP;
        }
        return newCP;
    }
    /**
     * Determines if the given object is equal to this UnitContext
     * 
     * @param o
     *                the object to compare to
     * 
     * @return <code>true</code> if the objects are equal return;
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof ContextPair) {
            ContextPair other = (ContextPair) o;
            return this.left.equals(other.left)
                    && this.right.equals(other.right);
        } else {
            return false;
        }
    }
    /**
     * Returns a hashcode for this object
     * 
     * @return the hashCode
     */
    public int hashCode() {
        return hashCode;
    }
    /**
     * Returns a string representation of the object
     */
    public String toString() {
        return "CP left: " + left + " right: " + right;
    }
    /**
     * Gets the left unit context
     * 
     * @return the left unit context
     */
    public UnitContext getLeftContext() {
        return left;
    }
    /**
     * Gets the right unit context
     * 
     * @return the right unit context
     */
    public UnitContext getRightContext() {
        return right;
    }
}
