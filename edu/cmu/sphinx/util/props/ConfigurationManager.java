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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import edu.cmu.sphinx.util.SphinxLogFormatter;

/**
 * Manages the configuration for the system. The configuration manager provides
 * the following services:
 * 
 * <ul>
 * <li>Loads configuration data from an XML-based configuration file.
 * <li>Manages the component life-cycle for Configurable objects
 * <li>Allows discovery of components via name or type.
 * </ul>
 * <p>
 * For an overview of how to use this configuration management system to create
 * and configure components please see: <b><a
 * href="doc-files/ConfigurationManagement.html"> Sphinx-4 Configuration
 * Management </a> </b>
 * <p>
 * For a description of how to create your own configurable components see: <b>
 * {@link edu.cmu.sphinx.util.props.Configurable}</b>
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

    /**
     * A common property (used by all components) that sets the log level for
     * the component.
     * 
     * @see java.util.logging.Level
     */
    public final static String PROP_COMMON_LOG_LEVEL = "logLevel";
    
    /**
     * A common property (used by all components) that sets the tersness
     * of the log output
     * 
     */
    public final static String PROP_COMMON_LOG_TERSE = "logTerse";

    private Map symbolTable = new LinkedHashMap();
    private Map rawPropertyMap;
    private Map globalProperties = new LinkedHashMap();
    private boolean showCreations;

    // this pattern matches strings of the form '${word}'
    private static Pattern globalSymbolPattern = 
        Pattern.compile("\\$\\{(\\w+)\\}");

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
        configureLogger();

        // we can't config the configuration manager with itself so we
        // do some of these config items manually.

        showCreations = "true".equals(getMyGlobalProperty(PROP_SHOW_CREATIONS));
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
                    registerCommonProperties(registry);
                    configurable.register(name, registry);
                    ValidatingPropertySheet propertySheet = new ValidatingPropertySheet(
                            this, registry, rpd);
                    symbol = new Symbol(name, propertySheet, registry,
                            configurable);
                    symbolTable.put(name, symbol);
                    configurable.newProperties(propertySheet);

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
     * Sets the property of the given component to the given value.
     * Component must be an exisiting, instantiatied component
     *
     * @param component an existing component
     * @param prop the property name
     * @param value the new value.
     */
    public void setProperty(String component, String prop, String value) 
            throws PropertyException {
        Symbol symbol = (Symbol) symbolTable.get(component);
        if (symbol != null) {
            PropertySheet ps = symbol.getPropertySheet();
            Configurable c = symbol.getObject();
            Object old = ps.getRawNoReplacment(prop);
            ps.setRaw(prop, value);
            try {
                c.newProperties(ps);
            } catch (PropertyException pe) {
                // if the newProperties throws an objection
                // then we need to restore the previous value
                // roll back the old value if we can
                ps.setRaw(prop, old);
                c.newProperties(ps);
                throw pe;
            }
        } else {
            throw new PropertyException(null, prop,
                "Can't find component " + component);
        }
    }

    /**
     * Saves the current configuration to the given file
     * 
     * @param file
     *                place to save the configuration
     * @throws IOException
     *                 if an error occurs while writing to the file
     */
    public void save(File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        PrintWriter writer = new PrintWriter(fos);

        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println();
        outputHeader(0, writer, "Sphinx-4 Configuration File");
        writer.println();

        writer.println("<config>");

        // save global symbols

        outputHeader(2, writer, "Global Properties");
        for (Iterator i = globalProperties.keySet().iterator(); i.hasNext(); ) {
            String name = (String) i.next();
            String value = (String) globalProperties.get(name);
            writer.println("        <property name=\"" +
                stripGlobalSymbol(name) + "\" value=\"" + value + "\"/>");
        }
        writer.println();

        outputHeader(2, writer, "Components");

        String[] allNames = getInstanceNames(Object.class);
        for (int i = 0; i < allNames.length; i++) {
            Symbol symbol = (Symbol) symbolTable.get(allNames[i]);
            PropertySheet ps = symbol.getPropertySheet();
            String[] names = ps.getNames();

            outputHeader(4, writer, symbol.getName());

            writer.println("    <component name=\"" + symbol.getName() + "\"" +
                "\n          type=\"" + symbol.getObject().getClass().getName()
                + "\">");
            for (int j = 0; j < names.length; j++) {
                Object obj = ps.getRawNoReplacment(names[j]);
                if (obj instanceof String) {
                    String value = (String) obj;
                    String pad = (value.length() > 25) ? "\n        " : "" ;
                    writer.println("        <property name=\"" + names[j]
                            + "\"" + pad + " value=\"" + value + "\"/>");
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
            writer.println();
        }
        writer.println("</config>");
        writer.println("<!-- Generated on " + new Date() + "-->");
        writer.close();
    }

    /**
     * Outputs the pretty header for a component
     *
     * @param indent the indentation level
     * @param writer where to write the header
     * @param name the component name
     */
    private void outputHeader(int indent, PrintWriter writer, String name) 
            throws
        IOException {
        writer.println(pad(' ', indent) + "<!-- " + pad('*', 50) + " -->");
        writer.println(pad(' ', indent) + "<!-- " + name + pad(' ', 50 -
                    name.length()) + " -->");
        writer.println(pad(' ', indent) + "<!-- " + pad('*', 50) + " -->");
        writer.println();
    }
     

    /**
     * Loads the configuration data from the given url and adds the info to the
     * symbol table. Throws an IOexception if there is a problem loading the
     * table. Note that this only performs a partial populating of the symbol
     * table entry
     * 
     * @param url
     *                the url to load from
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
     * @param global
     *                global properies
     * 
     * @throws PropertyException
     *                 if an attempt is made to set a parameter for an unknown
     *                 component.
     */
    private void applySystemProperties(Map rawMap, Map global)
            throws PropertyException {
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
     * @param key
     *                the symbol name to lookup
     * @return the symbol value
     */
    String globalLookup(String key) {
        return (String) globalProperties.get(key);
    }

    /**
     * Gets the config properties for the configuration manager itself
     * 
     * @param key
     *                the name of the property
     * @return the property value or null if it doesn't exist.
     */
    String getMyGlobalProperty(String key) {
        return globalLookup("${" + key + "}");
    }

    /**
     * Registers the properties commont to all components
     * 
     * @param registry
     *                a component registry
     * @throws PropertyException
     *                 if an error occurs while registering the properties.
     */
    private void registerCommonProperties(Registry registry)
            throws PropertyException {
        registry.register(PROP_COMMON_LOG_LEVEL, PropertyType.STRING);
    }

    /**
     * Configure the logger
     *  
     */
    private void configureLogger() {
        LogManager logManager = LogManager.getLogManager();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Properties props = new Properties();
        props.setProperty(".edu.cmu.sphinx.level", "FINEST");
        props.setProperty("handlers", "java.util.logging.ConsoleHandler");
        props.setProperty("java.util.logging.ConsoleHandler.level", "FINEST");
        props.setProperty("java.util.logging.ConsoleHandler.formatter",
                "edu.cmu.sphinx.util.SphinxLogFormatter");

        try {
            props.store(bos, "");
            bos.close();
            ByteArrayInputStream bis = new ByteArrayInputStream(bos
                    .toByteArray());
            logManager.readConfiguration(bis);
            bis.close();
        } catch (IOException ioe) {
            System.err
                    .println("Can't configure logger, using default configuration");
        }
        // Now we find the SphinxLogFormatter that the log manager created
        // and configure it.
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            if (handlers[i] instanceof ConsoleHandler) {
                if (handlers[i].getFormatter() instanceof SphinxLogFormatter) {
                    SphinxLogFormatter slf = (SphinxLogFormatter) handlers[i]
                            .getFormatter();
                    slf.setTerse("true"
                           .equals(getMyGlobalProperty(PROP_COMMON_LOG_TERSE)));
                }
            }
        }
    }

    /**
     * Shows the current configuration
     *  
     */
    public void showConfig() {
        System.out.println(" ============ config ============= ");
        String[] allNames = getInstanceNames(Object.class);
        for (int i = 0; i < allNames.length; i++) {
            showConfig(allNames[i]);
        }
    }


    /**
     * Show the configuration for the compnent with the given name
     *
     * @param name the component name
     */
    public void showConfig(String name) {
        Symbol symbol = (Symbol) symbolTable.get(name);

        if (symbol == null)  {
            System.out.println("No component: " + name);
            return;
        }
        System.out.println(symbol.getName() + ":");

        Registry registry = symbol.getRegistry();
        Collection propertyNames = registry.getRegisteredProperties();
        PropertySheet properties = symbol.getPropertySheet();

        for (Iterator j = propertyNames.iterator(); j.hasNext();) {
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
                for (Iterator k = l.iterator(); k.hasNext();) {
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
    }

    public void editConfig(String name) {
        Symbol symbol = (Symbol) symbolTable.get(name);
        boolean done = false;

        if (symbol == null)  {
            System.out.println("No component: " + name);
            return;
        }
        System.out.println(symbol.getName() + ":");

        Registry registry = symbol.getRegistry();
        Collection propertyNames = registry.getRegisteredProperties();
        PropertySheet properties = symbol.getPropertySheet();
        BufferedReader br = new BufferedReader(new
                InputStreamReader(System.in));

        for (Iterator j = propertyNames.iterator(); j.hasNext();) {
            try {
                String propName = (String) j.next();
                Object value = properties.getRaw(propName);
                String svalue = null;

                if (value instanceof List) {
                    continue;
                } else if (value instanceof String) {
                    svalue = (String) value;
                } else {
                    svalue = "DEFAULT";
                }
                done = false;

                while (!done) {
                    System.out.print("  " + propName + " [" + svalue + "]: ");
                    String in = br.readLine();
                    if (in.length() == 0) {
                        done = true;
                    } else if (in.equals(".")) {
                        return;
                    } else {
                        try {
                            setProperty(name, propName, in);
                            done = true;
                        } catch  (PropertyException pe) {
                            System.out.println("error setting value " + pe);
                            svalue = in;
                        }
                    }
                }
            } catch (PropertyException pe) {
                System.out.println("error getting values " + pe);
            } catch (IOException ioe) {
                System.out.println("Trouble reading input");
                return;
            }
        }
    }

    // TODO: some experimental dumping functions, dump the config
    // as HTML and as GDL. These should probably be moved out to
    // the config monitor.

    // TODO this dumping code is not done yet.

    /**
     * Dumps the config as a set of HTML tables
     * 
     * @param path
     *                where to output the HTML
     * @throws IOException
     *                 if an error occurs
     */
    public void showConfigAsHTML(String path) throws IOException {
        PrintStream out = new PrintStream(new FileOutputStream(path));
        dumpHeader(out);
        String[] allNames = getInstanceNames(Object.class);
        for (int i = 0; i < allNames.length; i++) {
            dumpComponentAsHTML(out, allNames[i]);
        }
        dumpFooter(out);
        out.close();
    }

    /**
     * Dumps the header for HTML output
     * 
     * @param out
     *                the output stream
     */
    private void dumpHeader(PrintStream out) {
        out.println("<html><head>");
        out.println("    <title> Sphinx-4 Configuration</title");
        out.println("</head>");
        out.println("<body>");
    }

    /**
     * Retrieves the global log level
     * 
     * @return the global log level
     */
    String getGlobalLogLevel() {
        String level = getMyGlobalProperty(ConfigurationManager.PROP_COMMON_LOG_LEVEL);
        if (level == null) {
            level = Level.WARNING.getName();
        }
        return level;
    }

    /**
     * Dumps the footer for HTML output
     * 
     * @param out
     *                the output stream
     */
    private void dumpFooter(PrintStream out) {
        out.println("</body>");
        out.println("</html>");
    }

    String docBase = "http://cmusphinx.sourceforge.net/sphinx4/javadoc/";

    /**
     * Dumps the given component as HTML to the given stream
     * 
     * @param out
     *                where to dump the HTML
     * @param name
     *                the name of the component to dump
     */
    private void dumpComponentAsHTML(PrintStream out, String name) {
        Symbol symbol = (Symbol) symbolTable.get(name);
        out.println("<table border=1>");
        //        out.println("<table border=1 width=\"%80\">");
        out.print("    <tr><th bgcolor=\"#CCCCFF\" colspan=2>");
        //       out.print("<a href="")
        out.print(name);
        out.print("</a>");
        out.println("</td></tr>");

        out
                .println("    <tr><th bgcolor=\"#CCCCFF\">Property</th><th bgcolor=\"#CCCCFF\"> Value</th></tr>");
        Registry registry = symbol.getRegistry();
        Collection propertyNames = registry.getRegisteredProperties();
        PropertySheet properties = symbol.getPropertySheet();

        for (Iterator j = propertyNames.iterator(); j.hasNext();) {
            String propName = (String) j.next();
            out.print("    <tr><th align=\"leftt\">" + propName + "</th>");
            Object obj;
            try {
                obj = properties.getRaw(propName);
            } catch (PropertyException e) {
                // this exception can occcur if a global name
                // can't be resolved ...
                obj = "[Unresolved!]";
                out.println("<td>" + obj + "</td></tr>");
            }
            if (obj instanceof String) {
                out.println("<td>" + obj + "</td></tr>");
            } else if (obj instanceof List) {
                List l = (List) obj;
                out.println("    <td><ul>");
                for (Iterator k = l.iterator(); k.hasNext();) {
                    out.println("        <li>" + k.next() + "</li>");
                }
                out.println("    </ul></td>");
            } else {
                out.println("<td>DEFAULT</td></tr>");
            }
        }
        out.println("</table><br>");
    }

    /**
     * Dumps the config as a GDL plot
     * 
     * @param path
     *                where to output the GDL
     * @throws IOException
     *                 if an error occurs
     */
    public void showConfigAsGDL(String path) throws IOException {
        PrintStream out = new PrintStream(new FileOutputStream(path));
        dumpGDLHeader(out);
        String[] allNames = getInstanceNames(Object.class);
        for (int i = 0; i < allNames.length; i++) {
            dumpComponentAsGDL(out, allNames[i]);
        }
        dumpGDLFooter(out);
        out.close();
    }

    /**
     * Dumps the given component as GDL to the given stream
     * 
     * @param out
     *                where to dump the GDL
     * @param name
     *                the name of the component to dump
     */
    private void dumpComponentAsGDL(PrintStream out, String name) {

        out.println("node: {title: \"" + name + "\" color: " + getColor(name)
                + "}");

        Symbol symbol = (Symbol) symbolTable.get(name);
        Registry registry = symbol.getRegistry();
        Collection propertyNames = registry.getRegisteredProperties();
        PropertySheet properties = symbol.getPropertySheet();

        for (Iterator i = propertyNames.iterator(); i.hasNext();) {
            String propName = (String) i.next();
            PropertyType type = registry.lookup(propName);
            try {
                Object val = properties.getRaw(propName);
                if (val != null) {
                    if (type == PropertyType.COMPONENT) {
                        out.println("edge: {source: \"" + name
                                + "\" target: \"" + val + "\"}");
                    } else if (type == PropertyType.COMPONENT_LIST) {
                        List list = (List) val;
                        for (Iterator j = list.iterator(); j.hasNext();) {
                            Object dest = j.next();
                            out.println("edge: {source: \"" + name
                                    + "\" target: \"" + dest + "\"}");
                        }
                    }
                }
            } catch (PropertyException e) {
                // nothing to do , its up to you
            }
        }

    }

    /**
     * Gets the color for the given component
     * 
     * @param componentName
     *                the name of the component
     * @return the color name for the component
     */
    private String getColor(String componentName) {
        try {
            Configurable c = lookup(componentName);
            Class cls = c.getClass();
            if (cls.getName().indexOf(".recognizer") > 1) {
                return "cyan";
            } else if (cls.getName().indexOf(".tools") > 1) {
                return "darkcyan";
            } else if (cls.getName().indexOf(".decoder") > 1) {
                return "green";
            } else if (cls.getName().indexOf(".frontend") > 1) {
                return "orange";
            } else if (cls.getName().indexOf(".acoustic") > 1) {
                return "turquoise";
            } else if (cls.getName().indexOf(".linguist") > 1) {
                return "lightblue";
            } else if (cls.getName().indexOf(".instrumentation") > 1) {
                return "lightgrey";
            } else if (cls.getName().indexOf(".util") > 1) {
                return "lightgrey";
            }
        } catch (InstantiationException e) {
            return "black";
        } catch (PropertyException e) {
            return "black";
        }
        return "darkgrey";

    }

    /**
     * Outputs the GDL header
     * 
     * @param out
     *                the output stream
     */
    private void dumpGDLHeader(PrintStream out) {
        out.println(" graph: {title: \"unix evolution\" ");
        out.println("         layoutalgorithm: tree");
        out.println("          scaling        : 2.0");
        out.println("          colorentry 42  : 152 222 255");
        out.println("     node.shape     : ellipse");
        out.println("      node.color     : 42 ");
        out.println("node.height    : 32  ");
        out.println("node.fontname  : \"helvB08\"");
        out.println("edge.color     : darkred");
        out.println("edge.arrowsize :  6    ");
        out.println("node.textcolor : darkblue ");
        out.println("splines        : yes");
    }

    /**
     * Dumps the footer for GDL output
     * 
     * @param out
     *                the output stream
     */
    private void dumpGDLFooter(PrintStream out) {
        out.println("}");
    }

    /**
     * Strips the ${ and } off of a global symbol of the form
     * ${symbol}.
     *
     * @param symbol the symbol to strip
     *
     * @return the stripped symbol
     */
    private String stripGlobalSymbol(String symbol) {
        Matcher matcher = globalSymbolPattern.matcher(symbol);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return symbol;
        }
    }

    /**
     * Generate a string of the given character 
     *
     * @param c the character
     * @param count the length of the string
     *
     * @return the padded string
     */
    private String pad(char c, int count) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
