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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Manages the configuration for the system. The configuration manager provides
 * the following services:
 * 
 * <ul>
 * <li>Loads configuration data from an XML-based configuration file.
 * <li>Manages the component life-cycle for Configurable objects
 * <li>Allows discovery of components via name or type.
 * </ul>
 * 
 * Configuration data is managed externally in an XML file format. A sample
 * format is here:
 * 
 * <code> <pre>
 *  &lt;config&gt;
 *  &lt;component name=&quot;sample&quot; type=&quot;edu.cmu.sphinx.util.props.SampleComponent&quot;&gt;
 *  &lt;property name=&quot;width&quot; value=&quot;100&quot;/&gt;
 *  &lt;property name=&quot;height&quot; value=&quot;10&quot;/&gt;
 *  &lt;propertylist name=&quot;subsample&quot;&gt;
 *  &lt;item&gt;subsample2&lt;/item&gt;
 *  &lt;item&gt;subsample3&lt;/item&gt;
 *  &lt;/propertylist&gt;
 *  &lt;propertylist name=&quot;myList&quot;&gt;
 *  &lt;item&gt;ba1r&lt;/item&gt;
 *  &lt;item&gt;bar2&lt;/item&gt;
 *  &lt;item&gt;bar3&lt;/item&gt;
 *  &lt;item&gt;bar4&lt;/item&gt;
 *  &lt;/propertylist&gt;
 *  &lt;property name=&quot;single&quot; value=&quot;simple&quot;/&gt;
 *  &lt;/component&gt;
 *  &lt;component name=&quot;subsample3&quot; type=&quot;edu.cmu.sphinx.util.props.SampleComponent&quot;&gt;
 *  &lt;property name=&quot;width&quot; value=&quot;33&quot;/&gt;
 *  &lt;property name=&quot;height&quot; value=&quot;34&quot;/&gt;
 *  &lt;property name=&quot;depth&quot; value=&quot;4.42&quot;/&gt;
 *  &lt;propertylist name=&quot;myList&quot;&gt;
 *  &lt;item&gt;3&lt;/item&gt;
 *  &lt;item&gt;three&lt;/item&gt;
 *  &lt;item&gt;III&lt;/item&gt;
 *  &lt;/propertylist&gt;
 *  &lt;property name=&quot;single&quot; value=&quot;simple&quot;/&gt;
 *  &lt;/component&gt;
 *  &lt;component name=&quot;subsample2&quot; type=&quot;edu.cmu.sphinx.util.props.SampleComponent&quot;&gt;
 *  &lt;property name=&quot;width&quot; value=&quot;22&quot;/&gt;
 *  &lt;property name=&quot;height&quot; value=&quot;23&quot;/&gt;
 *  &lt;property name=&quot;depth&quot; value=&quot;1&quot;/&gt;
 *  &lt;propertylist name=&quot;myList&quot;&gt;
 *  &lt;item&gt;two&lt;/item&gt;
 *  &lt;item&gt;2&lt;/item&gt;
 *  &lt;/propertylist&gt;
 *  &lt;property name=&quot;single&quot; value=&quot;simple&quot;/&gt;
 *  &lt;/component&gt;
 *  &lt;component name=&quot;simple&quot; type=&quot;edu.cmu.sphinx.util.props.SimpleComponent&quot;&gt;
 *  &lt;/component&gt;
 *  &lt;/config&gt;
 * </pre> </code>
 * <p>
 * Configuration can also be performed from system properties. These are
 * typically given at the command line. These look like this:
 * <p>
 * 
 * <code><pre>
 *  java -Dsubsample2[width]=1 -Dsubsample3[height]=36 Batch
 * </pre> </code>
 * 
 * Properties defined in this way will silently override any properties set in
 * the configuration file.
 *  
 */
public class ConfigurationManager {
    
    /**
     * Sphinx Property that defines whether or not the configuration manager
     * will trace object creations
     */
    
    public final static String PROP_SHOW_CREATIONS = "showCreations";
    /**
     * The default value for PROP_SHOW_CREATIONS
     */
    public final static boolean PROP_SHOW_CREATIONS_DEFAULT = false;
    
    private Map symbolTable = new HashMap();
    private Map rawPropertyMap;
    private Map globalProperties = new HashMap();
    private boolean showCreations;

