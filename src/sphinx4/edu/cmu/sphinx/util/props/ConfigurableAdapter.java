package edu.cmu.sphinx.util.props;

/**
 * An empty dummy implementation of the Configurable interface. The main purpose of this class is to avoid emtpy
 * implementations of newProperties for components which do not own any configurable properties.
 *
 * @author Holger Brandl
 */
public class ConfigurableAdapter implements Configurable {

    public void newProperties(PropertySheet ps) throws PropertyException {

    }
}
