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

package edu.cmu.sphinx.decoder.linguist.simple;

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
import edu.cmu.sphinx.knowledge.dictionary.Pronunciation;
import edu.cmu.sphinx.decoder.linguist.GrammarNode;
import edu.cmu.sphinx.decoder.linguist.GrammarArc;
import edu.cmu.sphinx.decoder.linguist.GrammarWord;
import edu.cmu.sphinx.decoder.linguist.SearchState;
import edu.cmu.sphinx.decoder.linguist.SearchStateArc;
import edu.cmu.sphinx.decoder.linguist.Grammar;
import edu.cmu.sphinx.decoder.linguist.Linguist;
import edu.cmu.sphinx.decoder.linguist.Color;


import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;

/**
 * Provides an implementation of a Linguist interface that maintains a
 * tree  or flat representation of the SentenceHMM. The StaticLinguist is
 * responible for taking the associated grammar (set by
 * <code>setGrammar</code>) and compiling it into a SentenceHMM which
 * can be used by a search algorithm during decoding.  
 *
 * The StaticLinguist will take the Grammar and decompose it into a
 * graph of SentenceHMM states. The set of SentenceHMM states is
 * commonly called the <i>SentenceHMM</i>.  
 *
 * Note that all probabilities are maintained in the LogMath log
 * domain
 */
public class StaticLinguist implements  Linguist {
    private static Logger logger = 
            Logger.getLogger("edu.cmu.sphinx.decoder.linguist.StaticLinguist");
    private final static boolean singlePass = false;
    public final static String PROP_IS_FLAT_SENTENCE_HMM = 
        "edu.cmu.sphinx.decoder.linguist.simple.StaticLinguist.isFlatSentenceHMM";

    private SphinxProperties props;

    private Grammar grammar;
    private SentenceHMMState initialSentenceHMMState;
    private LanguageModel languageModel;
    private AcousticModel acousticModel;
    private LogMath logMath;


    private double logWordInsertionProbability;
    private double logSilenceInsertionProbability;
    private double logUnitInsertionProbability;

    private int compositeThreshold;
    private boolean showSentenceHMM;
    private boolean validateSentenceHMM;
    private boolean joinPronunciations;
    private boolean fullLeftContexts = true;
    private boolean showCompilationProgress = true;
    private boolean autoLoopSilences = true;

    private StatisticsVariable totalUniqueStates;
    private StatisticsVariable totalAttachedStates;
    private StatisticsVariable averageBranches;

    private List grammarJob = new ArrayList();

    private boolean isExpandingInterNodeContexts;
    private Unit[] leftEmptyContext;
    private Unit[] rightEmptyContext;
    private Map existingStates = new HashMap();
    private boolean flatSentenceHMM = true;

    private transient int nBranches = 0;
    private transient int unitCount = 0;
    private transient int totalStateCounter = 0;
    private transient Collection stateSet = null;
    private transient int compositeCount = 0;
    private double logOne;


