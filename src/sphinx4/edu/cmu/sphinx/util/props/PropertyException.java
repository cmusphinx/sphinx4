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

/** Indicates that a problem occurred while setting one or more properties for this component */
public class PropertyException extends Exception {

    private String instanceName;
    private String propertyName;
    private String msg;


    /**
     * Creates a new property exception
     *
     * @param instanceName
     * @param propertyName the name of the property with the problem.
     * @param msg          a description of the problem.
     */
    PropertyException(String instanceName, String propertyName, String msg) {
        this.instanceName = instanceName;
        this.propertyName = propertyName;
        this.msg = msg;
    }


    /** @return Returns the msg. */
    public String getMsg() {
        return msg;
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
        return "Property Exception component:'" + instanceName
                + "' property:'" + propertyName + "' - " + msg;
    }
}
