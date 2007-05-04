package edu.cmu.sphinx.util.props;

/**
 * Describes all methods necessary to process change events of a <code>ConfigurationManager</code>.
 *
 * @author Holger Brandl
 * @see edu.cmu.sphinx.util.props.ConfigurationManager
 */

public interface ConfigurationChangeListener {

    /**
     * Called if a configurable with name <code>configurableName</code> was changed (which can be a creation, deletion
     * or property change).
     *
     * @param configurableName The name of the changed configurable.
     */
    public void configurationChanged(String configurableName);


}
