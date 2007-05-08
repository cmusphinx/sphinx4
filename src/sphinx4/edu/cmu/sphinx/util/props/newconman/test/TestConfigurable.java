package edu.cmu.sphinx.util.props.newconman.test;

import edu.cmu.sphinx.util.props.*;
import junit.framework.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * DOCUMENT ME!
 *
 * @author Holger Brandl
 */
public class TestConfigurable implements Configurable {

    // note: no default component here
    @S4Component(type = AnotherDummyProcessor.class)
    public static final String PROP_DATA_PROC = "dataProc";
    private DummyProcessor dataProc;

    @S4String
    public static final String PROP_ASTRING = "mystring";
    private String myString;

    @S4Double(defaultValue = 1.3)
    public static final String PROP_GAMMA = "gamma";
    private double gamma;


    public void newProperties(PropertySheet ps) throws PropertyException {
        dataProc = (DummyProcessor) ps.getComponent(PROP_DATA_PROC);
        myString = ps.getString(PROP_ASTRING);
        gamma = ps.getDouble(PROP_GAMMA);
    }


    public String getName() {
        return this.getClass().getName();
    }


    public DummyProcessor getDataProc() {
        return dataProc;
    }


    @Test
    // note: it is not a bug but a feature of this test to print a stacktrace
    public void testDynamicConfCreationWithoutDefaultProperty() {
        try {
            ConfigurationManager cm = new ConfigurationManager();

            String instanceName = "testconf";
            cm.addConfigurable(TestConfigurable.class, instanceName);

            Configurable configurable = cm.lookup(instanceName);
            Assert.fail("add didn't fail without given default frontend");
        } catch (NullPointerException e) {
        } catch (PropertyException e) {
        } catch (InstantiationException e) {
        }
    }


    @Test
    // note: it is not a bug but a feature of this test to print a stacktrace
    public void testNullStringProperty() throws PropertyException, InstantiationException {
        HashMap<String, Object> props = new HashMap<String, Object>();
        props.put("dataProc", AnotherDummyProcessor.class);

        TestConfigurable teco = (TestConfigurable) ConfigurationManager.getDefaultInstance(TestConfigurable.class, props);
        Assert.assertTrue(teco.myString == null);
    }



    @Test
    public void testPropSheetFromConfigurableInstance() throws PropertyException, InstantiationException {
        String testString = "test";
        double testDouble = 12;

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(PROP_ASTRING, testString);
        props.put(PROP_DATA_PROC, AnotherDummyProcessor.class);
        TestConfigurable tc = (TestConfigurable) ConfigurationManager.getDefaultInstance(TestConfigurable.class, props);

        // now create a property sheet in order to modify the configurable
        PropertySheet propSheet = new PropertySheet(tc, null, new RawPropertyData("tt", tc.getClass().getName()), new ConfigurationManager());
        propSheet.setComponent(PROP_DATA_PROC, "tt", new AnotherDummyProcessor() );
        tc.newProperties(propSheet);

        // test whether old props were preserved and new ones were applied
        // todo fixme: Its by design not possible to preserven the old properties without have a CM
        // probably we should remove the possiblitly to let the user create PropertySheet instances.
        
//        Assert.assertTrue(tc.myString.equals(testString));
//        Assert.assertTrue(tc.gamma == testDouble);
        Assert.assertTrue(tc.dataProc != null && tc.dataProc instanceof AnotherDummyProcessor);
    }
}
