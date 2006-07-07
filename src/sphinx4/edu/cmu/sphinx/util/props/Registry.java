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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
/**
 * Used to register properties
 */
public class Registry {
    // map holds the registered properties
    private Map map = new HashMap();
    private Configurable configurable;

    /**
     * Creates a new registry for an object
     * 
     * @param obj
     *            the configurable object for this registry
     */
    Registry(Configurable obj) {
        this.configurable = obj;
    }
    /**
     * Registers a property with this registry
     * 
     * @param propertyName
     *            the name of the property
     * @param myType
     *            the type of the property.
     * @throws PropertyException
     *             if a problem with a property is detected.
     */
    public void register(String propertyName, PropertyType myType)
            throws PropertyException {
        if (map.containsKey(propertyName)) {
            throw new PropertyException(configurable, propertyName,
                    "Duplicate registration");
        } else {
            map.put(propertyName, myType);
        }
    }
    /**
     * Lookup a property in the registry
     * 
     * @param propertyName
     *            the property name
     * @return the type of the property (or null if the property is not
     *         registered)
     */
    PropertyType lookup(String propertyName) {
        return (PropertyType) map.get(propertyName);
    }
    
    /**
     * Gets the owner for this registry
     * 
     * @return the owner
     */
    Configurable getOwner() {
        return configurable;
    }
    
    
    /**
     * Returns the list of registered properties
     * @return the list of property names
     */
    Collection  getRegisteredProperties() {
        return map.keySet();
    }
}
