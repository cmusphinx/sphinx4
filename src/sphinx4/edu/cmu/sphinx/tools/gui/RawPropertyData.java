/*
 *  
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004-2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.tools.gui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Holds the raw property data just as it has come in from the properties file. */
public class RawPropertyData {

    private String name;
    private String className;
    private Map properties;


    /**
     * Creates a raw property data item
     *
     * @param name      the name of the item
     * @param className the class name of the item
     */
    public RawPropertyData(String name, String className) {
        this.name = name;
        this.className = className;
        properties = new HashMap();
    }


    /**
     * Adds a new property
     *
     * @param propName  the name of the property
     * @param propValue the value of the property
     */
    public void add(String propName, String propValue) {
        properties.put(propName, propValue);
    }


    /**
     * Adds a new property
     *
     * @param propName  the name of the property
     * @param propValue the value of the property
     */
    public void add(String propName, List propValue) {
        properties.put(propName, propValue);
    }


    /**
     * Change property value
     *
     * @param propName  the name of the property
     * @param propValue the new value of the property
     */
    public void change(String propName, String propValue) {
        properties.remove(propName);
        properties.put(propName, propValue);
    }


    /**
     * Remove property
     *
     * @param propName the name of the property
     */
    public void remove(String propName) {
        properties.remove(propName);
    }


    /**
     * Change property value
     *
     * @param propName  the name of the property
     * @param propValue the new value of the property
     */
    public void change(String propName, List propValue) {
        properties.remove(propName);
        properties.put(propName, propValue);
    }


    /** @return Returns the className. */
    public String getClassName() {
        return className;
    }


    /** @return Returns the configuration name. */
    public String getName() {
        return name;
    }


    /** @return Returns the properties. */
    public Map getProperties() {
        return properties;
    }


    /**
     * Provide information stored inside this Object, used mainly for debugging / testing
     *
     * @return Description of object
     */
    public String toString() {
        String output = new String("name : " + name);
        for (Iterator it = properties.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            String groupname = (String) entry.getKey();
            if (entry.getValue() != null) {
                if (entry.getValue() instanceof String) {
                    output = output.concat("value string : " + (String) entry.getValue());
                } else { // is a list
                    output = output.concat(((List) entry.getValue()).toString());
                }
            }
        }
        return output;
    }


    /**
     * Determines if the map already contains an entry for this property
     *
     * @param propName the property of interest
     * @return true if the map already contains this property
     */

    public boolean contains(String propName) {
        return properties.get(propName) != null;
    }
}