/*
 * Copyright 1999-2013 Carnegie Mellon University.  
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.bandwidth;

import junit.framework.Assert;

import org.junit.Test;

public class BandDetectorTest {
    @Test
    public void test() {
        BandDetector detector = new BandDetector();
        Assert.assertTrue(detector.bandwidth("src/test/edu/cmu/sphinx/tools/bandwidth/10001-90210-01803-8khz.wav"));
        Assert.assertFalse(detector.bandwidth("src/test/edu/cmu/sphinx/tools/bandwidth/10001-90210-01803.wav"));
    }
}
