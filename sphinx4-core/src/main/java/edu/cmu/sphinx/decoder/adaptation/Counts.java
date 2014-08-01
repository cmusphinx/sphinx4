package edu.cmu.sphinx.decoder.adaptation;

public class Counts {
	
	private float[][][][] mean;
	private float[][][][] variance;
	private float[][][] dnom;
	private int[] veclen;
	private int nCb;
	private int nFeat;
	private int nDensity;
	private int pass2var;
	private String header;
	
	public Counts() {
		super();
		this.header="";
	}

	public Counts(int[] vectorLength, int numStates, int numStreams,
			int numGaussiansPerState) {
		this.header="";
		this.nCb = numStates;
		this.nFeat = numStreams;
		this.nDensity = numGaussiansPerState;
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

	public int getnCb() {
		return nCb;
	}

	public void setnCb(int nCb) {
		this.nCb = nCb;
	}

	public int getnFeat() {
		return nFeat;
	}

	public void setnFeat(int nFeat) {
		this.nFeat = nFeat;
	}

	public int getnDensity() {
		return nDensity;
	}

	public void setnDensity(int nDensity) {
		this.nDensity = nDensity;
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
