package edu.cmu.sphinx.util.props.test;

import edu.cmu.sphinx.util.props.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Some unit tests, which ensure a proper implementation of configuration management.
 *
 * @author Holger Brandl
 */

public class ConfigurationManagerTest {

    @Test
    public void testDynamicConfCreation() throws PropertyException, InstantiationException {
        ConfigurationManager cm = new ConfigurationManager();

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
        ConfigurationManager cm = new ConfigurationManager(configFile.toURI().toURL());

        File tmpFile = File.createTempFile("ConfigurationManager", ".tmp.xml");
        tmpFile.deleteOnExit();

        System.out.println(ConfigurationManagerUtils.toXML(cm));
        ConfigurationManagerUtils.save(cm, tmpFile);

        // now reload it
        ConfigurationManager cmReloaded = new ConfigurationManager(tmpFile.toURI().toURL());
        Assert.assertTrue("deserialzed cm doesn't equal its original", cmReloaded.equals(cm));
    }


    @Test
    public void testDynamicConfiguruationChange() throws IOException, PropertyException, InstantiationException {
        File configFile = new File("../../sphinx4/tests/other/testconfig.xml");
        ConfigurationManager cm = new ConfigurationManager(configFile.toURI().toURL());

        Assert.assertTrue(cm.getInstanceNames(DummyFrontEndProcessor.class).isEmpty());

        PropertySheet propSheet = cm.getPropertySheet("duco");

        propSheet.setDouble("alpha", 11);

        DummyComp duco = (DummyComp) cm.lookup("duco");

        Assert.assertTrue(cm.getInstanceNames(DummyFrontEndProcessor.class).size() == 1);

        // IMPORTANT because we assume the configurable to be instantiated first at
        // lookup there is no need to call newProperties here
        //        duco.newProperties(propSheet);

        Assert.assertTrue(duco.getAlpha() == 11);
    }


    @Test
    public void testSerializeDynamicConfiguration() throws PropertyException, InstantiationException {
        ConfigurationManager cm = new ConfigurationManager();
        String frontEndName = "myFrontEnd";

        cm.addConfigurable(DummyFrontEnd.class, frontEndName);
        PropertySheet propSheet = cm.getPropertySheet(frontEndName);
        propSheet.setComponentList("dataProcs", Arrays.asList("fooBar"), Arrays.<Configurable>asList(new AnotherDummyProcessor()));

        String xmlString = ConfigurationManagerUtils.toXML(cm);

        System.out.println(xmlString);
        Assert.assertTrue(xmlString.contains(frontEndName));
        Assert.assertTrue(xmlString.contains("fooBar"));

        // because it is already there: test whether dynamic list changing really works
        DummyFrontEnd frontEnd = (DummyFrontEnd) cm.lookup(frontEndName);
        Assert.assertTrue(frontEnd.getDataProcs().size() == 1);
        Assert.assertTrue(frontEnd.getDataProcs().get(0) instanceof AnotherDummyProcessor);
    }


}
