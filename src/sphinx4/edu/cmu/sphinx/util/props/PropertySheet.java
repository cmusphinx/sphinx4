package edu.cmu.sphinx.util.props;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A property sheet which  defines a collection of properties for a single component in the system.
 *
 * @author Holger Brandl
 */
public class PropertySheet {

    public enum PropertyType {

        INT, DOUBLE, BOOL, COMP, STRING, COMPLIST
    }

    private Map<String, S4PropWrapper> registeredProperties = new HashMap<String, S4PropWrapper>();
    private Map<String, Object> propValues = new HashMap<String, Object>();

    /**
     * Maps the names of the component properties to their (possibly unresolved) values.
     * <p/>
     * Example: <code>frontend</code> to <code>${myFrontEnd}</code>
     */
    private Map<String, Object> rawProps = new HashMap<String, Object>();

    private ConfigurationManager cm;
    private Configurable owner;
    private final Class<? extends Configurable> ownerClass;

    private String instanceName;


    public PropertySheet(Configurable configurable, String name, RawPropertyData rpd, ConfigurationManager ConfigurationManager) {
        this(configurable.getClass(), name, ConfigurationManager, rpd);
        owner = configurable;
    }


    public PropertySheet(Class<? extends Configurable> confClass, String name, ConfigurationManager cm, RawPropertyData rpd) {
        ownerClass = confClass;
        this.cm = cm;
        this.instanceName = name;

        processAnnotations(this, confClass);

        // now apply all xml properties
        Map<String, Object> flatProps = rpd.flatten(cm).getProperties();
        rawProps = new HashMap<String, Object>(rpd.getProperties());

        for (String propName : rawProps.keySet())
            propValues.put(propName, flatProps.get(propName));
    }


    /**
     * Registers a new property which type and default value are defined by the given sphinx property.
     *
     * @param propName The name of the property to be registered.
     * @param property The property annoation masked by a proxy.
     */
    private void registerProperty(String propName, S4PropWrapper property) {
        assert property != null && propName != null;

        registeredProperties.put(propName, property);
        propValues.put(propName, null);
        rawProps.put(propName, null);
    }


    /** Returns the property names <code>name</code> which is still wrapped into the annotation instance. */
    public S4PropWrapper getProperty(String name, Class propertyClass) throws PropertyException {
        if (!propValues.containsKey(name))
            throw new PropertyException(getInstanceName(), name,
                    "Unknown property '" + name + "' ! Make sure that you've annotated it.");

        S4PropWrapper s4PropWrapper = registeredProperties.get(name);

        try {
            propertyClass.cast(s4PropWrapper.getAnnotation());
        } catch (Exception e) {
            throw new PropertyException(getInstanceName(), name, name + " is not an annotated sphinx property of '" + owner.getClass().getName() + "' !");
        }

        return s4PropWrapper;
    }


    /**
     * Gets the value associated with this name
     *
     * @param name the name
     * @return the value
     */
    public String getString(String name) throws PropertyException {
        S4PropWrapper s4PropWrapper = getProperty(name, S4String.class);
        S4String s4String = ((S4String) s4PropWrapper.getAnnotation());

        if (propValues.get(name) == null) {
            String defValue = s4String.defaultValue();
            defValue = defValue.equals("nullnullnull") ? null : defValue;
            propValues.put(name, defValue);
        }

        String propValue = (String) propValues.get(name);

        //check range
        List<String> range = Arrays.asList(s4String.range());
        if (!range.isEmpty() && !range.contains(propValue))
            throw new PropertyException(getInstanceName(), name, " is not in range (" + range + ")");

        return propValue;
    }


    /**
     * Gets the value associated with this name
     *
     * @param name the name
     * @return the value
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the named property is not of this type
     */
    public int getInt(String name) throws PropertyException {
        S4PropWrapper s4PropWrapper = getProperty(name, S4Integer.class);
        S4Integer s4Integer = (S4Integer) s4PropWrapper.getAnnotation();

        if (propValues.get(name) == null)
            propValues.put(name, s4Integer.defaultValue());

        Object propObject = propValues.get(name);
        Integer propValue = propObject instanceof Integer ? (Integer) propObject : Integer.decode((String) propObject);

        int[] range = s4Integer.range();
        if (range.length != 2)
            throw new PropertyException(getInstanceName(), name, range + " is not of expected range type, which is {minValue, maxValue)");

        if (propValue < range[0] || propValue > range[1])
            throw new PropertyException(getInstanceName(), name, " is not in range (" + range + ")");

        return propValue;
    }


    /**
     * Gets the value associated with this name
     *
     * @param name the name
     * @return the value
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the named property is not of this type
     */
    public float getFloat(String name) throws PropertyException {
        return ((Double) getDouble(name)).floatValue();
    }


