/*
 * ConfigProperties.java
 *
 * Created on October 30, 2006, 4:17 PM
 *
 * Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui;

import java.util.Map;
import java.util.HashMap;

import edu.cmu.sphinx.util.props.RawPropertyData;

/**
 * This class stores all the configuration information to be written to output
 * and the properties loaded from input file
 * It does not create any HashMap instances, so these responsibilites are on the creator
 * and be careful when modifying the globalProperties and propertyMap because
 * the original data would also be modified
 *
 * @author Ariani
 */
public class ConfigProperties {
   
    /* the properties to be written */
    private Map<String, RawPropertyData> _propertyMap; // a HashMap for RawPropertyData map with component name as key
    private Map<String, String> _globalProperties; // a HashMap for global properties (often fine tuned properties)
    private Map<String, Map<String, Object>> _otherPropMap;
    // a two-tier HashMap for RawPropertyData with String classname as 1st key and rpd name as 2nd key
    
    /** 
     * Creates a new instance of ConfigProperties 
     */
    public ConfigProperties() { 
    }
    
    /**
     * @return get the Map of Global properties
     */
    public Map<String, String> getGlobal(){
        return _globalProperties;
    }
    
    /**
     * @return get the Map of component properties / configuration set
     *          with classname as the Map key
     */
    public Map<String, Map<String, Object>> getOtherProp(){
        return _otherPropMap;
    }
    
    /**
     * @return the configuration set/component properties
     *          with configuration name as Map key
     */
    public Map<String, RawPropertyData> getProperty(){
        return _propertyMap;
    }
    
    /** 
     * @param global Set the global Map to this Map
     */
    public void setGlobal (Map<String, String> global){
        _globalProperties = global;
    }

    /**
     * add a whole set of Raw Property Data properties ( NOT Global properties )
     * @param p <code>Map</code> of properties with key-value of 
     *          String name,RawPropertyData
     */
    public void addRPDProperties(Map<String, RawPropertyData> p)
    {   
        
        if (_propertyMap == null)
            _propertyMap = new HashMap<String, RawPropertyData>();
        if ( p != null && !p.isEmpty())
            _propertyMap.putAll(p);

        copyPropertiesToOtherMap(p);
        
    }
        
    /** copy all the data in the input Map into the Other Map
     */
    private void copyPropertiesToOtherMap(Map<String, RawPropertyData> from)
    {
        // add new properties to the other map
        if ( from != null && !from.isEmpty()){
            if ( _otherPropMap == null){
                _otherPropMap = new HashMap<String, Map<String, Object>>();
            }
            
            Map<String, Object> classmap;
            for (RawPropertyData rpd : from.values()) {
                String classname = rpd.getClassName();
                if (_otherPropMap.containsKey(classname)) {
                    classmap = _otherPropMap.get(classname);
                    if (!classmap.containsKey(rpd.getName()))
                        classmap.put(rpd.getName(), rpd);
                } else {
                    classmap = new HashMap<String, Object>();
                    classmap.put(rpd.getName(), rpd);
                    _otherPropMap.put(classname, classmap);
                }
            }
        }
    }
        
    /**
     * @param c Set the component properties to this one
     */
    public void setProperty(Map<String, RawPropertyData> c){
        _propertyMap = c;
        
        //create a duplicate map that has the classname 
        // as key instead of configuration name key
        if ( _otherPropMap == null)
            _otherPropMap = new HashMap<String, Map<String, Object>>();
        else
            _otherPropMap.clear();
        copyPropertiesToOtherMap(_propertyMap);
        
    }
    
}
