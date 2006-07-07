
/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.linguist.acoustic.tiedstate;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.util.LogMath;

/**
 * Defines the set of shared elements for a GaussianMixture. Since
 * these elements are potentially shared by a number of
 * {@link GaussianMixture GaussianMixtures},
 * these elements should not be written to. The
 * GaussianMixture defines a single probability density function along
 * with a set of adaptation parameters.
 * <p>
 * Note that all scores and weights are in LogMath log base
 */
 // [[[ QFSE: Since many of the subcomponents of a
 // MixtureComponent are shared, are there some potential
 // opportunities to reduce the number of computations in scoring
 // senones by sharing intermediate results for these subcomponents?
 //  ]]]

public class MixtureComponent  {
    private float[]   mean;
    /**
     * Mean after transformed by the adaptation parameters.
     */
    private float[]   meanTransformed;
    private float[][] meanTransformationMatrix;
    private float[]   meanTransformationVector;
    private float[]   variance;
    private float varianceFloor;
    /**
     * Precision is the inverse of the variance. This includes adaptation.
     */
    private float[]   precisionTransformed;
    private float[][] varianceTransformationMatrix;
    private float[]   varianceTransformationVector;

    private float distFloor = -Float.MAX_VALUE;  
    private float logPreComputedGaussianFactor;
    private LogMath logMath;


    /**
     * Create a MixtureComponent with the given sub components.
     *
     * @param logMath	the log math to use
     * @param mean	the mean vector for this PDF
     * @param meanTransformationMatrix transformation matrix for this pdf
     * @param meanTransformationVector transform vector for this PDF
     * @param variance  the variance for this PDF
     * @param varianceTransformationMatrix  var. transform matrix for this PDF
     * @param varianceTransformationVector var. transform vector for this PDF
     * @param distFloor the lowest score value
     * @param varianceFloor the lowest value for the variance
     */
    public MixtureComponent(
	LogMath logMath,
	float[]   mean,
	float[][] meanTransformationMatrix,
	float[]   meanTransformationVector,
	float[]   variance,
	float[][] varianceTransformationMatrix,
	float[]   varianceTransformationVector,
	float distFloor,
	float varianceFloor)  {

	assert variance.length == mean.length;

	this.logMath = logMath;
	this.mean = mean;
	this.meanTransformationMatrix = meanTransformationMatrix;
	this.meanTransformationVector = meanTransformationVector;
	this.variance = variance;
	this.varianceTransformationMatrix = varianceTransformationMatrix;
	this.varianceTransformationVector = varianceTransformationVector;
	this.varianceFloor = varianceFloor;

	logPreComputedGaussianFactor = precomputeDistance();
    }

    /**
     * Returns the mean for this component.
     *
     * @return the mean
     */
    public float[] getMean(){
	return mean;
    }

    /**
     * Returns the variance for this component.
     *
     * @return the variance
     */
    public float[] getVariance(){
	return variance;
    }

    /**
     * Calculate the score for this mixture against the given
     * feature. We model the output distributions using a mixture of
     * Gaussians, therefore the current implementation is simply the
     * computation of a multi-dimensional Gaussian.
     *
     * <p><b>Normal(x) = exp{-0.5 * (x-m)' * inv(Var) * (x-m)} /
     * {sqrt((2 * PI) ^ N) * det(Var))}</b></p>
     *
     * where <b>x</b> and <b>m</b> are the incoming cepstra and mean
     * vector respectivally, <b>Var</b> is the Covariance matrix,
     * <b>det()</b> is the determinant of a matrix, <b>inv()</b> is
     * its inverse, <b>exp</b> is the exponential operator, <b>x'</b>
     * is the transposed vector of <b>x</b> and <b>N</b> is the
     * dimension of the vectors <b>x</b> and <b>m</b>.
     *
     * @param feature the feature to score
     *
     * @return the score, in log, for the given feature
     */
    public float getScore(Data feature) {

        float[] data = ((FloatData) feature).getValues();
	 // float logVal = 0.0f;
	 float logDval = 0.0f;


	 // First, compute the argument of the exponential function in
	 // the definition of the Gaussian, then convert it to the
	 // appropriate base. If the log base is <code>Math.E</code>,
	 // then no operation is necessary.

	 for (int i = 0; i < data.length; i++) {
	     float logDiff = data[i] - meanTransformed[i];
	     logDval += logDiff * logDiff * precisionTransformed[i];
	 }
	 // logDval = -logVal / 2;

	 // At this point, we have the ln() of what we need, that is,
	 // the argument of the exponential in the javadoc comment.

	 // Convert to the appropriate base.
	 logDval = logMath.lnToLog(logDval);

	 // Add the precomputed factor, with the appropriate sign.
	 logDval -= logPreComputedGaussianFactor;


	 // System.out.println("MC: getscore " + logDval);

	 // TODO: Need to use mean and variance transforms here

	 if (Float.isNaN(logDval)) {
	     System.out.println("gs is Nan, converting to 0");
	     logDval = LogMath.getLogZero();
	 }

	 if (logDval < distFloor) {
	     logDval = distFloor;
	 }

	 return logDval;
     }


