package edu.cmu.sphinx.util.props.newconman;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.RawPropertyData;

import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A property sheet which  defines a collection of properties for a single component in the system.
 *
 * @author Holger Brandl
 */
public class PropSheet {

    public enum PropertyType {

        INT, DOUBLE, BOOL, COMP, STRING, COMPLIST
    }

    Map<String, S4PropWrapper> registeredProperties = new HashMap<String, S4PropWrapper>();
    Map<String, Object> propValues = new HashMap<String, Object>();

    /**
     * Maps the names of the component properties to their (possibly unresolved) values.
     * <p/>
     * Example: <code>frontend</code> to <code>${myFrontEnd}</code>
     */
    private Map<String, Object> rawProps = new HashMap<String, Object>();

    private SimpleConfigurable owner; //todo  use this one as owner for all PropertyExceptions as soon as the exception is refactored

    private ConMan cm;
    private final Class<? extends SimpleConfigurable> ownerClass;


    public PropSheet(SimpleConfigurable configurable, ConMan conMan, RawPropertyData rpd) {
        this(configurable.getClass(), conMan, rpd);
        owner = configurable;
    }


    public PropSheet(Class<? extends SimpleConfigurable> confClass, ConMan conMan, RawPropertyData rpd) {
        ownerClass = confClass;
        this.cm = conMan;

        processAnnotations(this, confClass);

        // now apply all xml properties
        Map<String, Object> flatProps = rpd.flatten(conMan.getGlobalProperties()).getProperties();
        rawProps = new HashMap<String, Object>(rpd.getProperties());

        for (String propName : rawProps.keySet()) {
            propValues.put(propName, flatProps.get(propName));

//            if (propVal instanceof SimpleConfigurable)
//                propValues.put(propName, new CompWrapper(propName, (SimpleConfigurable) propVal));
//            else if (propVal instanceof List)
//                propValues.put(propName, new CompListWrapper(new ArrayList<String>(), (List<SimpleConfigurable>) propVal));
//            else
        }
    }


    /**
     * Registers a new property which type and default value are defined by the given sphinx property.
     *
     * @param propName The name of the property to be registered.
     * @param property The property annoation masked by a proxy.
     */
    public void registerProperty(String propName, S4PropWrapper property) {
        assert property != null && propName != null;

        registeredProperties.put(propName, property);
        propValues.put(propName, null);
        rawProps.put(propName, null);
    }