    /**
     * Creates a new configuration manager. Initial properties are loaded from
     * the given URL. No need to keep the notion of 'context' around anymore we
     * will just pass around this property manager.
     * 
     * @param url
     *                place to load initial properties from
     * @throws IOException
     *                 if an error occurs while loading properties from the URL
     *  
     */
    public ConfigurationManager(URL url) throws IOException, PropertyException {
        rawPropertyMap = loader(url);
        applySystemProperties(rawPropertyMap, globalProperties);
        
        // we can't config the configuration manager with itself so we
        // do some of these config items manually.
        
        showCreations = "true".equals(getMyProperty(PROP_SHOW_CREATIONS));
    }

    /**
     * Returns the property sheet for the given object instance
     * 
     * @param instanceName
     *                the instance name of the object
     * @return the property sheet for the object.
     */
    public PropertySheet getPropertySheet(String instanceName) {
        PropertySheet propertySheet = null;
        Symbol symbol = (Symbol) symbolTable.get(instanceName);
        if (symbol != null) {
            propertySheet = symbol.getPropertySheet();
        }
        return propertySheet;
    }

    /**
     * Returns the registry for the given object instance
     * 
     * @param instanceName
     *                the instance name of the object
     * @return the property sheet for the object.
     */
    public Registry getRegistry(String instanceName) {
        Registry registry = null;
        Symbol symbol = (Symbol) symbolTable.get(instanceName);
        if (symbol != null) {
            registry = symbol.getRegistry();
        }
        return registry;
    }
    /**
     * Gets all instances that are of the given type or are assignable to that
     * type. Object.class matches all.
     * 
     * @param type
     *                the desired type of instance
     * @return the set of all instances
     */
    public String[] getInstanceNames(Class type) {
        List list = new ArrayList();
        for (Iterator i = symbolTable.values().iterator(); i.hasNext();) {
            Symbol symbol = (Symbol) i.next();
            if (type.isInstance(symbol.getObject())) {
                list.add(symbol.getName());
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    /**
     * Looks up a configurable component by name. Creates it if necessary
     * 
     * @param name
     *                the name of the component
     * @return the compnent, or null if a component was not found.
     * 
     * @throws InstantiationException
     *                 if the requested object could not be properly created,
     *                 or is not a configurable object.
     * 
     * @throws PropertyException
     *                 if an error occurs while setting a property
     */
    public Configurable lookup(String name) throws InstantiationException,
            PropertyException {
        Symbol symbol = (Symbol) symbolTable.get(name);
        if (symbol == null) {
            if (showCreations) {
                System.out.println("Creating: " + name);
            }
            // it is not in the symbol table, so construct
            // it based upon our raw property data
            RawPropertyData rpd = (RawPropertyData) rawPropertyMap.get(name);
            if (rpd != null) {
                String className = rpd.getClassName();
                try {
                    Class cls = Class.forName(className);
                    Configurable configurable = (Configurable) cls
                            .newInstance();
                    Registry registry = new Registry(configurable);
                    configurable.register(name, registry);
                    ValidatingPropertySheet propertySheet = new ValidatingPropertySheet(
                            this, registry, rpd);
                    configurable.newProperties(propertySheet);
                    symbol = new Symbol(name, propertySheet, registry, configurable);
                    symbolTable.put(name, symbol);

                    return symbol.getObject();
                } catch (ClassNotFoundException e) {
                    throw new InstantiationException("Can't find class "
                            + className + " object:" + name);
                } catch (IllegalAccessException e) {
                    throw new InstantiationException("Can't access class "
                            + className + " object:" + name);
                } catch (ClassCastException e) {
                    throw new InstantiationException("Not configurable class "
                            + className + " object:" + name);
                }
            }
        } else {
            return symbol.getObject();
        }
        return null;
    }

    /**
     * Saves the current configuration to the given file
     * 
     * @param file
     *                place to save the configuration
     * @throws IOException
     *                 if an error occurs while writing to the file
     */
    public void save(File file) throws IOException, PropertyException {
        FileOutputStream fos = new FileOutputStream(file);
        PrintWriter writer = new PrintWriter(fos);
        writer.println("<config>");
        String[] allNames = getInstanceNames(Object.class);
        for (int i = 0; i < allNames.length; i++) {
            Symbol symbol = (Symbol) symbolTable.get(allNames[i]);
            writer.println("    <component name=\"" + symbol.getName() + "\""
                    + "  type=\"" + symbol.getObject().getClass().getName()
                    + "\">");
            PropertySheet ps = symbol.getPropertySheet();
            String[] names = ps.getNames();
            for (int j = 0; j < names.length; j++) {
                Object obj = ps.getRaw(names[j]);
                if (obj instanceof String) {
                    String value = (String) obj;
                    writer.println("        <property name=\"" + names[j]
                            + "\" value=\"" + value + "\"/>");
                } else if (obj instanceof List) {
                    List list = (List) obj;
                    writer.println("        <propertylist name=\"" + names[j]
                            + "\">");
                    for (int k = 0; k < list.size(); k++) {
                        writer.println("            <item>" + list.get(k)
                                + "</item>");
                    }
                    writer.println("        </propertylist>");
                } else {
                    throw new IOException("Ill-formed xml");
                }
            }
            writer.println("    </component>");
        }
        writer.println("</config>");
        writer.close();
    }
    

    /**
     * Shows the current configuration
     * 
     */
    public void showConfig()  {
        System.out.println(" ============ config ============= ");
        String[] allNames = getInstanceNames(Object.class);
        for (int i = 0; i < allNames.length; i++) {
            Symbol symbol = (Symbol) symbolTable.get(allNames[i]);
            
            System.out.println(symbol.getName() + ":");
            
            Registry registry = symbol.getRegistry();
            Collection propertyNames = registry.getRegisteredProperties();
            PropertySheet properties = symbol.getPropertySheet();
            
            for (Iterator j = propertyNames.iterator(); j.hasNext(); ) {
                String propName = (String) j.next();
                System.out.print("    " + propName + " = ");
                Object obj;
                try {
                    obj = properties.getRaw(propName);
                } catch (PropertyException e) {
                    // this exception can occcur if a global name
                    // can't be resolved ...
                    obj = "[Unresolved!]";
                }
                if (obj instanceof String) {
                    System.out.println(obj);
                } else if (obj instanceof List) {
                    List l = (List) obj;
                    for (Iterator k = l.iterator(); k.hasNext(); ) {
                        System.out.print(k.next());
                        if (k.hasNext()) {
                            System.out.print(", ");
                        }
                    }
                    System.out.println();
                } else {
                    System.out.println("[DEFAULT]");
                }
            }
            System.out.println();
        }

    }

    /**
     * Loads the configuration data from the given url and adds the info to the
     * symbol table. Throws an IOexception if there is a problem loading the
     * table. Note that this only performs a partial populating of the symbol
     * table entry
     * 
     * @param url
     *                the url to load from
     * @param rawPropertyMap
     *                raw properties are placed here.
     * @throws IOException
     *                 if an error occurs while loading the symbol table
     */
    private Map loader(URL url) throws IOException {
        SaxLoader saxLoader = new SaxLoader(url, globalProperties);
        return saxLoader.load();
    }

    /**
     * Applies the system properties to the raw property map. System properties
     * should be of the form compName[paramName]=paramValue
     * 
     * List types cannot currently be set from system properties.
     * 
     * @param rawMap
     *                the map of raw property values
     * @param map global properies
     * 
     * @throws PropertyException
     *                 if an attempt is made to set a parameter for an unknown
     *                 component.
     */
    private void applySystemProperties(Map rawMap, Map global) throws PropertyException {
        Properties props = System.getProperties();
        for (Enumeration e = props.keys(); e.hasMoreElements();) {
            String param = (String) e.nextElement();
            String value = props.getProperty(param);
            
            // search for params of the form component[param]=value
            // thise go in the property sheet for the component
            int lb = param.indexOf('[');
            int rb = param.indexOf(']');

            if (lb > 0 && rb > lb) {
                String compName = param.substring(0, lb);
                String paramName = param.substring(lb + 1, rb);
                RawPropertyData rpd = (RawPropertyData) rawMap.get(compName);
                if (rpd != null) {
                    rpd.add(paramName, value);
                } else {
                    throw new PropertyException(null, param,
                            "System property attempting to set parameter "
                                    + " for unknown component " + compName
                                    + " (" + param + ")");
                }
            }
            
            // look for params of the form foo=fum
            // these go in the global map
            
            else if (param.indexOf('.') == -1) {
                String symbolName = "${" + param + "}";
                global.put(symbolName, value);
            }
        }
    }
    
    
    /**
     * lookup the global symbol with the given name
     * 
     * @param key the symbol name to lookup
     * @return the symbol value
     */
    String globalLookup(String key) {
        return (String) globalProperties.get(key);
    }
    
    /**
     * Gets the config properties for the configuration manager itself
     * @param key the name of the property
     * @return the property value or null if it doesn't exist.
     */
    private String getMyProperty(String key) {
        return globalLookup("${" + key + "}");
    }
    
}
