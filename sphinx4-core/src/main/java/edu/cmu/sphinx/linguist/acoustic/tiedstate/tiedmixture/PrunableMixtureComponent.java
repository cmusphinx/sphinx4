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

import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;
import edu.cmu.sphinx.util.LogMath;

/**
 * Mixture component with partial score computation result.
 * Allows to skip score computing if temporary score reaches provided threshold
 */
@SuppressWarnings("serial")
public class PrunableMixtureComponent extends MixtureComponent {

    private float score = LogMath.LOG_ZERO;
    private float partScore = LogMath.LOG_ZERO;
    private int id;
    
    public PrunableMixtureComponent(
            float[] mean,
            float[][] meanTransformationMatrix,
            float[] meanTransformationVector,
            float[] variance,
            float[][] varianceTransformationMatrix,
            float[] varianceTransformationVector,
            float distFloor,
            float varianceFloor,
            int id) {
        super(mean, meanTransformationMatrix, meanTransformationVector, variance, varianceTransformationMatrix, varianceTransformationVector, distFloor, varianceFloor);
        this.id = id;
    }
    
    private float convertScore(float val) {
        // Convert to the appropriate base.
        val = LogMath.getLogMath().lnToLog(val);

        // TODO: Need to use mean and variance transforms here

        if (Float.isNaN(val)) {
            System.out.println("gs is Nan, converting to 0");
            val = LogMath.LOG_ZERO;
        }

        if (val < distFloor) {
            val = distFloor;
        }
        
        return val;
    }
    
    public boolean isTopComponent(float[] feature, float threshold) {

        float logDval = logPreComputedGaussianFactor;

        // First, compute the argument of the exponential function in
        // the definition of the Gaussian, then convert it to the
        // appropriate base. If the log base is <code>Math.E</code>,
        // then no operation is necessary.
        for (int i = 0; i < feature.length; i++) {
            float logDiff = feature[i] - meanTransformed[i];
            logDval += logDiff * logDiff * precisionTransformed[i];
            if (logDval < threshold)
                return false;
        }
        
        partScore = logDval;  
        score = convertScore(logDval);
        return true;
    }
    
    public void updateScore(float[] feature) {
        
        float logDval = logPreComputedGaussianFactor;

        // First, compute the argument of the exponential function in
        // the definition of the Gaussian, then convert it to the
        // appropriate base. If the log base is <code>Math.E</code>,
        // then no operation is necessary.
        for (int i = 0; i < feature.length; i++) {
            float logDiff = feature[i] - meanTransformed[i];
            logDval += logDiff * logDiff * precisionTransformed[i];
        }
        
        partScore = logDval;  
        score = convertScore(logDval);
    }
    
    public float getStoredScore() {
        return score;
    }
    
    public float getPartialScore() {
        return partScore;
    }
    
    public int getId() {
        return id;
    }

}
