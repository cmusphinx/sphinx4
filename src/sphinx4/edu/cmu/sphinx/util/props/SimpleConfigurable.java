package edu.cmu.sphinx.util.props;

/**
 * Defines the interface that must be implemented by any configurable component in Sphinx-4.  The life cycle of a
 * component is as follows:
 * <p/>
 * <ul> <li> <b>Construction</b> - the component constructor is called. Typically the constructor does little, if any
 * work, since the component has not been configured yet.
 * <p/>
 * <li> <b> Registration of properties</b> - shortly after construction, the components <code>register</code> method is
 * called. This allows the component to register all configuration parameters.  This registration includes the names and
 * the types of all configuration data expected by the component. Only properties of these names and types will be
 * allowed by the configuration system.  Note that if a component is derived from another <code>Configurable</code> its
 * register method should call super.register before registering its own properties.
 * <p/>
 * <li> <b> Configuration</b> - shortly after registration, the component's <code>newProperties</code> method is called.
 * This method is called with a <code>PropertySheet</code> containing the properties (from the external config file).
 * The component should extract the properties from the property sheet and validate them.  Invalid properties should be
 * reported by throwing a <code>PropertyException</code>. Typically, once a component gets its configuration data via
 * the <code>newData</code> method, the component will initialize itself.  Currently, the <code>newProperties</code>
 * method is called only once as a result of system configuration during startup. However, future extensions to the
 * configuration manager may allow configuration changes while the system is running. Therefore, a well behaved
 * component should react properly to multiple <code>newProperties</code> calls. </ul>
 * <p/>
 * <p><b>Connecting to other components</b> <p> Components often need to interact with other components in the system.
 * One of the design goals of Sphinx-4 is that it allows for very flexible hookup of components in the system.
 * Therefore, it is *not* considered good S4 style to hardcode which subcomponents a particular subcomponent is
 * interacting with.  Instead, the component should use the configuration manager to provide the hookup to another
 * component.  For example, if a component needs to interact with a Linguist. Instead of explicitly setting which
 * linguist is to be used via a constructor or via a <code>setLinguist</code> call, the component should instead define
 * a configuration property for the linguist.  This would be done like so:
 * <p/>
 * <code> <pre>
 *     public static String PROP_LINGUIST = "linguist";
 * </pre> </code>
 * <p/>
 * <p> This is registered in the <code>register</code> method:<p>
 * <p/>
 * <code> <pre>
 *     public void register(String name, Registry register) {
 * registry.register(PROP_LINGUIST, PropertyType.COMPONENT);
 *     }
 * </pre> </code>
 * <p/>
 * <p> The linguist is made available in the <code>newProperties</code> method, like so: <p>
 * <p/>
 * <code> <pre>
 *     public void newProperties(PropertySheet propertySheet) {
 *      linguist = (Linguist)
 *            propertySheet.getComponent(PROP_LINGUIST, Linguist.class);
 *     }
 * </pre> </code>
 * <p/>
 * This <code>getComponent</code> call will find the proper linguist based upon the configuration data.  Thus, if the
 * configuration for this component had the 'linguist' defined to be 'dynamicLexTreeLinguist', then the configuration
 * manager will look up and return a linguist with that name, creating and configuring it as necessary.  Of course, the
 * dynamicLexTreeLinguist itself may have a number of sub-components that will be created and configured as a result. If
 * the component doesn't exist and no configuration information is found in the config file for it, or if it is of the
 * wrong type, a <code>PropertyException</code> will be thrown.
 */
public interface SimpleConfigurable {

    /**
     * This method is called when this configurable component has new data.  The component should first validate the
     * data. If it is bad the component should return false.  If the data is good, the component should record the the
     * data internally and return true.
     *
     * @param ps a property sheet holding the new data
     */
    public void newProperties(PropSheet ps) throws PropertyException;


    /**
     * Retrieves the name for this configurable component
     *
     * @return the name
     */
    public String getName();

}
