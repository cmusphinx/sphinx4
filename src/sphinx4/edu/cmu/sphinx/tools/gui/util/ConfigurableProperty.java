/*
 * ConfigurableProperty.java
 *
 * Created on November 29, 2006, 5:06 PM
 *
 * Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui.util;

/**
 * This class holds information about a <code>Configurable</code> property that 
 * is owned by a <code>Configurable</code> class. The information includes
 * name of property, default value, and description
 *
 * @author Ariani
 * @see ConfigurableComponent
 */
public class ConfigurableProperty {
    
    private String _name;
    private String _default;    
    private PropertyType _type;
    private String _classtype;
    private String _desc;
    private String _original_name;
        
    /** 
     * Creates a new instance of ConfigurableProperty 
     *
     * @param name Name of property
     * @param defaultval Default value
     * @param type Property type
     * @param desc Description about this property  
     * @param original Original name of property in the source code   
     */
    public ConfigurableProperty(String name, String defaultval, PropertyType type, String desc,
            String original) {
        _name = name;
        _desc = desc;
        _type = type;
        _default = defaultval;      
        _original_name = original;
        _classtype = null;
    }
    
    /** 
     * Creates a new instance of ConfigurableProperty, and this property needs
     * a certain class type as value
     *
     * @param name Name of property
     * @param defaultval Default value
     * @param type Property type
     * @param desc Description about this property  
     * @param original Original name of property in the source code   
     * @param class_type The type of class for this property
     */
    public ConfigurableProperty(String name, String defaultval, PropertyType type, String desc,
            String original, String class_type) {
        _name = name;
        _desc = desc;
        _type = type;
        _default = defaultval;      
        _original_name = original;
        _classtype = class_type;
    }
    
    /**
     * This method is used to desribe the values of the <code>ConfigurableProperty</code>
     * It is used mainly for debugging and testing purposes
     *
     * @return Description of this property
     */
    public String toString(){
        String output = new String("\nProperty name: "+_name+" ");
        if ( _default != null && !_default.equalsIgnoreCase("") )
            output = output.concat(" Default value : "+_default+" ");     
        if ( !_desc.equalsIgnoreCase("") )
            output = output.concat("\nDescription: " +_desc+" ");
        output = output.concat( "Original name : " + _original_name + "\n");
        return output;
    }

    /**
     * @return Property name
     */
    public String getName(){
        return _name;
    }
    
    /**
     * @return Default value of property
     */
    public String getDefault(){
        return _default;
    }
    
    /**
     * @return Type of property
     */
    public PropertyType getType(){
        if (_type == null)
            return null;
        else
            return _type;
    }
    
    /**
     * @return Type of class that's required for this property
     */
    public String getClassType(){
        return _classtype;      
    }
    
    /**
     * @return original property name inside the source code
     */
    public String getOriginalName(){
        return _original_name;
    }
    
    /**
     * @return Description of property
     */
    public String getDesc(){
        return _desc;
    }
}
