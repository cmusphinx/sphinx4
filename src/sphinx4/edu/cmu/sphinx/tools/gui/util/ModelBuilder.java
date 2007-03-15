/*
 * ModelBuilder.java
 *
 * Created on December 1, 2006, 4:51 PM
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

import edu.cmu.sphinx.tools.gui.ConfigProperties;
import edu.cmu.sphinx.tools.gui.RawPropertyData;
import edu.cmu.sphinx.tools.gui.GUIFileActionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;


/**
 * This class builds a model of the sphinx configurable system
 * it will create <code>ConfigurableComponent</code> and <code>ConfigurableProperty</code>
 * instances as the main components of the model.
 * <p>
 * Also holds configuration values and current state of the model
 * 
 * @author Ariani
 */
public class ModelBuilder implements GUIFileActionListener{
    
    private static final String CONFIG_PATH = "/edu/cmu/sphinx/tools/gui/util.conf";
    private static final String FOLDER_PROP = "edu/cmu/sphinx";
    private static final String PACKAGE_PROP = "edu.cmu.sphinx";
    private static final String SOURCE_PROP = "source_path";
    private static final String CLASSES_PROP = "classes_path";
    
    private Map _classes; // configurable classes (String name,ConfigurableComponent)
    private Map _groups; //String,Set of ConfigurableComponent
    // this is the root folders of all sphinx classes
    private String _folder_path = FOLDER_PROP;
    // this is the root package of all sphinx classes
    private String _package_path = PACKAGE_PROP;
    private String _source_path;  // path separated by '/', and ends with '/'
    private String _classes_path; // path separated by '/', and ends with '/'
    
    /** Creates a Singleton instance of ModelBuilder */
    private ModelBuilder() {
        _classes = new HashMap();
        _groups = new HashMap();
        _source_path = null;
        _classes_path = null;
                
    }
    
    private static class ModelBuilderHolder {
          private static ModelBuilder instance = new ModelBuilder();
    }
      
    /**
     * Obtain a reference to the <code>ModelBuilder</code> instance.
     * There is only one ModelBuilder instance created for the whole system.
     *
     * @return <code>ModelBuilder</code> singleton object
     */
    public static ModelBuilder getInstance(){
          return ModelBuilderHolder.instance;
    }
    
    /**
     * @return <code>Iterator</code> for all the classes in the model
     */
    public Iterator getClasses(){
        if (!_classes.isEmpty())
            return _classes.entrySet().iterator();
        else
            return null;
    }
    
    /**
     * @return main groups of the model
     */
    public Iterator getGroups(){
         if (!_groups.isEmpty())
            return _groups.entrySet().iterator();
        else
            return null;
    }
    
    /** 
     * Check if there is already a configuration set with this name in the whole model
     * 
     * @param name
     * @return <code>true</code> if configuration with specified name exists
     **/
    public boolean checkDuplicateConfigurationSet(String name){
        //iterate through all classes in the model, and check if the name exist
        // names are case-sensitive
        for( Iterator it = _classes.values().iterator(); it.hasNext();){
            ConfigurableComponent cc = (ConfigurableComponent)it.next();
            if ( cc.containsConfigurationSet(name) ){
                return true;
            }
        }
        return false;
    }
    
    /**
     * This method is inherited from GUIFileActionListener
     * Delete all configurable property values that are stored in the Model
     */
    public void clearAll(){
        for(Iterator it=_classes.values().iterator();it.hasNext();){
            // delete the configuration property values of each component in the model
            ConfigurableComponent cc = (ConfigurableComponent)it.next();            
            cc.deleteAllConfigurationProp();            
        }
    }
    
    /** This method is inherited from GUIFileActionListener
     * update the model by reloading the configuration values  
     * @param cp <code>ConfigProperty</code> that holds the new property values
     */ 
    public void update(ConfigProperties cp){
        clearAll();
        loadCurrentValues(cp);
        // printModel();
    }
    
    /** 
     * This method is inherited from GUIFileActionListener
     * Currently the saved data all come only from ModelBuilder
     * @param cp load all the data to be saved into cp
     */
    public void saveData(ConfigProperties cp){
        // copy all configuration properties from each class        
        for ( Iterator it = _classes.values().iterator(); it.hasNext(); ){
            ConfigurableComponent cc = (ConfigurableComponent)it.next();
            cp.addRPDProperties( cc.getConfigurationPropMap() );
        }
    }
    
    /** 
     * This method is inherited from GUIFileActionListener
     * Reload model
     *
     * @throws ConfigurableUtilException
     */
    public void modelRefresh()throws ConfigurableUtilException{
        refresh();
    }
    
