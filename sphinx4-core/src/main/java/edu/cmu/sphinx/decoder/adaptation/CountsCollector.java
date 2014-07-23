package edu.cmu.sphinx.decoder.adaptation;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.LogMath;

public class CountsCollector {

	private Counts counts;
	private Result result;
	private int[] vectorLength;
	private int numStates;
	private int numStreams;
	private int numGaussiansPerState;
    private LogMath logMath;


	public CountsCollector(int[] vectorLength, int numStates, int numStreams,
			int numGaussiansPerState, Result result) {
		super();
		this.vectorLength = vectorLength;
		this.numStates = numStates;
		this.numStreams = numStreams;
		this.numGaussiansPerState = numGaussiansPerState;
		this.result = result;
	}

	public Counts getCounts() {
		return counts;
	}

	public float computeTotalScore(float[] scores) {
		float totalScore = LogMath.LOG_ZERO;

		for (int i = 0; i < scores.length; i++) {
			totalScore = logMath.addAsLinear(totalScore, scores[i]);
		}

		return totalScore;
	}

	public float[] calculateComponentScore(DoubleData feature, HMMState state) {
		MixtureComponent[] mc = state.getMixtureComponents();
		float[] mw = state.getLogMixtureWeights();
		float[] featureVector = FloatData.toFloatData(feature).getValues();
		float[] logComponentScore = new float[mc.length];

		for (int i = 0; i < mc.length; i++) {
			logComponentScore[i] = mc[i].getScore(featureVector) + mw[i];
		}

		return logComponentScore;
	}

	public float computeLmr(HMMState searchState) {
		searchState.getState();
		return 0;
	}

	public void collect() throws Exception {
		Token token = result.getBestToken();
		HMMState state;
		float[] componentScore, featureVector;
		float totalScore, dn;
		int mId;

		if (token == null)
			throw new Exception("Best token not found!");

		float[][][] dnom;
		float[][][][] means;

		dnom = new float[numStates][numStreams][numGaussiansPerState];
		means = new float[numStates][numStreams][numGaussiansPerState][vectorLength[0]];

		do {
			DoubleData feature = (DoubleData) token.getData();
			state = (HMMState) token.getSearchState();
			componentScore = this.calculateComponentScore(feature, state);
			totalScore = this.computeTotalScore(componentScore);
			featureVector = FloatData.toFloatData(feature).getValues();
			mId = (int) state.getMixtureId();

			for (int i = 0; i < componentScore.length; i++) {
				dn = dnom[mId][0][i] = componentScore[i] / totalScore;
				for (int j = 0; j < featureVector.length; j++) {
					means[mId][0][i][j] = dn * featureVector[j];
				}
			}

			token = token.getPredecessor();
		} while (token != null);

	}
}
