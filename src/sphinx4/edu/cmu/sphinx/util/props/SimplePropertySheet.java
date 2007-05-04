package edu.cmu.sphinx.util.props;

import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Added another implemenation of the PropertySheet interface which does not requrire a xml-file. Possible applications
 * are the direct instantiation of <code>Configurable</code>s, unit tests and re-configuration of
 * <code>Configurable</code>s.
 *
 * @author Holger Brandl
 */
public class SimplePropertySheet {

    private Map<String, Object> properties = new HashMap<String, Object>();
    private Logger logger;


    public SimplePropertySheet() {
    }


    /**
     * Creates a new property sheet and initalizes it by using a set of key value pairs.
     * <p/>
     * Example: new SimplePropertySheet("sampleRate", 16000, "gainFactor", 3.4, "lalala", new SpeechMarker()); ...)
     * <p/>
     * The number of arguments is expected to be a multiple of two.
     */
    public SimplePropertySheet(Object... pairValuePairs) {
        assert pairValuePairs.length % 2 == 0 : "argument list does not conatain key-value-pairs!";

        for (int i = 0; i < pairValuePairs.length; i += 2) {
            properties.put((String) pairValuePairs[i], pairValuePairs[i + 1]);
        }
    }


    public void setString(String name, String value) throws PropertyException {
        properties.put(name, value);
    }


    public String getString(String name, String defaultValue) throws PropertyException {
        if (properties.containsKey(name))
            return (String) properties.get(name);
        else
            return defaultValue;
    }


    public void setInt(String name, int value) throws PropertyException {
        properties.put(name, value);
    }


    public int getInt(String name, int defaultValue) throws PropertyException {
        if (properties.containsKey(name))
            return (Integer) properties.get(name);
        else
            return defaultValue;
    }


    public void setFloat(String name, float value) throws PropertyException {
        properties.put(name, value);
    }


    public float getFloat(String name, float defaultValue) throws PropertyException {
        if (properties.containsKey(name))
            return (Float) properties.get(name);
        else
            return defaultValue;
    }


    public void setDouble(String name, double value) throws PropertyException {
        properties.put(name, value);
    }


    public double getDouble(String name, double defaultValue) throws PropertyException {
        if (properties.containsKey(name)) {
            if (properties.get(name) instanceof Double)
                return (Double) properties.get(name);
            else
                return (Float) properties.get(name);
        } else
            return defaultValue;
    }


    public void setBoolean(String propUseAddTable, boolean useAddTable) {
        properties.put(propUseAddTable, useAddTable);
    }


    public boolean getBoolean(String name, boolean defaultValue) throws PropertyException {
        if (properties.containsKey(name))
            return (Boolean) properties.get(name);
        else
            return defaultValue;
    }


    public void setComponent(String name, Configurable c) throws PropertyException {
        properties.put(name, c);
    }


    public Configurable getComponent(String name, Class type) throws PropertyException {
        if (properties.containsKey(name))
            return (Configurable) properties.get(name);
        else
            return null;
    }


    public void setStrings(String name, List<String> strings) throws PropertyException {
        properties.put(name, strings);
    }


    public List getStrings(String name) throws PropertyException {
        if (properties.containsKey(name))
            return (List) properties.get(name);
        else
            return null;
    }


    public void setLogger(Logger logger) {
        assert logger != null;
        this.logger = logger;
    }


    public Logger getLogger() throws PropertyException {
        return logger;
    }

    // todo implement remaining methods


    public void setRaw(String key, Object val) throws PropertyException {
        throw new RuntimeException("not implemented");
    }


    public URL getResource(String name) throws PropertyException {
        throw new RuntimeException("not implemented");
    }


    public List getComponentList(String name, Class type) throws PropertyException {
        throw new RuntimeException("not implemented");
    }


    public String[] getNames() {
        throw new RuntimeException("not implemented");
    }


    public Object getRaw(String name) throws PropertyException {
        throw new RuntimeException("not implemented");
    }


    public Object getRawNoReplacement(String name) {
        throw new RuntimeException("not implemented");
    }


    public ConfigurationManager getPropertyManager() throws PropertyException {
        throw new RuntimeException("not implemented");
    }


    public void dump(PrintStream out) {
        throw new RuntimeException("not implemented");
    }
}