     /**
      * Pre-compute factors for the Mahalanobis distance. Some of the
      * Mahalanobis distance computation can be carried out in
      * advance. Especifically, the factor containing only variance in
      * the Gaussian can be computed in advance, keeping in mind that
      * the the determinant of the covariance matrix, for the
      * degenerate case of a mixture with independent components -
      * only the diagonal elements are non-zero - is simply the
      * product of the diagonal elements.
      *
      * We're computing the expression:
      *
      * <p><b>{sqrt((2 * PI) ^ N) * det(Var))}</b></p>
      *
      * @return the precomputed distance
      */
     public float precomputeDistance() {
	 // First, apply transformations
	 transformStats();
	 float logPreComputedGaussianFactor = 0.0f; // = log(1.0)
	 // Compute the product of the elements in the Covariance
	 // matrix's main diagonal. Covariance matrix is assumed
	 // diagonal - independent dimensions. In log, the product
	 // becomes a summation.
	 for (int i = 0; i < variance.length; i++) {
	     logPreComputedGaussianFactor += 
			       logMath.linearToLog(precisionTransformed[i] * -2);
	     //	     variance[i] = 1.0f / (variance[i] * 2.0f);
	 }

	 // We need the minus sign since we computed
	 // logPreComputedGaussianFactor based on precision, which is
	 // the inverse of the variance. Therefore, in the log domain,
	 // the two quantities have opposite signs.

	 // The covariance matrix's dimension becomes a multiplicative
	 // factor in log scale.
	 logPreComputedGaussianFactor = 
	     logMath.linearToLog(2.0 * Math.PI) * variance.length
	     - logPreComputedGaussianFactor;

	 // The sqrt above is a 0.5 multiplicative factor in log scale.
	 return logPreComputedGaussianFactor * 0.5f;
     }

    /**
     * Applies transformations to means and variances.
     */
    private void transformStats() {
	int i, j; // indices for the matrices

	/**
	 * The transformed mean vector is given by:
	 *
	 * <p><b>M = A * m + B</b></p>
	 *
	 * where <b>M</b> and <b>m</b> are the mean vector after and
	 * before transformation, respectively, and <b>A</b> and
	 * <b>B</b> are the transformation matrix and vector,
	 * respectively.
	 */
	meanTransformed = new float[this.mean.length];
	for (i = 0; i < this.meanTransformationVector.length; i++) {
	    float tmpMean = 0.0f;
	    for (j = 0; j < this.meanTransformationMatrix[i].length; j++) {
		tmpMean += this.mean[j] * this.meanTransformationMatrix[i][j];
	    }
	    this.meanTransformed[i] = tmpMean 
		+ this.meanTransformationVector[i];
	}
	/**
	 * We do analogously with the variance. In this case, we also
	 * invert the variance, and work with precision instead of
	 * variance.
	 */
	precisionTransformed = new float[this.variance.length];
        for (i = 0; i < this.varianceTransformationVector.length; i++) {
	    float tmpVariance = 0.0f;
	    for (j = 0; j < this.varianceTransformationMatrix[i].length; j++) {
		tmpVariance += this.variance[j] 
		    * this.varianceTransformationMatrix[i][j];
	    }
	    tmpVariance += this.varianceTransformationVector[i];
	    if (tmpVariance < varianceFloor) {
		tmpVariance = varianceFloor;
	    }
	    precisionTransformed[i] = 1.0f / (-2.0f * tmpVariance);
	}
    }
}

