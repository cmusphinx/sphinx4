
/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.knowledge.acoustic;

import java.util.HashMap;
import java.util.logging.Logger;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.LogMath;


/**
 * Manages the HMM pools.
 */
class HMMPoolManager {

    private HMMManager hmmManager;
    private Pool meansPool;
    private Pool variancePool;
    private Pool matrixPool;
    private Pool mixtureWeightsPool;
    private Pool meanTransformationMatrixPool;
    private Pool meanTransformationVectorPool;
    private Pool varianceTransformationMatrixPool;
    private Pool varianceTransformationVectorPool;

    private Pool meansBufferPool;
    private Pool varianceBufferPool;
    private Pool matrixBufferPool;
    private Pool mixtureWeightsBufferPool;

    private Pool senonePool;
    private int vectorLength;
    private LogMath logMath;

    private float logMixtureWeightFloor;
    private float logTransitionProbabilityFloor;
    private float varianceFloor;

    /**
     * Constructor for this pool manager.
     * It gets the pointers to the pools from a loader.
     *
     * @param loader the loader
     */
    protected HMMPoolManager(Loader loader, SphinxProperties props) {
        hmmManager = loader.getHMMManager();
	meansPool = loader.getMeansPool();
	variancePool = loader.getVariancePool();
	mixtureWeightsPool = loader.getMixtureWeightPool();
	matrixPool = loader.getTransitionMatrixPool();
	senonePool = loader.getSenonePool();
	logMath = LogMath.getLogMath(props.getContext());
        float mixtureWeightFloor = 
	    props.getFloat(AcousticModel.PROP_MW_FLOOR, 
			   AcousticModel.PROP_MW_FLOOR_DEFAULT);
	logMixtureWeightFloor = logMath.linearToLog(mixtureWeightFloor);
        float transitionProbabilityFloor = 
	    props.getFloat(AcousticModel.PROP_TP_FLOOR, 
			   AcousticModel.PROP_TP_FLOOR_DEFAULT);
	logTransitionProbabilityFloor = 
	    logMath.linearToLog(transitionProbabilityFloor);
        varianceFloor = 
	    props.getFloat(AcousticModel.PROP_VARIANCE_FLOOR, 
			   AcousticModel.PROP_VARIANCE_FLOOR_DEFAULT);

	createBuffers();
    }

    /**
     * Create buffers for all pools used by the trainer in this pool manager.
     */
    protected void createBuffers() {
	meansBufferPool = create1DPoolBuffer(meansPool, false);
	varianceBufferPool = create1DPoolBuffer(variancePool, false);
	matrixBufferPool = create2DPoolBuffer(matrixPool, true);
	mixtureWeightsBufferPool = create1DPoolBuffer(mixtureWeightsPool, 
						      true);
    }

    /**
     * Create buffers for a given pool.
     */
    private Pool create1DPoolBuffer(Pool pool, boolean isLog) {
	Pool bufferPool = new Pool(pool.getName());

	for (int i = 0; i < pool.size(); i++) {
	    float[] element = (float [])pool.get(i);
	    Buffer buffer = new Buffer(element.length, isLog);
	    bufferPool.put(i, buffer);
	}
	return bufferPool;
    }

    /**
     * Create buffers for a given pool.
     */
    private Pool create2DPoolBuffer(Pool pool, boolean isLog) {
	Pool bufferPool = new Pool(pool.getName());

	for (int i = 0; i < pool.size(); i++) {
	    float[][] element = (float [][])pool.get(i);
	    int poolSize = element.length;
	    Buffer[] bufferArray = new Buffer[poolSize];
	    for (int j = 0; j < poolSize; j++) {
		bufferArray[j] = new Buffer(element[j].length, isLog);
	    }
	    bufferPool.put(i, bufferArray);
	}
	return bufferPool;
    }

