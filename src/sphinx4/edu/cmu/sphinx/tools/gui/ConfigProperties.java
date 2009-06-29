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
import java.util.Iterator;

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
    private Map _propertyMap=null; // a HashMap for RawPropertyData map with component name as key
    private Map _globalProperties=null; // a HashMap for global properties (often fine tuned properties)
    private Map _otherPropMap=null; 
    // a two-tier HashMap for RawPropertyData with String classname as 1st key and rpd name as 2nd key
    
    /** 
     * Creates a new instance of ConfigProperties 
     */
    public ConfigProperties() { 
    }
    
    /**
     * @return get the Map of Global properties
     */
    public Map getGlobal(){
        return _globalProperties;
    }
    
    /**
     * @return get the Map of component properties / configuration set
     *          with classname as the Map key
     */
    public Map getOtherProp(){
        return _otherPropMap;
    }
    
    /**
     * @return the configuration set/component properties
     *          with configuration name as Map key
     */
    public Map getProperty(){
        return _propertyMap;
    }
    
    /** 
     * @param global Set the global Map to this Map
     */
    public void setGlobal (Map global){
        _globalProperties = global;
    }

    /**
     * add a whole set of Raw Property Data properties ( NOT Global properties )
     * @param p <code>Map</code> of properties with key-value of 
     *          String name,RawPropertyData
     */
    public void addRPDProperties(Map p)
    {   
        
        if (_propertyMap == null)
            _propertyMap = new HashMap();
        if ( p != null && !p.isEmpty())
            _propertyMap.putAll(p);

        copyPropertiesToOtherMap(p);
        
    }
        
    /** copy all the data in the input Map into the Other Map
     */
    private void copyPropertiesToOtherMap(Map from)
    {
        // add new properties to the other map
        if ( from != null && !from.isEmpty()){
            if ( _otherPropMap == null){
                _otherPropMap = new HashMap();
            }
            
            Map classmap;
            for (Iterator it = from.values().iterator(); it.hasNext();){
                RawPropertyData rpd = (RawPropertyData)it.next();     
                String classname = rpd.getClassName();
                if ( _otherPropMap.containsKey(classname) ){
                   classmap = (Map)_otherPropMap.get(classname);     
                   if ( !classmap.containsKey(rpd.getName()) )
                       classmap.put(rpd.getName(),rpd);       
                }else{
                   classmap = new HashMap();
                   classmap.put(rpd.getName(),rpd);       
                   _otherPropMap.put(classname,classmap); 
                }
            }
        }
    }
        
    /**
     * @param c Set the component properties to this one
     */
    public void setProperty(Map c){
        _propertyMap = c;
        
        //create a duplicate map that has the classname 
        // as key instead of configuration name key
        if ( _otherPropMap == null)
            _otherPropMap = new HashMap();
        else
            _otherPropMap.clear();
        copyPropertiesToOtherMap(_propertyMap);
        
    }
    
}
