/*
 * 
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.util.props;

import java.util.List;

/**
 * A property sheet defines a collection of properties for a single component
 * in the system.
 */
public interface PropertySheet {
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
    void setString(String name, String value) throws PropertyException;
    /**
     * Sets the given property to the given name
     * 
     * @param name
     *            the simple property name
     * @param value
     *            the value for the property
     */
    void setInt(String name, int value) throws PropertyException;
    /**
     * Sets the given property to the given name
     * 
     * @param name
     *            the simple property name
     * @param value
     *            the value for the property
     */
    void setFloat(String name, float value) throws PropertyException;
    /**
     * Gets the value associated with this name
     * 
     * @param name
     *            the name
     * @param defaultValue
     *            the default value for the property
     * @return the value
     */
    String getString(String name, String defaultValue) throws PropertyException;
    /**
     * Gets the value associated with this name
     * 
     * @param name
     *            the name
     * @param defaultValue
     *            the default value for the property
     * @return the value
     * @throws PropertyException
     *             if the named property is not of this type
     */
    int getInt(String name, int defaultValue) throws PropertyException;
    /**
     * Gets the value associated with this name
     * 
     * @param name
     *            the name
     * @param defaultValue
     *            the default value
     * @return the value
     * @throws PropertyException
     *             if the named property is not of this type
     */
    float getFloat(String name, float defaultValue) throws PropertyException;
    /**
     * Gets the value associated with this name
     * 
     * @param name
     *            the name
     * @param defaultValue
     *            the default value
     * @return the value
     * @throws PropertyException
     *             if the named property is not of this type
     */
    boolean getBoolean(String name, boolean defaultValue) throws PropertyException;
    
    /**
     * Gets a comopnent associated with the given paramenter name
     * @param name the parameter name
     * @param type the desired component type
     * @return the component associated with the name
     * @throws PropertyException if the component does not exist or is of the
     * wrong type.
     * 
     */
    Configurable getComponent(String name, Class type) throws PropertyException;
    
    
    /**
     * Gets a list of components associated with the given paramenter name
     * @param name the parameter name
     * @param type the desired component type
     * @return the component associated with the name
     * @throws PropertyException if the component does not exist or is of the
     * wrong type.
     * 
     */
    List getComponentList(String name, Class type) throws PropertyException;
    /**
     * Gets the list of strings associated with this name
     * 
     * @param name
     *            the name
     * 
     * @return an array (possibly empty) of configurable strings
     * @throws PropertyException
     *             if the named property is not of this type
     */
    List getStrings(String name) throws PropertyException;
    /**
     * Retrieves the names of all the properties currently defined for this
     * property sheet
     * 
     * @return the array of names
     */
    String[] getNames();
    /**
     * Gets the raw value associated with this name
     * 
     * @param name
     *            the name
     * @return the value as an object (it could be a String or a String[]
     *         depending upon the property type)
     */
    Object getRaw(String name);
    /**
     * Gets the owning property manager
     * 
     * @return the property manager
     */
    ConfigurationManager getPropertyManager();
}
