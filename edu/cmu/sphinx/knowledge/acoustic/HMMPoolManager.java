
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
	logLikelihood = 0.0f;
    }

    /**
     * Create buffers for all pools used by the trainer in this pool manager.
     */
    protected void createBuffers() {
	// the option false or true refers to whether the buffer is in
	// log scale or not, true if it is.
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
     * @param index the current index into the TrainerScore vector
     * @param score the TrainerScore
     */
    protected void accumulate(int index, TrainerScore[] score) {
	accumulate(index, score, null);
    }

    /**
     * Accumulate the TrainerScore into the buffers.
     *
     * @param index the current index into the TrainerScore vector
     * @param score the TrainerScore for the current frame
     * @param nextScore the TrainerScore for the next time frame
     */
    protected void accumulate(int index, 
			      TrainerScore[] score, 
			      TrainerScore[] nextScore) {
	int senoneID;
	TrainerScore thisScore = score[index];

	Feature feature = thisScore.getFeature();
	// We should be doing this just once per utterance...
	currentLogLikelihood = thisScore.getLogLikelihood();

	HMMState state = thisScore.getState();
	if (state == null) {
	    // We only care about the case "all models"
	    senoneID = thisScore.getSenoneID();
	    if (senoneID == TrainerAcousticModel.ALL_MODELS) {
		accumulateMean(senoneID, score[index]);
		accumulateVariance(senoneID, score[index]);
		accumulateMixture(senoneID, score[index]);
		accumulateTransition(senoneID, index, score, nextScore);
	    }
	} else {
	    // If state is non-emitting, we presume there's only one
	    // transition out of it. Therefore, we only accumulate
	    // data for emitting states.
	    if (state.isEmitting()) {
		senoneID = senonePool.indexOf(state.getSenone());
		accumulateMean(senoneID, score[index]);
		accumulateVariance(senoneID, score[index]);
		accumulateMixture(senoneID, score[index]);
		accumulateTransition(senoneID, index, score, nextScore);
	    }
	}
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
     * @param indexScore the current index into the TrainerScore
     * @param score the score information
     * @param nextScore the score information for the next frame
     */
    private void accumulateStateTransition(int indexScore,
					   TrainerScore[] score, 
					   TrainerScore[] nextScore) {
	HMMState state = score[indexScore].getState();
	if (state == null) {
	    // Non-emitting state
	    return;
	}
	int indexState = state.getState();
	HMM hmm = state.getHMM();
	float[][] matrix = hmm.getTransitionMatrix();
	// Find the index for current matrix in the transition matrix pool
	int indexMatrix = matrixPool.indexOf(matrix);
	// Find the corresponding buffer
	Buffer[] bufferArray = 
	    (Buffer []) matrixBufferPool.get(indexMatrix);
	// Let's concentrate on the transitions *from* the current state
	float[] vector = matrix[indexState];

	for (int i = 0; i < vector.length; i++) {
	    // Make sure this is a valid transition
	    if (vector[i] != LogMath.getLogZero()) {
		// We're assuming that if the states have position "a"
		// and "b" in the HMM, they'll have positions "k+a"
		// and "k+b" in the graph, that is, their relative
		// position is the same.
		int dist = indexState - i;
		int indexNextScore = indexScore + dist;
		// Make sure the state is non-emitting (the last in
		// the HMM, or in the same HMM.
		assert ((nextScore[indexNextScore].getState() == null) || 
		     (nextScore[indexNextScore].getState().getHMM() == hmm));
		float alpha = score[indexScore].getAlpha();
		float beta = nextScore[indexNextScore].getBeta();
		float transitionProb = vector[i];
		float outputProb = nextScore[indexNextScore].getScore();
		float prob = alpha + beta + transitionProb + outputProb;
		prob -= currentLogLikelihood;
		// i is the index into the next state.
		bufferArray[indexState].logAccumulate(prob, i, logMath);
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
	// Find the transition matrix in this hmm
	float[][] matrix = hmm.getTransitionMatrix();
	// Find the vector with transitions from the current state to
	// other states.
	float[] stateVector = matrix[indexState];
	// Find the index of the current transition matrix in the
	// transition matrix pool.
	int indexMatrix = matrixPool.indexOf(matrix);
	// Find the buffer for the transition matrix.
	Buffer[] bufferArray = 
	    (Buffer []) matrixBufferPool.get(indexMatrix);
	// Accumulate for the transitions from current state
	for (int i = 0; i < stateVector.length; i++) {
	    // Make sure we're not trying to accumulate in an invalid
	    // transition.
	    if (stateVector[i] != logMath.getLogZero()) {
		bufferArray[indexState].logAccumulate(value, i, logMath);
	    }
	}
    }

    /**
     * Accumulate the transition probabilities.
     */
    private void accumulateTransition(int indexHmm, int indexScore,
				      TrainerScore[] score,
				      TrainerScore[] nextScore) {
	if (indexHmm == TrainerAcousticModel.ALL_MODELS) {
	    // Well, special case... we want to add an amount to all
	    // the states in all models
	    for (Iterator i = hmmManager.getIterator();
		 i.hasNext(); ) {
		HMM hmm = (HMM) i.next();
		for (int j = 0; j < hmm.getOrder(); j++) {
		    accumulateStateTransition(j, hmm, 
				      score[indexScore].getScore());
		}
	    }
	} else {
	    // For transition accumulation, we don't consider the last
	    // time frame, since there's no transition from there to
	    // anywhere...
	    if (nextScore != null) {
		accumulateStateTransition(indexScore, score, nextScore);
	    }
	}
    }

    /**
     * Update the log likelihood. This method should be called for
     * every utterance.
     */
    protected void updateLogLikelihood() {
	logLikelihood += currentLogLikelihood;
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
	    if (buffer.wasUsed()) {
		buffer.normalize();
	    }
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
	    if (buffer.wasUsed()) {
		buffer.logNormalize();
	    }
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
		if (bufferArray[j].wasUsed()) {
		    bufferArray[j].logNormalizeNonZero();
		}
	    }
	}
    }

    /** 
     * Update the models.
     */
    protected void update() {
	updateMeans();
	updateVariances();
	recomputeMixtureComponents();
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
	    if (buffer.wasUsed()) {
		float[] meansBuffer = (float [])buffer.getValues();
		copyVector(meansBuffer, means);
		means = (float [])meansPool.get(i);
	    }
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
	    if (buffer.wasUsed()) {
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
    }

    /**
     * Recompute the precomputed values in all mixture components.
     */
    private void recomputeMixtureComponents() {
	for (int i = 0; i < senonePool.size(); i++) {
	    GaussianMixture gMix = (GaussianMixture) senonePool.get(i);
	    MixtureComponent[] mixComponent = gMix.getMixtureComponents();
	    for (int j = 0; j < mixComponent.length; j++) {
		mixComponent[j].precomputeDistance();
	    }
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
	    if (buffer.wasUsed()) {
		if (buffer.logFloor(logMixtureWeightFloor)) {
		    buffer.logNormalizeToSum(logMath);
		}
		float[] mixtureWeightsBuffer = (float [])buffer.getValues();
		copyVector(mixtureWeightsBuffer, mixtureWeights);
	    }
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
		if (buffer.wasUsed()) {
		    for (int k = 0; k < matrix[j].length; k++) {
			float bufferValue = buffer.getValue(k);
			if (bufferValue != logMath.getLogZero()) {
			    assert matrix[j][k] != LogMath.getLogZero();
			    if (bufferValue < logTransitionProbabilityFloor) {
				buffer.setValue(k, 
					logTransitionProbabilityFloor);
			    }
			}
		    }
		    copyVector(buffer.getValues(), matrix[j]);
		}
	    }
	}
    }
}