    /**
     * Gets the value associated with this name
     *
     * @param name the name
     * @return the value
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the named property is not of this type
     */
    public double getDouble(String name) throws PropertyException {
        S4PropWrapper s4PropWrapper = getProperty(name, S4Double.class);
        S4Double s4Double = (S4Double) s4PropWrapper.getAnnotation();

        if (propValues.get(name) == null)
            propValues.put(name, s4Double.defaultValue());

        Object propObject = propValues.get(name);
        Double propValue = propObject instanceof Double ? (Double) propObject : Double.valueOf((String) propObject);

        double[] range = s4Double.range();
        if (range.length != 2)
            throw new PropertyException(getInstanceName(), name, range + " is not of expected range type, which is {minValue, maxValue)");

        if (propValue < range[0] || propValue > range[1])
            throw new PropertyException(getInstanceName(), name, " is not in range (" + range + ")");

        return propValue;
    }


    /**
     * Gets the value associated with this name
     *
     * @param name the name
     * @return the value
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the named property is not of this type
     */
    public Boolean getBoolean(String name) throws PropertyException {
        S4PropWrapper s4PropWrapper = getProperty(name, S4Boolean.class);
        S4Boolean s4Boolean = (S4Boolean) s4PropWrapper.getAnnotation();

        if (propValues.get(name) == null && !s4Boolean.isNotDefined())
            propValues.put(name, s4Boolean.defaultValue());

        Object propValue = propValues.get(name);
        if (propValue instanceof String)
            propValue = Boolean.valueOf((String) propValue);

        return (Boolean) propValue;
    }


