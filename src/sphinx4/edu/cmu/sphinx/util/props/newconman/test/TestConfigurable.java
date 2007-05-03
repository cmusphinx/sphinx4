package edu.cmu.sphinx.util.props.newconman.test;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.newconman.ConMan;
import edu.cmu.sphinx.util.props.newconman.PropSheet;
import edu.cmu.sphinx.util.props.newconman.S4Component;
import edu.cmu.sphinx.util.props.newconman.SimpleConfigurable;
import junit.framework.Assert;
import org.junit.Test;

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


    public void newProperties(PropSheet ps) throws PropertyException {
        dataProc = (DummyProcessor) ps.getComponent(PROP_DATA_PROC);
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
}
