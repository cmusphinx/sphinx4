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

package edu.cmu.sphinx.research.parallel;

import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;
import edu.cmu.sphinx.knowledge.acoustic.Context;
import edu.cmu.sphinx.knowledge.acoustic.HMM;
import edu.cmu.sphinx.knowledge.acoustic.HMMState;
import edu.cmu.sphinx.knowledge.acoustic.HMMStateArc;
import edu.cmu.sphinx.knowledge.acoustic.LeftRightContext;
import edu.cmu.sphinx.knowledge.acoustic.Unit;

import edu.cmu.sphinx.knowledge.language.LanguageModel;

import edu.cmu.sphinx.decoder.linguist.simple.AlternativeState;
import edu.cmu.sphinx.decoder.linguist.Color;
import edu.cmu.sphinx.decoder.linguist.Grammar;
import edu.cmu.sphinx.decoder.linguist.SearchState;
import edu.cmu.sphinx.decoder.linguist.SearchStateArc;
import edu.cmu.sphinx.decoder.linguist.GrammarArc;
import edu.cmu.sphinx.decoder.linguist.GrammarNode;
import edu.cmu.sphinx.decoder.linguist.simple.GrammarState;
import edu.cmu.sphinx.decoder.linguist.GrammarWord;
import edu.cmu.sphinx.decoder.linguist.simple.HMMStateState;
import edu.cmu.sphinx.decoder.linguist.Linguist;
import edu.cmu.sphinx.knowledge.dictionary.Pronunciation;
import edu.cmu.sphinx.decoder.linguist.simple.PronunciationState;
import edu.cmu.sphinx.decoder.linguist.simple.SentenceHMMState;
import edu.cmu.sphinx.decoder.linguist.simple.SentenceHMMStateArc;
import edu.cmu.sphinx.decoder.linguist.simple.UnitState;
import edu.cmu.sphinx.decoder.linguist.simple.WordState;

import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


/**
 * Constructs a SentenceHMM that is capable of decoding multiple
 * feature streams in parallel.
 */
public class ParallelLinguist implements Linguist {

    private static final String PROP_PREFIX = 
	"edu.cmu.sphinx.research.parallel.ParallelLinguist.";

    private static final String PROP_ADD_SELF_LOOP_WORD_END_SIL =
	PROP_PREFIX + "addSelfLoopWordEndSilence";

    private static final String PROP_TIE_LEVEL =
	PROP_PREFIX + "tieLevel";

    private static final String PROP_TOKEN_STACK_CAPACITY = 
        PROP_PREFIX + "tokenStackCapacity";

    private static final int PROP_TOKEN_STACK_CAPACITY_DEFAULT = 0;

    private static final double PROP_SILENCE_INSERTION_PROBABILITY_DEFAULT = 
        1.0;

    private static final double PROP_UNIT_INSERTION_PROBABILITY_DEFAULT = 1.0;

    private static final double PROP_WORD_INSERTION_PROBABILITY_DEFAULT = 1.0;


    private String context;
    private SentenceHMMState initialState;  // the first SentenceHMMState
    private LanguageModel languageModel;
    private AcousticModel[] acousticModels;

    private LogMath logMath;
    private String tieLevel;
    private double silenceInsertionProbability;
    private double unitInsertionProbability;
    private double wordInsertionProbability;
    private boolean addSelfLoopWordEndSilence;
    private int tokenStackCapacity;

