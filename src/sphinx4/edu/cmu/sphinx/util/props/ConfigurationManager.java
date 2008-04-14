package edu.cmu.sphinx.util.props;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;


/**
 * Manages a set of <code>Configurable</code>s, their parametrization and the relationships between them. Configurations
 * can be specified either by xml or on-the-fly during runtime.
 *
 * @see edu.cmu.sphinx.util.props.Configurable
 * @see edu.cmu.sphinx.util.props.PropertySheet
 */
public class ConfigurationManager implements Cloneable {

    private List<ConfigurationChangeListener> changeListeners = new ArrayList<ConfigurationChangeListener>();

    private Map<String, PropertySheet> symbolTable = new LinkedHashMap<String, PropertySheet>();
    private Map<String, RawPropertyData> rawPropertyMap = new HashMap<String, RawPropertyData>();
    private GlobalProperties globalProperties = new GlobalProperties();

    private boolean showCreations;
    private URL configURL;


    /**
     * Creates a new empty configuration manager. This constructor is only of use in cases when a system configuration
     * is created during runtime.
     */
    public ConfigurationManager() {
    }


    /**
     * Creates a new configuration manager. Initial properties are loaded from the given URL. No need to keep the notion
     * of 'context' around anymore we will just pass around this property manager.
     *
     * @param configFileName The location of the configuration file.
     */
    public ConfigurationManager(String configFileName) throws PropertyException {
        this(ConfigurationManagerUtils.getURL(new File(configFileName)));
    }