    /** Returns the property names <code>name</code> which is still wrapped into the annotation instance. */
    S4PropWrapper getProperty(String name, Class propertyClass) throws PropertyException {
        if (!propValues.containsKey(name))
            throw new PropertyException(null, name, "Unknown property");

        S4PropWrapper s4PropWrapper = registeredProperties.get(name);

        try {
            propertyClass.cast(s4PropWrapper.getAnnotation());
        } catch (Exception e) {
            throw new PropertyException(null, name, name + " is not a registered of type " + propertyClass);
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

        if (propValues.get(name) == null)
            propValues.put(name, s4String.defaultValue());

        String propValue = (String) propValues.get(name);

        //check range
        List<String> range = Arrays.asList(s4String.range());
        if (!range.isEmpty() && !range.contains(propValue))
            throw new PropertyException(null, name, " is not in range (" + range + ")");

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
            throw new PropertyException(null, name, range + " is not of expected range type, which is {minValue, maxValue)");

        if (propValue < range[0] || propValue > range[1])
            throw new PropertyException(null, name, " is not in range (" + range + ")");

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
            throw new PropertyException(null, name, range + " is not of expected range type, which is {minValue, maxValue)");

        if (propValue < range[0] || propValue > range[1])
            throw new PropertyException(null, name, " is not in range (" + range + ")");

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
    public boolean getBoolean(String name) throws PropertyException {
        S4PropWrapper s4PropWrapper = getProperty(name, S4Boolean.class);
        S4Boolean s4Boolean = (S4Boolean) s4PropWrapper.getAnnotation();

        if (propValues.get(name) == null)
            propValues.put(name, s4Boolean.defaultValue());

        Object propValue = propValues.get(name);
        return propValue instanceof Boolean ? (Boolean) propValue : Boolean.getBoolean((String) propValue);
    }


    /**
     * Gets a component associated with the given parameter name
     *
     * @param name the parameter name
     * @return the component associated with the name
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the component does not exist or is of the wrong type.
     */
    public SimpleConfigurable getComponent(String name) throws PropertyException {
        S4PropWrapper s4PropWrapper = getProperty(name, S4Component.class);

        S4Component s4Component = ((S4Component) s4PropWrapper.getAnnotation());
        Class expectedType = s4Component.type();

        if (propValues.get(name) == null || propValues.get(name) instanceof String) {
            SimpleConfigurable configurable;

            try {
                configurable = cm.lookup((String) propValues.get(name));

                if (!expectedType.isInstance(configurable) && !s4Component.mandatory())
                    throw new PropertyException(null, name, "mismatch between annoation and component type");
            } catch (InstantiationException e) {
                throw new PropertyException(null, name, "can not instantiate class");
            }

            assert configurable != null;
            propValues.put(name, configurable);
        }

        return (SimpleConfigurable) propValues.get(name);
    }


    private class CompWrapper {

        public final String lookupName;
        public final SimpleConfigurable configurable;


        public CompWrapper(String lookupName, SimpleConfigurable configurable) {
            this.lookupName = lookupName;
            this.configurable = configurable;
        }
    }


    /**
     * Gets a list of components associated with the given parameter name
     *
     * @param name the parameter name
     * @return the component associated with the name
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the component does not exist or is of the wrong type.
     */
    public List<SimpleConfigurable> getComponentList(String name) throws PropertyException {
        getProperty(name, S4ComponentList.class);

        List<String> components = (List) propValues.get(name);
        if (!components.isEmpty() && components.get(0) instanceof String) {
            Proxy annoation = registeredProperties.get(name).getAnnotation();

            if (annoation instanceof S4ComponentList) {
                List<SimpleConfigurable> list = new ArrayList<SimpleConfigurable>();

                for (String componentName : components)
                    try {
                        list.add(cm.lookup(componentName));
                    } catch (InstantiationException e) {
                        throw new PropertyException(null, name, "instantiation of list element failed.");
                    }

                propValues.put(name, list);
            }
//                propValues.put(name, new ArrayList<SimpleConfigurable>());
        }

        return (List<SimpleConfigurable>) propValues.get(name);
    }


    private class CompListWrapper {

        public final List<String> compNames;
        public final List<SimpleConfigurable> compList;


        public CompListWrapper(List<String> compNames, List<SimpleConfigurable> compList) {
            this.compNames = compNames;
            this.compList = compList;
        }
    }


    /**
     * Sets the raw property to the given name
     *
     * @param key the simple property name
     * @param val the value for the property
     */
    public void setRaw(String key, Object val) {

    }


    /**
     * Returns the owner of this property sheet. In most cases this will be the configurable instance which was
     * instrumented by this property sheet.
     */
    public synchronized SimpleConfigurable getOwner() throws InstantiationException {
        if (owner == null)
            try {
                owner = ownerClass.newInstance();
            } catch (IllegalAccessException e) {
                throw new InstantiationException("Can't access class " + ownerClass);
            } catch (InstantiationException e) {
                throw new InstantiationException("Not configurable class " + ownerClass);
            }

        return owner;
    }


    /** Returns the class of the owner configurable of this property sheet. */
    public Class getConfigurableClass() {
        return ownerClass;
    }


    /**
     * Gets a resource associated with the given parameter name
     *
     * @param name the parameter name
     * @return the resource associated with the name or NULL if it doesn't exist.
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the resource cannot be found
     */
    public URL getResource(String name) {
        return null;
    }


    /**
     * Gets the list of strings associated with this name
     *
     * @param name the name
     * @return an array (possibly empty) of configurable strings
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the named property is not of this type
     */
    public List getStrings(String name) {
        return null;
    }


    /**
     * Retrieves the names of all the properties currently defined for this property sheet
     *
     * @return the array of names
     */
    public String[] getNames() {
        return new String[0];
    }


    /**
     * Sets the given property to the given name
     *
     * @param name the simple property name
     */
    public void setString(String name) {

    }


    /**
     * Sets the given property to the given name
     *
     * @param name  the simple property name
     * @param value the value for the property
     */
    public void setInt(String name, int value) {

    }


    /**
     * Sets the given property to the given name
     *
     * @param name the simple property name
     */
    public void setFloat(String name) {
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
        return null;
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
    public ConMan getPropertyManager() {
        return cm;
    }


    /**
     * Returns a logger to use for this configurable component. The logger can be configured with the property:
     * 'logLevel' - The default logLevel value is define by the global property 'defaultLogLevel' (which defaults to
     * WARNING).
     *
     * @return the logger for this component
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if an error occurs
     */
    public Logger getLogger() {
        Logger logger = null;
        try {
            logger = Logger.getLogger(getOwner().getName());
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
//        Level level = getLogLevel();
        logger.setLevel(Level.FINE);
        return logger;
    }


    /**
     * Dumps this sheet to the given stream
     *
     * @param out the print stream to dump the sheet on
     */
    public void dump(PrintStream out) {

    }


    /** Returns the names of registered properties of this PropSheet object. */
    public Collection<String> getRegisteredProperties() {
        return Collections.unmodifiableCollection(registeredProperties.keySet());
    }


    /**
     * Returns true if two property sheet define the same object in terms of configuration. The owner (and the parent
     * configuration manager) are not expected to be the same.
     */
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PropSheet))
            return false;

        PropSheet ps = (PropSheet) obj;
        if (!rawProps.keySet().equals(ps.rawProps.keySet()))
            return false;

        // maybe we could test a little bit more here. suggestions?
        return true;
    }


    /**
     * use annotation based class parsing to detect the configurable properties of a <code>Configurable</code>-class
     *
     * @param propertySheet of type PropSheet
     * @param configurable  of type Class<? extends SimpleConfigurable>
     */
    public static void processAnnotations(PropSheet propertySheet, Class<? extends SimpleConfigurable> configurable) {
        Field[] classFields = configurable.getFields();

        for (Field field : classFields) {
            Annotation[] annotations = field.getAnnotations();

            for (Annotation annotation : annotations) {
                Annotation[] superAnnotations = annotation.annotationType().getAnnotations();

                for (Annotation superAnnotation : superAnnotations) {
                    if (superAnnotation instanceof S4Property) {
                        int fieldModifiers = field.getModifiers();
                        assert Modifier.isStatic(fieldModifiers);
                        assert Modifier.isPublic(fieldModifiers);
                        assert field.getType().equals(String.class);

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
