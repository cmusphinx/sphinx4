/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.util;

import java.net.URL;
import java.util.Properties;
import java.io.IOException;
import java.io.PrintStream;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;


/**
 * Manages configuration data for sphinx.  Configuration data in
 * Sphinx is managed via property files. Objects that need
 * configuration data should get the SphinxProperties object for their
 * context and extract properties. Property names have the form:       
 *
 *	objectName.propertyName
 *
 *  There is a hierarchy of Configuration  data. When a getProperty
 *  method is called on a SphinxProperties object, the System
 *  properties are checked first to see if there is a property with
 *  the given name, if so, the system property is used, otherwise the
 *  properties associated with the URL are checked and if found that
 *  is used, otherwise the default value for the property is used.
 */
public class SphinxProperties {

    
    /**
     * The context pool contains a mapping from the context to either
     * the URL for the context or once the properties has been loaded
     * from the URL, the properties object itself
     */
    private static Map contextPool = new HashMap();

    // This empty SphinxProperties is used when a request is made for
    // a SphinxProperties from a context that does not exist.
    // It allows the application to continue with getXXX calls (which
    // will supply the inline default values).
    private static SphinxProperties EMPTY_SPHINX_PROPERTIES = 
		new SphinxProperties();

    private URL url;		// URL associated with the properties
    private Properties props;	// the actual properties
    private Properties shadowProps; // actuall requested properties
    private String contextName;	// name of the associated context
    private boolean warnIfNoProperty = false;


    /**
     * SphinxProperty specifying whether a warning message should be
     * printed out if a queried property is not defined.
     */
    public final static String PROP_WARN_NO_PROPERTY =
        "edu.cmu.sphinx.util.SphinxProperties.warnIfPropertyNotDefined";


    /**
     * The default value of PROP_WARN_NO_PROPERTY.
     */
    public final static boolean PROP_WARN_NO_PROPERTY_DEFAULT = false;


    /**
     * Initialize a particular context. There can be multiple contexts
     * in a Sphinx system. Each context can have its own
     * SphinxProperties. This method associates a URL with a
     * particular context.
     *
     * @param context the name of the context
     * @param url the location of the property sheet, or null if there
     * is no property sheet.
     *
     * @throws IOException if the property list cannot be loaded from
     * the given url
     */
    static public void initContext(String context, URL url) 	
				throws IOException {
	if (contextPool.containsKey(context)) {
	    SphinxProperties cur = (SphinxProperties) contextPool.get(context);
	    if (!url.equals(cur.url)) {
	        throw new Error("Cannot init SphinxProperties with same context: "+
			    context);
	    }
	} else {
	    contextPool.put(context, new SphinxProperties(context, url));
	}
    }


    /**
     * Initialize a new SphinxProperty with the given Properties object and
     * context.
     *
     * @param context the name of the context
     * @param properties the Properties object where our Properties reside
     */
    static public void initContext(String context, Properties properties) {
	if (contextPool.containsKey(context)) {
	    throw new Error("Cannot init SphinxProperties with same context: "+
			    context);
	} else {
	    contextPool.put(context, 
			    new SphinxProperties(context, properties));
	}
    }


    /**
     * Retrieves the SphinxProperties for the particular context. 
     *
     * @param context  the context of interest
     *
     * @return the SphinxProperties associated with the context. If
     * the given context does not exist an empty SphinxProperties 
     * is returned
     *
     */
    static public SphinxProperties getSphinxProperties(String context) {
	SphinxProperties sp = (SphinxProperties) contextPool.get(context);
	if (sp == null) {
	    sp = EMPTY_SPHINX_PROPERTIES;
	}

        sp.warnIfNoProperty = sp.getBoolean(PROP_WARN_NO_PROPERTY, false);
	return sp;
    }