    /**
     * Gets a component associated with the given parameter name
     *
     * @param name the parameter name
     * @return the component associated with the name
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the component does not exist or is of the wrong type.
     */
    public Configurable getComponent(String name) throws PropertyException {
        S4PropWrapper s4PropWrapper = getProperty(name, S4Component.class);

        S4Component s4Component = (S4Component) s4PropWrapper.getAnnotation();
        Class expectedType = s4Component.type();

        if (propValues.get(name) == null || propValues.get(name) instanceof String) {
            Configurable configurable = null;

            try {
                if (propValues.get(name) != null) {
                    PropertySheet ps = cm.getPropertySheet((String) propValues.get(name));
                    if (ps != null)
                        configurable = ps.getOwner();
                }

                if (configurable != null && !expectedType.isInstance(configurable))
                    throw new PropertyException(getInstanceName(), name, "mismatch between annoation and component type");

                if (configurable == null) {
                    Class<? extends Configurable> defClass;

                    if (propValues.get(name) != null)
                        defClass = (Class<? extends Configurable>) Class.forName((String) propValues.get(name));
                    else
                        defClass = s4Component.defaultClass();

                    if (Modifier.isAbstract(defClass.getModifiers()))
                        throw new PropertyException(getInstanceName(), name, defClass.getName() + " is abstract!");

                    // because we're forced to use the default type, assert that it is set
                    if (defClass.equals(Configurable.class))
                        throw new PropertyException(getInstanceName(), name, instanceName + ": no default class defined for " + name);

                    configurable = ConfigurationManager.getDefaultInstance(defClass);
                }

            } catch (InstantiationException e) {
                throw new PropertyException(getInstanceName(), name, "can not instantiate class");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            assert configurable != null;
            propValues.put(name, configurable);
        }

        return (Configurable) propValues.get(name);
    }


    /** Returns the class of of a registered component property without instantiating it. */
    public Class<? extends Configurable> getComponentClass(String propName) {
        Class<? extends Configurable> defClass = null;

        if (propValues.get(propName) != null)
            try {
                defClass = (Class<? extends Configurable>) Class.forName((String) propValues.get(propName));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        else {
            S4Component comAnno = (S4Component) registeredProperties.get(propName).getAnnotation();
            defClass = comAnno.defaultClass();
            if (comAnno.mandatory())
                defClass = null;
        }

        return defClass;
    }


    /**
     * Gets a list of components associated with the given parameter name
     *
     * @param name the parameter name
     * @return the component associated with the name
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the component does not exist or is of the wrong type.
     */
    public List getComponentList(String name) throws PropertyException {
        getProperty(name, S4ComponentList.class);

        List components = (List) propValues.get(name);

        assert registeredProperties.get(name).getAnnotation() instanceof S4ComponentList;
        S4ComponentList annoation = (S4ComponentList) registeredProperties.get(name).getAnnotation();

        // no componets names are available and no comp-list was yet loaded
        // therefore load the default list of components from the annoation
        if (components == null) {
            List<Class<? extends Configurable>> defClasses = Arrays.asList(annoation.defaultList());
            components = new ArrayList();

            for (Class<? extends Configurable> defClass : defClasses) {
                try {
                    components.add(ConfigurationManager.getDefaultInstance(defClass));
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
            }

            propValues.put(name, components);
        }

        if (!components.isEmpty() && !(components.get(0) instanceof Configurable)) {

            List<Configurable> list = new ArrayList<Configurable>();

            for (Object componentName : components)
                try {
                    Configurable configurable = cm.lookup((String) componentName);
                    assert configurable != null;
                    list.add(configurable);
                } catch (InstantiationException e) {
                    throw new PropertyException(getInstanceName(), name, "instantiation of list element failed.");
                }

            propValues.put(name, list);
        }

        return (List) propValues.get(name);
    }


    public String getInstanceName() {
        return instanceName;
    }


    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }


    /** Returns true if the owner of this property sheet is already instanciated. */
    public boolean isInstanciated() {
        return !(owner == null);
    }


    /**
     * Returns the owner of this property sheet. In most cases this will be the configurable instance which was
     * instrumented by this property sheet.
     */
    public synchronized Configurable getOwner() throws InstantiationException, PropertyException {
        try {

            if (!isInstanciated()) {
                owner = ownerClass.newInstance();
                owner.newProperties(this);
            }
        } catch (IllegalAccessException e) {
            throw new InstantiationException("Can't access class " + ownerClass);
        } catch (InstantiationException e) {
            throw new InstantiationException("Not configurable class " + ownerClass);
        }

        return owner;
    }


    /** Returns the class of the owner configurable of this property sheet. */
    public Class<? extends Configurable> getConfigurableClass() {
        return ownerClass;
    }


    /**
     * Sets the given property to the given name
     *
     * @param name the simple property name
     */
    public void setString(String name, String value) throws PropertyException {
        // ensure that there is such a property
        assert registeredProperties.keySet().contains(name) : "'" + name + "' is not a registered compontent";

        Proxy annotation = registeredProperties.get(name).getAnnotation();
        assert annotation instanceof S4String;

        applyConfigurationChange(name, value, value);
    }


    /**
     * Sets the given property to the given name
     *
     * @param name  the simple property name
     * @param value the value for the property
     */
    public void setInt(String name, int value) throws PropertyException {
        // ensure that there is such a property
        assert registeredProperties.keySet().contains(name) : "'" + name + "' is not a registered compontent";

        Proxy annotation = registeredProperties.get(name).getAnnotation();
        assert annotation instanceof S4Integer;

        applyConfigurationChange(name, value, value);
    }


    /**
     * Sets the given property to the given name
     *
     * @param name  the simple property name
     * @param value the value for the property
     */
    public void setDouble(String name, double value) throws PropertyException {
        // ensure that there is such a property
        assert registeredProperties.keySet().contains(name) : "'" + name + "' is not a registered compontent";

        Proxy annotation = registeredProperties.get(name).getAnnotation();
        assert annotation instanceof S4Double;

        applyConfigurationChange(name, value, value);
    }


    /**
     * Sets the given property to the given name
     *
     * @param name  the simple property name
     * @param value the value for the property
     */
    public void setBoolean(String name, boolean value) throws PropertyException {
        // ensure that there is such a property
        assert registeredProperties.keySet().contains(name) : "'" + name + "' is not a registered compontent";

        Proxy annotation = registeredProperties.get(name).getAnnotation();
        assert annotation instanceof S4Boolean;

        applyConfigurationChange(name, value, value);
    }


    /**
     * Sets the given property to the given name
     *
     * @param name   the simple property name
     * @param cmName the name of the configurable within the configuration manager (required for serialization only)
     * @param value  the value for the property
     */
    public void setComponent(String name, String cmName, Configurable value) throws PropertyException {
        // ensure that there is such a property
        assert registeredProperties.keySet().contains(name) : "'" + name + "' is not a registered compontent";

        Proxy annotation = registeredProperties.get(name).getAnnotation();
        assert annotation instanceof S4Component;

        applyConfigurationChange(name, cmName, value);
    }


    /**
     * Sets the given property to the given name
     *
     * @param name       the simple property name
     * @param valueNames the list of names of the configurables within the configuration manager (required for
     *                   serialization only)
     * @param value      the value for the property
     */
    public void setComponentList(String name, List<String> valueNames, List<Configurable> value) throws PropertyException {
        // ensure that there is such a property
        assert registeredProperties.keySet().contains(name) : "'" + name + "' is not a registered compontent";

        Proxy annotation = registeredProperties.get(name).getAnnotation();
        assert annotation instanceof S4ComponentList;

//        assert valueNames.size() == value.size();

        rawProps.put(name, valueNames);
        propValues.put(name, value);

        applyConfigurationChange(name, valueNames, value);
    }


    private void applyConfigurationChange(String name, Object cmName, Object value) throws PropertyException {
        rawProps.put(name, cmName);
        propValues.put(name, value);

        if (getInstanceName() != null)
            cm.fireConfChanged(getInstanceName());

        if (owner != null)
            owner.newProperties(this);
    }


    /**
     * Sets the raw property to the given name
     *
     * @param key the simple property name
     * @param val the value for the property
     */
    public void setRaw(String key, Object val) {
        rawProps.put(key, val);
    }


    /**
     * Gets the raw value associated with this name
     *
     * @param name the name
     * @return the value as an object (it could be a String or a String[] depending upon the property type)
     */
    public Object getRaw(String name) {
        return rawProps.get(name);
    }


    /**
     * Gets the raw value associated with this name, no global symbol replacement is performed.
     *
     * @param name the name
     * @return the value as an object (it could be a String or a String[] depending upon the property type)
     */
    public Object getRawNoReplacement(String name) {
        return rawProps.get(name);
    }


    /** Returns the type of the given property. */
    public PropertyType getType(String propName) {
        Proxy annotation = registeredProperties.get(propName).getAnnotation();
        if (annotation instanceof S4Component)
            return PropertyType.COMP;
        else if (annotation instanceof S4ComponentList)
            return PropertyType.COMPLIST;
        else if (annotation instanceof S4Integer)
            return PropertyType.INT;
        else if (annotation instanceof S4Double)
            return PropertyType.DOUBLE;
        else if (annotation instanceof S4Boolean)
            return PropertyType.BOOL;
        else if (annotation instanceof S4String)
            return PropertyType.STRING;
        else
            throw new RuntimeException("Unknown property type");
    }


    /**
     * Gets the owning property manager
     *
     * @return the property manager
     */
    ConfigurationManager getPropertyManager() {
        return cm;
    }


    /**
     * Returns a logger to use for this configurable component. The logger can be configured with the property:
     * 'logLevel' - The default logLevel value is defined (within the xml configuration file by the global property
     * 'defaultLogLevel' (which defaults to WARNING).
     * <p/>
     * implementation note: the logger became configured within the constructor of the parenting configuration manager.
     *
     * @return the logger for this component
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if an error occurs
     */
    public Logger getLogger() {
        Logger logger;

        if (instanceName != null) {
            logger = Logger.getLogger(ownerClass.getName() + "." + instanceName);
        } else
            logger = Logger.getLogger(ownerClass.getName());

        // if there's a logLevel set for component apply to the logger
        if (rawProps.get("logLevel") != null)
            logger.setLevel(Level.parse((String) rawProps.get("logLevel")));

        return logger;

    }


    /** Returns the names of registered properties of this PropertySheet object. */
    public Collection<String> getRegisteredProperties() {
        return Collections.unmodifiableCollection(registeredProperties.keySet());
    }


    /**
     * Returns true if two property sheet define the same object in terms of configuration. The owner (and the parent
     * configuration manager) are not expected to be the same.
     */
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PropertySheet))
            return false;

