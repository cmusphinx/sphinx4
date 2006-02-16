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

package edu.cmu.sphinx.research.parallel;

import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.HMMStateArc;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.Context;
import edu.cmu.sphinx.linguist.acoustic.LeftRightContext;

import edu.cmu.sphinx.linguist.dictionary.Dictionary;

import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;
import edu.cmu.sphinx.linguist.language.grammar.Grammar;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;

import edu.cmu.sphinx.decoder.search.*;
import edu.cmu.sphinx.decoder.pruner.Pruner;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.Utilities;

import edu.cmu.sphinx.linguist.*;
import edu.cmu.sphinx.linguist.flat.*;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import java.io.IOException;

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
 * A extended form of the {@link edu.cmu.sphinx.linguist.flat.FlatLinguist
 * FlatLinguist} that creates a search graph with states for multiple
 * feature streams.
 * <p>
 * The only difference in the topology of the search graph created by
 * this linguist and the FlatLinguist is that at a certain level
 * (either the unit or the state level), the graph splits into two
 * parallel branches of states, one for each feature stream.
 * Moreover, at the end of that same level, the multiple streams are again
 * merged.
 */
public class ParallelSimpleLinguist extends FlatLinguist {

    /**
     * The sphinx property that specifies the height of the token stacks.
     */
    public static final String PROP_STACK_CAPACITY = "tokenStackCapacity";

    /**
     * The default value for the property PROP_STACK_CAPACITY, which is 0.
     */
    public static final int PROP_STACK_CAPACITY_DEFAULT = 0;

    /**
     * The sphinx property that specifies the level at which the parallel
     * states tie. Values can be "unit" or "state".
     */
    public static final String PROP_TIE_LEVEL = "tieLevel";

    /**
     * The default value for the property PROP_TIE_LEVEL, which is "unit".
     */
    public static final String PROP_TIE_LEVEL_DEFAULT = "unit";

    /**
     * Property that specifies the feature streams.
     */
    public static final String PROP_FEATURE_STREAMS = "featureStreams";

    
    private List featureStreams;

    private int tokenStackCapacity;

