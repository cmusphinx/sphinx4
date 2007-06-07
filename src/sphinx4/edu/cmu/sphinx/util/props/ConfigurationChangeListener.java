package edu.cmu.sphinx.util.props;

/**
 * Describes all methods necessary to process change events of a <code>ConfigurationManager</code>.
 *
 * @author Holger Brandl
 * @see edu.cmu.sphinx.util.props.ConfigurationManager
 */

public interface ConfigurationChangeListener {

    /**
     * Called if the configuration of a registered compoenent named <code>configurableName</code> was changed.
     *
     * @param configurableName The name of the changed configurable.
     * @param propertyName The name of the property which was changed
     * @param cm The <code>ConfigurationManager</code>-instance this component is registered to
     */
    public void configurationChanged(String configurableName, String propertyName, ConfigurationManager cm);


    /**
     * Called if a new compoenent named <code>configurableName</code> was registered to the ConfigurationManager
     * <code>cm</code>.
     */
    public void componentAdded(String configurableName, ConfigurationManager cm);


    /**
     * Called if a compoenent named <code>configurableName</code> was unregistered (removed) from the
     * ConfigurationManager <code>cm</code>.
     */
    public void componentRemoved(String configurableName, ConfigurationManager cm);
}