    private Set allStates;

    
    /**
     * Initializes this ParallelLinguist, which means creating the 
     * SentenceHMM with parallel acoustic models.
     *
     * @param context the context to associate this linguist with
     * @param languageModel the languageModel for this linguist
     * @param grammar the grammar for this linguist
     * @param model this is not used in the ParallelLinguist, since
     *    we might have more than one acoustic models.
     */
    public void initialize(String context, LanguageModel languageModel,
			   Grammar grammar, AcousticModel[] models) {
	this.context = context;
	this.languageModel = languageModel;
	this.logMath = LogMath.getLogMath(context);
	this.acousticModels = models;
        this.allStates = new HashSet();

	System.out.println("ParallelLinguist: using " + models.length +
			   " acoustic models");
	
	SphinxProperties props = SphinxProperties.getSphinxProperties(context);

	silenceInsertionProbability = logMath.linearToLog
	    (props.getDouble(Linguist.PROP_SILENCE_INSERTION_PROBABILITY,
			     PROP_SILENCE_INSERTION_PROBABILITY_DEFAULT));
	unitInsertionProbability = logMath.linearToLog
	    (props.getDouble(Linguist.PROP_UNIT_INSERTION_PROBABILITY,
                             PROP_UNIT_INSERTION_PROBABILITY_DEFAULT));
	wordInsertionProbability = logMath.linearToLog
	    (props.getDouble(Linguist.PROP_WORD_INSERTION_PROBABILITY,
                             PROP_WORD_INSERTION_PROBABILITY_DEFAULT));

	addSelfLoopWordEndSilence =
	    props.getBoolean(PROP_ADD_SELF_LOOP_WORD_END_SIL, true);
	tieLevel =
	    props.getString(PROP_TIE_LEVEL, "word");
        tokenStackCapacity = props.getInt
            (PROP_TOKEN_STACK_CAPACITY, PROP_TOKEN_STACK_CAPACITY_DEFAULT);

	compileGrammar(grammar);
        System.out.println("ParallelLinguist total states: " + 
                           allStates.size());
    }


    /**
     * Returns the initial SearchState.
     *
     * @return the initial SearchState
     */
    public SearchState getInitialSearchState() {
	return initialState;
    }


    /**
     * Called before a recognition.
     */
    public void start() {
        // clear out all the SentenceHMMStates
        for (Iterator i = allStates.iterator(); i.hasNext(); ) {
            SentenceHMMState state = (SentenceHMMState) i.next();
            // state.clear();  // BUG: fixme
        }
    }


    /**
     * Called after a recognition.
     */
    public void stop() {
    }


    /**
     * Returns the language model for this ParallelLinguist.
     *
     * @return the language model (or null if there is none)
     */
    public LanguageModel getLanguageModel() {
	return languageModel;
    }

    
    /**
     * Compiles the given Grammar object into a SentenceHMM.
     *
     * @param grammar the Grammar object to compile
     */
    private void compileGrammar(Grammar grammar) {
	Timer compileTimer = Timer.getTimer(context, "compileGrammar");

	compileTimer.start();

	Map compiledNodes = new HashMap();

	GrammarNode firstGrammarNode = grammar.getInitialNode();
	this.initialState = compileGrammarNode(firstGrammarNode,compiledNodes);

	compileTimer.stop();

	assert this.initialState != null;
    }


    /**
     * Returns a GrammarState for the given GrammarNode.
     *
     * @param grammarNode the GrammarNode to return a GrammarState for
     */
    private GrammarState getGrammarState(GrammarNode grammarNode) {
	GrammarState grammarState = new GrammarState(grammarNode);
	return grammarState;
    }


    /**
     * Attach one state (dest) to another state (src).
     *
     * @param src the source state
     * @param dest the destination state
     * @param acousticProbability the acoustic probability of the transition
     * @param languageProbability the language probability of the transition
     * @param insertionProbability the insertion probability of the transition
     */
    private void attachState(SentenceHMMState src, SentenceHMMState dest,
			     double acousticProbability,
			     double languageProbability,
			     double insertionProbability) {
	src.connect(new SentenceHMMStateArc(dest,
					    (float) acousticProbability,
					    (float) languageProbability,
					    (float) insertionProbability));
        allStates.add(src);
        allStates.add(dest);
    }


