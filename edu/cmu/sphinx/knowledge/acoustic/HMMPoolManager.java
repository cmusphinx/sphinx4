
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
    private float logLikelihood;
    private float currentLogLikelihood;

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
	logLikelihood = LogMath.getLogZero();
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
	accumulate(score, null);
    }

    /**
     * Accumulate the TrainerScore into the buffers.
     *
     * @param score the TrainerScore
     * @param nextScore the TrainerScore for the next time frame
     */
    protected void accumulate(TrainerScore score, TrainerScore[] nextScore) {
	int senoneID;
	HMMState state = score.getState();
	if (state == null) {
	    senoneID = score.getSenoneID();
	} else {
	    senoneID = senonePool.indexOf(state.getSenone());
	}
	Feature feature = score.getFeature();
	// We should be doing this just once per utterance...
	currentLogLikelihood = score.getLogLikelihood();
	float prob = score.getScore();
	accumulateMean(senoneID, score);
	accumulateVariance(senoneID, score);
	accumulateMixture(senoneID, score);
	accumulateTransition(senoneID, score, nextScore);
    }

    /**
     * Accumulate the means.
     */
    private void accumulateMean(int senone, TrainerScore score) {
	if (senone == TrainerAcousticModel.ALL_MODELS) {
	    for (int i = 0; i < senonePool.size(); i++) {
		accumulateMean(i, score);
	    }
	} else {
	    GaussianMixture gaussian = 
		(GaussianMixture) senonePool.get(senone);
	    MixtureComponent[] mix = gaussian.getMixtureComponents();
	    for (int i = 0; i < mix.length; i++) {
		float[] mean = mix[i].getMean();
		int indexMean = meansPool.indexOf(mean);
		Buffer buffer = (Buffer) meansBufferPool.get(indexMean);
		float[] feature = score.getFeature().getFeatureData();
		float[] data = new float[feature.length];
		float prob = score.getComponentGamma()[i];
		prob -= currentLogLikelihood;
		prob = (float) logMath.logToLinear(prob);
		for (int j = 0; j < data.length; j++) {
		    data[j] = feature[j] * prob;
		}
		buffer.accumulate(data, prob);
	    }
	}
    }

    /**
     * Accumulate the variance.
     */
    private void accumulateVariance(int senone, TrainerScore score) {
	if (senone == TrainerAcousticModel.ALL_MODELS) {
	    for (int i = 0; i < senonePool.size(); i++) {
		accumulateVariance(i, score);
	    }
	} else {
	    GaussianMixture gaussian = 
		(GaussianMixture) senonePool.get(senone);
	    MixtureComponent[] mix = gaussian.getMixtureComponents();
	    for (int i = 0; i < mix.length; i++) {
		float[] mean = mix[i].getMean();
		float[] variance = mix[i].getVariance();
		int indexVariance = variancePool.indexOf(variance);
		Buffer buffer = 
		    (Buffer) varianceBufferPool.get(indexVariance);
		float[] feature = score.getFeature().getFeatureData();
		float[] data = new float[feature.length];
		float prob = score.getComponentGamma()[i];
		prob -= currentLogLikelihood;
		prob = (float) logMath.logToLinear(prob);
		for (int j = 0; j < data.length; j++) {
		    data[j] = (feature[j] - mean[j]);
		    data[j] *= data[j] * prob;
		}
		buffer.accumulate(data, prob);
	    }
	}
    }


    /**
     * Accumulate the mixture weights.
     */
    private void accumulateMixture(int senone, TrainerScore score) {
	// The index into the senone pool and the mixture weight pool
	// is the same
	if (senone == TrainerAcousticModel.ALL_MODELS) {
	    for (int i = 0; i < senonePool.size(); i++) {
		accumulateMixture(i, score);
	    }
	} else {
	    Buffer buffer = (Buffer) mixtureWeightsBufferPool.get(senone);
	    float[] mixw = (float [])mixtureWeightsPool.get(senone);
	    for (int i = 0; i < mixw.length; i++) {
		float prob = score.getComponentGamma()[i];
		prob -= currentLogLikelihood;
		buffer.logAccumulate(prob, i, logMath);
	    }
	}
    }

    /**
     * Accumulate transitions from a given state.
     *
     * @param indexState the state index
     * @param score the score information
     */
    private void accumulateStateTransition(int indexState, TrainerScore score, 
					   TrainerScore[] nextScore) {
	HMMState state = score.getState();
	HMM hmm = state.getHMM();
	float[][] matrix = hmm.getTransitionMatrix();
	int indexMatrix = matrixPool.indexOf(matrix);
	Buffer[] bufferArray = 
	    (Buffer []) matrixBufferPool.get(indexMatrix);
	for (int i = 0; i < nextScore.length; i++) {
	    HMMState nextState = nextScore[i].getState();
	    int indexNextState = nextState.getState();
	    HMM nextHmm = nextState.getHMM();
	    if (hmm != nextHmm) {
		continue;
	    }
	    if (matrix[indexState][indexNextState] != 
		logMath.getLogZero()) {
		float alpha = score.getAlpha();
		float beta = nextScore[i].getBeta();
		float transitionProb = 
		    hmm.getTransitionProbability(indexState, 
						 indexNextState);
		float outputProb = nextScore[i].getScore();
		float prob = alpha + beta + transitionProb + outputProb;
		prob -= currentLogLikelihood;
		bufferArray[indexState].
		    logAccumulate(prob, indexNextState, logMath);
	    }
	}
    }

    /**
     * Accumulate transitions from a given state.
     *
     * @param indexState the state index
     * @param hmm the HMM
     * @param value the value to accumulate
     */
    private void accumulateStateTransition(int indexState, HMM hmm, 
					   float value) {
	float[][] matrix = hmm.getTransitionMatrix();
	float[] stateVector = matrix[indexState];
	int indexMatrix = matrixPool.indexOf(matrix);
	Buffer[] bufferArray = 
	    (Buffer []) matrixBufferPool.get(indexMatrix);
	for (int i = 0; i < stateVector.length; i++) {
	    if (stateVector[i] != logMath.getLogZero()) {
		bufferArray[indexState].logAccumulate(value, i, logMath);
	    }
	}
    }

    /**
     * Accumulate the transition probabilities.
     */
    private void accumulateTransition(int indexHmm, TrainerScore score,
				      TrainerScore[] nextScore) {
	if (indexHmm == TrainerAcousticModel.ALL_MODELS) {
	    // Well, special case... we want to add an amount to all
	    // the states in all models
	    for (Iterator i = hmmManager.getIterator();
		 i.hasNext(); ) {
		HMM hmm = (HMM) i.next();
		for (int j = 0; j < hmm.getOrder(); j++) {
		    accumulateStateTransition(j, hmm, score.getScore());
		}
	    }
	} else {
	    HMMState state = score.getState();
	    accumulateStateTransition(state.getState(), score, nextScore);
	}
    }

    /** 
     * Normalize the buffers.
     *
     * @return the log likelihood associated with the current training set
     */
    protected float normalize() {
	normalizePool(meansBufferPool);
	normalizePool(varianceBufferPool);
	logNormalizePool(mixtureWeightsBufferPool);
	logNormalize2DPool(matrixBufferPool);
	return logLikelihood;
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
