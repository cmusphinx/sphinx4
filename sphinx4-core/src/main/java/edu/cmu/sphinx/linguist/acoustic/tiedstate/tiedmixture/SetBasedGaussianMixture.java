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

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.GaussianMixture;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;

/**
 * Represents gaussian mixture that is based on provided mixture component set
 * <p/>
 * All scores and weights are maintained in LogMath log base.
 */

@SuppressWarnings("serial")
public class SetBasedGaussianMixture extends GaussianMixture {

    private MixtureComponentSet mixtureComponentSet;
    
    public SetBasedGaussianMixture(float[] logMixtureWeights,
            MixtureComponentSet mixtureComponentSet, long id) {
        super(logMixtureWeights, null, id);
        this.mixtureComponentSet = mixtureComponentSet;
    }
    
    @Override
    public float calculateScore(Data feature) {
        return mixtureComponentSet.calculateScore(feature, logMixtureWeights);
    }

    /**
     * Calculates the scores for each component in the senone.
     *
     * @param feature the feature to score
     * @return the LogMath log scores for the feature, one for each component
     */
    @Override
    public float[] calculateComponentScore(Data feature) {
        return mixtureComponentSet.calculateComponentScore(feature, logMixtureWeights);
    }
    
    @Override
    public MixtureComponent[] getMixtureComponents() {
        return mixtureComponentSet.toArray();
    }
    
    @Override
    public int dimension() {
        return mixtureComponentSet.dimension();
    }
    
    /** @return the number of component densities of this <code>GaussianMixture</code>. */
    @Override
    public int numComponents() {
        return mixtureComponentSet.size();
    }

}
