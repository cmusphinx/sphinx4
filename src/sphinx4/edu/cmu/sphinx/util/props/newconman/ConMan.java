package edu.cmu.sphinx.util.props.newconman;

import edu.cmu.sphinx.util.props.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;


/** A configuration manager which enables xml-based system configuration.  ...to be continued! */
public class ConMan {

    /** A common property (used by all components) that sets the log level for the component. */
    public final static String PROP_COMMON_LOG_LEVEL = "logLevel";


    private Map<String, SimpleSymbol> symbolTable = new LinkedHashMap<String, SimpleSymbol>();
    private Map<String, RawPropertyData> rawPropertyMap;
    private Map<String, String> globalProperties = new LinkedHashMap<String, String>();

    private boolean showCreations;


    private ConMan() {
    }


    /**
     * Creates a new configuration manager. Initial properties are loaded from the given URL. No need to keep the notion
     * of 'context' around anymore we will just pass around this property manager.
     *
     * @param url place to load initial properties from
     * @throws java.io.IOException if an error occurs while loading properties from the URL
     */
    public ConMan(URL url) throws IOException, PropertyException {
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
    public PropSheet getPropertySheet(String instanceName) {
        PropSheet propertySheet = null;
        SimpleSymbol symbol = symbolTable.get(instanceName);
        if (symbol != null) {
            propertySheet = symbol.getPropertySheet();
        }
        return propertySheet;
    }


    /**
     * Gets all instances that are of the given type or are assignable to that type. Object.class matches all.
     *
     * @param type the desired type of instance
     * @return the set of all instances
     */
    public String[] getInstanceNames(Class<? extends Object> type) {
        List<String> list = new ArrayList<String>();
        for (SimpleSymbol symbol : symbolTable.values()) {
            if (type.isInstance(symbol.getObject())) {
                list.add(symbol.getName());
            }
        }
        return list.toArray(new String[list.size()]);
    }


    /**
     * Looks up a configurable component by name. Creates it if necessary
     *
     * @param name the name of the component
     * @return the compnent, or null if a component was not found.
     * @throws InstantiationException if the requested object could not be properly created, or is not a configurable
     *                                object.
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *                                if an error occurs while setting a property
     */
    public SimpleConfigurable lookup(String name) throws InstantiationException,
            PropertyException {
        SimpleSymbol symbol = symbolTable.get(name);
        if (symbol == null) {
            if (showCreations) {
                System.out.println("Creating: " + name);
            }
            // it is not in the symbol table, so construct
            // it based upon our raw property data
            RawPropertyData rpd = rawPropertyMap.get(name);
            if (rpd != null) {
                String className = rpd.getClassName();
                try {
                    Class cls = Class.forName(className);
                    SimpleConfigurable configurable = (SimpleConfigurable) cls.newInstance();
//                    Registry registry = new Registry(configurable);
//                    registerCommonProperties(registry);
//                    configurable.register(name, registry);  // no register anymore :-)

                    // now load the property-sheet by using the class annotation
                    PropSheet propertySheet = new PropSheetImpl(configurable, this, rpd.flatten(globalProperties));

                    symbol = new SimpleSymbol(name, propertySheet, configurable);
                    symbolTable.put(name, symbol);

                    // apply all new propeties to the model
                    configurable.newProperties(propertySheet);

                    //todo registerCommonProperties -> register logLevel
                    return symbol.getObject();
                } catch (ClassNotFoundException e) {
                    throw new InstantiationException("Can't find class "
                            + className + " object:" + name);
                } catch (IllegalAccessException e) {
                    throw new InstantiationException("Can't access class "
                            + className + " object:" + name);
                } catch (ClassCastException e) {
                    throw new InstantiationException("Not configurable class "
                            + className + " object:" + name);
                }
            }
        } else {
            return symbol.getObject();
        }
        return null;
    }


    /**
     * Sets the property of the given component to the given value. Component must be an existing, instantiated
     * component
     *
     * @param component an existing component
     * @param prop      the property name
     * @param value     the new value.
     */
    public void setProperty(String component, String prop, String value) throws PropertyException {
        SimpleSymbol symbol = symbolTable.get(component);
        if (symbol != null) {
            PropSheet ps = symbol.getPropertySheet();
            SimpleConfigurable c = symbol.getObject();
//            Object old = ps.getRawNoReplacement(prop);
            ps.setRaw(prop, value);
            c.newProperties(ps);
        } else {
            throw new PropertyException(null, prop,
                    "Can't find component " + component);
        }
    }


    /**
     * Gets the given property for the given component.  Returns either the string representation of the value or a list
     * of strings
     *
     * @param component    the component containing the property to lookup
     * @param propertyName the name of the property to lookup
     * @return the string representation of the property or a list of such strings
     */
    public Object getProperty(String component, String propertyName) throws PropertyException {
        Object obj;

        SimpleSymbol symbol = symbolTable.get(component);
        if (symbol != null) {
            PropSheet ps = symbol.getPropertySheet();
            obj = ps.getRawNoReplacement(propertyName);
        } else {
            throw new PropertyException(null, propertyName,
                    "Can't find component " + component);
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
    private void registerCommonProperties(Registry registry)
            throws PropertyException {
        registry.register(PROP_COMMON_LOG_LEVEL, PropertyType.STRING);
    }


    /**
     * Retrieves the global log level
     *
     * @return the global log level
     */
    String getGlobalLogLevel() {
        String level = globalProperties.get("logLevel");
        if (level == null) {
            level = Level.WARNING.getName();
        }

        return level;
    }


    /**
     * Creates an instance of the given @class Configurable. Uses default parameters as defined by the property names
     * annotations to parameterize the component.
     */
    public static SimpleConfigurable getDefaultInstance(Class<? extends SimpleConfigurable> targetClass) {
        return getDefaultInstance(targetClass, new HashMap<String, Object>());
    }


    public static SimpleConfigurable getDefaultInstance(Class<? extends SimpleConfigurable> targetClass, Map<String, Object> defaultProps) {
        try {
            ConMan conMan = new ConMan();
            RawPropertyData rpd = new RawPropertyData(null, targetClass.getName());

            for (String confName : defaultProps.keySet()) {
                Object property = defaultProps.get(confName);

                if (property instanceof SimpleConfigurable)
                    conMan.symbolTable.put(confName, new SimpleSymbol(confName, null, (SimpleConfigurable) property));
                else
                    rpd.getProperties().put(confName, property);
            }


            SimpleConfigurable configurable = targetClass.newInstance();
            PropSheetImpl ps = new PropSheetImpl(configurable, conMan, rpd);
            PropSheetImpl.processAnnotations(ps, targetClass);

            configurable.newProperties(ps);

            return configurable;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (PropertyException e) {
            e.printStackTrace();
        }

        return null;

    }


    // todo remove me
    public Map<String, SimpleSymbol> getSymbols() {
        return Collections.unmodifiableMap(symbolTable);
    }


    public Map<String, String> getGlobalProperties() {
        return Collections.unmodifiableMap(globalProperties);
    }
}

