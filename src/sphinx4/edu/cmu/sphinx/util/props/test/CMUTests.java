package edu.cmu.sphinx.util.props.test;

import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.instrumentation.AccuracyTracker;
import edu.cmu.sphinx.instrumentation.BestPathAccuracyTracker;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.ConfigurationManagerUtils;
import junit.framework.Assert;
import org.junit.Test;

/** Some unit-tests for the ConfigurationManagerUtils. */
public class CMUTests {

    @Test
    public void testClassTesting() {
        Assert.assertTrue(ConfigurationManagerUtils.isImplementingInterface(FrontEnd.class, DataProcessor.class));
        Assert.assertTrue(ConfigurationManagerUtils.isImplementingInterface(DataProcessor.class, Configurable.class));
        Assert.assertFalse(ConfigurationManagerUtils.isImplementingInterface(Configurable.class, Configurable.class));

        Assert.assertFalse(ConfigurationManagerUtils.isSubClass(Configurable.class, Configurable.class));
        Assert.assertTrue(ConfigurationManagerUtils.isSubClass(Integer.class, Object.class));
        Assert.assertFalse(ConfigurationManagerUtils.isSubClass(Object.class, Object.class));

        Assert.assertTrue(ConfigurationManagerUtils.isSubClass(BestPathAccuracyTracker.class, AccuracyTracker.class));
    }
}
