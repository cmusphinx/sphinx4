/*
 * Copyright 2014 Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.decoder.search;

// a test search manager.

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import edu.cmu.sphinx.decoder.pruner.Pruner;
import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.linguist.allphone.PhoneHmmSearchState;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist.LexTreeHMMState;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist.LexTreeNonEmittingHMMState;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist.LexTreeWordState;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist.LexTreeEndUnitState;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4Double;
import edu.cmu.sphinx.util.props.S4Integer;

/**
 * Provides the breadth first search with fast match heuristic included to
 * reduce amount of tokens created.
 * <p>
 * All scores and probabilities are maintained in the log math log domain.
 */

public class WordPruningBreadthFirstLookaheadSearchManager extends WordPruningBreadthFirstSearchManager {

    /** The property that to get direct access to gau for score caching control. */
    @S4Component(type = Loader.class)
    public final static String PROP_LOADER = "loader";

    /**
     * The property that defines the name of the linguist to be used for fast
     * match.
     */
    @S4Component(type = Linguist.class)
    public final static String PROP_FASTMATCH_LINGUIST = "fastmatchLinguist";

    @S4Component(type = ActiveListFactory.class)
    /** The property that defines the type active list factory for fast match */
    public final static String PROP_FM_ACTIVE_LIST_FACTORY = "fastmatchActiveListFactory";

    @S4Double(defaultValue = 1.0)
    public final static String PROP_LOOKAHEAD_PENALTY_WEIGHT = "lookaheadPenaltyWeight";

    /**
     * The property that controls size of lookahead window. Acceptable values
     * are in range [1..10].
     */
    @S4Integer(defaultValue = 5)
    public final static String PROP_LOOKAHEAD_WINDOW = "lookaheadWindow";

    // -----------------------------------
    // Configured Subcomponents
    // -----------------------------------
    private Linguist fastmatchLinguist; // Provides phones info for fastmatch
    private Loader loader;
    private ActiveListFactory fastmatchActiveListFactory;

    // -----------------------------------
    // Lookahead data
    // -----------------------------------
    private int lookaheadWindow;
    private float lookaheadWeight;
    private HashMap<Integer, Float> penalties;
    private LinkedList<FrameCiScores> ciScores;

    // -----------------------------------
    // Working data
    // -----------------------------------
    private int currentFastMatchFrameNumber; // the current frame number for
                                             // lookahead matching
    protected ActiveList fastmatchActiveList; // the list of active tokens for
                                              // fast match
    protected Map<SearchState, Token> fastMatchBestTokenMap;
    private boolean fastmatchStreamEnd;

    /**
     * Creates a pruning manager with lookahead
     * @param linguist a linguist for search space
     * @param fastmatchLinguist a linguist for fast search space
     * @param pruner pruner to drop tokens
     * @param loader model loader
     * @param scorer scorer to estimate token probability
     * @param activeListManager active list manager to store tokens
     * @param fastmatchActiveListFactory fast match active list factor to store phoneloop tokens
     * @param showTokenCount show count during decoding
     * @param relativeWordBeamWidth relative beam for lookahead pruning
     * @param growSkipInterval skip interval for grown
     * @param checkStateOrder check order of states during growth
     * @param buildWordLattice build a lattice during decoding
     * @param maxLatticeEdges max edges to keep in lattice
     * @param acousticLookaheadFrames frames to do lookahead
     * @param keepAllTokens keep tokens including emitting tokens
     * @param lookaheadWindow window for lookahead
     * @param lookaheadWeight weight for lookahead pruning
     */
    public WordPruningBreadthFirstLookaheadSearchManager(Linguist linguist, Linguist fastmatchLinguist, Loader loader,
            Pruner pruner, AcousticScorer scorer, ActiveListManager activeListManager,
            ActiveListFactory fastmatchActiveListFactory, boolean showTokenCount, double relativeWordBeamWidth,
            int growSkipInterval, boolean checkStateOrder, boolean buildWordLattice, int lookaheadWindow, float lookaheadWeight,
            int maxLatticeEdges, float acousticLookaheadFrames, boolean keepAllTokens) {

        super(linguist, pruner, scorer, activeListManager, showTokenCount, relativeWordBeamWidth, growSkipInterval,
                checkStateOrder, buildWordLattice, maxLatticeEdges, acousticLookaheadFrames, keepAllTokens);

        this.loader = loader;
        this.fastmatchLinguist = fastmatchLinguist;
        this.fastmatchActiveListFactory = fastmatchActiveListFactory;
        this.lookaheadWindow = lookaheadWindow;
        this.lookaheadWeight = lookaheadWeight;
        if (lookaheadWindow < 1 || lookaheadWindow > 10)
            throw new IllegalArgumentException("Unsupported lookahead window size: " + lookaheadWindow
                    + ". Value in range [1..10] is expected");
        this.ciScores = new LinkedList<FrameCiScores>();
        this.penalties = new HashMap<Integer, Float>();
        if (loader instanceof Sphinx3Loader && ((Sphinx3Loader) loader).hasTiedMixtures())
            ((Sphinx3Loader) loader).setGauScoresQueueLength(lookaheadWindow + 2);
    }