    private String tieLevel;


    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
        throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_STACK_CAPACITY, PropertyType.INT);
        registry.register(PROP_TIE_LEVEL, PropertyType.STRING);
        registry.register(PROP_FEATURE_STREAMS, PropertyType.COMPONENT_LIST);
    }


    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        tokenStackCapacity = ps.getInt(PROP_STACK_CAPACITY,
                                       PROP_STACK_CAPACITY_DEFAULT);
        tieLevel = ps.getString(PROP_TIE_LEVEL, PROP_TIE_LEVEL_DEFAULT);
    }

    /**
     * Sets up the acoustic model.
     *
     * @param ps the PropertySheet from which to obtain the acoustic model
     */
    protected void setupAcousticModel(PropertySheet ps) 
        throws PropertyException {
        featureStreams = ps.getComponentList
            (PROP_FEATURE_STREAMS, FeatureStream.class);
    }

    /**
     * Allocates the acoustic model(s).
     */
    protected void allocateAcousticModel() throws IOException {
        for (Iterator i = featureStreams.iterator(); i.hasNext(); ) {
            FeatureStream stream = (FeatureStream) i.next();
            stream.getAcousticModel().allocate();
        }
    }
    
    /**
     * Frees the acoustic model(s) used.
     */
    protected void freeAcousticModels() {
        for (Iterator i = featureStreams.iterator(); i.hasNext(); ) {
            ((FeatureStream) i.next()).freeAcousticModel();
        }
    }


    /**
     * Returns an Iterator of the feature streams.
     *
     * @return an iterator of the feature streams
     */
    public Iterator getFeatureStreams() {
        return featureStreams.iterator();
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
            FeatureStream stream = (FeatureStream) featureStreams.get(0);
            return stream.getAcousticModel().getLeftContextSize();
        }
        
        /**
         * Returns the size of the right context.
         *
         * @return the size of the right context
         */
        protected int getRightContextSize() {
            FeatureStream stream = (FeatureStream) featureStreams.get(0);
            return stream.getAcousticModel().getRightContextSize();
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
            SentenceHMMState tail = null;

            if (tieLevel.equals("unit")) {
                tail = getTiedHMMs(unit);
            } else if (tieLevel.equals("state")) {
                tail = getTiedHMMStates(unit);
            }

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
         * parallel HMMs.
         *
         * @param unitState the UnitState to expand
         *
         * @return the last SentenceHMMState from the expansion
         */
        private SentenceHMMState getTiedHMMs(UnitState unitState) {

            SentenceHMMState combineState = new CombineState
                (unitState.getParent(), unitState.getWhich());
            
            // create an HMM branch for each acoustic model            
            for (Iterator i = featureStreams.iterator(); i.hasNext(); ) {

                FeatureStream stream = (FeatureStream) i.next();
                AcousticModel model = stream.getAcousticModel();

                HMM hmm = model.lookupNearestHMM
                    (unitState.getUnit(), unitState.getPosition(), false);

                ParallelHMMStateState firstHMMState = 
                    new ParallelHMMStateState(unitState, stream,
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
                    expandParallelHMMTree(firstHMMState, stream, hmmStates);

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
         * @param stream the FeatureStream of the relevant acoustic model
         * @param expandedStates the map of HMMStateStates
         *
         * @return the last state of the expanded tree
         */
        private SentenceHMMState expandParallelHMMTree
        (ParallelHMMStateState hmmStateState, FeatureStream stream,
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
                            (hmmStateState.getParent(), stream,
                             nextHmmState, tokenStackCapacity);
                        expandedStates.put(nextHmmState, nextState);
                    }
                    
                    // Color.GREEN indicates an in-feature-stream state
                    nextState.setColor(Color.GREEN);
                    
                    attachState(hmmStateState, nextState, 
                                arcs[i].getLogProbability(),
                                getLogMath().getLogOne(),
                                getLogMath().getLogOne());
                    
                    lastState = expandParallelHMMTree
                        (nextState, stream, expandedStates);
                }
            }
            
            return lastState;
        }

        /**
         * Expands the given UnitState into the set of associated
         * HMMStateStates that tie at the state level.
         *
         * @param unitState the UnitState to expand
         *
         * @return the last SentenceHMMState from the expansion
         */
        private SentenceHMMState getTiedHMMStates(UnitState unitState) {
            SentenceHMMState combineState = new CombineState
                (unitState.getParent(), unitState.getWhich());
            
            HMM[] hmms = new HMM[featureStreams.size()];
            
            SentenceHMMState lastState = unitState;
            
            int s = 0;
            // create an HMM branch for each feature stream
            for (Iterator i = featureStreams.iterator(); i.hasNext(); s++) {
                FeatureStream stream = (FeatureStream) i.next();
                hmms[s] = stream.getAcousticModel().lookupNearestHMM
                    (unitState.getUnit(), unitState.getPosition(), false);
            }
            
            lastState = getHMMTiedStates(hmms, unitState);
            
            return lastState;
        }
        
        
        /**
         * Converts the given HMMs into a network of SentenceHMMStates
         * tied at the state level.
         *
         * @param hmms the HMMs to convert
         * @param unitState the UnitState that corresponds to these HMMs
         *
         * @return the last SentenceHMMState from the expansion
         */
        private SentenceHMMState getHMMTiedStates(HMM[] hmms, 
                                                  UnitState unitState) {
            
            SentenceHMMState lastState = new CombineState(unitState, 0);
            
            HMMStateArc[] arcs = new HMMStateArc[featureStreams.size()];
            
            //
            // In this for loop, we connect the unitState to the 
            // ParallelHMMState created from the first HMMState of each of 
            // the HMMs. We then connect each of the ParallelHMMStates
            // to a combining state. Lastly, if the HMMState has a
            // self-transition, we create a transition from the combining
            // state to the ParallelHMMState, using the self-transition
            // probability.
            //
            for (int i = 0; i < hmms.length; i++) {
                HMMState hmmState = hmms[i].getInitialState();
                FeatureStream stream = (FeatureStream) featureStreams.get(i);

                ParallelHMMStateState firstHMMState = new ParallelHMMStateState
                    (unitState, stream, hmmState, tokenStackCapacity);
                
                // Color.GREEN indicates an in-feature-stream state
                firstHMMState.setColor(Color.GREEN);
                
                // connect previous last state to this HMMState
                attachState(unitState, firstHMMState,
                            getLogMath().getLogOne(),
                            getLogMath().getLogOne(),
                            getLogMath().getLogOne());
                
                // connect this HMMState to the next combining state
                attachState(firstHMMState, lastState,
                            getLogMath().getLogOne(),
                            getLogMath().getLogOne(),
                            getLogMath().getLogOne());
                
                HMMStateArc selfTransition = getSelfTransition(hmmState);
                
                if (selfTransition != null) {
                    // connect the next combining state to this HMMState
                    attachState(lastState, firstHMMState, 
                                selfTransition.getLogProbability(),        
                                getLogMath().getLogOne(),
                                getLogMath().getLogOne());
                }
                
                arcs[i] = getTransitionToNextState(hmmState);
            }

            // 
            // We then start with the second HMMState of each HMM, and do
            // the same thing as the previous for loop: connect each
            // ParallelHMMState (created from the HMMState) to a combining
            // state, and if that HMMState has a self transition, connect
            // that combining state to the ParallelHMMState using the
            // self transition probability
            // 
            for (int i = 1; i <= hmms[0].getOrder(); i++) {
                
                SentenceHMMState combineState = new CombineState(unitState, i);
                
                for (int a = 0; a < arcs.length; a++) {
                    HMMStateArc arc = arcs[a];
                    HMMState hmmState = arc.getHMMState();
                    FeatureStream stream = (FeatureStream)featureStreams.get(a);

                    ParallelHMMStateState hmmStateState = 
                    new ParallelHMMStateState
                        (unitState, stream, hmmState, tokenStackCapacity);
                    
                    // Color.GREEN indicates an in-feature-stream state
                    hmmStateState.setColor(Color.GREEN);
                    
                    // connect lastState and this HMMStateState
                    attachState(lastState, hmmStateState,
                                arc.getLogProbability(),
                                getLogMath().getLogOne(),
                                getLogMath().getLogOne());
                    
                    // connect this HMMStateState and the combineState
                    attachState(hmmStateState, combineState, 
                                getLogMath().getLogOne(),
                                getLogMath().getLogOne(),
                                getLogMath().getLogOne());
                    
                    // connect the self-transition
                    HMMStateArc selfTransition = getSelfTransition(hmmState);
                    
                    if (selfTransition != null) {
                        // connect the next combining state to this HMMState
                        attachState(combineState, hmmStateState,
                                    selfTransition.getLogProbability(),       
                                    getLogMath().getLogOne(),
                                    getLogMath().getLogOne());
                    }
                    arcs[a] = getTransitionToNextState(hmmState);
                }
                
                lastState = combineState;
            }

            return lastState;
        }
        
        /**
         * Returns the self-transitioning HMMStateArc of the given HMMState.
         */ 
        private HMMStateArc getSelfTransition(HMMState hmmState) {
            HMMStateArc[] arcs = hmmState.getSuccessors();
            for (int i = 0; i < arcs.length; i++) {
                HMMState nextHmmState = arcs[i].getHMMState();
                if (nextHmmState == hmmState) {
                    return arcs[i];
                }
            }
            return null;
        }
        
        /**
         * Returns the HMMStateArc that transitioin to the next HMMState.
         */
        private HMMStateArc getTransitionToNextState(HMMState hmmState) {
            HMMStateArc[] arcs = hmmState.getSuccessors();
            for (int i = 0; i < arcs.length; i++) {
                HMMState nextHmmState = arcs[i].getHMMState();
                if (nextHmmState != hmmState) {
                    return arcs[i];
                }
            }
            return null;
        }
    }
}