    /**
     * This method would return the source code for the specified class name
     *
     * @param classname Name of class to be searched
     */
    public String getSourceCode(String classname){
        String text = "";
        if (_source_path == null){  
            return new String("== Source code not available ==");
        }
        try {
            String completename =  classname.trim().replace('.','/');
            if ( !_source_path.trim().endsWith("/")) // add  '/' at the end of path
                completename = _source_path.trim().concat("/"+completename);
            else
                completename = _source_path.trim().concat(completename);
            
            //add the .java at the end of class name
            if(!completename.endsWith(".java"))
                completename = completename.concat(".java");
                       
            BufferedReader br = new BufferedReader(new FileReader(completename));  
            String thisline ;
            while( (thisline  = br.readLine()) != null ){
                text = text.concat(thisline+'\n');
            }
            br.close();

        }catch(FileNotFoundException e){
            return null;
        }catch(IOException e){
            return null;
        }
        return text;
    }
    
    /**
     * get list of Configurable classes that is of specified type
     * 
     * @param classtype Type of class
     * @return List of classes
     */
    public Map getclasslist(String classtype){
        try{
            Class searchclass = Class.forName(classtype);       
            Map myreturn = new HashMap();
            if (_classes != null && classtype != null && 
                    !classtype.trim().equalsIgnoreCase("")) {
                for(Iterator it = _classes.values().iterator(); it.hasNext();){
                    ConfigurableComponent cc = (ConfigurableComponent)it.next();
                    Class c = cc.getComponentClass();
                    Map configset = cc.getConfigurationPropMap();           
                    
                    if(searchclass.isAssignableFrom(c) && configset!= null &&
                            !configset.isEmpty())
                    {                    
                        for(Iterator it2 = configset.keySet().iterator();it2.hasNext();)
                        {
                            String configname = (String)it2.next();                        
                            myreturn.put(c.getName(),configname);
                        }
                    }

                }
            }
            return myreturn;
         }catch(ClassNotFoundException e){
            return null;
        }        
    }
    
    
    /**
     * Load values in ConfigProperties into the model 
     *
     * @param cp <code>ConfigProperty</code> that holds the new property values
     */
    private void loadCurrentValues(ConfigProperties cp){
        Map rpdMap = cp.getOtherProp(); // get map that has classname as key
        
         // iterate through the property map and retrieve the values to initialize the model 
        for(Iterator it = rpdMap.entrySet().iterator();it.hasNext();){           
            Map.Entry entry = (Map.Entry) it.next();            
            String classname = (String)entry.getKey();   
            //System.out.println("$$ "+classname);
            for(Iterator it2 = ((Map)entry.getValue()).entrySet().iterator();it2.hasNext();){   
                Map.Entry nextentry = (Map.Entry)it2.next();                
                RawPropertyData rpd = (RawPropertyData)nextentry.getValue() ;
                if ( _classes.containsKey(classname) ){
                    // copy the value from rpd
                    ConfigurableComponent cc = (ConfigurableComponent)_classes.get(classname);
                    Map propertyMap = rpd.getProperties();
                    checkProperty(propertyMap, cc);  // check if rpd properties are valid
                    addIncompleteProps(propertyMap, cc); // add incomplete properties into rpd
                    cc.addConfigurationProp(rpd); // add rpd into the model

                } // end _classes contain this classname
            
            } // end for
        } // end for
    } // end loadCurrentValues
    
    /**
     * add the properties that are in the Configurable Component model, 
     * but not in the configurablecomponent yet,
     * by adding them as default
     * this function should never be public
     */
    private void addIncompleteProps(Map propertyMap, ConfigurableComponent checker){
        Map completePropMap = checker.getPropertyMap();
        for (Iterator it = completePropMap.entrySet().iterator();it.hasNext();){
            Map.Entry propentry = (Map.Entry)it.next();
            String propname = (String)propentry.getKey();
                       
            // if it doesn't exist yet
            if( !propertyMap.containsKey(propname) ){
                ConfigurableProperty prop = (ConfigurableProperty)propentry.getValue();
                String defaultVal = prop.getDefault();
                if( defaultVal != null && !defaultVal.trim().equalsIgnoreCase("")){
                    propertyMap.put(propname,defaultVal);
                    //  System.out.println("***** add prop "+propname);
                }
                else {
                    propertyMap.put(propname, null);           
                    //  System.out.println("***** add null prop "+propname);
                }
            }
        }
    }
    
