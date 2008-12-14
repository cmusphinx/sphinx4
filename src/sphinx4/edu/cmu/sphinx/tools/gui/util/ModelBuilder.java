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
import edu.cmu.sphinx.tools.gui.GUIFileActionListener;
import edu.cmu.sphinx.tools.gui.RawPropertyData;
import edu.cmu.sphinx.util.props.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;


/**
 * This class builds a model of the sphinx configurable system it will create <code>ConfigurableComponent</code> and
 * <code>ConfigurableProperty</code> instances as the main components of the model.
 * <p/>
 * Also holds configuration values and current state of the model
 *
 * @author Ariani
 */
public class ModelBuilder implements GUIFileActionListener {

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
    private static String _source_path;  // path separated by '/', and ends with '/'
    private static String _classes_path; // path separated by '/', and ends with '/'


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
     * Obtain a reference to the <code>ModelBuilder</code> instance. There is only one ModelBuilder instance created for
     * the whole system.
     *
     * @return <code>ModelBuilder</code> singleton object
     */
    public static ModelBuilder getInstance() {
        return ModelBuilderHolder.instance;
    }


    /** @return <code>Iterator</code> for all the classes in the model */
    public Iterator getClasses() {
        if (!_classes.isEmpty())
            return _classes.entrySet().iterator();
        else
            return null;
    }


