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

/**
 * Indicates that a problem occurred while setting one or more
 * properties for this component
 */
public class PropertyException extends Exception {
    private Configurable source;
    private String propertyName;
    private String msg;
    
    /**
     * Creates a new property exception
     * 
     * @param source the source of the problem
     * @param propertyName the name of the property with the problem.
     * @param msg a description of the problem.
     */
    public PropertyException(Configurable source, String propertyName, String msg) {
        this.source = source;
        this.propertyName = propertyName;
        this.msg = msg;
    }
    
    
    /**
     * @return Returns the msg.
     */
    public String getMsg() {
        return msg;
    }
    /**
     * @return Returns the source of this exception.
     */
    public Configurable getSource() {
        return source;
    }
    
    /**
     * Retrieves the name of the offending property
     * 
     * @return the name of the offending property
     */
    public String getProperty() {
        return propertyName;
    }
    
    /**
     * Returns a string representation of this object
     * 
     * @return the string representation of the object.
     */
    public String toString() {
        String name = "";
        
        if (source != null) {
            name = source.getName();
        }
        return "Property Exception component:'" + name
           + "' property:'" + propertyName + "' - " + msg;
    }
}