    /**
     * Contructs a new SphinxProperties from the given URL and
     * contextName. 
     *
     * @param contextName the name of the context
     * @param url the url associated with the context (or null)
     *
     * @throws IOException if there was an error loading the property
     * sheet.
     */
    private SphinxProperties(String contextName, URL url) throws IOException {
	this.contextName = contextName;
	this.url = url;
	props = new Properties();
	shadowProps = new Properties();
	if (url != null) {
	    props.load(url.openStream());
	}
    }


    /**
     * Constructs a new SphinxProperties from the given context and Properties.
     *
     * @param context the name of the context
     * @param properties the Properties object
     */
    private SphinxProperties(String context, Properties properties) {
	this.contextName = context;
	this.url = null;
	props = properties;
	shadowProps = new Properties();
    }


    /**
     * Creates an empty SphinxProperties
     */
    private SphinxProperties() {
	this.contextName = null;
	this.url = null;
	props = new Properties();
	shadowProps = new Properties();
    }


    /**
     * Retrieves the context name for this SphinxProperties
     *
     * @return the context name
     */
    public String getContext() {
	return contextName;
    }

    /**
     * Prints this property list out to the specified output stream
     *
     * @param out an output stream.
     */
    public void list(PrintStream out) {
        // lists the used properties
        out.println("##### Used properties #####");
        listProperties(shadowProps, out);

        // lists the un-used properties
        listUnused(out);
    }


    /**
     * Sets a sphinx property.  Note that there is no guarantee that modified
     * properties will be actually used
     *
     * @param name the name of the property
     * @param value the new value for the property
     *
     */
    public void setProperty(String name, String value) {
        props.setProperty(name, value);
    }


    /**
     * Prints the list of unused properties to the specified stream.
     * Unused properties are properties that were defined in the
     * property list or in the system environment but were never used
     * by the system. They are potentially mispellings or properties
     * that are no longer used. They almost always indicate that some
     * sort of error has taken place.  
     *
     * @param out an output stream.
     */
    public void listUnused(PrintStream out) {
        out.println("##### Unused properties #####");
        out.println("# Unused properties defined in " + url + ":" );
        for (Iterator i = getSortedIterator(props); i.hasNext(); ) {
            String key = (String) i.next();
            if (shadowProps.get(key) == null) {
		String value = (String) props.get(key);
                out.println(" " + key + "=" + value);
            }
        }

        out.println("# Unused system properties:" );
        for (Iterator i = getSortedIterator(System.getProperties());
             i.hasNext(); ) {
            String key = (String) i.next();
            if (key.startsWith("edu.cmu")) {
                if (shadowProps.get(key) == null) {
		    String value = (String) props.get(key);
		    out.println(" " + key + "=" + value);
                }
            }
        }
    }


    /**
     * Returns a sorted Iterator of the given Properties object.
     *
     * @param props the Properties to get an Iterator of
     *
     * @return a sorted Iterator of the given Properties object
     */
    private Iterator getSortedIterator(Properties props) {
        List sortedKeyList = Collections.list(props.keys());
        Collections.sort(sortedKeyList);
        return sortedKeyList.iterator();
    }


    /**
     * Prints the given Properties to the PrintStream in sorted order.
     *
     * @param props the Properties to print
     * @param out the PrintStream to print to
     */
    private void listProperties(Properties props, PrintStream out) {
        for (Iterator i = getSortedIterator(props); i.hasNext(); ) {
            String key = (String) i.next();
            String value = (String) props.get(key);
            out.println(key + "=" + value);
        }
    } 


    /**
     * Returns a new Property object that contains all the properties of
     * this SphinxProperties.
     *
     * @return a new Property object of all the properties of 
     *    this SphinxProperties
     */
    public Properties getProperties() {
	Properties properties = new Properties();
	properties.putAll(props);
	return properties;
    }

    /**
     * Returns true if this SphinxProperties contains the given
     * property.
     *
     * @return true if it has the given property, false otherwise
     */
    public boolean contains(String propertyName) {
	String value = System.getProperty(propertyName);
	if (value == null) {
	    return props.contains(propertyName);
	} else {
	    return true;
	}
    }


