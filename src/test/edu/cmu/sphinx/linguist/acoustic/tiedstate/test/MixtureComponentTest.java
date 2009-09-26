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

package edu.cmu.sphinx.linguist.acoustic.tiedstate.test;

import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Math.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Some tests which test the functionality of <code>MixtureComponentt</code>s.
 * <p/>
 * Currently testing is restricted to univariate gaussians. It should be extended to test highdimensional gaussians as
 * well.
 */
public class MixtureComponentTest {

    private LogMath lm;


    @Before
    public void setup() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(LogMath.PROP_USE_ADD_TABLE, false);

        lm = ConfigurationManager.getInstance(LogMath.class, props);
    }


    /**
     * Compute the density values of a sampled interval with an univariate <code>MixtureComponent</code> and compare
     * values with the precomputed-computed ones.
     */
    @Test
    public void testUnivariateDensity() {

        float minX = 10;
        float maxX = 30;
        float resolution = 0.1f;

        float mean = 20;
        float var = 3;

        MixtureComponent gaussian = new MixtureComponent(lm, new float[]{mean}, new float[]{var});

        for (float curX = minX; curX <= maxX; curX += resolution) {
            double gauLogScore = gaussian.getScore(new FloatData(new float[]{curX}, 0, 0, 0));

            double manualScore = (1 / sqrt(var * 2 * PI)) * exp((-0.5 / var) * (curX - mean) * (curX - mean));
            double gauScore = lm.logToLinear((float) gauLogScore);

            assertEquals(manualScore, gauScore, 1E-5);
        }
    }


    /** Tests whether working with different types transformations works properly. */
    @Test
    public void testUnivariateMeanTransformation() {
        float mean = 20;
        float var = 0.001f;

        MixtureComponent gaussian = new MixtureComponent(lm, new float[]{mean}, new float[][]{{2}}, new float[]{5}, new float[]{var}, null, null);
        assertTrue(lm.logToLinear(gaussian.getScore(new float[]{2 * mean + 5})) > 10);
    }


    /** Tests whether a <code>MixtureComponent</code>s can be cloned (using deep copying). */
    @Test
    public void testClone() throws CloneNotSupportedException {
        MixtureComponent gaussian = new MixtureComponent(lm, new float[]{2}, new float[][]{{3}}, new float[]{4}, new float[]{5}, new float[][]{{6}}, new float[]{7});

        MixtureComponent clonedGaussian = gaussian.clone();

        assertTrue(!clonedGaussian.equals(gaussian));

        assertTrue(gaussian.getMean() != clonedGaussian.getMean());
        assertTrue(gaussian.getVariance() != clonedGaussian.getVariance());
        assertTrue(gaussian.getScore(new float[]{2}) == clonedGaussian.getScore(new float[]{2}));
    }
}