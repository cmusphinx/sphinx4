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

import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.HMMStateArc;
import edu.cmu.sphinx.linguist.flat.FlatLinguist;
import edu.cmu.sphinx.linguist.flat.SentenceHMMState;
import edu.cmu.sphinx.linguist.flat.SentenceHMMState.Color;
import edu.cmu.sphinx.linguist.flat.UnitState;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A extended form of the {@link edu.cmu.sphinx.linguist.flat.FlatLinguist FlatLinguist} that creates a search graph
 * with states for multiple feature streams.
 * <p/>
 * The only difference in the topology of the search graph created by this linguist and the FlatLinguist is that at a
 * certain level (either the unit or the state level), the graph splits into two parallel branches of states, one for
 * each feature stream. Moreover, at the end of that same level, the multiple streams are again merged.
 */
public class ParallelSimpleLinguist extends FlatLinguist {

    /** The property that specifies the height of the token stacks. */
    @S4Integer(defaultValue = 0)
    public static final String PROP_STACK_CAPACITY = "tokenStackCapacity";

    /** The property that specifies the level at which the parallel states tie. Values can be "unit" or "state". */
    @S4String(defaultValue = "unit")
    public static final String PROP_TIE_LEVEL = "tieLevel";

    /** The property that specifies the feature streams. */
    @S4ComponentList(type = FeatureStream.class)
    public static final String PROP_FEATURE_STREAMS = "featureStreams";

    private List<FeatureStream> featureStreams;

    private int tokenStackCapacity;

    private String tieLevel;

