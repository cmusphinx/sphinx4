package edu.cmu.sphinx.decoder.adaptation;

public class Counts {

	private float[][][][] mean;
	private float[][][][] variance;
	private float[][][] dnom;
	private int[] veclen;
	private int numStates;
	private int numStreams;
	private int numGaussianSperState;
	private int pass2var;
	private String header;

	public Counts() {
		super();
		this.header = "";
	}

	public Counts(int[] vectorLength, int numStates, int numStreams,
			int numGaussiansPerState) {
		this.header = "";
		this.numStates = numStates;
		this.numStreams = numStreams;
		this.numGaussianSperState = numGaussiansPerState;
		this.veclen = vectorLength;
		this.dnom = new float[numStates][numStreams][numGaussiansPerState];
		this.mean = new float[numStates][numStreams][numGaussiansPerState][vectorLength[0]];
	}

	public float[][][][] getMean() {
		return mean;
	}

	public void setMean(float[][][][] mean) {
		this.mean = mean;
	}

	public float[][][][] getVariance() {
		return variance;
	}

	public void setVariance(float[][][][] variance) {
		this.variance = variance;
	}

	public float[][][] getDnom() {
		return dnom;
	}

	public void setDnom(float[][][] dnom) {
		this.dnom = dnom;
	}

	public int[] getVeclen() {
		return veclen;
	}

	public void setVeclen(int[] veclen) {
		this.veclen = veclen;
	}

	public int getNumStates() {
		return numStates;
	}

	public void setNumStates(int numStates) {
		this.numStates = numStates;
	}

	public int getNumStreams() {
		return numStreams;
	}

	public void setNumStreams(int numStreams) {
		this.numStreams = numStreams;
	}

	public int getNumGaussiansPerState() {
		return this.numGaussianSperState;
	}

	public void setNumGaussiansPerState(int numGaussiansPerState) {
		this.numGaussianSperState = numGaussiansPerState;
	}

	public int getPass2var() {
		return pass2var;
	}

	public void setPass2var(int pass2var) {
		this.pass2var = pass2var;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

}