    /**
     * Creates a new configuration manager. Initial properties are loaded from the given URL. No need to keep the notion
     * of 'context' around anymore we will just pass around this property manager.
     *
     * @param url The location of the configuration file.
     */
    public ConfigurationManager(URL url) throws PropertyException {
        configURL = url;
        SaxLoader saxLoader = new SaxLoader(url, globalProperties, null);

        try {
            rawPropertyMap = saxLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ConfigurationManagerUtils.applySystemProperties(rawPropertyMap, globalProperties);
        ConfigurationManagerUtils.configureLogger(this);

        // we can't config the configuration manager with itself so we
        // do some of these config items manually.
        GlobalProperty showCreations = globalProperties.get("showCreations");
        if (showCreations != null)
            this.showCreations = "true".equals(showCreations.getValue());
    }


    /**
     * Returns the property sheet for the given object instance
     *
     * @param instanceName the instance name of the object
     * @return the property sheet for the object.
     */
    public PropertySheet getPropertySheet(String instanceName) {
        if (!symbolTable.containsKey(instanceName)) {
            // if it is not in the symbol table, so construct
            // it based upon our raw property data
            RawPropertyData rpd = rawPropertyMap.get(instanceName);
            if (rpd != null) {
                String className = rpd.getClassName();
                try {
                    Class cls = Class.forName(className);

                    // now load the property-sheet by using the class annotation
//                    PropertySheet propertySheet = new PropertySheet(cls, this, rpd.flatten(globalProperties));
                    PropertySheet propertySheet = new PropertySheet(cls, instanceName, this, rpd);

                    symbolTable.put(instanceName, propertySheet);

                } catch (ClassNotFoundException e) {
                    System.err.println("class not found !" + e.toString());
                } catch (ClassCastException e) {
                    System.err.println("can not cast class !" + e.toString());
                }
            }
        }

        return symbolTable.get(instanceName);
    }


    /**
     * Gets all instances that are of the given type.
     *
     * @param type the desired type of instance
     * @return the set of all instances
     */
    public Collection<String> getInstanceNames(Class<? extends Configurable> type) {
        Collection<String> instanceNames = new ArrayList<String>();

        for (PropertySheet ps : symbolTable.values()) {
            if (!ps.isInstanciated())
                continue;

            if (ConfigurationManagerUtils.isDerivedClass(ps.getConfigurableClass(), type))
                instanceNames.add(ps.getInstanceName());
        }

        return instanceNames;
    }


    /**
     * Returns all names of configurables registered to this instance. The resulting set includes instantiated and
     * noninstantiated components.
     *
     * @return all component named registered to this instance of <code>ConfigurationManager</code>
     */
    public Collection<String> getComponentNames() {
        return Arrays.asList(rawPropertyMap.keySet().toArray(new String[rawPropertyMap.size()]));
    }


    /**
     * Looks up a configurable component by name. Creates it if necessary
     *
     * @param instanceName the name of the component
     * @return the compnent, or null if a component was not found.
     * @throws InternalConfigurationException If the requested object could not be properly created, or is not a
     *                                        configurable object, or if an error occured while setting a component
     *                                        property.
     */
    public Configurable lookup(String instanceName) throws InternalConfigurationException {
        // apply all new propeties to the model
        instanceName = getStrippedComponentName(instanceName);

        PropertySheet ps = getPropertySheet(instanceName);
        if (ps == null)
            return null;

        if (showCreations)
            System.out.println("Creating: " + instanceName);

        return ps.getOwner();
    }


    /**
     * Given a <code>Configurable</code>-class/interface, all property-sheets which are subclassing/implemting this
     * class/interface are collected and returned.  No <code>Configurable</code> will be instantiated by this method.
     */
    public List<PropertySheet> getPropSheets(Class<? extends Configurable> confClass) {
        List<PropertySheet> psCol = new ArrayList<PropertySheet>();

        for (PropertySheet ps : symbolTable.values()) {
            if (ConfigurationManagerUtils.isDerivedClass(ps.getConfigurableClass(), confClass))
                psCol.add(ps);
        }

        return psCol;
    }


    /**
     * Registers a new configurable to this configuration manager.
     *
     * @param confClass The class of the configurable to be instantiated and to be added to this configuration manager
     *                  instance.
     * @param name      The desired lookup-name of the configurable
     * @throws IllegalArgumentException if the there's already a component with the same <code>name</code> registered to
     *                                  this configuration manager instance.
     */
    public void addConfigurable(Class<? extends Configurable> confClass, String name) {
        addConfigurable(confClass, name, new HashMap<String, Object>());
    }


    /**
     * Registers a new configurable to this configuration manager.
     *
     * @param confClass The class of the configurable to be instantiated and to be added to this configuration manager
     *                  instance.
     * @param name      The desired  lookup-name of the configurable
     * @param props     The properties to be used for component configuration
     * @throws IllegalArgumentException if the there's already a component with the same <code>name</code> registered to
     *                                  this configuration manager instance.
     */
    public void addConfigurable(Class<? extends Configurable> confClass, String name, Map<String, Object> props) {
        if (name == null) // use the class name as default if no name is given
            name = confClass.getName();

        if (symbolTable.containsKey(name))
            throw new IllegalArgumentException("tried to override existing component name");

        PropertySheet ps = getPropSheetInstanceFromClass(confClass, props, name, this);
        symbolTable.put(name, ps);
        rawPropertyMap.put(name, new RawPropertyData(name, confClass.getName()));

        for (ConfigurationChangeListener changeListener : changeListeners)
            changeListener.componentAdded(this, ps);
    }


    /**
     * Adds an already instantiated <code>Configurable</code> to this configuration manager.
     *
     * @param name The desired lookup-instanceName of the configurable
     */
    public void addConfigurable(Configurable configurable, String name) {
        if (symbolTable.containsKey(name))
            throw new IllegalArgumentException("tried to override existing component name");

        RawPropertyData dummyRPD = new RawPropertyData(name, configurable.getClass().getName());

        PropertySheet ps = new PropertySheet(configurable, name, dummyRPD, this);
        symbolTable.put(name, ps);
        rawPropertyMap.put(name, dummyRPD);

        for (ConfigurationChangeListener changeListener : changeListeners)
            changeListener.componentAdded(this, ps);
    }


    public void renameConfigurable(String oldName, String newName) {
        PropertySheet ps = getPropertySheet(oldName);

        if (ps == null) {
            throw new RuntimeException("no configurable (to be renamed) named " + oldName + " is contained in the CM");
        }

        ConfigurationManagerUtils.renameComponent(this, oldName, newName);

        symbolTable.remove(oldName);
        symbolTable.put(newName, ps);

        RawPropertyData rpd = rawPropertyMap.remove(oldName);
        rawPropertyMap.put(newName, new RawPropertyData(newName, rpd.getClassName(), rpd.getProperties()));

        fireRenamedConfigurable(oldName, newName);
    }


    /** Removes a configurable from this configuration manager. */
    public void removeConfigurable(String name) {
        assert getComponentNames().contains(name);

        PropertySheet ps = symbolTable.remove(name);
        rawPropertyMap.remove(name);

        for (ConfigurationChangeListener changeListener : changeListeners)
            changeListener.componentRemoved(this, ps);
    }


    public void addSubConfiguration(ConfigurationManager subCM) {
        Collection<String> compNames = getComponentNames();

        for (String addCompName : subCM.getComponentNames()) {
            if (compNames.contains(addCompName)) {
                throw new RuntimeException(addCompName + " is already registered to system configuration");
            }
        }

        for (String globProp : subCM.globalProperties.keySet()) {
            // the second test is necessary because system-props will be global-props in both CMs
            if (globalProperties.keySet().contains(globProp) && !System.getProperties().keySet().contains(globProp)) {
                throw new IllegalArgumentException(globProp + " is already registered as global property");
            }
        }

        globalProperties.putAll(subCM.globalProperties);
        for (PropertySheet ps : subCM.symbolTable.values()) {
            ps.setCM(this);
        }

        symbolTable.putAll(subCM.symbolTable);
        rawPropertyMap.putAll(subCM.rawPropertyMap);
    }


    /** Returns a copy of the map of global properties set for this configuration manager. */
    public GlobalProperties getGlobalProperties() {
        return new GlobalProperties(globalProperties);
    }


    /**
     * Returns a global property.
     *
     * @param propertyName The name of the global property or <code>null</code> if no such property exists
     */
    public String getGlobalProperty(String propertyName) {
//        propertyName = propertyName.startsWith("$") ? propertyName : "${" + propertyName + "}";
        GlobalProperty globProp = globalProperties.get(propertyName);
        return globProp != null ? globProp.toString() : null;
    }


    public GlobalProperty getGloPropReference(String propertyName) {
        return globalProperties.get(propertyName);
    }


    /**
     * Returns the url of the xml-configuration which defined this configuration or <code>null</code>  if it was created
     * dynamically.
     */
    public URL getConfigURL() {
        return configURL;
    }


    /**
     * Sets a global property.
     *
     * @param propertyName The name of the global property.
     * @param value        The new value of the global property. If the value is <code>null</code> the property becomes
     *                     removed.
     */
    public void setGlobalProperty(String propertyName, String value) {
        if (value == null)
            globalProperties.remove(propertyName);
        else
            globalProperties.setValue(propertyName, value);

        // update all component configurations because they might be affected by the change
        for (String instanceName : getInstanceNames(Configurable.class)) {
            PropertySheet ps = getPropertySheet(instanceName);
            if (ps.isInstanciated())
                try {
                    ps.getOwner().newProperties(ps);
                } catch (PropertyException e) {
                    e.printStackTrace();
                }
        }
    }


    public String getStrippedComponentName(String propertyName) {
        assert propertyName != null;

        while (propertyName.startsWith("$"))
            propertyName = globalProperties.get(ConfigurationManagerUtils.stripGlobalSymbol(propertyName)).toString();

        return propertyName;
    }


    /** Adds a new listener for configuration change events. */
    public void addConfigurationChangeListener(ConfigurationChangeListener l) {
        if (l == null)
            return;

        changeListeners.add(l);
    }


    /** Removes a listener for configuration change events. */
    public void removeConfigurationChangeListener(ConfigurationChangeListener l) {
        if (l == null)
            return;

        changeListeners.remove(l);
    }


    /**
     * Informs all registered <code>ConfigurationChangeListener</code>s about a configuration changes the component
     * named <code>configurableName</code>.
     */
    void fireConfChanged(String configurableName, String propertyName) {
        assert getComponentNames().contains(configurableName);

        for (ConfigurationChangeListener changeListener : changeListeners)
            changeListener.configurationChanged(configurableName, propertyName, this);
    }


    /**
     * Informs all registered <code>ConfigurationChangeListener</code>s about the component previously namesd
     * <code>oldName</code>
     */
    void fireRenamedConfigurable(String oldName, String newName) {
        assert getComponentNames().contains(newName);

        for (ConfigurationChangeListener changeListener : changeListeners) {
            changeListener.componentRenamed(this, getPropertySheet(newName), oldName);
        }
    }


    /**
     * Test wether the given configuration manager instance equals this instance in terms of same configuration. This
     * This equals implemenation does not care about instantiation of components.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof ConfigurationManager))
            return false;

        ConfigurationManager cm = (ConfigurationManager) obj;

        Collection<String> setA = new HashSet<String>(getComponentNames());
        Collection<String> setB = new HashSet<String>(cm.getComponentNames());
        if (!setA.equals(setB))
            return false;

        // make sure that all components are the same
        for (String instanceName : getComponentNames()) {
            PropertySheet myPropSheet = getPropertySheet(instanceName);
            PropertySheet otherPropSheet = cm.getPropertySheet(instanceName);

            if (!otherPropSheet.equals(myPropSheet))
                return false;
        }

        // make sure that both configuration managers have the same set of global properties
        return cm.getGlobalProperties().equals(getGlobalProperties());
    }


    /** Creates a deep copy of the given CM instance. */
    // This is not tested yet !!!
    public Object clone() throws CloneNotSupportedException {
        ConfigurationManager cloneCM = (ConfigurationManager) super.clone();

        cloneCM.changeListeners = new ArrayList<ConfigurationChangeListener>();
        cloneCM.symbolTable = new LinkedHashMap<String, PropertySheet>();
        for (String compName : symbolTable.keySet()) {
            cloneCM.symbolTable.put(compName, (PropertySheet) symbolTable.get(compName).clone());
        }

        cloneCM.globalProperties = new GlobalProperties(globalProperties);
        cloneCM.rawPropertyMap = new HashMap<String, RawPropertyData>(rawPropertyMap);


        return cloneCM;
    }


    /**
     * Creates an instance of the given <code>Configurable</code> by using the default parameters as defined by the
     * class annotations to parameterize the component.
     */
    public static <C extends Configurable> C getInstance(Class<C> targetClass) throws PropertyException {
        return getInstance(targetClass, new HashMap<String, Object>());
    }


    /**
     * Creates an instance of the given <code>Configurable</code> by using the default parameters as defined by the
     * class annotations to parameterize the component. Default prarmeters will be overrided if a their names are
     * containd in the given <code>props</code>-map
     */
    public static <C extends Configurable> C getInstance(Class<C> targetClass, Map<String, Object> props) throws PropertyException {
        PropertySheet ps = getPropSheetInstanceFromClass(targetClass, props, null, new ConfigurationManager());
        Configurable configurable = ps.getOwner();
        return (C) configurable;
    }


    /**
     * Instantiates the given <code>targetClass</code> and instruments it using default properties or the properties
     * given by the <code>defaultProps</code>.
     */
    private static PropertySheet getPropSheetInstanceFromClass(Class<? extends Configurable> targetClass, Map<String, Object> defaultProps, String componentName, ConfigurationManager cm) {
        RawPropertyData rpd = new RawPropertyData(componentName, targetClass.getName());

        for (String confName : defaultProps.keySet()) {
            Object property = defaultProps.get(confName);

            if (property instanceof Class)
                property = ((Class) property).getName();

            rpd.getProperties().put(confName, property);
        }

        return new PropertySheet(targetClass, componentName, cm, rpd);
    }
}

