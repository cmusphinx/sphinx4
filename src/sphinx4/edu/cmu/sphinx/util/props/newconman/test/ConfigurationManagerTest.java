package edu.cmu.sphinx.util.props.newconman.test;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.newconman.ConMan;
import edu.cmu.sphinx.util.props.newconman.ConfigurationManagerUtils;
import edu.cmu.sphinx.util.props.newconman.SimpleConfigurable;
import junit.framework.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Some unit tests, which ensure a proper implementation of configuration management.
 *
 * @author Holger Brandl
 */

public class ConfigurationManagerTest {

    //    @Test
    // todo this test should become obsolete as soon as we have default properties for components (and lists)
    public void testDynamicConfCreationWithoutDefaultProperty() throws PropertyException, InstantiationException {
        try {
            ConMan cm = new ConMan();

            String instanceName = "docu";
            cm.addConfigurable(DummyComp.class, instanceName);
            SimpleConfigurable configurable = cm.lookup(instanceName);
            Assert.fail("add didn't fail without given default frontend");
        } catch (Throwable t) {
        }
    }


    @Test
    public void testDynamicConfCreation() throws PropertyException, InstantiationException {
        ConMan cm = new ConMan();

        String instanceName = "docu";
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DummyComp.PROP_FRONTEND, new DummyFrontEnd());
        cm.addConfigurable(DummyComp.class, instanceName, props);

        Assert.assertTrue(cm.getPropertySheet(instanceName) != null);
        Assert.assertTrue(cm.lookup(instanceName) != null);
        Assert.assertTrue(cm.lookup(instanceName) instanceof DummyComp);
    }


    @Test
    public void testSerialization() throws IOException, PropertyException {
        File configFile = new File("../../sphinx4/tests/other/testconfig.xml");
        ConMan cm = new ConMan(configFile.toURI().toURL());

        File tmpFile = File.createTempFile("conman", ".tmp.xml");
        tmpFile.deleteOnExit();

        System.out.println(ConfigurationManagerUtils.toXML(cm));
        ConfigurationManagerUtils.toConfigFile(cm, tmpFile);

        // now reload it
        ConMan cmReloaded = new ConMan(tmpFile.toURI().toURL());
        Assert.assertTrue("deserialzed cm doesn't equal its original", cmReloaded.equals(cm));

    }
}
