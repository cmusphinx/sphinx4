/*
 * 
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.util.props;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.io.PrintStream;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * An implementation of the property sheet that validates the properties
 * against a registry.
 *  
 */
class ValidatingPropertySheet implements PropertySheet {
    private ConfigurationManager cm;
    private Map properties = new HashMap();
    private Registry registry;
    private String className;
    private final static List EMPTY = new ArrayList();
    /**
     * Creates a buildable property sheet
     * 
     * @param cm
     *            the configuration manager
     * 
     * @param registry
     *            controls what properties are allowed in this property sheet.
     * @throws PropertyException
     *             if there is a problem with any of the properties.
     */
    ValidatingPropertySheet(ConfigurationManager cm, Registry registry,
            RawPropertyData rpd) throws PropertyException {
        this.cm = cm;
        this.registry = registry;
        // for each property in the raw property data, check that it
        // is a registered property, and that it is of the proper type.
        Map raw = rpd.getProperties();
        className = rpd.getClassName();
        for (Iterator i = raw.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            Object val = raw.get(key);
            setRaw(key, val);
        }
    }
    /**
     * Adds a new property to the set of properties
     * 
     * @param name
     *            the name of the property
     * @param value
     *            the value of the proberty
     * @throws PropertyException
     *             if the property is not a registered property or the value is
     *             not of the proper type.
     */
    void addProperty(String name, String value) {
        properties.put(name, value);
    }
    /**
     * Sets the given property to the given name
     * 
     * @param name
     *            the simple property name
     * @param value
     *            the value for the property
     * 
     *  
     */
    public void setString(String name, String value) {
        throw new UnsupportedOperationException();
    }
    /**
     * Sets the given property to the given name
     * 
     * @param name
     *            the simple property name
     * @param value
     *            the value for the property
     */
    public void setInt(String name, int value) {
        throw new UnsupportedOperationException();
    }
    /**
     * Sets the given property to the given name
     * 
     * @param name
     *            the simple property name
     * @param value
     *            the value for the property
     * @throws PropertyException
     *             if the property is not a registered property or the value is
     *             not of the proper type.
     */
    public void setFloat(String name, float value) {
        throw new UnsupportedOperationException();
    }
    /**
     * Sets the property
     * 
     * @param key
     *            the property name
     * @param val
     *            the property (either a String or a String[])
     * @throws PropertyException
     *             if the property is not a registered property or the value is
     *             not of the proper type.
     */
    public void setRaw(String key, Object val) throws PropertyException {
        PropertyType type = registry.lookup(key);
        if (type == null) {
            throw new PropertyException(registry.getOwner(), key,
                    "Attempt to set unregistered property");
        } else if (val instanceof String) {
            String sval = (String) val;
            
            if (!isGlobalVariable(sval) && !type.isValid(val)) {
                throw new PropertyException(registry.getOwner(), key, 
                        "value (" + sval + ")" + " is not a valid " + type);
            } else {
                properties.put(key, val);
            }
        }  else if (!type.isValid(val)) {
            throw new PropertyException(registry.getOwner(), key, val
                    + " is not a valid " + type);
        } else {
            properties.put(key, val);
        }
    }
    /**
     * Gets a property by name from the property map
     * 
     * @param name
     *            the name of the property
     * @return the return value
     */
    public Object getRaw(String name) throws PropertyException {
        Object value = getRawNoReplacment(name);
        
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            String sval = (String) value;
            if (sval.startsWith("${")) {
                value = cm.globalLookup(sval);
                if (value == null) {
                    throw new PropertyException(registry.getOwner(), name,
                            "Can't find global property " + sval);
                }
            }
        } else if (value instanceof List) {
            List lval = (List) value;
            for (ListIterator i = lval.listIterator(); i.hasNext(); ) {
                String sval = (String) i.next();
                if (sval.startsWith("${")) {
                    String itemVal = cm.globalLookup(sval);
                    if (itemVal == null) {
                        throw new PropertyException(registry.getOwner(), name,
                                "Can't find global property " + sval);
                    } else {
                        i.set(itemVal);
                    }
                }
            }
        }
        return value;
    }

    /**
     * Gets a property by name from the property map, no global symbol
     * replacement is done
     * 
     * @param name
     *            the name of the property
     * @return the return value
     */
    public Object getRawNoReplacment(String name) {
        return properties.get(name);
    }

    /**
     * Gets the value associated with this name. Note that is considered legal
     * to get a string version of any property
     * 
     * @param name
     *            the name
     * @param defaultValue
     *            the default value for the property
     * @return the value
     */
    public String getString(String name, String defaultValue)
            throws PropertyException {
        checkType(name, PropertyType.STRING);
        String value = (String) getRaw(name);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }
    /**
     * Gets the value associated with this name
     * 
     * @param name
     *            the name
     * @param defaultValue
     *            the default value for the property
     * @return the value
     * @throws PropertyException
     *             if the property is not a registered property or the value is
     *             not of the proper type.
     */
    public int getInt(String name, int defaultValue) throws PropertyException {
        checkType(name, PropertyType.INT);
        try {
            String val = (String) getRaw(name);
            if (val == null) {
                return defaultValue;
            } else {
                return Integer.parseInt(val);
            }
        } catch (NumberFormatException e) {
            throw new PropertyException(registry.getOwner(), name,
                    "bad integer format");
        }
    }
    /**
     * Gets the value associated with this name
     * 
     * @param name
     *            the name
     * @param defaultValue
     *            the default value
     * @return the value
     */
    public float getFloat(String name, float defaultValue)
            throws PropertyException {
        checkType(name, PropertyType.FLOAT);
        try {
            String val = (String) getRaw(name);
            if (val == null) {
                return defaultValue;
            } else {
                return Float.parseFloat(val);
            }
        } catch (NumberFormatException e) {
            throw new PropertyException(registry.getOwner(), name,
                    "bad float format");
        }
    }
    
    /**
     * Gets the value associated with this name
     * 
     * @param name
     *            the name
     * @param defaultValue
     *            the default value
     * @return the value
     */
    public double getDouble(String name, double defaultValue)
            throws PropertyException {
        checkType(name, PropertyType.DOUBLE);
        try {
            String val = (String) getRaw(name);
            if (val == null) {
                return defaultValue;
            } else {
                return Double.parseDouble(val);
            }
        } catch (NumberFormatException e) {
            throw new PropertyException(registry.getOwner(), name,
                    "bad double format");
        }
    }
    
    /**
     * Gets the value associated with this name
     * 
     * @param name
     *            the name
     * @param defaultValue
     *            the default value
     * @return the value
     */
    public boolean getBoolean(String name, boolean defaultValue)
            throws PropertyException {
        checkType(name, PropertyType.BOOLEAN);
        String val = (String) getRaw(name);
        if (val == null) {
            return defaultValue;
        } else {
            return Boolean.valueOf((String) getRaw(name)).booleanValue();
        }
    }


    /**
     * a regular expression that matches our own resource 'protocol'.
     *
     * This matches urls of the form:   resource:/package.a.class!/resource/location
     */
    private static Pattern jarPattern =
            Pattern.compile("resource:/([.\\w]+?)!(.*)",
                    Pattern.CASE_INSENSITIVE);
    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.PropertySheet#getResource(java.lang.String)
     */
    public URL getResource(String name) throws PropertyException {
        URL url = null;
        checkType(name, PropertyType.RESOURCE);
        String location = (String) getRaw(name);
        if (location == null) {
            throw new PropertyException(registry.getOwner(), name, 
              "Required resource property '" + name + "' not set");
        }

        Matcher jarMatcher = jarPattern.matcher(location);
        if (jarMatcher.matches()) {
            String className = jarMatcher.group(1);
            String resourceName = jarMatcher.group(2);

            try {
                Class cls = Class.forName(className);
                url = cls.getResource(resourceName);
                if (url == null) {
                    // getResource doesn't usually find directories 
                    // If the resource is a directory and we
                    // can't find it, we will instead try to  find the class 
                    // anchor and backup to the top level and try again
                    String classPath = className.replaceAll("\\.", "/") + ".class";
                    url = cls.getClassLoader().getResource(classPath);
                    if (url != null) {
                        // we should have something like this, so replace everything
                        // jar:file:/foo.jar!/a/b/c/HelloWorld.class
                        // after the ! with the resource name
                        String urlString = url.toString();
                        urlString = urlString.replaceAll("/" + classPath, resourceName);
                        try {
                            url = new URL(urlString);
                        } catch (MalformedURLException mfe) {
                            throw new PropertyException(registry.getOwner(), 
                                    name, "Bad URL " + urlString + mfe.getMessage());
                        }
                    }
                }
                if (url == null) {
                    throw new PropertyException(registry.getOwner(),
                            name, "Can't locate resource " + resourceName);
                } else {
                    // System.out.println("URL FOUND " + url);
                }
            } catch (ClassNotFoundException cnfe) {
                throw new PropertyException(registry.getOwner(),
                    name, "Can't locate resource:/" + className);
            }
        } else {
            if (location.indexOf(":") == -1) {
                location = "file:" + location;
            }

            try {
                url = new URL(location);
            } catch (MalformedURLException e) {
                throw new PropertyException(registry.getOwner(), 
                        name, "Bad URL " + location + e.getMessage());
            }
        } 
        return url;
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.PropertySheet#getComponent(java.lang.String, java.lang.Class)
     */
    public Configurable getComponent(String name, Class type) throws PropertyException {
        checkType(name, PropertyType.COMPONENT);
        String val = (String) getRaw(name);
        
        if (val == null) {
            throw new PropertyException(registry.getOwner(), name, 
              "Required component property '" + name + "' not set");
        }
        Configurable c = null;
        try {
            c = cm.lookup(val);
            if (c == null){
                throw new PropertyException(registry.getOwner(), name,
                        "Can't find component: " + val);    
            }
            if (!type.isInstance(c)) {
                throw new PropertyException(registry.getOwner(), name,
                        "type mismatch. Expected type: " + type.getName() +
                        " found component of type: " + c.getClass().getName());
            }
        } catch (InstantiationException e) {
            throw new PropertyException(registry.getOwner(), name,
                    "Can't instantiate: " + val + " " + e.getMessage());
        } 
        return c;
    }
    /**
     * Gets the list of strings associated with this name
     * 
     * @param name
     *            the name
     * 
     * @return an array (possibly empty) of configurable strings
     * @throws PropertyException
     *             if the property is not a registered property or the value is
     *             not of the proper type.
     */
    public List getStrings(String name) throws PropertyException {
        checkType(name, PropertyType.STRING_LIST);
        Object obj = getRaw(name);
        if (obj == null) {
            return EMPTY;
        } else if (obj instanceof List) {
            return (List) obj;
        }
        throw new PropertyException(registry.getOwner(), name, "internal error");
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.PropertySheet#getComponentList(java.lang.String, java.lang.Class)
     */
    public List getComponentList(String name, Class type) throws PropertyException {
        checkType(name, PropertyType.COMPONENT_LIST);
        List list = (List) getRaw(name);
        if (list == null) {
            return EMPTY;
        } 
        
        List objectList = new ArrayList();
        
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            String compName = (String) i.next();
            Configurable c = null;
            try {
                c = cm.lookup(compName);
                if (c == null){
                    throw new PropertyException(registry.getOwner(), name,
                            "Can't find component: " + compName);    
                }
                if (!type.isInstance(c)) {
                    throw new PropertyException(registry.getOwner(), name,
                            "type mismatch. Expected type: " + type.getName() +
                            " found component of type: " + c.getClass().getName());
                }
                objectList.add(c);
            } catch (InstantiationException e) {
                throw new PropertyException(registry.getOwner(), name,
                        "Can't instantiate: " + compName);
            } 
        }
        return objectList;
    }
    
    /**
     * Retrieves the names of all the properties currently defined for this
     * property sheet
     * 
     * @return the array of names
     */
    public String[] getNames() {
        Set keys = properties.keySet();
        return (String[]) keys.toArray(new String[keys.size()]);
    }
    /**
     * Gets the owning property manager
     * 
     * @return the property manager
     */
    public ConfigurationManager getPropertyManager() {
        return cm;
    }
    /**
     * Checks to make sure that the given registered type for the given
     * property is what we expect.
     * 
     * @param name
     *            the name of the property
     * @param expectedType
     *            the expected type of the property
     * @throws PropertyException
     *             if the expected type does not match the
     *  
     */
    private void checkType(String name, PropertyType expectedType)
            throws PropertyException {
        PropertyType registeredType = registry.lookup(name);
        if (registeredType == null) {
            throw new PropertyException(registry.getOwner(), name,
                    "Unknown property");
        } else if (registeredType != expectedType) {
            throw new PropertyException(registry.getOwner(), name,
                    "Type mismatch, requested " + expectedType
                            + " registered type:" + registeredType);
        }
    }
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String[] names = getNames();
        for (int j = 0; j < names.length; j++) {
            Object obj;
            try {
                obj = getRaw(names[j]);
            } catch (PropertyException e) {
                obj = "ERROR(not set)";
            }
            if (obj instanceof String) {
                String value = (String) obj;
                sb.append("<property name=\"");
                sb.append(names[j]);
                sb.append("\" value=\"");
                sb.append(value);
                sb.append("\"/>\n");
            } else if (obj instanceof List) {
                List  values = (List) obj;
                sb.append("<list name=\"");
                sb.append(names[j]);
                sb.append("\">\n");
                for (int k = 0; k < values.size(); k++) {
                    sb.append("    <item>");
                    sb.append((String) values.get(k));
                    sb.append("</item>\n");
                }
                sb.append("</list>\n");
            }
        }
        return sb.toString();
    }

    /*
     * Dumps the property sheet to the given stream
     * 
     * @param out the stream
     */
    public void dump(PrintStream out) {
        String[] names = getNames();
        for (int j = 0; j < names.length; j++) {
            Object obj;
            try {
                obj = getRaw(names[j]);
            } catch (PropertyException e) {
                obj = "ERROR(not set)";
            }
            if (obj instanceof String) {
                out.println("  " + names[j] + ": " + obj);
            } else if (obj instanceof List) {
                List values = (List) obj;
                out.print("  " + names[j] + ": " );
                for (int k = 0; k < values.size(); k++) {
                    out.println("        " + (String) values.get(k));
                }
                out.println();
            }
        }
    }
    
    /**
     * determines if the string is a valid format for a global variable
     * 
     * @param val the string to check
     * @return true if the string is a valid format for a global variable
     * 
     */
    private boolean isGlobalVariable(String val) {
        return val.startsWith("${");
    }

    /**
     * Gets the log level for this component
     * @return the log level
     */
    private Level getLogLevel()  throws PropertyException {
        Level level = null;
        
        String levelName = getString(ConfigurationManager.PROP_COMMON_LOG_LEVEL,
                cm.getGlobalLogLevel());
                

        if (levelName == null) {
            level  = Level.WARNING;
        } else {
            try {
                level = Level.parse(levelName);
            } catch (IllegalArgumentException e) {
                throw new PropertyException(registry.getOwner(), 
                    ConfigurationManager.PROP_COMMON_LOG_LEVEL,
                    "Bad 'level' specifier " + levelName);
            }
        }
        return level;
    }
    
   
    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.PropertySheet#getLogger()
     */
    public Logger getLogger() throws PropertyException {
//        Logger logger = Logger.getLogger(className + "."
//             + registry.getOwner().getName());
        Logger logger = Logger.getLogger(registry.getOwner().getName());
        Level level = getLogLevel();
        logger.setLevel(level);
        return logger;
    }

}
