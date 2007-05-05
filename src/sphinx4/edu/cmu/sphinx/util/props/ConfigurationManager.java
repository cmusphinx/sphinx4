package edu.cmu.sphinx.util.props;

import java.io.IOException;
import java.net.URL;
import java.util.*;


/** A configuration manager which enables xml-based system configuration.  ...to be continued! */
public class ConfigurationManager {

    /** A common property (used by all components) that sets the log level for the component. */
    public final static String PROP_COMMON_LOG_LEVEL = "logLevel";

    private List<ConfigurationChangeListener> changeListeners = new ArrayList<ConfigurationChangeListener>();

    private Map<String, PropertySheet> symbolTable = new LinkedHashMap<String, PropertySheet>();
    private Map<String, RawPropertyData> rawPropertyMap = new HashMap<String, RawPropertyData>();
    private Map<String, String> globalProperties = new LinkedHashMap<String, String>();

    private boolean showCreations;


    public ConfigurationManager() {
    }


    /**
     * Creates a new configuration manager. Initial properties are loaded from the given URL. No need to keep the notion
     * of 'context' around anymore we will just pass around this property manager.
     *
     * @param url place to load initial properties from
     * @throws java.io.IOException if an error occurs while loading properties from the URL
     */
    public ConfigurationManager(URL url) throws IOException, PropertyException {
        SaxLoader saxLoader = new SaxLoader(url, globalProperties);
        rawPropertyMap = saxLoader.load();

        applySystemProperties(rawPropertyMap, globalProperties);
        ConfigurationManagerUtils.configureLogger(this);

        // we can't config the configuration manager with itself so we
        // do some of these config items manually.
        showCreations = "true".equals(globalProperties.get("showCreations"));
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
                    PropertySheet propertySheet = new PropertySheet(cls, this, rpd);

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
     * Gets all instances that are of the given type or are assignable to that type. Object.class matches all.
     *
     * @param type the desired type of instance
     * @return the set of all instances
     */
    public String[] getInstanceNames(Class<? extends Object> type) {
        return symbolTable.keySet().toArray(new String[]{});
    }


    /**
     * Returns all names of configurables registered to this instance. The resulting set includes instantiated and
     * noninstantiated components.
     *
     * @return all component named registered to this instance of <code>ConfigurationManager</code>
     */
    public List<String> getComponentNames() {
        return Arrays.asList(rawPropertyMap.keySet().toArray(new String[]{}));
    }


    /**
     * Looks up a configurable component by name. Creates it if necessary
     *
     * @param instanceName the name of the component
     * @return the compnent, or null if a component was not found.
     * @throws InstantiationException if the requested object could not be properly created, or is not a configurable
     *                                object.
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *                                if an error occurs while setting a property
     */
    public Configurable lookup(String instanceName) throws InstantiationException, PropertyException {
        // apply all new propeties to the model
        instanceName = getStrippedComponentName(instanceName);

        PropertySheet ps = getPropertySheet(instanceName);
        if (ps == null)
            return null;

        if (showCreations)
            System.out.println("Creating: " + instanceName);

        Configurable instance = ps.getOwner();
        instance.newProperties(ps);

        //todo registerCommonProperties -> register logLevel
        return instance;

    }


    /**
     * Adds a new configurable to this configuration manager.
     *
     * @param confClass The class of the configurable to be instantiated and to be added to this configuration manager
     *                  instance.
     * @param name      The desired  lookup-name of the configurable
     * @param props     The properties to be used for component configuration
     * @throws IllegalArgumentException if the there's already a component with the same <code>name</code> registered to
     *                                  this configuration manager instance.
     */
    public void addConfigurable(Class<? extends Configurable> confClass, String name, Map<String, Object> props) {
        if (name == null)
            name = confClass.getName();
        if (symbolTable.containsKey(name))
            throw new IllegalArgumentException("tried to override existing component name");

        PropertySheet ps = getPropSheetInstanceFromClass(confClass, props);
        symbolTable.put(name, ps);
        rawPropertyMap.put(name, new RawPropertyData(name, confClass.getName()));

        informListeners(name);
    }


    /**
     * Adds a new configurable to this configuration manager.
     *
     * @param confClass The class of the configurable to be instantiated and to be added to this configuration manager
     *                  instance.
     * @param name      The desired  lookup-name of the configurable
     * @throws IllegalArgumentException if the there's already a component with the same <code>name</code> registered to
     *                                  this configuration manager instance.
     */
    public void addConfigurable(Class<? extends Configurable> confClass, String name) {
        addConfigurable(confClass, name, new HashMap<String, Object>());
    }


    /**
     * Sets the property of the given component to the given value. Component must be an existing, instantiated
     * component
     *
     * @param instanceName an existing component
     * @param prop         the property name
     * @param value        the new value.
     */
    public void setProperty(String instanceName, String prop, String value) throws PropertyException {
        PropertySheet ps = symbolTable.get(instanceName);
        if (ps != null) {
            Configurable c = null;
            try {
                c = ps.getOwner();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }

            ps.setRaw(prop, value);

            assert c != null;
            c.newProperties(ps);

            informListeners(instanceName);
        } else {
            throw new PropertyException(null, prop, "Can't find component " + instanceName);
        }
    }


    /**
     * Gets the given property for the given component.  Returns either the string representation of the value or a list
     * of strings
     *
     * @param instanceName the component containing the property to lookup
     * @param propertyName the name of the property to lookup
     * @return the string representation of the property or a list of such strings
     */
    public Object getProperty(String instanceName, String propertyName) throws PropertyException {
        Object obj;

        PropertySheet ps = symbolTable.get(instanceName);

        if (ps != null) {
            obj = ps.getRawNoReplacement(propertyName);
        } else {
            throw new PropertyException(null, propertyName, "Can't find component " + instanceName);
        }
        return obj;
    }


    /**
     * Applies the system properties to the raw property map. System properties should be of the form
     * compName[paramName]=paramValue
     * <p/>
     * List types cannot currently be set from system properties.
     *
     * @param rawMap the map of raw property values
     * @param global global properies
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if an attempt is made to set a parameter for an unknown component.
     */
    private void applySystemProperties(Map<String, RawPropertyData> rawMap, Map<String, String> global)
            throws PropertyException {
        Properties props = System.getProperties();
        for (Enumeration e = props.keys(); e.hasMoreElements();) {
            String param = (String) e.nextElement();
            String value = props.getProperty(param);

            // search for params of the form component[param]=value
            // thise go in the property sheet for the component
            int lb = param.indexOf('[');
            int rb = param.indexOf(']');

            if (lb > 0 && rb > lb) {
                String compName = param.substring(0, lb);
                String paramName = param.substring(lb + 1, rb);
                RawPropertyData rpd = rawMap.get(compName);
                if (rpd != null) {
                    rpd.add(paramName, value);
                } else {
                    throw new PropertyException(null, param,
                            "System property attempting to set parameter "
                                    + " for unknown component " + compName
                                    + " (" + param + ")");
                }
            }

            // look for params of the form foo=fum
            // these go in the global map

            else if (param.indexOf('.') == -1) {
                String symbolName = "${" + param + "}";
                global.put(symbolName, value);
            }
        }
    }


    /**
     * Registers the properties commont to all components
     *
     * @param registry a component registry
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if an error occurs while registering the properties.
     */
    private void registerCommonProperties(Registry registry) throws PropertyException {
        registry.register(PROP_COMMON_LOG_LEVEL, PropertyType.STRING);
    }


    /** Returns a copy of the map of global properties set for this configuration manager. */
    public Map<String, String> getGlobalProperties() {
        return Collections.unmodifiableMap(globalProperties);
    }


    /**
     * Returns a global property.
     *
     * @param propertyName The name of the global property
     */
    public String getGlobalProperty(String propertyName) {
//        propertyName = propertyName.startsWith("$") ? propertyName : "${" + propertyName + "}";
        return globalProperties.get(propertyName);
    }


    public String getStrippedComponentName(String propertyName) {
        assert propertyName != null;

        while (propertyName.startsWith("$"))
            propertyName = globalProperties.get(ConfigurationManagerUtils.stripGlobalSymbol(propertyName));

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


    private void informListeners(String instanceName) {
        for (ConfigurationChangeListener changeListeners : this.changeListeners)
            changeListeners.configurationChanged(instanceName);
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
        if (!cm.getGlobalProperties().equals(getGlobalProperties()))
            return false;

        return true;
    }


    /**
     * Creates an instance of the given <code>Configurable</code> by using the default parameters as defined by the
     * class annotations to parameterize the component.
     */
    public static Configurable getDefaultInstance(Class<? extends Configurable> targetClass) throws InstantiationException, PropertyException {
        return getDefaultInstance(targetClass, new HashMap<String, Object>());
    }


    /**
     * Creates an instance of the given <code>Configurable</code> by using the default parameters as defined by the
     * class annotations to parameterize the component. Default prarmeters will be overrided if a their names are
     * containd in the given <code>props</code>-map
     */
    public static Configurable getDefaultInstance(Class<? extends Configurable> targetClass, Map<String, Object> props) throws InstantiationException, PropertyException {
        PropertySheet ps = getPropSheetInstanceFromClass(targetClass, props);
        Configurable configurable = ps.getOwner();
        configurable.newProperties(ps);
        return configurable;
    }


    /**
     * Instantiates the given <code>targetClass</code> and instruments it using default properties or the properties
     * given by the <code>defaultProps</code>.
     */
    private static PropertySheet getPropSheetInstanceFromClass(Class<? extends Configurable> targetClass, Map<String, Object> defaultProps) {
        try {
            ConfigurationManager ConfigurationManager = new ConfigurationManager();
            RawPropertyData rpd = new RawPropertyData(null, targetClass.getName());

            for (String confName : defaultProps.keySet()) {
                Object property = defaultProps.get(confName);

//                if (property instanceof Configurable) {
//                    RawPropertyData dummyRPD = new RawPropertyData(confName, property.getClass().getName());
//                    ConfigurationManager.symbolTable.put(confName, new PropertySheet((Configurable) property, ConfigurationManager, dummyRPD));
//                } else
                if (property instanceof Class)
                    property = ((Class) property).getName();

                rpd.getProperties().put(confName, property);
            }


            Configurable configurable = targetClass.newInstance();
            return new PropertySheet(configurable, ConfigurationManager, rpd);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

}

