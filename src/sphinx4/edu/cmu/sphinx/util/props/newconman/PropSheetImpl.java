package edu.cmu.sphinx.util.props.newconman;

import edu.cmu.sphinx.util.props.ConfigurationManager;
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

/** @author Holger Brandl */
public class PropSheetImpl implements PropSheet {

    Map<String, S4PropWrapper> registeredProperties = new HashMap<String, S4PropWrapper>();
    Map<String, Object> propValues = new HashMap<String, Object>();

    SimpleConfigurable owner; //todo  this one will be used as owner for all PropertyExceptions as soon as the exception is refactored

    private ConMan cm;


    public PropSheetImpl(SimpleConfigurable owner, ConMan conMan, RawPropertyData rpd) throws PropertyException, InstantiationException {
        this.owner = owner;
        this.cm = conMan;

        processAnnotations(this, owner.getClass());

        // now apply all xml properties
        Map<String, Object> rawProps = rpd.getProperties();
        for (String propName : rawProps.keySet()) {

            if (!registeredProperties.containsKey(propName))
                throw new PropertyException(null, propName, "Attempting to set unregistered property: " + propName);

            Proxy annoation = registeredProperties.get(propName).getAnnotation();

            if (annoation instanceof S4Component)
                propValues.put(propName, cm.lookup((String) rawProps.get(propName)));

            else if (annoation instanceof S4ComponentList) {
                List<SimpleConfigurable> list = new ArrayList<SimpleConfigurable>();
                List<String> componentNames = (List<String>) rawProps.get(propName);

                for (String componentName : componentNames)
                    list.add(cm.lookup(rpd.getGlobalProperty(componentName, cm.getGlobalProperties())));

                propValues.put(propName, list);

            } else
                propValues.put(propName, rawProps.get(propName));
        }
    }


    public void registerProperty(String propName, S4PropWrapper property) {
        assert property != null && propName != null;

        registeredProperties.put(propName, property);
        propValues.put(propName, null);
    }


    private S4PropWrapper getProperty(String name, Class propertyClass) throws PropertyException {
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


    public float getFloat(String name) throws PropertyException {
        return ((Double) getDouble(name)).floatValue();
    }


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


    public boolean getBoolean(String name) throws PropertyException {
        S4PropWrapper s4PropWrapper = getProperty(name, S4Boolean.class);
        S4Boolean s4Boolean = (S4Boolean) s4PropWrapper.getAnnotation();

        if (propValues.get(name) == null)
            propValues.put(name, s4Boolean.defaultValue());

        Object propValue = propValues.get(name);
        return propValue instanceof Boolean ? (Boolean) propValue : Boolean.getBoolean((String) propValue);
    }


    public SimpleConfigurable getComponent(String name) throws PropertyException {
        S4PropWrapper s4PropWrapper = getProperty(name, S4Component.class);

        S4Component s4Component = ((S4Component) s4PropWrapper.getAnnotation());
        Class expectedType = s4Component.type();

        if (propValues.get(name) == null) {
            SimpleConfigurable configurable = null;

            try {
                configurable = cm.lookup(name);

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


    public List<SimpleConfigurable> getComponentList(String name) throws PropertyException {
        getProperty(name, S4ComponentList.class);

        if (propValues.get(name) == null) {
            propValues.put(name, new ArrayList<SimpleConfigurable>());
        }

        return (List<SimpleConfigurable>) propValues.get(name);
    }


    public void setRaw(String key, Object val) {

    }


    public URL getResource(String name) {
        return null;
    }


    public List getStrings(String name) {
        return null;
    }


    public String[] getNames() {
        return new String[0];
    }


    public void setString(String name) {

    }


    public void setInt(String name, int value) {

    }


    public void setFloat(String name) {
    }


    public Object getRaw(String name) {
        return null;
    }


    public Object getRawNoReplacement(String name) {
        return null;
    }


    public ConfigurationManager getPropertyManager() {
        return null;
    }


    public Logger getLogger() {
        Logger logger = Logger.getLogger(owner.getName());
//        Level level = getLogLevel();
        logger.setLevel(Level.FINE);
        return logger;
    }


    public void dump(PrintStream out) {

    }


    public Collection<String> getRegisteredProperties() {
        return Collections.unmodifiableCollection(registeredProperties.keySet());
    }


    static void processAnnotations(PropSheet propertySheet, Class<? extends SimpleConfigurable> configurable) {
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
