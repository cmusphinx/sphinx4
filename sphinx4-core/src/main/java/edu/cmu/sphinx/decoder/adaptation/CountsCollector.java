package edu.cmu.sphinx.decoder.adaptation;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.LogMath;

public class CountsCollector {

	private Counts counts;
	private int[] vectorLength;
	private int numStates;
	private int numStreams;
	private int numGaussiansPerState;
	private LogMath logMath;

	public Counts getCounts() {
		return counts;
	}
	
	public int[] getVectorLength() {
		return vectorLength;
	}

	public int getNumStates() {
		return numStates;
	}

	public int getNumStreams() {
		return numStreams;
	}

	public int getNumGaussiansPerState() {
		return numGaussiansPerState;
	}

	public CountsCollector(int[] vectorLength, int numStates, int numStreams,
			int numGaussiansPerState) {
		super();
		this.vectorLength = vectorLength;
		this.numStates = numStates;
		this.numStreams = numStreams;
		this.numGaussiansPerState = numGaussiansPerState;
		this.counts = new Counts(vectorLength, numStates, numStreams,
				numGaussiansPerState);
		this.logMath = LogMath.getInstance();
	}

	public float[] calculateComponentScore(FloatData feature,
			HMMSearchState state) {
		MixtureComponent[] mc = state.getHMMState().getMixtureComponents();
		float[] mw = state.getHMMState().getLogMixtureWeights();
		float[] featureVector = FloatData.toFloatData(feature).getValues();
		float[] logComponentScore = new float[mc.length];

		for (int i = 0; i < mc.length; i++) {
			logComponentScore[i] = mc[i].getScore(featureVector) + mw[i];
		}

		return logComponentScore;
	}

	public float[] computePosterios(float[] componentScores) {
		float max;
		float[] posteriors = componentScores;

		max = posteriors[0];
		
		for (int i = 1; i < componentScores.length; i++) {
			if (posteriors[i] > max){
				max = posteriors[i];
			}
		}

		for (int i = 0; i < componentScores.length; i++) {
			posteriors[i] = (float) logMath.logToLinear(posteriors[i] - max);
		}

		return posteriors;
	}

	public void collect(Result result) throws Exception {
		Token token = result.getBestToken();
		HMMSearchState state;
		float[] componentScore, featureVector, posteriors;
		int mId;

		if (token == null)
			throw new Exception("Best token not found!");

		do {
			FloatData feature = (FloatData) token.getData();
			SearchState ss = token.getSearchState();

			if (!(ss instanceof HMMSearchState && ss.isEmitting())) {
				token = token.getPredecessor();
				continue;
			}

			state = (HMMSearchState) token.getSearchState();
			componentScore = this.calculateComponentScore(feature, state);
			featureVector = FloatData.toFloatData(feature).getValues();
			mId = (int) state.getHMMState().getMixtureId();
			posteriors = this.computePosterios(componentScore);

			for (int i = 0; i < componentScore.length; i++) {
				counts.getDnom()[mId][0][i] += posteriors[i];
				for (int j = 0; j < featureVector.length; j++) {
					counts.getMean()[mId][0][i][j] += posteriors[i] * featureVector[j];
				}
			}

			token = token.getPredecessor();
		} while (token != null);

	}
}
