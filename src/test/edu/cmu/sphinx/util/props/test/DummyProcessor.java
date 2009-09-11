package edu.cmu.sphinx.util.props.test;

import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyException;

/**
 * DOCUMENT ME!
 *
 * @author Holger Brandl
 */
public class DummyProcessor implements DummyFrontEndProcessor {


    public void newProperties(PropertySheet ps) throws PropertyException {
    }


    public String getName() {
        return this.getClass().getName();
    }
}
