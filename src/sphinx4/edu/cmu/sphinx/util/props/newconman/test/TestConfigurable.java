package edu.cmu.sphinx.util.props.newconman.test;

import edu.cmu.sphinx.util.props.*;
import junit.framework.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * DOCUMENT ME!
 *
 * @author Holger Brandl
 */
public class TestConfigurable implements SimpleConfigurable {

    // note: no default component here
    @S4Component(type = AnotherDummyProcessor.class)
    public static final String PROP_DATA_PROC = "dataProc";
    private DummyProcessor dataProc;

    @S4String
    public static final String PROP_ASTRING = "mystring";
    private String myString;


    public void newProperties(PropSheet ps) throws PropertyException {
        dataProc = (DummyProcessor) ps.getComponent(PROP_DATA_PROC);
        myString = ps.getString(PROP_ASTRING);
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
            ConMan cm = new ConMan();

            String instanceName = "testconf";
            cm.addConfigurable(TestConfigurable.class, instanceName);

            SimpleConfigurable configurable = cm.lookup(instanceName);
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

        TestConfigurable teco = (TestConfigurable) ConMan.getDefaultInstance(TestConfigurable.class, props);
        Assert.assertTrue(teco.myString == null);
    }

}