    /**
     * Searches for and returns the value of the given property.
     *
     * @param propertyName the property to search for
     *
     * @return the value of the property, or null if the property
     *    does not exists
     */
    private String getString(String propertyName) {
	String value = System.getProperty(propertyName);
	if (value == null) {
	    value = props.getProperty(propertyName);
	}
	return value;
    }


    /**
     * Searches for the property with the specified name.
     *
     * @param propertyName the name of the property to search for
     * @param defaultValue the value to return if the property is not
     *    found
     *
     * @return the value of the property
     */
    public String getString(String propertyName, String defaultValue) {
	String value = getString(propertyName);
	if (value == null) {
	    warnNoProperty(propertyName, defaultValue);
	    value = defaultValue;
	}
	if (value != null) {
            value = value.trim();
	    shadowProps.setProperty(propertyName, value);
	}
	return value;
    }


    /**
     * Searches for the property with the specified name of a
     * particular instance.  This is a conveniance method, since many 
     * property accesses are built by concatenating the instance name
     * and the property name, this method is provided to make it
     * easier.  
     *
     * @param instanceName the name of the particular instance
     * @param propertyName the name of the property to search for
     * @param defaultValue the value to return if the property is not
     *    found
     *
     * @return the value of the property
     */
    public String getString(String instanceName,
			    String propertyName, String defaultValue) {
        String value = getString(instanceName + ";" + propertyName);
        if (value == null) {
            value = getString(propertyName, defaultValue);
        } else {
            shadowProps.setProperty(instanceName + ";" + propertyName, value);
        }
	if (value != null) {
	    value = value.trim();
	}
	return value;
    }


    /**
     * Searches for the property with the specified name.
     *
     * @param propertyName the name of the property to search for
     * @param defaultValue the value to return if the property is not
     *    found
     *
     * @return the value of the property
     *
     * @throws NumberFormatException if the property cannot be
     * 	converted to the number format
     */
    public float getFloat(String propertyName, float defaultValue) 
    		throws NumberFormatException {
	float value = defaultValue;
	String svalue = getString(propertyName, String.valueOf(defaultValue));
       	if (svalue != null) {
	    value = Float.parseFloat(svalue);
	}
	return value;
    }


    /**
     * Searches for the property with the specified name of a
     * particular instance.  This is a conveniance method, since many 
     * property accesses are built by concatenating the instance name
     * and the property name, this method is provided to make it
     * easier.  
     *
     * @param instanceName the name of the particular instance
     * @param propertyName the name of the property to search for
     * @param defaultValue the value to return if the property is not
     *    found
     *
     * @return the value of the property
     * @throws NumberFormatException if the property cannot be
     * 	converted to the number format
     */
    public float getFloat(String instanceName, 
                          String propertyName, float defaultValue) 
    		throws NumberFormatException {
        return Float.parseFloat(getString(instanceName, propertyName,
                                          String.valueOf(defaultValue)));
    }


    /**
     * Searches for the property with the specified name.
     *
     * @param propertyName the name of the property to search for
     * @param defaultValue the value to return if the property is not
     *    found
     *
     * @return the value of the property
     *
     * @throws NumberFormatException if the property cannot be
     * 	converted to the number format
     */
    public double getDouble(String propertyName, double defaultValue) 
    		throws NumberFormatException {
	double value = defaultValue;
	String svalue = getString(propertyName, String.valueOf(defaultValue));
	if (svalue != null) {
	    value = Double.parseDouble(svalue);
	}
	return value;
    }