    /**
     * Compile the given GrammarNode and its successors into a series
     * of SentenceHMMStates.
     *
     * @param grammarNode the grammarNode to compile
     * @param compiledNodes a mapping of the compiled GrammarNodes
     *    and initial GrammarStates
     *
     * @return the first SentenceHMMState after compiling the GrammarNode
     *    into SentenceHMMStates
     */
    private SentenceHMMState compileGrammarNode(GrammarNode grammarNode,
						Map compiledNodes) {

	// create and expand the GrammarState for the GrammarNode
	GrammarState firstState = getGrammarState(grammarNode);
        allStates.add(firstState);
	compiledNodes.put(grammarNode, firstState);

	SentenceHMMState lastState = expandGrammarState(firstState);

	// expand the successor GrammarNodes
	for (int i = 0; i < grammarNode.getSuccessors().length; i++) {
	    GrammarArc arc = grammarNode.getSuccessors()[i];
	    GrammarNode nextNode = arc.getGrammarNode();
	    SentenceHMMState nextFirstState = null;

	    if (compiledNodes.containsKey(nextNode)) {
		// nextNode has already been compiled
		nextFirstState = (SentenceHMMState)compiledNodes.get(nextNode);
	    } else {
		// get the first state from compiling the next GrammarNode
		nextFirstState =
		    compileGrammarNode(nextNode, compiledNodes);
	    }
	    
	    // connect the last state and the first state
	    attachState(lastState, nextFirstState, 
			logMath.getLogOne(), 
			arc.getProbability(),
			logMath.getLogOne());
	}

	return firstState;
    }


    /**
     * Expands the given GrammarState into the full set of SentenceHMMStates
     * of AlternativeStates, WordStates, PronunciationStates, 
     * UnitStates, and HMMStateStates.
     *
     * @param grammarState the GrammarState to expand
     *
     * @return the last state after expanding the given GrammarState
     */
    private SentenceHMMState expandGrammarState(GrammarState grammarState) {
	GrammarWord[][] alternatives =
	    grammarState.getGrammarNode().getAlternatives();

	if (alternatives.length == 0) {
	    return grammarState;
	} else {
	    SentenceHMMState endGrammarState =
		new CombineState(grammarState, 0);
	    
	    for (int i = 0; i < alternatives.length; i++) {
		AlternativeState alternativeState =
		    new AlternativeState(grammarState, i);

		// add this AlternativeState after the GrammarState
		// Note that "logMath.getLogOne() - logValue" is
		// the equivalent of "-logValue", since the inverse (1/x)
		// in linear scale becomes the opposite in log scale (-x)
		attachState(grammarState, alternativeState,
			    logMath.getLogOne(),
			    logMath.getLogOne()
			    - logMath.linearToLog(alternatives.length),
			    logMath.getLogOne());

		SentenceHMMState lastState = expandAlternative
		    (alternativeState);

                // if there is only one alternative, we don't need
                // the CombineState

                if (alternatives.length == 1) {
                    endGrammarState = lastState;
                } else {
                    // add the last state from the expansion 
                    // to the endGrammarState
                    attachState(lastState, endGrammarState,
				logMath.getLogOne(),
				logMath.getLogOne(),
				logMath.getLogOne());
                }
            }
	    return endGrammarState;
	}
    }


    /**
     * Expands the given AlternativeState into the set of associated
     * WordStates, PronunciationStates, UnitStates, and HMMStateStates.
     * 
     * @param state the AlternativeState to expand
     *
     * @return the last SentenceHMMState from the expansion
     */
    private SentenceHMMState expandAlternative(AlternativeState state) {
	GrammarWord alternative[] = state.getAlternative();
	SentenceHMMState lastState = state;

	for (int i = 0; i < alternative.length; i++) {
	    WordState wordState = new WordState(state, i);
            
            // if its silence, use silenceInsertionProbability
            double insertionProbability = wordInsertionProbability;
            if (wordState.getWord().isSilence()) {
                insertionProbability = silenceInsertionProbability;
            }

	    attachState(lastState, wordState,
			logMath.getLogOne(),
			logMath.getLogOne(),
			insertionProbability);

	    lastState = expandWord(wordState);
	}
	return lastState;
    }


