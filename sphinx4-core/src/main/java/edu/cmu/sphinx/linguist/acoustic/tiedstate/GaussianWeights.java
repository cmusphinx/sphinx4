/*
* Copyright 2014 Carnegie Mellon University.
* All Rights Reserved.  Use is subject to license terms.
*
* See the file "license.terms" for information on usage and
* redistribution of this file, and for a DISCLAIMER OF ALL
* WARRANTIES.
*
*/

package edu.cmu.sphinx.linguist.acoustic.tiedstate;

import java.util.logging.Logger;

/** Structure to store weights for all gaussians in AM. 
 * Supposed to provide faster access in case of large models */
public class GaussianWeights {

    private final float[][] weights;
    private final int numStates;
    private final int gauPerState;
    private final int numStreams;
    private final String name;

    public GaussianWeights(String name, int numStates, int gauPerState, int numStreams) {
        this.numStates = numStates;
        this.gauPerState = gauPerState;
        this.numStreams = numStreams;
        this.name = name;
        weights = new float[gauPerState][numStates * numStreams];
    }
    
    public void put(int stateId, int streamId, float[] gauWeights) {
        assert gauWeights.length == gauPerState;
        for (int i = 0; i < gauPerState; i++)
            weights[i][stateId * numStreams + streamId] = gauWeights[i];
    }
    
    public float get(int stateId, int streamId, int gaussianId) {
        return weights[gaussianId][stateId * numStreams + streamId];
    }
    
    public int getStatesNum() {
        return numStates;
    }
    
    public int getGauPerState() {
        return gauPerState;
    }
    
    public int getStreamsNum() {
        return numStreams;
    }
    
    public String getName() {
        return name;
    }
    
    public void logInfo(Logger logger) {
        logger.info("Gaussian weights: " + name + ". Entries: " + numStates * numStreams);
    }
    
    public Pool<float[]> convertToPool() {
        return null;
    }
}