    /**
     * check if the set of configuration to be loaded corresponds to the property listed 
     * in the sphinx model. if it doesn't exist in sphinx model, delete it
     * this method should always be private
     */     
    private void checkProperty(Map propertyMap,ConfigurableComponent checker)
    {
        for( Iterator it2 = propertyMap.entrySet().iterator();it2.hasNext();){
            Map.Entry propentry = (Map.Entry) it2.next();            
            String propname = (String)propentry.getKey(); 
                    
            if ( !checker.containsProperty(propname) ){        
                propertyMap.remove(propname); 
                // delete because it doesn't exist in sphinx model               
            }
        } // end for      
    }
    
    
    /**
     * @return component <code>Iterator</code> for the specified group
     */
    public Iterator getClassGroup(String groupname){
        if(!_groups.entrySet().isEmpty() && 
                groupname != null && _groups.containsKey(groupname.toLowerCase())){
                return ((Set)_groups.get(groupname)).iterator();
        }
        else 
            return null;
    }
    
   
    /**
     * This function is used to trace the whole Sphinx model and print out all 
     * the Component and Property information, including its configuration values
     */
    public void printModel(){        
        //group members
        for(Iterator it=_groups.entrySet().iterator();it.hasNext();){            
            Map.Entry entry = (Map.Entry) it.next();            
            String groupname = (String)entry.getKey();            
            Set groupmembers = (Set)entry.getValue() ;
            System.out.println("Group : "+groupname);
            for( Iterator it2=groupmembers.iterator();it2.hasNext(); ){
                System.out.println("-"+((ConfigurableComponent)it2.next()).getName());
            }
        }
        for(Iterator it3=_classes.values().iterator();it3.hasNext();){
            ConfigurableComponent cc = (ConfigurableComponent)it3.next();            
            System.out.print(cc.toString());
            
        }
        System.out.println();
        System.out.flush();
    }
        
    
    /**
     * Refresh the Sphinx model 
     *
     * @throws ConfigurableUtilException when there is an error during re-reading 
     *          of Sphinx system
     */
     public void refresh() throws ConfigurableUtilException
    {
         _classes.clear();
         _groups.clear();
         
        //init config data
        clear_config();
        try {
            read_config_prop();
        }catch (ConfigurableUtilException e){
            // the util.conf configuration read is not successful
            // probably the file does not exist 
            // (must be in the same dir as GUIMainDriver.class)
            // so - no features that requires the source code    
            System.err.println("Error:Configuration file not loaded successfully\n" + 
                    "Error:Features that require access to source code will not be available");
        }
        read_classpath();
             
        // scan again and refresh the model
        scan(_folder_path,_package_path);
    }
     
          
     // clear the classpath and config properties
     private void clear_config(){
         _classes_path = null;
         _source_path = null;
     }
     
     // read the system properties, and get the classpath value
     // replace the old value it's still null
     private void read_classpath(){
          Properties props = System.getProperties();
          if(_classes_path == null){ // class path property not set from .conf file
              _classes_path = props.getProperty("java.class.path");     
          }
     }
    
     // read the configuration file that keeps the path to
     // where Sphinx system source code files and compiled .class files 
     // should be located
    private void read_config_prop()throws ConfigurableUtilException{
        try {            
            InputStream in = this.getClass().getResourceAsStream(CONFIG_PATH);            
            //FileInputStream in = new FileInputStream(CONFIG_PATH);
            Properties configuration  = new Properties();                             
            configuration.load(in);   //load the properties from configuration file           
            in.close();               
            _source_path = (String)configuration.get(SOURCE_PROP);                
            _classes_path = (String)configuration.get(CLASSES_PROP);
            
        }catch(FileNotFoundException fe){
            throw new ConfigurableUtilException("Configuration file "+CONFIG_PATH+
                    " not found",ConfigurableUtilException.UTIL_INIT);
        }catch(IOException ie){
            throw new ConfigurableUtilException("Configuration " +
                    "File I/O Error",ConfigurableUtilException.UTIL_INIT);
        }catch(java.lang.NullPointerException e){
            throw new ConfigurableUtilException("Configuration " +
                    "File Load Error",ConfigurableUtilException.UTIL_INIT);
        }
    }
    
