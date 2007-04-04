package edu.cmu.sphinx.util.props.newconman.test;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.newconman.PropSheet;
import edu.cmu.sphinx.util.props.newconman.SimpleConfigurable;

/**
 * DOCUMENT ME!
 *
 * @author Holger Brandl
 */
public class DummyProcessor implements SimpleConfigurable {


    public void newProperties(PropSheet ps) throws PropertyException {
    }


    public String getName() {
        return this.getClass().getName();
    }
}