    /**
     * Creates a tree linguist associated with the given context
     *
     * @param context the context to associate this linguist with
     * @param languageModel the language model
     * @param grammar the grammar for this linguist
     * @param model the acoustic model used by this StaticLinguist;
     *    there should only be one acoustic model, if there is less than
     *    or more than one, an IllegalArgumentException will be thrown
     */
    public void initialize(String context, 
                        LanguageModel languageModel,
                        Grammar grammar, AcousticModel[] model) {
        this.props = SphinxProperties.getSphinxProperties(context);
        this.grammar = grammar;
        this.logMath = LogMath.getLogMath(context);
        this.languageModel = languageModel;

        logOne = logMath.getLogOne();
	
	// check the number of acoustic models
	if (model.length != 1) {
	    throw new IllegalArgumentException
		("Number of acoustic models is not one!");
	} else {
	    this.acousticModel = model[0];
	}

        logWordInsertionProbability = logMath.linearToLog(
            props.getDouble(Linguist.PROP_WORD_INSERTION_PROBABILITY, 1.0));

        logSilenceInsertionProbability = logMath.linearToLog(
            props.getDouble(Linguist.PROP_SILENCE_INSERTION_PROBABILITY, 1.0));

        logUnitInsertionProbability =  logMath.linearToLog(
            props.getDouble(Linguist.PROP_UNIT_INSERTION_PROBABILITY, 1.0));

        compositeThreshold = 
            props.getInt(Linguist.PROP_COMPOSITE_THRESHOLD, 1000);
        showSentenceHMM = 
            props.getBoolean(Linguist.PROP_SHOW_SEARCH_SPACE, false);

        validateSentenceHMM = 
            props.getBoolean(Linguist.PROP_VALIDATE_SEARCH_SPACE, false);

        joinPronunciations = 
            props.getBoolean(Linguist.PROP_JOIN_PRONUNCIATIONS, false);

        autoLoopSilences = 
            props.getBoolean(Linguist.PROP_AUTO_LOOP_SILENCES, false);

        flatSentenceHMM = props.getBoolean(PROP_IS_FLAT_SENTENCE_HMM, false);

        isExpandingInterNodeContexts =
            props.getBoolean(Linguist.PROP_EXPAND_INTER_NODE_CONTEXTS, false);

        showCompilationProgress =
            props.getBoolean(Linguist.PROP_SHOW_COMPILATION_PROGRESS, false);

        totalUniqueStates = StatisticsVariable.getStatisticsVariable(
                props.getContext(), "totalUniqueStatesSentenceHMM");
        totalAttachedStates = StatisticsVariable.getStatisticsVariable(
                props.getContext(), "totalAttachedStatesSentenceHMM");
        averageBranches = StatisticsVariable.getStatisticsVariable(
                props.getContext(), "averageBranches");
        GrammarPoint.setBounded(!isExpandingInterNodeContexts);
	logger.info("Compiling grammar");
        compileGrammar();
        totalUniqueStates.value = existingStates.size();
        averageBranches.value = getPerplexity();


        // after we have compiled the grammar, we no longer need
        // these things, so release them so that resources can be
        // reclaimed.

        existingStates = null;
        acousticModel = null;
        
    }

    /**
     * 
     * Called before a recognition
     */
    public void start() {
        if (languageModel != null) {
            languageModel.start();
        }
    }

    /**
     * Called after a recognition
     */
    public void stop() {
        if (languageModel != null) {
            languageModel.stop();
        }
    }

