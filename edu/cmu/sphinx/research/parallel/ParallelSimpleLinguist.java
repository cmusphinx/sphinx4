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

import edu.cmu.sphinx.knowledge.acoustic.HMM;
import edu.cmu.sphinx.knowledge.acoustic.HMMState;
import edu.cmu.sphinx.knowledge.acoustic.HMMStateArc;
import edu.cmu.sphinx.knowledge.acoustic.HMMPosition;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;
import edu.cmu.sphinx.knowledge.acoustic.Context;
import edu.cmu.sphinx.knowledge.acoustic.LeftRightContext;
import edu.cmu.sphinx.knowledge.language.LanguageModel;

import edu.cmu.sphinx.decoder.search.*;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.decoder.linguist.*;
import edu.cmu.sphinx.knowledge.dictionary.Pronunciation;


import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * A simple form of the linguist.  It makes the following simplifying
 * assumptions:
 *
 *      1) Only one word per grammar node
 *      2) No fan-in allowed ever
 *      3) No composites (yet)
 *      4) Only Unit, HMMState, and pronunciation states (and the
 *        initial grammar state are in the graph (no word, alternative
 *        or grammar states attached).
 *      5) Only valid tranisitions (matching contexts) are allowed
 *      6) No tree organization of units
 *      7) Branching grammar states are allowed
 *
 * Note that all probabilties are maintained in the log math domain
 */
public class ParallelSimpleLinguist extends SimpleLinguist {

    private static final String PROP_PREFIX = 
        "edu.cmu.sphinx.research.parallel.ParallelSimpleLinguist.";

    private static final String PROP_STACK_CAPACITY = 
        PROP_PREFIX + "tokenStackCapacity";

    private AcousticModel[] acousticModels;

    private int tokenStackCapacity;


    /**
     * Creates a tree linguist associated with the given context
     *
     * @param context the context to associate this linguist with
     * @param languageModel the language model
     * @param grammar the grammar for this linguist
     * @param models the acoustic model used by this linguist
     */
    public void initialize(String context, LanguageModel languageModel,
                        Grammar grammar, AcousticModel[] models) {
        
        SphinxProperties props = SphinxProperties.getSphinxProperties(context);
        this.tokenStackCapacity = props.getInt(PROP_STACK_CAPACITY, 0);

        super.initialize(context, languageModel, grammar, models);
    }


    /**
     * Sets the acoustic model(s) used.
     *
     * @param models the acoustic models to use
     */
    protected void setAcousticModels(AcousticModel[] models) {
        this.acousticModels = models;
    }


    /**
     * Frees the acoustic model(s) used.
     */
    protected void freeAcousticModels() {
        for (int i = 0; i < acousticModels.length; i++) {
            acousticModels[i] = null;
        }
    }


    /**
     * Returns a new GState for the given GrammarNode.
     *
     * @return a new GState for the given GrammarNode
     */
    protected GState createGState(GrammarNode grammarNode) {
        return (new ParallelGState(grammarNode));
    }


    /**
     * This is a nested class that is used to manage the construction
     * of the states in a grammar node.  There is one GState created
     * for each grammar node. The GState is used to collect the entry
     * and exit points for the grammar node and for connecting up the
     * grammar nodes to each other.
     */
    class ParallelGState extends GState {

        /**
         * Creates a GState for a grammar ndoe
         *
         * @param node the grammar node
         */
        ParallelGState(GrammarNode node) {
            super(node);
        }

        /**
         * Returns the size of the left context.
         *
         * @return the size of the left context
         */
        protected int getLeftContextSize() {
            return acousticModels[0].getLeftContextSize();
        }
        
        /**
         * Returns the size of the right context.
         *
         * @return the size of the right context
         */
        protected int getRightContextSize() {
            return acousticModels[0].getRightContextSize();
        }
        
        /**
         * Expands the unit into a set of HMMStates. If the unit is a
         * silence unit add an optional loopback to the tail.
         *
         * @param unit the unit to expand
         *
         * @return the head of the hmm tree
         */
        protected SentenceHMMState expandUnit(UnitState unit) {
            SentenceHMMState tail =  getParallelHMMStates(unit);

            // if the unit is a silence unit add a loop back from the
            // tail silence unit
            if (unit.getUnit().isSilence()) {
                // add the loopback, but don't expand it // anymore
                attachState(tail, unit, 
                            getLogMath().getLogOne(), 
                            getLogMath().getLogOne(),
                            getLogSilenceInsertionProbability());
            }
            return tail;
        }

        /**
         * Expands the given UnitState into the set of associated
         * HMMStateStates.
         *
         * @param unitState the UnitState to expand
         *
         * @return the last SentenceHMMState from the expansion
         */
        private SentenceHMMState getParallelHMMStates(UnitState unitState) {

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
                            getLogMath().getLogOne(),
                            getLogMath().getLogOne(),
                            getLogMath().getLogOne());
                
                // expand the HMM and connect the lastState w/ the combineState
                Map hmmStates = new HashMap();
                hmmStates.put(firstHMMState.getHMMState(), firstHMMState);
                
                SentenceHMMState lastState =
                    expandParallelHMMTree(firstHMMState, 
                                          acousticModels[i].getName(),
                                          hmmStates);
                attachState(lastState, combineState,
                            getLogMath().getLogOne(),
                            getLogMath().getLogOne(),
                            getLogMath().getLogOne());
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
        private SentenceHMMState expandParallelHMMTree
        (ParallelHMMStateState hmmStateState, String modelName,
         Map expandedStates) {
            
            SentenceHMMState lastState = hmmStateState;
            
            HMMState hmmState = hmmStateState.getHMMState();
            HMMStateArc[] arcs = hmmState.getSuccessors();
            
            for (int i = 0; i < arcs.length; i++) {
                
                HMMState nextHmmState = arcs[i].getHMMState();
                
                if (nextHmmState == hmmState) {
                    
                    // this is a self-transition
                    attachState(hmmStateState, hmmStateState,
                                getLogMath().linearToLog
                                (arcs[i].getProbability()),
                                getLogMath().getLogOne(),
                                getLogMath().getLogOne());
                    
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
                                getLogMath().linearToLog
                                (arcs[i].getProbability()),
                                getLogMath().getLogOne(),
                                getLogMath().getLogOne());
                    
                    lastState = expandParallelHMMTree
                        (nextState, modelName, expandedStates);
                }
            }
            
            return lastState;
        }
    }
}