    /**
     * Searches for the property with the specified name of a
     * particular instance.  This is a conveniance method, since many 
     * property accesses are built by concatenating the instance name
     * and the property name, this method is provided to make it
     * easier.  
     *
     * @param instanceName the name of the particular instance
     * @param propertyName the name of the property to search for
     * @param defaultValue the value to return if the property is not
     *    found
     *
     * @return the value of the property
     * @throws NumberFormatException if the property cannot be
     * 	converted to the number format
     */
    public double getDouble(String instanceName, 
                            String propertyName, double defaultValue) 
        throws NumberFormatException {
        return Double.parseDouble(getString(instanceName, propertyName,
                                            String.valueOf(defaultValue)));
    }


    /**
     * Searches for the property with the specified name.
     *
     * @param propertyName the name of the property to search for
     * @param defaultValue the value to return if the property is not
     *    found
     *
     * @return the value of the property
     *
     * @throws NumberFormatException if the property cannot be
     * 	converted to the number format
     */
    public int getInt(String propertyName, int defaultValue) 
        throws NumberFormatException {
	int value = defaultValue;
	String svalue = getString(propertyName, String.valueOf(defaultValue));
	if (svalue != null) {
	    value = Integer.parseInt(svalue);
	}
	return value;
    }


    /**
     * Searches for the property with the specified name of a
     * particular instance.  This is a conveniance method, since many 
     * property accesses are built by concatenating the instance name
     * and the property name, this method is provided to make it
     * easier.  
     *
     * @param instanceName the name of the particular instance
     * @param propertyName the name of the property to search for
     * @param defaultValue the value to return if the property is not
     *    found
     *
     * @return the value of the property
     * @throws NumberFormatException if the property cannot be
     * 	converted to the number format
     */
    public int getInt(String instanceName, 
                      String propertyName, int defaultValue) 
        throws NumberFormatException {
        return Integer.parseInt(getString(instanceName, propertyName,
                                          String.valueOf(defaultValue)));
    }


    /**
     * Searches for the property with the specified name. The string
     * values: true, on, 1 and enabled all correspond to true,
     * anything else is considered false.
     *
     * @param propertyName the name of the property to search for
     * @param defaultValue the value to return if the property is not
     *    found
     *
     * @return the value of the property
     *
     */
    public boolean getBoolean(String propertyName, boolean defaultValue) {
	boolean value = defaultValue;
	String svalue = getString(propertyName, String.valueOf(defaultValue));
	if (svalue != null) {
	    value = isTrue(svalue);
	}
	return value;
    }


    /**
     * Searches for the property with the specified name of a
     * particular instance.  This is a conveniance method, since many 
     * property accesses are built by concatenating the instance name
     * and the property name, this method is provided to make it
     * easier.  
     *
     * @param instanceName the name of the particular instance
     * @param propertyName the name of the property to search for
     * @param defaultValue the value to return if the property is not
     *    found
     *
     * @return the value of the property
     * @throws NumberFormatException if the property cannot be
     * 	converted to the number format
     */
    public boolean getBoolean(String instanceName, 
	    		String propertyName, boolean defaultValue) {
        return isTrue(getString(instanceName, propertyName,
                                String.valueOf(defaultValue)));
    }


    /**
     * Returns true if the given string is either "true", "on", "1", or
     * "enabled" (case ignored).
     *
     * @param booleanString the string to inspect
     *
     * @return true if the string represents true, false otherwise
     */
    private boolean isTrue(String booleanString) {
        return (booleanString.equalsIgnoreCase("true") ||
                booleanString.equalsIgnoreCase("on") ||
                booleanString.equalsIgnoreCase("1") ||
                booleanString.equalsIgnoreCase("enabled"));
    }


    /**
     * Prints out a warning to System.out saying that the given
     * property does not exist and is using the given default value.
     *
     * @param propertyName the name of the property
     * @param defaultValue the default value used
     */
    private void warnNoProperty(String propertyName, String defaultValue) {
	// print out the warning only if the property has never been
	// searched for
	if (warnIfNoProperty && !shadowProps.containsKey(propertyName)) {
	    System.out.println("WARNING: no property, " 
                    + propertyName + "\n" 
                    + "         using the default value " 
                    + defaultValue);
	}
    }
}