    /**
     * Retrieves the language model for this linguist
     *
     * @return the language model (or null if there is none)
     */
    public LanguageModel getLanguageModel() {
        return languageModel;
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
     * Retrieves initial search state
     * 
     * @return the set of initial search state
     */
    public SearchState getInitialSearchState() {
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
    protected void compileGrammar() {
        Timer compileTimer = 
            Timer.getTimer(props.getContext(), "compileGrammar");

        compileTimer.start();

	logger.info("Creating initial job");
        createInitialJob();

        while (isJob()) {
            GrammarJob gjob = getJob();

            List newTails = expandGrammarJob(gjob);
            if (newTails != null && newTails.size() > 0) {
                GrammarArc[] successors = gjob.getNode().getSuccessors();
                for (int i = 0; i < successors.length; i++) {
                    GrammarArc arc = successors[i];
                    addJob(newTails, arc.getGrammarNode(), 
                            arc.getProbability());
                }
            }
        }

	logger.info("Grammar job done");

        dumpState("Tree Grammar", initialSentenceHMMState);
        if (showSentenceHMM) {
            initialSentenceHMMState.dumpAll();
        }

        stateSet = existingStates.values();

        compileTimer.stop();

        if (validateSentenceHMM) {
            initialSentenceHMMState.validateAll();
        }

        if (true) {
            System.out.println("Composite count: " + compositeCount);
        }
        // dumpTreeStats("tree grammar stats", initialSentenceHMMState);
    }



    /**
     * Creates the initial grammar job for the initial grammar node
     */
    private void createInitialJob() {
        List tails  = new ArrayList();
        GrammarNode gnode = grammar.getInitialNode();
        ContextBucket emptyContext = new
            ContextBucket(acousticModel.getLeftContextSize());
        initialSentenceHMMState = new GrammarState(gnode);

        // Make sure that the first grammar node does not have any
        // words


        // assert gnode.getAlternatives().length == 0;

        tails.add(new StatePath(initialSentenceHMMState, emptyContext));

        for (int i = 0; i < gnode.getSuccessors().length; i++) {
            GrammarArc arc = gnode.getSuccessors()[i];
            addJob(tails, arc.getGrammarNode(), arc.getProbability());
        }
    }


    /**
     * Expands a grammar job by creating a SentenceHMMState for the
     * grammar node  expanding it fully into a set of hmm states and
     * connecting it up to each of the tails specified in the grammar
     * job.
     *
     * @param job the job to expand
     *
     * @return a list of leaf states 
     */
    // A grammar job consists of a grammar node and a list of tails.
    // This method appends attaches the grammar state associated with
    // the grammar node to each of the tails.
    private List expandGrammarJob(GrammarJob job) {
        List tails = new ArrayList();
        for (Iterator i = job.getTails().iterator(); i.hasNext(); ) {
            StatePath tail = (StatePath) i.next();
            GrammarState gstate = getGrammarState(job.getNode());
            List newTails = expandGrammar(tail, gstate);
            tails.addAll(newTails);
            attachState(tail.getState(), 
                    gstate, logOne, job.getProbability(), logOne);
        }
        return tails;
    }


    /**
     * Gets the grammar state associated with the given grammar node
     *
     * @param node the grammar ndoe
     *
     * @return the grammar state associated with the ndoe
     */
    private GrammarState getGrammarState(GrammarNode node) {
        GrammarState grammarState = new GrammarState(node);
        grammarState.setFanIn(true);
        GrammarState gstate = (GrammarState) getExistingState(grammarState);
        if (gstate == null) {
           gstate = grammarState;
           addStateToCache(gstate);
        }
        return gstate;
    }


    /**
     * Expands a grammar state
     *
     * @param grammarState the top of the tree to be expanded
     *
     * @return the set of tails
     */
    private List expandGrammar(StatePath tail, GrammarState grammarState) {
        List nextTails = new ArrayList();
        timerStart("expandGrammar");
        GrammarWord[][] alternatives =
            grammarState.getGrammarNode().getAlternatives();

        // System.out.println("Attaching " + grammarState + " to " + tail);

        if (alternatives.length == 0) {
            nextTails.add(new StatePath(grammarState, tail.getContext()));
        } else for (int i = 0; i < alternatives.length; i++) {
            AlternativeState alternativeState =
                    new AlternativeState(grammarState, i);
            alternativeState.setFanIn(true);
            List altTails = expandAlternative(tail, alternativeState);

            // iterate through all the return paths and add the
            // alternate state to the path

            for (Iterator iter = altTails.iterator(); iter.hasNext(); ) {
                StatePath atail = (StatePath) iter.next();
                SentenceHMMState state = atail.getState();
		// "one - logValue" (i.e., the inverse of a number in
		// log scale) is the equivalent of "-logValue".
                SentenceHMMState newState = attachState(state,
                   alternativeState, logOne, logOne
                       - logMath.linearToLog(alternatives.length),
                         logOne);

                StatePath newPath = new StatePath(newState, atail.getContext());
                nextTails.add(newPath);
            }
        }
        timerStop("expandGrammar");

        // System.out.println("expandGrammar " + nextTails.size());
        return nextTails;
    }


    /**
     * Expands the given alternative state into the set of associated
     * words
     *
     * @param previous the place to expand the nodes
     * @param alternativeState the state to be expanded
     *
     * @return the list of tail states after the expansion
     */
    private List expandAlternative(StatePath tail,
            AlternativeState alternativeState) {
        GrammarWord alternative[] = alternativeState.getAlternative();
        List nextTails = new ArrayList();
        nextTails.add(tail);

        for (int i = 0; i < alternative.length; i++) {
            List lastTails = nextTails;
            nextTails = new ArrayList();
            for (Iterator iter = lastTails.iterator();iter.hasNext();) {
                StatePath nextTail = (StatePath) iter.next();
                WordState  wordState = new WordState(alternativeState, i);
                wordState.setFanIn(true);
                nextTails.addAll(expandWord(nextTail, wordState));
            }
        }
        // System.out.println("expandAlternative " + nextTails.size());
        return nextTails;
    }


    /**
     * Expands the given wordstate, the wordstate is attached to the
     * given tail. 
     *
     * @param tail the new state is attached here
     * @param wordState the state to be expanded
     *
     * @return a list of tails
     */
    private List expandWord(StatePath tail, WordState wordState) {
        GrammarWord word = wordState.getWord();
        Pronunciation[] pronunciations = word.getPronunciations();
        List nextTails = new ArrayList();

        for (int j = 0; j < pronunciations.length; j++) {
            PronunciationState pronunciationState =
                new PronunciationState(wordState, j);
            pronunciationState.setFanIn(true);
            attachState(pronunciationState, wordState, logOne, logOne,
                    logOne);
            List pTails = expandPronunciation(tail, pronunciationState);

            for (Iterator pIter= pTails.iterator(); pIter.hasNext(); ) {
                StatePath path = (StatePath) pIter.next();
                SentenceHMMState pTail = path.getState();

                attachState(pTail, pronunciationState, logOne, 
                    logOne -
                    logMath.linearToLog(pronunciations.length), logOne);
                nextTails.add(new StatePath(wordState, path.getContext()));
            }
        }

        // System.out.println("expandWord " + nextTails.size());
        return nextTails;
    }

    /**
     * Expand the given pronunciation 
     *
     * @param previous the state to attach this all too
     * @param pronunciationState the state to expand.
     *
     * @return the set of tails for this tree
     */
    private List expandPronunciation(StatePath tail,
                PronunciationState pronunciationState) {
        List nextTails = new ArrayList();
        Pronunciation pronunciation = pronunciationState.getPronunciation();
        Unit[] units = pronunciation.getUnits();
        nextTails.add(tail);

        for (int i = 0; i < units.length; i++) {
            List tails = nextTails;
            nextTails = new ArrayList();
            for (Iterator iter = tails.iterator(); iter.hasNext(); ) {
                StatePath path = (StatePath) iter.next();
                nextTails.addAll(expandUnit(path, pronunciationState, i));
            }
            nextTails = advanceContext(units[i], nextTails);
        }
        return nextTails;
    }


    /**
     * Given a list of tails and units, advance the context in each
     * tail to include the given unit
     *
     * @param unit the new unit to be added to the context
     * @param tails the set of paths
     *
     * @return a new list of tails
     */
    private List advanceContext(Unit unit, List tails) {
        List newTails = new ArrayList();

        for (Iterator i = tails.iterator(); i.hasNext(); ) {
            StatePath path = (StatePath) i.next();
            newTails.add(new StatePath(path.getState(),
                    new ContextBucket(path.getContext(), unit)));
        }
        return newTails;
    }

    /**
     * Expand the given unit of the given pronunciation
     *
     * @param previous expanded states are attached here
     * @param parent parent of this unit
     * @param which which unit to expand
     *
     * @return the list of tail units 
     */
    private List expandUnit(StatePath previous,
            PronunciationState parent, int which) {
        List nextTails = new ArrayList();

        Collection rcList = getNextRightContexts(parent, which);
        Pronunciation pronunciation = parent.getPronunciation();
        Unit[] units = pronunciation.getUnits();
        Unit[] lc = previous.getContext().getUnits();
        double logInsertionProbability;

        // if this is the first unit of the pronunciation, then its
        // the beginning of a new word so use the WIP, otherwise,
        // if a filler use the SIP, otherwise just the UIP

        if (units[which].isFiller()) {
            logInsertionProbability = logSilenceInsertionProbability;
        } else if (which == 0) {
            logInsertionProbability = logWordInsertionProbability;
        } else {
            logInsertionProbability = logUnitInsertionProbability;
        }

        for (Iterator i = rcList.iterator(); i.hasNext(); ) {
            StatePath path;
            Unit[] rc = (Unit[]) i.next();
            LeftRightContext context = LeftRightContext.get(lc, rc);
            Unit cdUnit = new Unit(units[which].getName(),
                        units[which].isFiller(), context);

    // When expanding a unit, there are 3 different ways in which the
    // new unit can be attached to the tree:
    //     1)   if a unit with the same signature already exists, then
    //          we transition to that sentence state and expand this
    //          branch no further.
    //     2)   if an identical (but possibly with a different right
    //          context) unit is already attached to the parent, we'll
    //          continue to expand this branch at the tail of the
    //          identical unit
    //     3)   If neither (1) or (2) hold true then we add the unit
    //          in the normal fashion and expand it
        
            UnitState cdUnitState = new UnitState(parent, which, cdUnit);
            SentenceHMMState existingState = getExistingState(cdUnitState);
            if (existingState != null) {  // case 1
                attachExistingState(previous.getState(), 
                  existingState, logOne, logOne, logInsertionProbability);
            } else {
                UnitState attachedCDState = (UnitState)
                      attachState(previous.getState(), cdUnitState,
                              logOne, logOne, logInsertionProbability);
                if (attachedCDState != cdUnitState) {    // case 2
                    path = attachedCDState.getTail();
                    addExistingStateToCache(cdUnitState, attachedCDState);
                    assert path != null;
                } else {                        // case 3
                    SentenceHMMState state
                        = expandContextDependentUnit(attachedCDState);
                    path = new StatePath(state, previous.getContext());
                    attachedCDState.setTail(path);
                }
                nextTails.add(path);
            }
        }
        // System.out.println("expanding " + units[which] + " into " +
                // nextTails.size() + " tails");
        return nextTails;
    }


    /**
     * Expand a cd unit state with its HMMs
     *
     * @param state the unit state to expand
     */
    private SentenceHMMState expandContextDependentUnit(UnitState state) {
        HMMTree hmmTree = getHMMStates(state);
        attachState(state, hmmTree.head, logOne, logOne, logOne);
        return hmmTree.tail;
    }


    /**
     * Given a unit state, return the set of sentence hmm states
     * associated with the unit
     *
     * @param unitState the unit state of intereset
     *
     * @return the hmm tree for the unit
     */
    private HMMTree getHMMStates(UnitState unitState) {
        HMMStateState hmmTree = null;
        HMMStateState finalState = null;

        Unit unit = unitState.getUnit();
        int which = unitState.getWhich();
        HMMPosition position = unitState.getPosition();

        HMM hmm = acousticModel.lookupNearestHMM(unit, position, false);
        HMMState initialState = hmm.getInitialState();

        hmmTree = new HMMStateState(unitState, initialState);

        finalState = expandHMMTree(unitState, hmmTree);

        // Expanding a silence node is a special case. Whenever we
        // have the possibility of an inserted silence, we need to
        // allow for any length of silence, so we allow the sentence
        // hmm tree associated with the silence unit to loop back on
        // its self so multiple silences can be inserted.

        if (autoLoopSilences && unitState.getUnit().isSilence()) {
            totalAttachedStates.value++;
            finalState.connect(
                new SentenceHMMStateArc(unitState, (float) logOne,
                    (float) logOne, (float) logSilenceInsertionProbability));
        }
        return new HMMTree(hmmTree, finalState);
    }


    /**
     * Expands the given hmm state tree
     * 
     * @param parent the parent of the tree
     * @param tree the tree to expand
     *
     * @return the final state in the tree
     */
    private HMMStateState expandHMMTree(UnitState parent, HMMStateState tree) {
        HMMStateState retState = tree;
        HMMStateArc[] arcs = tree.getHMMState().getSuccessors();
        for (int i = 0; i < arcs.length; i++) {
            HMMStateState newState = new HMMStateState(
                    parent, arcs[i].getHMMState());
            /*
            if (newState.isEmitting()) {
                newState.setColor(Color.GREEN);
            }
            */
            // we want to color all hmmstates green, not just the
            // emitting ones now.
            newState.setColor(Color.GREEN);
            SentenceHMMState attachedState = attachState(tree, newState,
                    (arcs[i].getLogProbability()), logOne, logOne);
            if (attachedState == newState) {
                retState = expandHMMTree(parent, (HMMStateState) attachedState);
            } else {
            }
        }
        return retState;
    }


    /**
     * Attaches one SentenceHMMState as a child to another, the
     * transition has the given probability
     *
     * @param parent the parent state
     * @param nextState the child state
     * @param logAcousticProbability acoustic probability of 
     * transition occuring (in LogMath log domain)
     * @param logLanguageProbablity the language probability of 
     * transition occuring (in LogMath log domain)
     * @param logInsertionProbablity insertion probability of 
     * transition occuring (in LogMath log domain)
     *
     * @return the state that was attached
     */
    private SentenceHMMState attachState(SentenceHMMState parent,
            SentenceHMMState nextState, double logAcousticProbability,
            double logLanguageProbablity, double logInsertionProbablity) {

        // check to see if the parent already has this node, if so,
        // then reuse it

        SentenceHMMState attachedState = findAttachedState(parent, nextState);

        if (attachedState == null) {

            // in the tree form of the sentence hmms, the only
            // possible loops within a particular grammar node are at
            // the HMM State level.  Here we do an explicit check to
            // see if we are looping to ourself, and if so we make sure
            // that we do not have any self reference on the previous
            // pointer.

            SentenceHMMState existingState = getExistingState(nextState);

            if (existingState != null) {
                nextState = existingState;
            } else {
                addStateToCache(nextState);
            }

            totalAttachedStates.value++;
            parent.connect(new SentenceHMMStateArc(nextState,
                (float) (logAcousticProbability), 
                (float) (logLanguageProbablity), 
                (float) (logInsertionProbablity)));
        } else { 
            nextState = attachedState; 
        } 

        if (showCompilationProgress && totalStateCounter++ % 1000 == 0) {
            System.out.print(".");
        }
        return nextState; 
   }

    /**
     * Attach an existing state
     *
     * @param parent the parent state
     * @param nextState the existing child state
     * @param logAcousticProbability acoustic probability of 
     * transition occuring (in logMath log domain)
     * @param logLanguageProbablity the language probability of  
     * transition occuring (in logMath log domain)
     * @param logInsertionProbablity insertion probability of 
     * transition occuring (in logMath log domain)
     *
     */
    private void  attachExistingState(SentenceHMMState parent,
            SentenceHMMState nextState, double logAcousticProbability,
            double logLanguageProbablity, double logInsertionProbablity) {

        totalAttachedStates.value++;
        parent.connect(new SentenceHMMStateArc(nextState,
            (float) (logAcousticProbability), (float) (logLanguageProbablity), 
            (float) (logInsertionProbablity)));

        if (showCompilationProgress && totalStateCounter++ % 1000 == 0) {
            System.out.print(".");
        }
   }

    /**
     * Determines if the two given states form a cycle
     *
     * @param state the new state being connected
     * @param parent the new state is connected to this parent state
     *
     * @return true if the two states form a cycle
     */
    private boolean isLoop(SentenceHMMState state, SentenceHMMState parent) {
        boolean loop = false;
        if (state instanceof HMMStateState) {
            HMMStateState hmmStateState = (HMMStateState) state;
            if (parent instanceof HMMStateState) {
                HMMStateState pState = (HMMStateState) parent;
                loop = hmmStateState.getHMMState().equals(pState.getHMMState());
            }
        }
        return loop;
    }


    /**
     * See if the given state is already attached to the
     * parent state, if so returned the parent, otherwise return null
     *
     * @param parent the parent state
     * @param newState the new state to attach
     *
     * @return  the attached state or null if no attached matching
     * state could be found
     */
    private SentenceHMMState findAttachedState(
            SentenceHMMState parent, SentenceHMMState newState) {

        SentenceHMMState attachedState = null;
        SentenceHMMStateArc arc;

        if (flatSentenceHMM) {
            return null;
        }
        assert parent != null;

        if (! (newState instanceof UnitState)) {
            return null;
        }

        arc = parent.findArc(newState);
        if (arc != null) {
            attachedState = arc.getNextState();
        }
        return attachedState;
    }


    /**
     * Visit all of the states in the SentenceHMM and determine the
     * average number of branches per state. This should only be
     * called after the SentenceHMM is built. This method relies on
     * the existingState cache to contain all of the nodes. If the
     * caching strategy is changed, this method would likely have to
     * change as well.
     *
     * @return the perplexity (as an average number of branches per
     * state)
     */
    private float getPerplexity() {
        float branchCount = 0;

        if (existingStates.size() == 0) {
            return 0.0f;
        } else {
            for (Iterator i = existingStates.values().iterator();i.hasNext();) {
                SentenceHMMState state = (SentenceHMMState) i.next();
                branchCount += state.getNumSuccessors();
            }
            return branchCount / existingStates.size();
        }
    }

    /**
     * Retreives the next right context for the given state
     *
     * @param state the pronunciation state to start generating 
     *  the left context for
     * @param unit the index of the unit of interest
     *
     * @return a list containing the next right contexts
     */
    private Collection getNextRightContexts(PronunciationState state,int unit) {
        int size = acousticModel.getRightContextSize();
        GrammarPoint gp = new GrammarPoint(state, unit);
        Collection rc = gp.getRightContexts(size, false, compositeThreshold);

        if (rc.size() >= compositeThreshold) {
            compositeCount++;
            rc = new ArrayList(1);
            rc.add(null);
        }
        return rc;
    }


    /**
     * Checks to see if this state is already in the tree. If it is,
     * the existing state is returned, otherwise null is returned
     *
     * @param state the state to check
     *
     * @return the existing state or null if the state does not
     * already exists
     */
    private SentenceHMMState getExistingState(SentenceHMMState state) {
        return (SentenceHMMState) existingStates.get(state.getSignature());
    }

    /**
     * Adds the state to the set of existingStates
     *
     * @param state the state to add
     */
    private void addStateToCache(SentenceHMMState state) {
        existingStates.put(state.getSignature(), state);
    }

    /**
     * Adds the state to the set of existingStates.  This is used when
     * constructing a tree and one node is representing several nodes
     *
     *
     * @param state the state to add
     * @param sharedState the actual state used to represent state
     */
    private void addExistingStateToCache(SentenceHMMState state,
            SentenceHMMState sharedState) {
        existingStates.put(state.getSignature(), sharedState);
    }


   /**
    * Adds a new gramar job to the grammar job queue. The grammar job
    * queue is a queue of grammar jobs. A grammar job consists of a
    * a grammar node and the set of sentence hmms in which the new
    * GrammarState should be attached.  The gramar job queue is used
    * to allow us to expand grammar nodes in a non-recursive fashion.
    * This avoids stack-overflows when compiling large and deep trees.
    *
    * @param tails the list of SentenceHMMStates that the new grammar
    * state should be appended to
    *
    * @param node the next grammar node. A GrammarState will be
    * created for this node and appended to each state in the list of
    * tails.
    *
    * @param probability the probability of the transition to the new
    * GrammarState
    */
    private void addJob(List tails, GrammarNode node, double probability) {
        grammarJob.add(new GrammarJob(tails, node, probability));
    }

    
    /**
     * Gets the next grammar job off of the queue. Use
     * <code>isJob</code> to see if there are jobs available first.
     *
     * @return the next grammar job
     */
    private GrammarJob getJob() {
        return (GrammarJob) grammarJob.remove(0);
    }

    /**
     * Determines if there is a job waiting on the grammar job queue
     *
     * @return <code> true </code> if there is at least one job on the
     * grammar job queue.
     */
    private boolean isJob() {
        return grammarJob.size() > 0;
    }

    /**
     * Dumps a SentenceHMMState
     *
     * @param msg title message to display with the dump
     * @param state the staet to dump
     */
    private void dumpState(String msg, SentenceHMMState state) {
        dumpState(msg, state, false);
    }

    /**
     * Dumps a SentenceHMMState
     *
     * @param msg title message to display with the dump
     * @param state the staet to dump
     * @param force force the dump regardless of * showSentenceHMM setting
     */
    private void dumpState(String msg, SentenceHMMState state, boolean force) {
        if (force || showSentenceHMM) {
            StringBuffer sb = new StringBuffer();
            System.out.println(msg);
            state.resetAllProcessed();
            dumpState(sb, state, force);
        }
    }


    /**
     * Provides a 'pretty' recursive dump of the given state
     *
     * @param sb a string buffer containg the current line
     * @param state the state to dump
     * @param force force the dump regardless of * showSentenceHMM setting
     *
     */
    private void dumpState(StringBuffer sb, SentenceHMMState state,
            boolean force) {
        int curSize;

        StringBuffer padBuffer = sb;

        if (state.isProcessed()) {
            sb.append("+");
        } else {
            sb.append("-");
        }

        sb.append(state.getPrettyName());

        if (state.isProcessed()) {
            System.out.println(sb.toString());
            return;
        } else {
            state.setProcessed(true);
        }

        curSize = sb.length();

        if (state.getNumSuccessors() == 0) {
            System.out.println(sb);
        } else {
            SearchStateArc[] arcs = state.getSuccessors();
            for (int i = 0; i< arcs.length; i++) {
                dumpState(padBuffer, (SentenceHMMState) 
                        arcs[i].getState(), force);
                padBuffer = getPadBuffer(curSize);
            }
        }
    }

    /**
     * Supplies a string buffer, padded to the given size with whitespace
     *
     * @param size the desired size
     *
     * @return the padded StringBuffer
     */
    private static StringBuffer getPadBuffer(int size) {
        StringBuffer result = new StringBuffer(size);
        for (int i = 0; i < size; i++) {
            result.append(' ');
        }
        return result;
    }


    /**
     * Starts the timer with the given name
     *
     * @param name the name of the timer
     */
    private void timerStart(String name) {
        Timer.getTimer(props.getContext(), name).start();
    }

    /**
     * Stops the timer with the given name
     *
     * @param name the name of the timer
     */
    private void timerStop(String name) {
        Timer.getTimer(props.getContext(), name).stop();
    }

    private void t(String msg) {
        System.out.println(msg);
    }

    /**
     * Traverse through the tree and dump out some stats about it
     *
     * @param tre the top of the tree to be expanded
     */
    private void dumpTreeStats(String msg, SentenceHMMState tree) {
        StatCollector sc = new StatCollector();
        SentenceHMMState.visitStates(sc, tree, false);
        sc.dumpStats(msg);
    }


static long startTime = System.currentTimeMillis();

     /**
      * A testing method for invoking the GC with a timestamp
      * and reporting claimed and available memory
      *
      * @param msg a title message
      */
     private  void gc(String msg) {
         Runtime rt = Runtime.getRuntime();
         long free = rt.freeMemory();
         System.out.println(" --- GC " + msg + " ---");
         System.out.println("Time     : " + 
                 ((System.currentTimeMillis() - startTime)/1000.0));
         rt.gc();
         long reclaimed = rt.freeMemory() - free;
         System.out.println("Free     : " + 
                 (rt.freeMemory() / (1024. * 1024)) + "mb" );
         System.out.println("Reclaimed: " + 
                 (reclaimed / (1024. * 1024)) + "mb" );
         System.out.println("Total    : " + 
                 (rt.totalMemory() / (1024. * 1024)) + "mb" );
     }
}



/**
 * This class is used to define the head and tail of an
 * HMM subtree. These subtrees have a single head and tail, and we
 * often need to find the tail, so instead of chasing through the tree
 * to find the tail, we can use this class to keep track of it.
 */
class HMMTree {
    HMMStateState head;
    HMMStateState tail;

    public HMMTree(HMMStateState head, HMMStateState tail) {
        this.head = head;
        this.tail = tail;
    }
}


/**
 * A SentenceHMMStateVisitor that collects tree stats
 */
class StatCollector implements SentenceHMMStateVisitor {
    private int arcs = 0;
    private int maxArcs = 0;
    private int states = 0;

    /**
     * Visits the states in the tree and collects stats
     */
     public boolean visit(SentenceHMMState state) {
         states++;
         int numArcs = state.getNumSuccessors();
         arcs += numArcs;
         if (numArcs > maxArcs) {
             maxArcs = numArcs;
         }
         return false;
     }


     /**
      * Dumps the collected states
      * 
      * @param msg the title message for the dump
      */
     public void dumpStats(String msg) {
        System.out.println(msg + " Tree Stats");
        System.out.println("Num states    : " + states);
        System.out.println("Num arcs      : " + arcs);
        System.out.println("Max arcs      : " + maxArcs);
     }

}

/**
 * A GramamrJob is used to represent an expansion of a grammar node.
 * When expanding a grammar node, a new GrammarState associated with
 * the node needs to be attached to a set of SentenceHMMStates with a
 * a given probability.  The GrammarJob encapsulates the set of
 * SentenceHMMStates, the grammar node and the probability into a
 * single object. This GrammarJob object is typically posted to a
 * grammar job queue to be processed by the grammar expansion loop.
 * Using the GrammarJob helps us to avoid a recursive expansion which
 * can fail due to lack of resources when compiling large and deep
 * grammars.
 */
class GrammarJob {
     List tails;                // list of SentenceHMMStates
     GrammarNode node;          // Grammar Node for this job
     double logProbability;         // log probability of transitioning to node

     /**
      * Creates a new GrammarJob
      *
      * @param tails the list of SentenceHMMStates that should
      * transition to the new grammar node
      *
      * @param node a new GrammarState should be created associated
      * with this grammar node
      *
      * @param logProbability the log probability of transitioning to the new
      * grammar state (in the log math log domain)
      */
     GrammarJob(List tails, GrammarNode node, double logProbability) {
         this.tails = tails;
         this.node = node;
         this.logProbability = logProbability;
     }

     /**
      * Retrieves the list of tails
      *
      * @return a list of SentenceHMMStates
      */
     public List getTails() {
         return tails;
     }

     /**
      * Gets the grammar node
      *
      * @return the grammar node associated with this job
      */
     public GrammarNode getNode() {
         return node;
     }

     /**
      * Gets the log probability of transitioning to the grammar state
      *
      * @return the log probability of transitioning
      */
     public double getProbability() {
         return logProbability;
     }
}