    /** @return main groups of the model */
    public Iterator getGroups() {
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
     */
    public boolean checkDuplicateConfigurationSet(String name) {
        //iterate through all classes in the model, and check if the name exist
        // names are case-sensitive
        for (Object o : _classes.values()) {
            ConfigurableComponent cc = (ConfigurableComponent) o;
            if (cc.containsConfigurationSet(name)) {
                return true;
            }
        }
        return false;
    }


    /**
     * This method is inherited from GUIFileActionListener Delete all configurable property values that are stored in
     * the Model
     */
    public void clearAll() {
        for (Object o : _classes.values()) {
            // delete the configuration property values of each component in the model
            ConfigurableComponent cc = (ConfigurableComponent) o;
            cc.deleteAllConfigurationProp();
        }
    }


    /**
     * This method is inherited from GUIFileActionListener update the model by reloading the configuration values
     *
     * @param cp <code>ConfigProperty</code> that holds the new property values
     */
    public void update(ConfigProperties cp) {
        clearAll();
        loadCurrentValues(cp);
        // printModel();
    }


    /**
     * This method is inherited from GUIFileActionListener Currently the saved data all come only from ModelBuilder
     *
     * @param cp load all the data to be saved into cp
     */
    public void saveData(ConfigProperties cp) {
        // copy all configuration properties from each class        
        for (Object o : _classes.values()) {
            ConfigurableComponent cc = (ConfigurableComponent) o;
            cp.addRPDProperties(cc.getConfigurationPropMap());
        }
    }


    /**
     * This method is inherited from GUIFileActionListener Reload model
     *
     * @throws ConfigurableUtilException
     */
    public void modelRefresh() throws ConfigurableUtilException {
        refresh();
    }


    /**
     * This method would return the source code for the specified class name
     *
     * @param classname Name of class to be searched
     */
    public String getSourceCode(String classname) {
        String text = "";
        if (_source_path == null) {
            return "== Source code not available ==";
        }
        try {
            String completename = classname.trim().replace('.', '/');
            if (!_source_path.trim().endsWith("/")) // add  '/' at the end of path
                completename = _source_path.trim().concat("/" + completename);
            else
                completename = _source_path.trim().concat(completename);

            //add the .java at the end of class name
            if (!completename.endsWith(".java"))
                completename = completename.concat(".java");

            BufferedReader br = new BufferedReader(new FileReader(completename));
            String thisline;
            while ((thisline = br.readLine()) != null) {
                text = text.concat(thisline + '\n');
            }
            br.close();

        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
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
    public Map getclasslist(String classtype) {
        try {
            Class searchclass = Class.forName(classtype);
            Map<String, String> myreturn = new HashMap<String, String>();
            if (_classes != null && classtype != null &&
                    !classtype.trim().equalsIgnoreCase("")) {
                for (Object o : _classes.values()) {
                    ConfigurableComponent cc = (ConfigurableComponent) o;
                    Class c = cc.getComponentClass();
                    Map configset = cc.getConfigurationPropMap();

                    if (searchclass.isAssignableFrom(c) && (configset != null) &&
                            !configset.isEmpty()) {
                        // System.out.println("&&&" + searchclass.getName() + "is a superclass of" + c.getName());
                        for (Object o1 : configset.keySet()) {
                            String configname = (String) o1;
                            myreturn.put(configname, c.getName());
                            // System.out.println(" $$ " + c.getName() + " " + configname);

                        }
                    }

                }
            }
            return myreturn;
        } catch (ClassNotFoundException e) {
            // System.err.println("$$ class not found exception !");
            return null;
        }
    }

//    /** 
//     * private helper method to check if superclass is the superclass or 
//     * superinterface of subclass
//     * @param superclass super class
//     * @param subclass subclass
//     * @return true if the relationship is true
//     */
//    private boolean isSuperClassOf(Class superclass, Class subclass){
//       System.out.println("$$ checking "+superclass.getName() +" and " + subclass.getName());
//       if ()
//           return true;
//       if(subclass.getSuperclass() != null){
//           if(isSuperClassOf(superclass,subclass.getSuperclass()))
//               return true;
//       }
//       Class[] myinterfaces = subclass.getInterfaces();
//       if(myinterfaces != null){
//           for(int i =0;i<myinterfaces.length;i++){
//               if(isSuperClassOf(superclass,myinterfaces[i]))
//                   return true;
//           }
//       }
//       return false;
//    }


    /**
     * Load values in ConfigProperties into the model
     *
     * @param cp <code>ConfigProperty</code> that holds the new property values
     */
    private void loadCurrentValues(ConfigProperties cp) {
        Map rpdMap = cp.getOtherProp(); // get map that has classname as key

        // iterate through the property map and retrieve the values to initialize the model
        for (Object o : rpdMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String classname = (String) entry.getKey();
            for (Object o1 : ((Map) entry.getValue()).entrySet()) {
                Map.Entry nextentry = (Map.Entry) o1;
                RawPropertyData rpd = (RawPropertyData) nextentry.getValue();
                if (_classes.containsKey(classname)) {
                    // copy the value from rpd
                    ConfigurableComponent cc = (ConfigurableComponent) _classes.get(classname);
                    Map propertyMap = rpd.getProperties();
                    checkProperty(propertyMap, cc);  // check if rpd properties are valid
                    addIncompleteProps(propertyMap, cc); // add incomplete properties into rpd
                    cc.addConfigurationProp(rpd); // add rpd into the model

                } // end _classes contain this classname

            } // end for
        } // end for
    } // end loadCurrentValues


    /**
     * add the properties that are in the Configurable Component model, but not in the configurablecomponent yet, by
     * adding them as default this function should never be public
     */
    private void addIncompleteProps(Map propertyMap, ConfigurableComponent checker) {
        Map completePropMap = checker.getPropertyMap();
        for (Object o : completePropMap.entrySet()) {
            Map.Entry propentry = (Map.Entry) o;
            String propname = (String) propentry.getKey();

            // if it doesn't exist yet
            if (!propertyMap.containsKey(propname)) {
                ConfigurableProperty prop = (ConfigurableProperty) propentry.getValue();
                String defaultVal = prop.getDefault();
                if (defaultVal != null && !defaultVal.trim().equalsIgnoreCase("")) {
                    propertyMap.put(propname, defaultVal);
                    //  System.out.println("***** add prop "+propname);
                } else {
                    propertyMap.put(propname, null);
                    //  System.out.println("***** add null prop "+propname);
                }
            }
        }
    }


    /**
     * check if the set of configuration to be loaded corresponds to the property listed in the sphinx model. if it
     * doesn't exist in sphinx model, delete it this method should always be private
     */
    private void checkProperty(Map propertyMap, ConfigurableComponent checker) {
        for (Object o : propertyMap.entrySet()) {
            Map.Entry propentry = (Map.Entry) o;
            String propname = (String) propentry.getKey();

            if (!checker.containsProperty(propname)) {
                propertyMap.remove(propname);
                // delete because it doesn't exist in sphinx model               
            }
        } // end for      
    }


    /** @return component <code>Iterator</code> for the specified group */
    public Iterator getClassGroup(String groupname) {
        if (!_groups.entrySet().isEmpty() &&
                groupname != null && _groups.containsKey(groupname.toLowerCase())) {
            return ((Set) _groups.get(groupname)).iterator();
        } else
            return null;
    }


    /**
     * This function is used to trace the whole Sphinx model and print out all the Component and Property information,
     * including its configuration values
     */
    public void printModel() {
        //group members
        for (Object o : _groups.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String groupname = (String) entry.getKey();
            Set groupmembers = (Set) entry.getValue();
            System.out.println("Group : " + groupname);
            for (Object groupmember : groupmembers) {
                System.out.println("-" + ((ConfigurableComponent) groupmember).getName());
            }
        }
        for (Object o : _classes.values()) {
            ConfigurableComponent cc = (ConfigurableComponent) o;
            System.out.print(cc.toString());

        }
        System.out.println();
        System.out.flush();
    }


    /**
     * Refresh the Sphinx model
     *
     * @throws ConfigurableUtilException when there is an error during re-reading of Sphinx system
     */
    public void refresh() throws ConfigurableUtilException {
        _classes.clear();
        _groups.clear();

        //init config data
        clear_config();
        try {
            read_config_prop();
        } catch (ConfigurableUtilException e) {
            // the util.conf configuration read is not successful
            // probably the file does not exist 
            // (must be in the same dir as GUIMainDriver.class)
            // so - no features that requires the source code    
            System.err.println("Error:Configuration file not loaded successfully\n" +
                    "Error:Features that require access to source code will not be available");
        }
        read_classpath();

        // scan again and refresh the model
        scan(_folder_path, _package_path);
    }


    // clear the classpath and config properties
    private void clear_config() {
        _classes_path = null;
        _source_path = null;
    }


    // read the system properties, and get the classpath value
    // replace the old value it's still null
    private void read_classpath() {
        Properties props = System.getProperties();
        if (_classes_path == null) { // class path property not set from .conf file
            _classes_path = props.getProperty("java.class.path");
        }
    }


    // read the configuration file that keeps the path to
    // where Sphinx system source code files and compiled .class files
    // should be located
    private void read_config_prop() throws ConfigurableUtilException {
        try {
            InputStream in = this.getClass().getResourceAsStream(CONFIG_PATH);
            //FileInputStream in = new FileInputStream(CONFIG_PATH);
            Properties configuration = new Properties();
            configuration.load(in);   //load the properties from configuration file           
            in.close();
            _source_path = (String) configuration.get(SOURCE_PROP);
            System.out.println("source path: " + _source_path);
            _classes_path = (String) configuration.get(CLASSES_PROP);

        } catch (FileNotFoundException fe) {
            throw new ConfigurableUtilException("Configuration file " + CONFIG_PATH +
                    " not found", ConfigurableUtilException.UTIL_INIT);
        } catch (IOException ie) {
            throw new ConfigurableUtilException("Configuration " +
                    "File I/O Error", ConfigurableUtilException.UTIL_INIT);
        } catch (java.lang.NullPointerException e) {
            throw new ConfigurableUtilException("Configuration " +
                    "File Load Error", ConfigurableUtilException.UTIL_INIT);
        }
    }


    /**
     * Does the scanning starting from the given directory and create the list of configurable classes
     *
     * @param startDir     the root directory of Sphinx
     * @param startPackage the starting package of Sphinx
     * @throws <code>ConfigurableUtilException</code>
     *          when there is an error while scanning folders and building the model
     */
    private void scan(String startDir, String startPackage)
            throws ConfigurableUtilException {
        try {
            // get back a set of folder names that is directly under startDir
            List myFolders = ClassFinder.findFolder(startDir);

            Set myClasses;
            Set thisgroup;
            String tempName;
            ConfigurableComponent tempcc;

            // get the list of classes from each group
            for (Object myFolder : myFolders) {
                thisgroup = new HashSet();

                tempName = (String) myFolder;
                //   System.out.println("Folder "+tempName);

                myClasses = new HashSet(); // classes from this section
                ClassFinder.findClasses(startDir + '/' + tempName,
                        startPackage + '.' + tempName, myClasses);

                // get the classes for this section and build the model
                for (Object myClass : myClasses) {
                    try {
                        tempcc = createcomponent((Class) myClass,
                                startPackage + '.' + tempName);
                        _classes.put(tempcc.getName(), tempcc);
                        thisgroup.add(tempcc);
                    } catch (IllegalAccessException e) {
                        throw new ConfigurableUtilException
                                ("IllegalAccessException while creating configurable component",
                                        ConfigurableUtilException.UTIL_BUILDER);
                    }
                }

                // add the group to the Map with the section name as key    
                _groups.put(startPackage + '.' + tempName, thisgroup);

            }

        } catch (ClassNotFoundException e) {
            throw new ConfigurableUtilException("class not found " + e.getMessage(),
                    ConfigurableUtilException.UTIL_BUILDER);
        } catch (FileNotFoundException e) {
            throw new ConfigurableUtilException("file not found " + e.getMessage(),
                    ConfigurableUtilException.UTIL_BUILDER);
        } catch (IOException e) {
            throw new ConfigurableUtilException("Classpath loader error " + e.getMessage(),
                    ConfigurableUtilException.UTIL_PATHHACKER);
        }

    }//end scan method


    /**
     * Create a new ConfigurableComponent based on this <code>Class</code>
     *
     * @param c     Class that the ConfigurableComponent should be based on
     * @param group The name of the group/section this component belongs to
     * @return <code>ConfigurableComponent</code> instance
     */
    private ConfigurableComponent createcomponent(Class c, String group)
            throws ConfigurableUtilException, IllegalAccessException {
        ConfigurableComponent cc =
                new ConfigurableComponent(group, c, c.getName(), "");
        System.out.println("***** create ConfigurableComponent " +
                c.getName() + " for " + group);

        // check all the public fields
        // if any of them is with modifier 'public static final' and starts with 'PROP_'        
        Field[] publicFields = c.getFields();
        for (int i = 0; i < publicFields.length; i++) {
            int m = publicFields[i].getModifiers();
            String fieldname = publicFields[i].getName();
            //System.out.println("*** checking field "+fieldname);
            if (Modifier.isPublic(m) && Modifier.isStatic(m) && Modifier.isFinal(m)) {
                if (fieldname.startsWith("PROP_") && !fieldname.trim().endsWith("_DEFAULT")) {
                    //name of property
//                    String propname = new String((String) publicFields[i].get(null));
                    //create the configurable property
                    //System.out.println("***** create ConfigurableProperty " + 
                    //       (String)publicFields[i].get(null));
                    ConfigurableProperty cp = createProperty(c, publicFields[i]);
                    cc.addProperty(cp);
                }
            }
        }
        return cc;
    }//end createcomponent method


    /**
     * private method to retrieve complete info of a configurable property and create a
     * <code>ConfigurableProperty</code>
     *
     * @param c     Class that own this property
     * @param field Field that we are want to create a Configurable Property from
     * @return ConfigurableProperty
     */
    private ConfigurableProperty createProperty(Class c, Field field) throws ConfigurableUtilException {
        ConfigurableProperty cp;
        try {
            cp = CreatePropertyHelper.createProperty(field, c);
            //    cp = CreatePropertyHelper.createProperty(field,c);
        } catch (IllegalAccessException e) {
            throw new ConfigurableUtilException
                    ("IllegalAccessException while creating configurable property",
                            ConfigurableUtilException.UTIL_BUILDER);
        }
        return cp;
    }


    /**
     * This is a static class that is used as a helper to get information about a configurable property and to create a
     * ConfigurableProperty instace
     */
    private static class CreatePropertyHelper {

        //  create this private class to hold 
        //  property type, class type and default value info that are retrieved 
        //  from annotation of the property
        //  in sphinx4 source code

        private static Class _class;
        private static String _fieldname;
        private static Field _field;

        private static PropertyType _type = null;
        private static String _defval = null;
        private static String _classtype = null; // component type for this property 

//        private void setDefault(String defval){
//            _defval = defval;
//        }
//        
//        private void setType(PropertyType type){
//            _type = type;
//        }
//        
//        private void setClassType (String classtype){
//            _classtype = classtype;
//        }

        // will retrieve something 


        private static void processAnnotation() throws IllegalAccessException {

            Annotation[] annotations = _field.getAnnotations();

            // check each annotation belongs to this field
            for (Annotation annotation : annotations) {
                //search for the super common annotation of all s4 properties
                Annotation[] superAnnotations = annotation.annotationType().getAnnotations();
                for (Annotation superAnnotation : superAnnotations) {

                    // if this annotation belongs to a sphinx4 configurable property, 
                    // then only we get its info
                    if (superAnnotation instanceof S4Property) {
                        System.out.println("*** s4 property member");

                        // get info from the s4 property annotation
                        System.out.println(_field.getName() + " : *** " + annotation.annotationType().getName());
                        if (annotation instanceof S4Double) {
                            _type = PropertyType.DOUBLE;
                            _defval = Double.toString(((S4Double) annotation).defaultValue());
                        } else if (annotation instanceof S4Integer) {
                            _type = PropertyType.INT;
                            _defval = Integer.toString(((S4Integer) annotation).defaultValue());
                        } else if (annotation instanceof S4Component) {
                            _type = PropertyType.COMPONENT;
                            _classtype = ((S4Component) annotation).type().getName();
                        } else if (annotation instanceof S4String) {
                            _type = PropertyType.STRING;
                            _defval = ((S4String) annotation).defaultValue();
                        } else if (annotation instanceof S4Boolean) {
                            _type = PropertyType.BOOLEAN;
                            _defval = String.valueOf(((S4Boolean) annotation).defaultValue());
                        } else if (annotation instanceof S4ComponentList) {
                            _type = PropertyType.COMPONENT_LIST;
                            _classtype = ((S4ComponentList) annotation).type().getName();
                        }
//                        System.out.println(" *** Property type : " + _type);
//                        System.out.println(" *** class type : " + _classtype );
//                        System.out.println(" *** default value : " + _defval);
                    }
                }
            }

            // if we find no annotation for the default field, 
            // we try to look for a property with same name 
            // that ends with '_DEFAULT'
            if (_defval == null) {
                try {
                    Field tempField = _class.getField(_fieldname.trim() + "_DEFAULT");
                    if (tempField.get(null) != null) {// with default value
                        _defval = tempField.get(null).toString().trim();
                    }
                } catch (NoSuchFieldException e) {
                    // do nothing
                }
            }
        } // end processAnnotation


        private static ConfigurableProperty createProperty(Field f, Class c) throws IllegalAccessException {
            _class = c;
            _field = f;
            _fieldname = f.getName();
            _type = null;
            _defval = null;
            _classtype = null;

            ConfigurableProperty cp;
            String propname = (String) _field.get(null); //name of configurable property

            //javadoc comment of the property
            String field_comment = JavadocExtractor.getJavadocComment(_class.getName(),
                    _classes_path, _source_path, _fieldname);
            if (field_comment == null) // no comment
                field_comment = "";
            System.out.println(" Comment *** : " + field_comment);

            processAnnotation();

            if (_defval == null)
                _defval = "";

            if (_type != null &&
                    (_type == PropertyType.COMPONENT || _type == PropertyType.COMPONENT_LIST)) {
                // it's a class component type, 
                // _classtype info should exist if it is specified in sphinx4 source code                                    
                cp = new ConfigurableProperty
                        (propname, _defval, _type, field_comment, _fieldname, _classtype);
                // System.out.println("with type "+proptype+" and class type : " + classtype);

            } else { // it's a native java type
                cp = new ConfigurableProperty
                        (propname, _defval, _type, field_comment, _fieldname);
                // System.out.println("with type "+proptype);
            }
            return cp;
        } // end createProperty
    }// end static class CreatePropertyHelper

}//end ModelBuilder class