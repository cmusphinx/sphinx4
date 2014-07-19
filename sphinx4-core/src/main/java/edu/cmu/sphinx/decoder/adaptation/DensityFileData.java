package edu.cmu.sphinx.decoder.adaptation;

import static edu.cmu.sphinx.linguist.acoustic.tiedstate.Pool.Feature.NUM_GAUSSIANS_PER_STATE;
import static edu.cmu.sphinx.linguist.acoustic.tiedstate.Pool.Feature.NUM_SENONES;
import static edu.cmu.sphinx.linguist.acoustic.tiedstate.Pool.Feature.NUM_STREAMS;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.*;
import edu.cmu.sphinx.util.Utilities;

public class DensityFileData {

	private Sphinx3Loader loader;
	private String filePath;
	private float varFloor;
	private Properties props;
	private DataInputStream dis;
	private String version;
	private String checksum;
	private int numStates;
	private int numStreams;
	private int numGaussiansPerState;
	private int[] vectorLength;
	Pool<float[]> pool;
	
	
	public DensityFileData(String filePath, float varFloor, Sphinx3Loader loader) throws IOException, URISyntaxException {
		super();
		this.filePath = filePath;
		this.varFloor = varFloor;
		this.loader = loader;
		this.loadFile();
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


	public int[] getVectorLength() {
		return vectorLength;
	}


	public Pool<float[]> getPool() {
		return pool;
	}


	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}


	public void setVarFloor(float varFloor) {
		this.varFloor = varFloor;
	}


	public void loadFile() throws IOException, URISyntaxException{
	        int blockSize = 0, rawLength;

	        this.props = new Properties();
	        this.dis = loader.readS3BinaryHeader(this.filePath, props);
	        this.version = props.getProperty("version");
	        this.checksum = props.getProperty("chksum0");
	        this.numStates = loader.readInt(dis);
	        this.numStreams = loader.readInt(dis);
	        this.numGaussiansPerState = loader.readInt(dis);
	        this.vectorLength = new int[this.numStreams];
	 
	        for (int i = 0; i < numStreams; i++) {
	            vectorLength[i] = loader.readInt(dis);
	        }

	        rawLength = loader.readInt(dis);

	        for (int i = 0; i < numStreams; i++) {
	            blockSize += vectorLength[i];
	        }

	        pool = new Pool<float[]>(this.filePath);
	        pool.setFeature(NUM_SENONES, numStates);
	        pool.setFeature(NUM_STREAMS, numStreams);
	        pool.setFeature(NUM_GAUSSIANS_PER_STATE, numGaussiansPerState);

	        for (int i = 0; i < numStates; i++) {
	            for (int j = 0; j < numStreams; j++) {
	                for (int k = 0; k < numGaussiansPerState; k++) {
	                    float[] density = loader.readFloatArray(dis, vectorLength[j]);
	                    Utilities.floorData(density, this.varFloor);
	                    pool.put(i * numStreams * numGaussiansPerState + j
	                            * numGaussiansPerState + k, density);
	                }
	            }
	        }

	        dis.close();
	}
	
	
}