    /**
     * Accumulate the TrainerScore into the buffers.
     *
     * @param score the TrainerScore
     */
    protected void accumulate(TrainerScore score) {
	int senoneID = score.getSenoneID();
	Feature feature = score.getFeature();
	float prob = score.getScore();
	accumulateMean(senoneID, feature, prob);
	accumulateVariance(senoneID, feature, prob);
	accumulateMixture(senoneID, prob);
	accumulateTransition(senoneID, prob);
    }

    /**
     * Accumulate the means.
     */
    private void accumulateMean(int senone, Feature feature, float logProb) {
	if (senone == TrainerAcousticModel.ALL_MODELS) {
	    for (int i = 0; i < meansBufferPool.size(); i++) {
		accumulateMean(i, feature, logProb);
	    }
	} else {
	    // TODO: case where we have > 1 gaussians: senone isn't
	    // enough, we need to add info to the TrainerScore
	    Buffer buffer = (Buffer) meansBufferPool.get(senone);
	    float[] data = feature.getFeatureData();
	    float prob = (float) logMath.logToLinear(logProb);
	    buffer.accumulate(data, prob);
	}
    }

    /**
     * Accumulate the variance.
     */
    private void accumulateVariance(int senone, Feature feature, 
				    float logProb) {
	if (senone == TrainerAcousticModel.ALL_MODELS) {
	    for (int i = 0; i < varianceBufferPool.size(); i++) {
		accumulateVariance(i, feature, logProb);
	    }
	} else {
	    // TODO: case where we have > 1 gaussians: senone isn't
	    // enough, we need to add info to the TrainerScore
	    Buffer buffer = (Buffer) varianceBufferPool.get(senone);
	    float prob = (float) logMath.logToLinear(logProb);
	    float[] data = feature.getFeatureData();
	    float[] dataSquared = new float[data.length];
	    for (int i = 0; i < data.length; i++) {
		dataSquared[i] = data[i] * data[i];
	    }
	    buffer.accumulate(dataSquared, prob);
	}
    }

    /**
     * Accumulate the mixture weights.
     */
    private void accumulateMixture(int senone, float prob) {
	if (senone == TrainerAcousticModel.ALL_MODELS) {
	    for (int i = 0; i < mixtureWeightsBufferPool.size(); i++) {
		accumulateMixture(i, prob);
	    }
	} else {
	    Buffer buffer = (Buffer) mixtureWeightsBufferPool.get(senone);
	    float[] mixw = (float [])mixtureWeightsPool.get(senone);
	    for (int i = 0; i < mixw.length; i++) {
		buffer.logAccumulate(prob, i, logMath);
	    }
	}
    }

    /**
     * Accumulate the transition probabilities.
     */
    private void accumulateTransition(int senone, float prob) {
	if (senone == TrainerAcousticModel.ALL_MODELS) {
	    for (int i = 0; i < matrixBufferPool.size(); i++) {
		accumulateTransition(i, prob);
	    }
	} else {
	    Buffer[] bufferArray = (Buffer []) matrixBufferPool.get(senone);
	    float[][] matrix = (float [][])matrixPool.get(senone);
	    for (int i = 0; i < bufferArray.length; i++) {
		for (int j = 0; j < bufferArray.length; j++) {
		    if (matrix[i][j] != logMath.getLogZero()) {
			bufferArray[i].logAccumulate(prob, j, logMath);
		    }
		}
	    }
	}
    }

    /** 
     * Normalize the buffers.
     */
    protected void normalize() {
	normalizePool(meansBufferPool);
	normalizePool(varianceBufferPool);
	logNormalizePool(mixtureWeightsBufferPool);
	logNormalize2DPool(matrixBufferPool);
   }

    /**
     * Normalize a single buffer pool.
     *
     * @param pool the buffer pool to normalize
     */
    private void normalizePool(Pool pool) {
	assert pool != null;
	for (int i = 0; i < pool.size(); i++) {
	    Buffer buffer = (Buffer)pool.get(i);
	    buffer.normalize();
	}
    }

