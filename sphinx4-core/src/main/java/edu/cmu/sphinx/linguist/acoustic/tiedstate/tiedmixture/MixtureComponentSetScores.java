/*
 * Copyright 2014 Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.linguist.acoustic.tiedstate.tiedmixture;

/**
 * Class to keep scores of mixture components for certain frame.
 * Is use useful in case of fast match to avoid scoring gaussians twice
 */
public class MixtureComponentSetScores {

    private float[][] scores; //scores[featureStreamIdx][gaussianIndex]
    private int[][] ids;       //id[featureStreamIdx][gaussianIndex]
    private long frameStartSample;
    
    public MixtureComponentSetScores(int numStreams, int gauNum, long frameStartSample) {
        scores = new float[numStreams][gauNum];
        ids = new int[numStreams][gauNum];
        this.frameStartSample = frameStartSample;
    }
    
    public void setScore(int featStream, int gauIdx, float score) {
        scores[featStream][gauIdx] = score;
    }
    
    public void setGauId(int featStream, int gauIdx, int id) {
        ids[featStream][gauIdx] = id;
    }
    
    public float getScore(int featStream, int gauIdx) {
        return scores[featStream][gauIdx];
    }
    
    public int getGauId(int featStream, int gauIdx) {
        return ids[featStream][gauIdx];
    }
    
    public long getFrameStartSample() {
        return frameStartSample;
    }
    
    
}