        PropertySheet ps = (PropertySheet) obj;
        if (!rawProps.keySet().equals(ps.rawProps.keySet()))
            return false;

        // maybe we could test a little bit more here. suggestions?
        return true;
    }


    /**
     * use annotation based class parsing to detect the configurable properties of a <code>Configurable</code>-class
     *
     * @param propertySheet of type PropertySheet
     * @param configurable  of type Class<? extends Configurable>
     */
    public static void processAnnotations(PropertySheet propertySheet, Class<? extends Configurable> configurable) {
        Field[] classFields = configurable.getFields();

        for (Field field : classFields) {
            Annotation[] annotations = field.getAnnotations();

            for (Annotation annotation : annotations) {
                Annotation[] superAnnotations = annotation.annotationType().getAnnotations();

                for (Annotation superAnnotation : superAnnotations) {
                    if (superAnnotation instanceof S4Property) {
                        int fieldModifiers = field.getModifiers();
                        assert Modifier.isStatic(fieldModifiers) : "property fields are assumed to be static";
                        assert Modifier.isPublic(fieldModifiers) : "property fields are assumed to be public";
                        assert field.getType().equals(String.class) : "properties fields are assumed to be instances of java.lang.String";

                        try {
                            String propertyName = (String) field.get(null);
                            propertySheet.registerProperty(propertyName, new S4PropWrapper((Proxy) annotation));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        }
    }
}