    public WordPruningBreadthFirstLookaheadSearchManager() {

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util
     * .props.PropertySheet)
     */
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);

        fastmatchLinguist = (Linguist) ps.getComponent(PROP_FASTMATCH_LINGUIST);
        fastmatchActiveListFactory = (ActiveListFactory) ps.getComponent(PROP_FM_ACTIVE_LIST_FACTORY);
        loader = (Loader) ps.getComponent(PROP_LOADER);
        lookaheadWindow = ps.getInt(PROP_LOOKAHEAD_WINDOW);
        lookaheadWeight = ps.getFloat(PROP_LOOKAHEAD_PENALTY_WEIGHT);
        if (lookaheadWindow < 1 || lookaheadWindow > 10)
            throw new PropertyException(WordPruningBreadthFirstLookaheadSearchManager.class.getName(), PROP_LOOKAHEAD_WINDOW,
                    "Unsupported lookahead window size: " + lookaheadWindow + ". Value in range [1..10] is expected");
        ciScores = new LinkedList<FrameCiScores>();
        penalties = new HashMap<Integer, Float>();
        if (loader instanceof Sphinx3Loader && ((Sphinx3Loader) loader).hasTiedMixtures())
            ((Sphinx3Loader) loader).setGauScoresQueueLength(lookaheadWindow + 2);
    }

    /**
     * Performs the recognition for the given number of frames.
     * 
     * @param nFrames
     *            the number of frames to recognize
     * @return the current result
     */
    @Override
    public Result recognize(int nFrames) {
        boolean done = false;
        Result result = null;
        streamEnd = false;

        for (int i = 0; i < nFrames && !done; i++) {
            if (!fastmatchStreamEnd)
                fastMatchRecognize();
            penalties.clear();
            ciScores.poll();
            done = recognize();
        }

        if (!streamEnd) {
            result = new Result(loserManager, activeList, resultList, currentCollectTime, done, linguist.getSearchGraph()
                    .getWordTokenFirst(), true);
        }

        // tokenTypeTracker.show();
        if (showTokenCount) {
            showTokenCount();
        }
        return result;
    }

    private void fastMatchRecognize() {
        boolean more = scoreFastMatchTokens();

        if (more) {
            pruneFastMatchBranches();
            currentFastMatchFrameNumber++;
            createFastMatchBestTokenMap();
            growFastmatchBranches();
        }
    }

    /**
     * creates a new best token map with the best size
     */
    protected void createFastMatchBestTokenMap() {
        int mapSize = fastmatchActiveList.size() * 10;
        if (mapSize == 0) {
            mapSize = 1;
        }
        fastMatchBestTokenMap = new HashMap<SearchState, Token>(mapSize);
    }

    /**
     * Gets the initial grammar node from the linguist and creates a
     * GrammarNodeToken
     */
    @Override
    protected void localStart() {
        currentFastMatchFrameNumber = 0;
        if (loader instanceof Sphinx3Loader && ((Sphinx3Loader) loader).hasTiedMixtures())
            ((Sphinx3Loader) loader).clearGauScores();
        // prepare fast match active list
        fastmatchActiveList = fastmatchActiveListFactory.newInstance();
        SearchState fmInitState = fastmatchLinguist.getSearchGraph().getInitialState();
        fastmatchActiveList.add(new Token(fmInitState, currentFastMatchFrameNumber));
        createFastMatchBestTokenMap();
        growFastmatchBranches();
        fastmatchStreamEnd = false;
        for (int i = 0; (i < lookaheadWindow - 1) && !fastmatchStreamEnd; i++)
            fastMatchRecognize();

        super.localStart();
    }

    /**
     * Goes through the fast match active list of tokens and expands each token,
     * finding the set of successor tokens until all the successor tokens are
     * emitting tokens.
     */
    protected void growFastmatchBranches() {
        growTimer.start();
        ActiveList oldActiveList = fastmatchActiveList;
        fastmatchActiveList = fastmatchActiveListFactory.newInstance();
        float fastmathThreshold = oldActiveList.getBeamThreshold();
        // TODO more precise range of baseIds, remove magic number
        float[] frameCiScores = new float[100];

        Arrays.fill(frameCiScores, -Float.MAX_VALUE);
        float frameMaxCiScore = -Float.MAX_VALUE;
        for (Token token : oldActiveList) {
            float tokenScore = token.getScore();
            if (tokenScore < fastmathThreshold)
                continue;
            // filling max ci scores array that will be used in general search
            // token score composing
            if (token.getSearchState() instanceof PhoneHmmSearchState) {
                int baseId = ((PhoneHmmSearchState) token.getSearchState()).getBaseId();
                if (frameCiScores[baseId] < tokenScore)
                    frameCiScores[baseId] = tokenScore;
                if (frameMaxCiScore < tokenScore)
                    frameMaxCiScore = tokenScore;
            }
            collectFastMatchSuccessorTokens(token);
        }
        ciScores.add(new FrameCiScores(frameCiScores, frameMaxCiScore));
        growTimer.stop();
    }

    protected boolean scoreFastMatchTokens() {
        boolean moreTokens;
        scoreTimer.start();
        Data data = scorer.calculateScoresAndStoreData(fastmatchActiveList.getTokens());
        scoreTimer.stop();

        Token bestToken = null;
        if (data instanceof Token) {
            bestToken = (Token) data;
        } else {
            fastmatchStreamEnd = true;
        }

        moreTokens = (bestToken != null);
        fastmatchActiveList.setBestToken(bestToken);

        // monitorWords(activeList);
        monitorStates(fastmatchActiveList);

        // System.out.println("BEST " + bestToken);

        curTokensScored.value += fastmatchActiveList.size();
        totalTokensScored.value += fastmatchActiveList.size();

        return moreTokens;
    }

    /** Removes unpromising branches from the fast match active list */
    protected void pruneFastMatchBranches() {
        pruneTimer.start();
        fastmatchActiveList = pruner.prune(fastmatchActiveList);
        pruneTimer.stop();
    }

    protected Token getFastMatchBestToken(SearchState state) {
        return fastMatchBestTokenMap.get(state);
    }

    protected void setFastMatchBestToken(Token token, SearchState state) {
        fastMatchBestTokenMap.put(state, token);
    }

    protected void collectFastMatchSuccessorTokens(Token token) {
        SearchState state = token.getSearchState();
        SearchStateArc[] arcs = state.getSuccessors();
        // For each successor
        // calculate the entry score for the token based upon the
        // predecessor token score and the transition probabilities
        // if the score is better than the best score encountered for
        // the SearchState and frame then create a new token, add
        // it to the lattice and the SearchState.
        // If the token is an emitting token add it to the list,
        // otherwise recursively collect the new tokens successors.
        for (SearchStateArc arc : arcs) {
            SearchState nextState = arc.getState();
            // We're actually multiplying the variables, but since
            // these come in log(), multiply gets converted to add
            float logEntryScore = token.getScore() + arc.getProbability();
            Token predecessor = getResultListPredecessor(token);

            // if not emitting, check to see if we've already visited
            // this state during this frame. Expand the token only if we
            // haven't visited it already. This prevents the search
            // from getting stuck in a loop of states with no
            // intervening emitting nodes. This can happen with nasty
            // jsgf grammars such as ((foo*)*)*
            if (!nextState.isEmitting()) {
                Token newToken = new Token(predecessor, nextState, logEntryScore, arc.getInsertionProbability(),
                        arc.getLanguageProbability(), currentFastMatchFrameNumber);
                tokensCreated.value++;
                if (!isVisited(newToken)) {
                    collectFastMatchSuccessorTokens(newToken);
                }
                continue;
            }

            Token bestToken = getFastMatchBestToken(nextState);
            if (bestToken == null) {
                Token newToken = new Token(predecessor, nextState, logEntryScore, arc.getInsertionProbability(),
                        arc.getLanguageProbability(), currentFastMatchFrameNumber);
                tokensCreated.value++;
                setFastMatchBestToken(newToken, nextState);
                fastmatchActiveList.add(newToken);
            } else {
                if (bestToken.getScore() <= logEntryScore) {
                    bestToken.update(predecessor, nextState, logEntryScore, arc.getInsertionProbability(),
                            arc.getLanguageProbability(), currentFastMatchFrameNumber);
                }
            }
        }
    }

    /**
     * Collects the next set of emitting tokens from a token and accumulates
     * them in the active or result lists
     * 
     * @param token
     *            the token to collect successors from be immediately expanded
     *            are placed. Null if we should always expand all nodes.
     */
    @Override
    protected void collectSuccessorTokens(Token token) {

        // tokenTracker.add(token);
        // tokenTypeTracker.add(token);

        // If this is a final state, add it to the final list

        if (token.isFinal()) {
            resultList.add(getResultListPredecessor(token));
            return;
        }

        // if this is a non-emitting token and we've already
        // visited the same state during this frame, then we
        // are in a grammar loop, so we don't continue to expand.
        // This check only works properly if we have kept all of the
        // tokens (instead of skipping the non-word tokens).
        // Note that certain linguists will never generate grammar loops
        // (lextree linguist for example). For these cases, it is perfectly
        // fine to disable this check by setting keepAllTokens to false

        if (!token.isEmitting() && (keepAllTokens && isVisited(token))) {
            return;
        }

        SearchState state = token.getSearchState();
        SearchStateArc[] arcs = state.getSuccessors();
        Token predecessor = getResultListPredecessor(token);

        // For each successor
        // calculate the entry score for the token based upon the
        // predecessor token score and the transition probabilities
        // if the score is better than the best score encountered for
        // the SearchState and frame then create a new token, add
        // it to the lattice and the SearchState.
        // If the token is an emitting token add it to the list,
        // otherwise recursively collect the new tokens successors.

        float tokenScore = token.getScore();
        float beamThreshold = activeList.getBeamThreshold();
        boolean stateProducesPhoneHmms = state instanceof LexTreeNonEmittingHMMState || state instanceof LexTreeWordState
                || state instanceof LexTreeEndUnitState;
        for (SearchStateArc arc : arcs) {
            SearchState nextState = arc.getState();

            // prune states using lookahead heuristics
            if (stateProducesPhoneHmms) {
                if (nextState instanceof LexTreeHMMState) {
                    Float penalty;
                    int baseId = ((LexTreeHMMState) nextState).getHMMState().getHMM().getBaseUnit().getBaseID();
                    if ((penalty = penalties.get(baseId)) == null)
                        penalty = updateLookaheadPenalty(baseId);
                    if ((tokenScore + lookaheadWeight * penalty) < beamThreshold)
                        continue;
                }
            }

            if (checkStateOrder) {
                checkStateOrder(state, nextState);
            }

            // We're actually multiplying the variables, but since
            // these come in log(), multiply gets converted to add
            float logEntryScore = tokenScore + arc.getProbability();

            Token bestToken = getBestToken(nextState);

            if (bestToken == null) {
                Token newBestToken = new Token(predecessor, nextState, logEntryScore, arc.getInsertionProbability(),
                        arc.getLanguageProbability(), currentCollectTime);
                tokensCreated.value++;
                setBestToken(newBestToken, nextState);
                activeListAdd(newBestToken);
            } else if (bestToken.getScore() < logEntryScore) {
                // System.out.println("Updating " + bestToken + " with " +
                // newBestToken);
                Token oldPredecessor = bestToken.getPredecessor();
                bestToken.update(predecessor, nextState, logEntryScore, arc.getInsertionProbability(),
                        arc.getLanguageProbability(), currentCollectTime);
                if (buildWordLattice && nextState instanceof WordSearchState) {
                    loserManager.addAlternatePredecessor(bestToken, oldPredecessor);
                }
            } else if (buildWordLattice && nextState instanceof WordSearchState) {
                if (predecessor != null) {
                    loserManager.addAlternatePredecessor(bestToken, predecessor);
                }
            }
        }
    }

    private Float updateLookaheadPenalty(int baseId) {
        if (ciScores.isEmpty())
            return 0.0f;
        float penalty = -Float.MAX_VALUE;
        for (FrameCiScores frameCiScores : ciScores) {
            float diff = frameCiScores.scores[baseId] - frameCiScores.maxScore;
            if (diff > penalty)
                penalty = diff;
        }
        penalties.put(baseId, penalty);
        return penalty;
    }

    private class FrameCiScores {
        public final float[] scores;
        public final float maxScore;

        public FrameCiScores(float[] scores, float maxScore) {
            this.scores = scores;
            this.maxScore = maxScore;
        }
    }

}
