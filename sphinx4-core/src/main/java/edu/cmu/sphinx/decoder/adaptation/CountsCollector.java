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
	private int[] vectorLength;
	private int numStates;
	private int numStreams;
	private int numGaussiansPerState;
	private LogMath logMath;

	public Counts getCounts() {
		return counts;
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
	}

	public float computeTotalScore(float[] scores) {
		float totalScore = LogMath.LOG_ZERO;

		for (int i = 0; i < scores.length; i++) {
			totalScore = logMath.addAsLinear(totalScore, scores[i]);
		}

		return totalScore;
	}

	public float[] calculateComponentScore(FloatData feature, HMMState state) {
		MixtureComponent[] mc = state.getMixtureComponents();
		float[] mw = state.getLogMixtureWeights();
		float[] featureVector = FloatData.toFloatData(feature).getValues();
		float[] logComponentScore = new float[mc.length];

		for (int i = 0; i < mc.length; i++) {
			logComponentScore[i] = mc[i].getScore(featureVector) + mw[i];
		}

		return logComponentScore;
	}

	public void addCounts(float[][][] denominatorArray, float[][][][] meansArray) {
		float[][][] dnom;
		float[][][][] means;

		dnom = counts.getDnom();
		means = counts.getMean();
		for (int i = 0; i < numStates; i++) {
			for (int j = 0; j < numStreams; j++) {
				for (int k = 0; k < numGaussiansPerState; k++) {
					dnom[i][j][k] += denominatorArray[i][j][k];
					for (int l = 0; l < vectorLength[0]; l++) {
						means[i][j][k][l] += meansArray[i][j][k][l];
					}
				}
			}
		}

		counts.setDnom(dnom);
		counts.setMean(means);
	}

	public void collect(Result result) throws Exception {
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
			FloatData feature = (FloatData) token.getData();

			if(!(token.getSearchState() instanceof HMMState)){
				token = token.getPredecessor();
				continue;
			}
			
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


		} while (token != null);

		this.addCounts(dnom, means);
	}
}