    /**
     * Expands the given WordState into the set of associated
     * PronunciationStates, UnitStates, and HMMStateStates.
     *
     * @param wordState the WordState to expand
     *
     * @return the last SentenceHMMState from the expansion
     */
    private SentenceHMMState expandWord(WordState wordState) {
	GrammarWord word = wordState.getWord();
	Pronunciation[] pronunciations = word.getPronunciations();

	// create the combining state for the multiple pronunciations
	SentenceHMMState endWordState = new CombineState
	    (wordState.getParent(), wordState.getWhich());

	for (int i = 0; i < pronunciations.length; i++) {

	    // attach all pronunciation states to the wordState
	    PronunciationState pronunciationState =
		new PronunciationState(wordState, i);

	    attachState(wordState, pronunciationState,
			logMath.getLogOne(),
			logMath.getLogOne()
			- logMath.linearToLog(pronunciations.length),
			logMath.getLogOne());

	    // attach last state from expansion to endWordState
	    SentenceHMMState lastState =
		expandPronunciation(pronunciationState);

	    // use the combining state only if there are more than
	    // one pronunciations

	    if (pronunciations.length == 1) {
		endWordState = lastState;
	    } else {
		attachState(lastState, endWordState,
			    logMath.getLogOne(),
			    logMath.getLogOne(),
			    logMath.getLogOne());
	    }
	}

	return endWordState;
    }


    /**
     * Expands the given PronunciationState into the set of associated
     * UnitStates and HMMStateStates.
     *
     * @param pronunciationState the PronunciationState to expand
     *
     * @return the last SentenceHMMState from the expansion
     */
    private SentenceHMMState expandPronunciation(PronunciationState state) {
	Pronunciation pronunciation = state.getPronunciation();
	Unit[] units = pronunciation.getUnits();

	SentenceHMMState lastState = state;

	// creates: - P - U - ... - U - ... - U - ...

	for (int i = 0; i < units.length; i++) {

	    UnitState unitState = null;

	    if (i == 0 || i == (units.length - 1)) {
		// the first and last units will be context-independent
	        unitState = new UnitState(state, i, units[i]);
	    } else {
		// the middle units will be context-dependent
		Unit[] leftContext = new Unit[1];
		Unit[] rightContext = new Unit[1];
		leftContext[0] = units[i-1];
		rightContext[0] = units[i+1];

		Context context = LeftRightContext.get
		    (leftContext, rightContext);
		Unit unit = new Unit(units[i].getName(), units[i].isFiller(),
				     context);
		unitState = new UnitState(state, i, unit);
	    }

	    // attach new unitState to 'lastState'
	    attachState(lastState, unitState, 
			logMath.getLogOne(),
			logMath.getLogOne(),
			unitInsertionProbability);

	    lastState = expandUnit(unitState);

            // silence should be self-looping
            if (unitState.getUnit().isSilence()) {
                attachState(lastState, unitState,
			    logMath.getLogOne(),
			    logMath.getLogOne(),
			    logMath.getLogOne());
            }
	}

        // add optional, self-looping word-ending silence
        Unit lastUnit = units[units.length - 1];

        if (addSelfLoopWordEndSilence && !lastUnit.isSilence()) {
            addLoopSilence(lastState, state);
        }

	return lastState;
    }


    /**
     * Adds a self-looping silence to the given SentenceHMMState.
     * The resulting states look like:
     * <code>
     *           ----------------
     *          |                |
     *          V                |
     *         -- U - H - H - H -
     *        /                  |
     *       /                   |
     *   state <-----------------
     *
     * </code>
     *
     * @param state the SentenceHMMState to add the looping silence to
     */
    private void addLoopSilence(SentenceHMMState state,
				PronunciationState pronunciationState) {

	// FIX: 'which' shouldn't go beyond the length of pronunciation
	int which = pronunciationState.getPronunciation().getUnits().length;

	UnitState unitState = new UnitState
	    (pronunciationState, which, Unit.SILENCE);
	
	// add the silence UnitState to the state
	attachState(state, unitState, 
		    logMath.getLogOne(),
		    logMath.getLogOne(),
		    silenceInsertionProbability);
	
	// expand the HMMTree
	SentenceHMMState lastSilenceState = expandUnit(unitState);
	
	// add a self-loop
	attachState(lastSilenceState, unitState,
		    logMath.getLogOne(),
		    logMath.getLogOne(),
		    logMath.getLogOne());
	
	// connect the last silence state back to the last state
	attachState(lastSilenceState, state,
		    logMath.getLogOne(),
		    logMath.getLogOne(),
		    logMath.getLogOne());
    }