    /**
     * Does the scanning starting from the given directory and 
     * create the list of configurable classes
     *
     * @param startDir the root directory of Sphinx 
     * @param startPackage the starting package of Sphinx
     * @throws <code>ConfigurableUtilException</code> when there is an error while
     *          scanning folders and building the model
     */
    private void scan(String startDir, String startPackage)
        throws ConfigurableUtilException
    {        
        try{
            // get back a set of folder names that is directly under startDir
            List myFolders = ClassFinder.findFolder(startDir);
                        
            Set myClasses;
            Set thisgroup;
            String tempName;
            ConfigurableComponent tempcc;
            
            // get the list of classes from each group
            for(Iterator it=myFolders.iterator();it.hasNext();){                
                thisgroup = new HashSet();
                
                tempName = (String)it.next();
             //   System.out.println("Folder "+tempName);
                
                myClasses =  new HashSet(); // classes from this section
                ClassFinder.findClasses(startDir+'/'+tempName, 
                        startPackage+'.'+tempName, myClasses);
               
                // get the classes for this section and build the model
                for(Iterator it2=myClasses.iterator();it2.hasNext();){
                    try{
                       tempcc = createcomponent((Class)it2.next(), 
                                 startPackage+'.'+tempName);                                
                        _classes.put(tempcc.getName(),tempcc);
                        thisgroup.add(tempcc);
                    } catch(IllegalAccessException e){
                        throw new ConfigurableUtilException
                           ("IllegalAccessException while creating configurable component",
                           ConfigurableUtilException.UTIL_BUILDER);
                    }
                }
               
                // add the group to the Map with the section name as key    
                _groups.put(startPackage+'.'+tempName,thisgroup);
                
            }
                
        }catch (ClassNotFoundException e){
            throw new ConfigurableUtilException("class not found "+e.getMessage(),
                        ConfigurableUtilException.UTIL_BUILDER);
        }catch (FileNotFoundException e){
            throw new ConfigurableUtilException("file not found "+e.getMessage(),
                    ConfigurableUtilException.UTIL_BUILDER);
        }catch(IOException e){
            throw new ConfigurableUtilException("Classpath loader error "+ e.getMessage(),
                    ConfigurableUtilException.UTIL_PATHHACKER);
        }
        
    }//end scan method

    /**
     * Create a new ConfigurableComponent based on this <code>Class</code>
     * 
     * @param c Class that the ConfigurableComponent should be based on
     * @param group The name of the group/section this component belongs to
     * @return <code>ConfigurableComponent</code> instance
     */
    private ConfigurableComponent createcomponent(Class c, String group)
        throws ConfigurableUtilException, IllegalAccessException
    {
        ConfigurableComponent cc = 
                new ConfigurableComponent(new String(group),c,c.getName(),new String(""));
        System.out.println("***** create ConfigurableComponent " + 
            c.getName() + " for " + group );    
        
        Map classinfo = getMapConfigurationInfo(c); // get information set for class properties
        
        // check all the public fields
        // if any of them is with modifier 'public static final' and starts with 'PROP_'        
        Field[] publicFields = c.getFields();             
        for (int i = 0; i < publicFields.length ; i++){
            int m = publicFields[i].getModifiers();
            String fieldname = publicFields[i].getName();
            //System.out.println("*** checking field "+fieldname);
            if (Modifier.isPublic(m) && Modifier.isStatic(m)&&  Modifier.isFinal(m)){      
                if(fieldname.startsWith("PROP_") && !fieldname.trim().endsWith("_DEFAULT") ){                     
                    //name of property
                    String propname = new String((String)publicFields[i].get(null));
                     //create the configurable property
                    //System.out.println("***** create ConfigurableProperty " + 
                    //       (String)publicFields[i].get(null));
                    ConfigurableProperty cp = createProperty(c,fieldname,propname,classinfo);                    
                    cc.addProperty(cp);
                }
            }                
        }      
        return cc;        
    }//end createcomponent method

    /**
     * private method to retrieve complete info of a configurable property and 
     * create a <code>ConfigurableProperty</code>
     * 
     * @param c Class that own this property
     * @param fieldname Name of the attribute field
     * @param propname Value of the attribute - which is the name of configurable property
     * @param classinfo Map containing additional information about configurable properties
     *          of this class
     * @return ConfigurableProperty
     */
    private ConfigurableProperty createProperty(Class c,String fieldname,String propname,
            Map classinfo) throws ConfigurableUtilException{
           
        ConfigurableProperty cp=null;
        //javadoc comment of the property
        String field_comment = JavadocExtractor.getJavadocComment(c.getName(), 
                    _classes_path, _source_path, fieldname);
        if (field_comment == null) // no comment
            field_comment = new String("");

        String default_value=null;
        try{
            // default value if it exists
            default_value = getDefaultValue(c,fieldname);
            //System.out.println("*** with default =" + default_value + "=" );
        }catch(IllegalAccessException e){
                        throw new ConfigurableUtilException
                           ("IllegalAccessException while creating configurable property",
                           ConfigurableUtilException.UTIL_BUILDER);
        }
        if (default_value == null) // no default value
            default_value = new String("");

        PropertyType proptype = getPropType(classinfo,fieldname);        
        if ( proptype !=null && 
                proptype == PropertyType.COMPONENT){ // it's a class component type                    
            String classtype = getInfo(classinfo,fieldname,"_CLASSTYPE");
            cp = new ConfigurableProperty
                 (propname,default_value, proptype, field_comment,fieldname,classtype);
             System.out.println("with type "+proptype+" and class type : " + classtype);
        }else{ // it's a native java type                 
            cp = new ConfigurableProperty
                    (propname,default_value, proptype, field_comment,fieldname);            
             System.out.println("with type "+proptype);
        }
        return cp;
    }
    
