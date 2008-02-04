package edu.cmu.sphinx.util.props.test;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * DOCUMENT ME!
 *
 * @author Holger Brandl
 */
public class ComponentListTests {


    @Test
    public void testInvalidList() {
        ConfigurationManager cm = new ConfigurationManager();

        Map<String, Object> props = new HashMap<String, Object>();
        cm.addConfigurable(DummyProcessor.class, "dummyA");
        props.put(DummyFrontEnd.DATA_PROCS, Arrays.asList("dummyA, dummyB"));
        cm.addConfigurable(DummyFrontEnd.class, "dfe", props);

        cm.lookup("dfe");
    }

}
