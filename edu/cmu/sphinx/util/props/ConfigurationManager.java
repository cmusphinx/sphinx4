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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Enumeration;



/**
 * Manages the configuration for the system. The configuration manager
 * provides the following services:
 * 
 * <ul>
 * <li> Loads configuration data from an XML-based configuration file.
 * <li> Manages the component life-cycle for Configurable objects
 * <li> Allows discovery of components via name or type.
 * </ul>
 * 
 * Configuration data is managed externally in an XML file format. A sample 
 * format is here:
 * 
 * <code> <pre>
 * &lt;config>
 *     &lt;component name="sample"  type="edu.cmu.sphinx.util.props.SampleComponent">
 *         &lt;property name="width" value="100"/>
 *         &lt;property name="height" value="10"/>
 *         &lt;propertylist name="subsample">
 *             &lt;item>subsample2&lt;/item>
 *             &lt;item>subsample3&lt;/item>
 *         &lt;/propertylist>
 *         &lt;propertylist name="myList">
 *             &lt;item>ba1r&lt;/item>
 *             &lt;item>bar2&lt;/item>
 *             &lt;item>bar3&lt;/item>
 *             &lt;item>bar4&lt;/item>
 *        &lt;/propertylist>
 *         &lt;property name="single" value="simple"/>
 *     &lt;/component>
 *     &lt;component name="subsample3"  type="edu.cmu.sphinx.util.props.SampleComponent">
 *         &lt;property name="width" value="33"/>
 *         &lt;property name="height" value="34"/>
 *         &lt;property name="depth" value="4.42"/>
 *         &lt;propertylist name="myList">
 *            &lt;item>3&lt;/item>
 *             &lt;item>three&lt;/item>
 *             &lt;item>III&lt;/item>
 *         &lt;/propertylist>
 *         &lt;property name="single" value="simple"/>
 *     &lt;/component>
 *     &lt;component name="subsample2"  type="edu.cmu.sphinx.util.props.SampleComponent">
 *         &lt;property name="width" value="22"/>
 *         &lt;property name="height" value="23"/>
 *         &lt;property name="depth" value="1"/>
 *         &lt;propertylist name="myList">
 *             &lt;item>two&lt;/item>
 *             &lt;item>2&lt;/item>
 *         &lt;/propertylist>
 *         &lt;property name="single" value="simple"/>
 *     &lt;/component>
 *     &lt;component name="simple"  type="edu.cmu.sphinx.util.props.SimpleComponent">
 *     &lt;/component>
 * &lt;/config>
 * </pre> </code>
 * <p>
 * Configuration can also be performed from system properties. These are 
 * typically given at the command line. These look like this: <p>
 * 
 * <code><pre>
 *     java -Dsubsample2[width]=1 -Dsubsample3[height]=36 Batch
 * </pre> </code>
 * 
 * Properties defined in this way will silently override any properties set in
 * the configuration file.
 *  
 */
public class ConfigurationManager {
    private Map symbolTable = new HashMap();
    private Map rawPropertyMap;
    /**
     * Creates a new configuratin manager. Initial properties are loaded from the
     * given URL. No need to keep the notion of 'context' around anymore we
     * will just pass around this property manager.
     * 
     * @param url
     *            place to load initial properties from
     * @throws IOException
     *             if an error occurs while loading properties from the URL
     *  
     */
    public ConfigurationManager(URL url) throws IOException, PropertyException {
        rawPropertyMap = loader(url);
        applySystemProperties(rawPropertyMap);
    }

    /**
     * Returns the property sheet for the given object instance
     * 
     * @param instanceName
     *            the instance name of the object
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
     * Gets all instances that are of the given type or are assignable to that
     * type. Object.class matches all.
     * 
     * @param type
     *            the desired type of instance
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
     *            the name of the component
     * @return the compnent, or null if a component was not found.
     * 
     * @throws InstantiationException
     *             if the requested object could not be properly created, or is
     *             not a configurable object.
     * 
     * @throws PropertyException
     *             if an error occurs while setting a property
     */
    public Configurable lookup(String name) throws InstantiationException,
            PropertyException {
        Symbol symbol = (Symbol) symbolTable.get(name);
        if (symbol == null) {
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
                    symbol = new Symbol(name, propertySheet, configurable);
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
     *            place to save the configuration
     * @throws IOException
     *             if an error occurs while writing to the file
     */
    public void save(File file) throws IOException {
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
                    writer.println("        <propertylist name=\"" + names[j] + "\">");
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
     * Loads the configuration data from the given url and adds the info to the
     * symbol table. Throws an IOexception if there is a problem loading the
     * table. Note that this only performs a partial populating of the symbol
     * table entry
     * 
     * @param url
     *            the url to load from
     * @param rawPropertyMap
     *            raw properties are placed here.
     * @throws IOException
     *             if an error occurs while loading the symbol table
     */
    private Map loader(URL url) throws IOException {
        SaxLoader saxLoader = new SaxLoader(url);
        return saxLoader.load();
    }
    
    /**
     * Applies the system properties to the raw property map. System properties
     * should be of the form  compName[paramName]=paramValue
     * 
     * List types cannot currently be set from system properties.
     * 
     * @param rawMap the map of raw property values
     * 
     * @throws PropertyException if an attempt is made to set a parameter
     * for an unknown component.
     */
    private void applySystemProperties(Map rawMap) throws PropertyException {
        Properties props = System.getProperties();
        for (Enumeration e = props.keys(); e.hasMoreElements(); ) {
            String param = (String) e.nextElement();
            String value = props.getProperty(param);
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
                            "System property attempting to set parameter " +
                            " for unknown component " + 
                            compName + " (" + param + ")");
                }
            }
        }
    }
}