    private final static float logOne = LogMath.getLogOne();

    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        tokenStackCapacity = ps.getInt(PROP_STACK_CAPACITY);
        tieLevel = ps.getString(PROP_TIE_LEVEL);
    }


    /**
     * Sets up the acoustic model.
     *
     * @param ps the PropertySheet from which to obtain the acoustic model
     */
    protected void setupAcousticModel(PropertySheet ps)
            throws PropertyException {
        featureStreams = ps.getComponentList(PROP_FEATURE_STREAMS, FeatureStream.class);
    }


    /** Allocates the acoustic model(s). */
    protected void allocateAcousticModel() throws IOException {
        for (FeatureStream stream : featureStreams)
            stream.getAcousticModel().allocate();
    }


    /** Frees the acoustic model(s) used. */
    protected void freeAcousticModels() {
        for (FeatureStream stream : featureStreams)
            stream.freeAcousticModel();
    }


    /**
     * Returns an Iterator of the feature streams.
     *
     * @return an iterator of the feature streams
     */
    public Iterator<FeatureStream> getFeatureStreams() {
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
     * This is a nested class that is used to manage the construction of the states in a grammar node.  There is one
     * GState created for each grammar node. The GState is used to collect the entry and exit points for the grammar
     * node and for connecting up the grammar nodes to each other.
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
            FeatureStream stream = featureStreams.get(0);
            return stream.getAcousticModel().getLeftContextSize();
        }


        /**
         * Returns the size of the right context.
         *
         * @return the size of the right context
         */
        protected int getRightContextSize() {
            FeatureStream stream = featureStreams.get(0);
            return stream.getAcousticModel().getRightContextSize();
        }


        /**
         * Expands the unit into a set of HMMStates. If the unit is a silence unit add an optional loopback to the
         * tail.
         *
         * @param unit the unit to expand
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
                        LogMath.getLogOne(),
                        getLogSilenceInsertionProbability());
            }
            return tail;
        }


        /**
         * Expands the given UnitState into the set of associated parallel HMMs.
         *
         * @param unitState the UnitState to expand
         * @return the last SentenceHMMState from the expansion
         */
        private SentenceHMMState getTiedHMMs(UnitState unitState) {

            SentenceHMMState combineState = new CombineState
                    (unitState.getParent(), unitState.getWhich());

            // create an HMM branch for each acoustic model            
            for (FeatureStream stream : featureStreams) {
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
                        logOne,
                        logOne);

                // expand the HMM and connect the lastState w/ the combineState
                Map<HMMState, ParallelHMMStateState> hmmStates = new HashMap<HMMState, ParallelHMMStateState>();
                hmmStates.put(firstHMMState.getHMMState(), firstHMMState);

                SentenceHMMState lastState =
                        expandParallelHMMTree(firstHMMState, stream, hmmStates);

                attachState(lastState, combineState,
                        logOne,
                        logOne);
            }

            return combineState;
        }


        /**
         * Expands the given HMM tree into the full set of HMMStateStates.
         *
         * @param hmmStateState  the first state of the HMM tree
         * @param stream         the FeatureStream of the relevant acoustic model
         * @param expandedStates the map of HMMStateStates
         * @return the last state of the expanded tree
         */
        private SentenceHMMState expandParallelHMMTree
                (ParallelHMMStateState hmmStateState, FeatureStream stream,
                 Map<HMMState, ParallelHMMStateState> expandedStates) {

            SentenceHMMState lastState = hmmStateState;

            HMMState hmmState = hmmStateState.getHMMState();

            for (HMMStateArc arc : hmmState.getSuccessors()) {

                HMMState nextHmmState = arc.getHMMState();

                if (nextHmmState == hmmState) {

                    // this is a self-transition
                    attachState(hmmStateState, hmmStateState, logOne, arc.getLogProbability());

                    lastState = hmmStateState;
                } else {

                    // transition to the next state
                    ParallelHMMStateState nextState;

                    if (expandedStates.containsKey(nextHmmState)) {
                        nextState = expandedStates.get(nextHmmState);
                    } else {
                        nextState = new ParallelHMMStateState
                                (hmmStateState.getParent(), stream,
                                        nextHmmState, tokenStackCapacity);
                        expandedStates.put(nextHmmState, nextState);
                    }

                    // Color.GREEN indicates an in-feature-stream state
                    nextState.setColor(Color.GREEN);

                    attachState(hmmStateState, nextState,
                            logOne,
                            arc.getLogProbability());

                    lastState = expandParallelHMMTree
                            (nextState, stream, expandedStates);
                }
            }

            return lastState;
        }


        /**
         * Expands the given UnitState into the set of associated HMMStateStates that tie at the state level.
         *
         * @param unitState the UnitState to expand
         * @return the last SentenceHMMState from the expansion
         */
        private SentenceHMMState getTiedHMMStates(UnitState unitState) {
            HMM[] hmms = new HMM[featureStreams.size()];

            SentenceHMMState lastState;

            int s = 0;
            // create an HMM branch for each feature stream
            for (FeatureStream stream : featureStreams) {
                hmms[s] = stream.getAcousticModel().lookupNearestHMM
                        (unitState.getUnit(), unitState.getPosition(), false);
            }

            lastState = getHMMTiedStates(hmms, unitState);

            return lastState;
        }


        /**
         * Converts the given HMMs into a network of SentenceHMMStates tied at the state level.
         *
         * @param hmms      the HMMs to convert
         * @param unitState the UnitState that corresponds to these HMMs
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
                FeatureStream stream = featureStreams.get(i);

                ParallelHMMStateState firstHMMState = new ParallelHMMStateState
                        (unitState, stream, hmmState, tokenStackCapacity);

                // Color.GREEN indicates an in-feature-stream state
                firstHMMState.setColor(Color.GREEN);

                // connect previous last state to this HMMState
                attachState(unitState, firstHMMState,
                        logOne,
                        logOne);

                // connect this HMMState to the next combining state
                attachState(firstHMMState, lastState,
                        logOne,
                        logOne);

                HMMStateArc selfTransition = getSelfTransition(hmmState);

                if (selfTransition != null) {
                    // connect the next combining state to this HMMState
                    attachState(lastState, firstHMMState,
                            logOne, selfTransition.getLogProbability());
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
                    FeatureStream stream = featureStreams.get(a);

                    ParallelHMMStateState hmmStateState =
                            new ParallelHMMStateState
                                    (unitState, stream, hmmState, tokenStackCapacity);

                    // Color.GREEN indicates an in-feature-stream state
                    hmmStateState.setColor(Color.GREEN);

                    // connect lastState and this HMMStateState
                    attachState(lastState, hmmStateState,
                            logOne,
                            arc.getLogProbability());

                    // connect this HMMStateState and the combineState
                    attachState(hmmStateState, combineState,
                            logOne,
                            logOne);

                    // connect the self-transition
                    HMMStateArc selfTransition = getSelfTransition(hmmState);

                    if (selfTransition != null) {
                        // connect the next combining state to this HMMState
                        attachState(combineState, hmmStateState,
                                logOne, selfTransition.getLogProbability());
                    }
                    arcs[a] = getTransitionToNextState(hmmState);
                }

                lastState = combineState;
            }

            return lastState;
        }


        /** Returns the self-transitioning HMMStateArc of the given HMMState. */
        private HMMStateArc getSelfTransition(HMMState hmmState) {
            for (HMMStateArc arc : hmmState.getSuccessors()) {
                HMMState nextHmmState = arc.getHMMState();
                if (nextHmmState == hmmState) {
                    return arc;
                }
            }
            return null;
        }


        /** Returns the HMMStateArc that transition to the next HMMState. */
        private HMMStateArc getTransitionToNextState(HMMState hmmState) {
            for (HMMStateArc arc : hmmState.getSuccessors()) {
                HMMState nextHmmState = arc.getHMMState();
                if (nextHmmState != hmmState) {
                    return arc;
                }
            }
            return null;
        }
    }
}
