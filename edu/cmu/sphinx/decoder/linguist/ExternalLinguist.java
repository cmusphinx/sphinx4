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

package edu.cmu.sphinx.decoder.linguist;

import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.knowledge.language.LanguageModelFactory;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.decoder.linguist.SentenceHMMState;
import edu.cmu.sphinx.decoder.linguist.Grammar;
import edu.cmu.sphinx.decoder.linguist.Linguist;


import java.util.Collection;
import java.util.Iterator;


/**
 * Provides an implementation of a Linguist interface reads the
 * sentence hmm from an external binary file.
 *
 */
public class ExternalLinguist implements  Linguist {
    public final static String PROP_PATH =
	"edu.cmu.sphinx.decoder.linguist.ExternalLinguist.path";

    private SphinxProperties props;
    private SentenceHMMState initialSentenceHMMState;
    private LanguageModel languageModel;

    private boolean showSentenceHMM;

    private StatisticsVariable totalUniqueStates;
    private StatisticsVariable averageBranches;

    private transient Collection stateSet = null;


    /**
     * Creates an external linguist associated with the given context
     *
     * @param context the context to associate this linguist with
     * @param languageModel the languageModel for this linguist
     * @param grammar the grammar for this linguist
     * @param model the acoustic model, which is not used by this
     *    ExternalLinguist
     */
    public void initialize(String context, LanguageModel languageModel,
			   Grammar grammar, AcousticModel[] model) {
	String path;

	this.props = SphinxProperties.getSphinxProperties(context);
	languageModel = LanguageModelFactory.createLanguageModel(context);

	showSentenceHMM = 
	    props.getBoolean(Linguist.PROP_SHOW_SENTENCE_HMM, false);


	path = props.getString(PROP_PATH, "SentenceHMM.bin");

	Timer loadTimer = Timer.getTimer(props.getContext(), "loadSentenceHMM");
	     
	loadTimer.start();
	initialSentenceHMMState =
	    SentenceHMMState.importBinaryRepresentation(path);

	if (initialSentenceHMMState == null) {
	    throw new Error("Can't load SentenceHMM from " + path);
	}

	if (showSentenceHMM) {
	    initialSentenceHMMState.dumpAll();
	}

	stateSet = SentenceHMMState.collectStates(initialSentenceHMMState);
	collectStats();
	loadTimer.stop();
    }


    /**
     * 
     * Called before a recognition
     */
    public void start() {
	// reset all of the sentence hmm states
	for (Iterator i = stateSet.iterator(); i.hasNext(); ) {
	    SentenceHMMState state = (SentenceHMMState) i.next();
	    state.clear();
	}
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
     * Compiles the grammar into a sentence hmm.  
     */
    protected void compileGrammar() {
    }

    /**
     * Collects statistics  for this linguist
     */
    private void collectStats() {
	double totalBranches = 0.0;

	totalUniqueStates = StatisticsVariable.getStatisticsVariable(
		props.getContext(), "totalUniqueStatesSentenceHMM");
	averageBranches = StatisticsVariable.getStatisticsVariable(
		props.getContext(), "averageBranches");
	totalUniqueStates.value = stateSet.size();

	// this also has the side affect of getting the successor
	// array for all the states, which is then cached in the
	// state, so doing this now takes a little bit of time, but it
	// will make the recognition start off as fast as possible.

	for (Iterator i = stateSet.iterator(); i.hasNext(); ) {
	    SentenceHMMState state = (SentenceHMMState) i.next();
	    totalBranches += state.getSuccessorArray().length;
	}

	averageBranches.value = totalBranches / stateSet.size();
    }

}