    /**
     * private method to get more information about this property
     *
     * @param myinfo Map that holds additional information of all properties in the class
     * @param propname Property name that we're interested in
     * @return PropertyType of this property
     */
    private PropertyType getPropType(Map myinfo, String fieldname){
        PropertyType myType = null;
        String proptype = getInfo(myinfo,fieldname,new String("_TYPE"));                
        
        if ( proptype == null ){
            return null;
        }
        else if (proptype.equals("COMPONENT")){            
            myType= PropertyType.COMPONENT;
        }            
        else if (proptype.equals("INTEGER")){
           myType= PropertyType.INT;
        }            
        else if (proptype.equals("BOOLEAN")){
            myType= PropertyType.BOOLEAN;
        }
        else if (proptype.equals("STRING_LIST")){
           myType=PropertyType.STRING_LIST;
        }
        else if (proptype.equals("DOUBLE")){
            myType=PropertyType.DOUBLE;
        }
        else if (proptype.equals("COMPONENT_LIST")){
            myType= PropertyType.COMPONENT_LIST;                
        }
        else if (proptype.equals("STRING")){
            myType=PropertyType.STRING;
        }
        else if (proptype.equals("RESOURCE")){
            myType=PropertyType.RESOURCE;
        }
        else if (proptype.equals("FLOAT")){
            myType= PropertyType.FLOAT;
        }       
        else
            return null;
        return myType;
    }

    /**
     * private method to get more information about this property
     *
     * @param myinfo Map that holds additional information of all properties in the class
     * @param propname Property name that we're interested in
     * @param infotype The information type needed
     * @return String information of this property
     */
    private String getInfo(Map myinfo, String fieldname,String infotype){
        
        if (myinfo == null || !myinfo.containsKey(fieldname+infotype))
            return null; 
        else{                              
            return (String)myinfo.get(fieldname+infotype);
        }
    }
    
    /** 
     * private method to check if this property has default value
     * and to retrieve the default value
     *
     * @param c Class to be checked
     * @param fieldname Name of the property
     * @return <code>null</code> if there is no default value, otherwise
     *          String of default value
     */
    private String getDefaultValue(Class c, String fieldname)throws IllegalAccessException{
        String default_value;
        try {
            Field tempField=c.getField(fieldname.trim()+ "_DEFAULT");     
            if (tempField.get(null) != null){// with default value   
                default_value = tempField.get(null).toString().trim();
            }else{// no default value
                 return null;
            }                            
        }catch (NoSuchFieldException e){
            return null;// no default value
        }
        return default_value;
    }
    
    /**
     * private method to get Map containing information about all the 
     * property PROP_ in this Configurable class
     *
     * @param c Configurable class
     * @return Map containing information about the configurable property 
     */
    private Map getMapConfigurationInfo(Class c){
        try{
            Method infomethod = c.getMethod("getConfigurationInfo",(Class[])null);
            try {                
                Map myinfo = (Map)infomethod.invoke(null, (Object[]) null);                              
                return myinfo;
            }catch ( IllegalAccessException e) {
                System.err.println(e.getMessage());
                return null;
            }catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
                return null;
            }catch (InvocationTargetException e) {
                System.err.println(e.getMessage());
                return null;
            }
        }catch(NoSuchMethodException e){
            System.err.println("NoSuchMethodException:"+e.getMessage());
            return null;
        }catch(NullPointerException e){
            System.err.println("NullPointerException"+e.getMessage());
            return null;            
        }catch(SecurityException e){
            System.err.println("SecurityException"+e.getMessage());
            return null;
        }
    } // end getMapConfigurationInfo
    
}//end ModelBuilder class