    /**
     * Normalize a single buffer pool in log scale.
     *
     * @param pool the buffer pool to normalize
     */
    private void logNormalizePool(Pool pool) {
	assert pool != null;
	for (int i = 0; i < pool.size(); i++) {
	    Buffer buffer = (Buffer)pool.get(i);
	    buffer.logNormalize();
	}
    }

    /**
     * Normalize a single buffer pool in log scale.
     *
     * @param pool the buffer pool to normalize
     */
    private void logNormalize2DPool(Pool pool) {
	assert pool != null;
	for (int i = 0; i < pool.size(); i++) {
	    Buffer[] bufferArray = (Buffer []) pool.get(i);
	    for (int j = 0; j < bufferArray.length; j++) {
		bufferArray[j].logNormalizeNonZero();
	    }
	}
    }

    /** 
     * Update the models.
     */
    protected void update() {
	updateMeans();
	updateVariances();
	updateMixtureWeights();
	updateTransitionMatrices();
    }

    /**
     * Copy one vector onto another.
     *
     * @param in the source vector
     * @param out the destination vector
     */
    private void copyVector(float[] in, float out[]) {
	assert in.length == out.length;
	for (int i = 0; i < in.length; i++) {
	    out[i] = in[i];
	}
    }

    /**
     * Update the means.
     */
    private void updateMeans() {
	assert meansPool.size() == meansBufferPool.size();
	for (int i = 0; i < meansPool.size(); i++) {
	    float[] means = (float [])meansPool.get(i);
	    Buffer buffer = (Buffer) meansBufferPool.get(i);
	    float[] meansBuffer = (float [])buffer.getValues();
	    copyVector(meansBuffer, means);
	}
    }

    /**
     * Update the variances.
     */
    private void updateVariances() {
	assert variancePool.size() == varianceBufferPool.size();
	for (int i = 0; i < variancePool.size(); i++) {
	    float[] means = (float [])meansPool.get(i);
	    float[] variance = (float [])variancePool.get(i);
	    Buffer buffer = (Buffer) varianceBufferPool.get(i);
	    float[] varianceBuffer = (float [])buffer.getValues();
	    assert means.length == varianceBuffer.length;
	    for (int j = 0; j < means.length; j++) {
		varianceBuffer[j] -= means[j] * means[j];
		if (varianceBuffer[j] < varianceFloor) {
		    varianceBuffer[j] = varianceFloor;
		}
	    }
	    copyVector(varianceBuffer, variance);
	}
    }

    /**
     * Update the mixture weights.
     */
    private void updateMixtureWeights() {
	assert mixtureWeightsPool.size() == mixtureWeightsBufferPool.size();
	for (int i = 0; i < mixtureWeightsPool.size(); i++) {
	    float[] mixtureWeights = (float [])mixtureWeightsPool.get(i);
	    Buffer buffer = (Buffer) mixtureWeightsBufferPool.get(i);
	    if (buffer.logFloor(logMixtureWeightFloor)) {
		buffer.logNormalizeToSum(logMath);
	    }
	    float[] mixtureWeightsBuffer = (float [])buffer.getValues();
	    copyVector(mixtureWeightsBuffer, mixtureWeights);
	}
    }

    /**
     * Update the transition matrices.
     */
    private void updateTransitionMatrices() {
	assert matrixPool.size() == matrixBufferPool.size();
	for (int i = 0; i < matrixPool.size(); i++) {
	    float[][] matrix = (float [][])matrixPool.get(i);
	    Buffer[] bufferArray = (Buffer []) matrixBufferPool.get(i);
	    for (int j = 0; j < matrix.length; j++) {
		Buffer buffer = bufferArray[j];
		for (int k = 0; k < matrix[j].length; k++) {
		    float bufferValue = buffer.getValue(k);
		    if (bufferValue != logMath.getLogZero()) {
			if (bufferValue < logTransitionProbabilityFloor) {
			    buffer.setValue(k, logTransitionProbabilityFloor);
			}
		    }
		}
		copyVector(buffer.getValues(), matrix[j]);
	    }
	}
    }
}
