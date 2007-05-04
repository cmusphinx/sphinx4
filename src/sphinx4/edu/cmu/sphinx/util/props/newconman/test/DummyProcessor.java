package edu.cmu.sphinx.util.props.newconman.test;

import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.Registry;

/**
 * DOCUMENT ME!
 *
 * @author Holger Brandl
 */
public class DummyProcessor implements DummyFrontEndProcessor {


    public void register(String name, Registry registry) throws PropertyException {
    }


    public void newProperties(PropertySheet ps) throws PropertyException {
    }


    public String getName() {
        return this.getClass().getName();
    }
}