    /**
     * Expands the given UnitState into the set of associated
     * HMMStateStates.
     *
     * @param unitState the UnitState to expand
     *
     * @return the last SentenceHMMState from the expansion
     */
    private SentenceHMMState expandUnit(UnitState unitState) {

	/*
        SentenceHMMState splitState = new SplitState
            (unitState.getParent(), unitState.getWhich());

        attachState(unitState, splitState,
	logMath.getLogOne(),
	logMath.getLogOne(),
	logMath.getLogOne());
	*/

	SentenceHMMState combineState = new CombineState
	    (unitState.getParent(), unitState.getWhich());

	// create an HMM branch for each acoustic model

	for (int i = 0; i < acousticModels.length; i++) {

	    HMM hmm = acousticModels[i].lookupNearestHMM
		(unitState.getUnit(), unitState.getPosition(), false);

	    ParallelHMMStateState firstHMMState = 
		new ParallelHMMStateState(unitState,
					  acousticModels[i].getName(),
					  hmm.getInitialState(),
                                          tokenStackCapacity);
	   
	    // Color.GREEN indicates an in-feature-stream state
	    firstHMMState.setColor(Color.GREEN);

	    // attach first HMMStateState to the splitState
	    attachState(unitState, firstHMMState,
			logMath.getLogOne(),
			logMath.getLogOne(),
			logMath.getLogOne());
	    
	    // expand the HMM and connect the lastState w/ the combineState
	    Map hmmStates = new HashMap();
	    hmmStates.put(firstHMMState.getHMMState(), firstHMMState);

	    SentenceHMMState lastState =
		expandHMMTree(firstHMMState, acousticModels[i].getName(),
			      hmmStates);
	    attachState(lastState, combineState,
			logMath.getLogOne(),
			logMath.getLogOne(),
			logMath.getLogOne());
	}

	return combineState;
    }


    /**
     * Expands the given HMM tree into the full set of HMMStateStates.
     *
     * @param hmmStateState the first state of the HMM tree
     * @param modelName the name of the acoustic model behind this HMM tree
     * @param expandedStates the map of HMMStateStates
     *
     * @return the last state of the expanded tree
     */
    private SentenceHMMState expandHMMTree(ParallelHMMStateState hmmStateState,
					   String modelName,
					   Map expandedStates) {

	SentenceHMMState lastState = hmmStateState;

	HMMState hmmState = hmmStateState.getHMMState();
	HMMStateArc[] arcs = hmmState.getSuccessors();

	for (int i = 0; i < arcs.length; i++) {

	    HMMState nextHmmState = arcs[i].getHMMState();

	    if (nextHmmState == hmmState) {

		// this is a self-transition
		attachState(hmmStateState, hmmStateState,
			    arcs[i].getLogProbability(),
			    logMath.getLogOne(),
			    logMath.getLogOne());

		lastState = hmmStateState;
	    } else {

		// transition to the next state
		ParallelHMMStateState nextState = null;
		
		if (expandedStates.containsKey(nextHmmState)) {
		    nextState = (ParallelHMMStateState) 
			expandedStates.get(nextHmmState);
		} else {
		    nextState = new ParallelHMMStateState
			(hmmStateState.getParent(), modelName, 
                         nextHmmState, tokenStackCapacity);
		    expandedStates.put(nextHmmState, nextState);
		}

		// Color.GREEN indicates an in-feature-stream state
		nextState.setColor(Color.GREEN);

		attachState(hmmStateState, nextState, 
			    arcs[i].getLogProbability(),
			    logMath.getLogOne(),
			    logMath.getLogOne());

		lastState = expandHMMTree
		    (nextState, modelName, expandedStates);
	    }
	}

	return lastState;
    }
}
