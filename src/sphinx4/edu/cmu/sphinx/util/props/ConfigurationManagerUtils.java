package edu.cmu.sphinx.util.props;

import edu.cmu.sphinx.util.SphinxLogFormatter;

import java.io.*;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DOCUMENT ME!
 *
 * @author Holger Brandl
 */
public class ConfigurationManagerUtils {

    // this pattern matches strings of the form '${word}'
    private static Pattern globalSymbolPattern = Pattern.compile("\\$\\{(\\w+)\\}");
    /** A common property (used by all components) that sets the tersness of the log output */
    public final static String PROP_COMMON_LOG_TERSE = "logTerse";


    /**
     * Dumps the config as a set of HTML tables
     *
     * @param path where to output the HTML
     * @throws java.io.IOException if an error occurs
     */
    public static void showConfigAsHTML(ConfigurationManager ConfigurationManager, String path) throws IOException {
        PrintStream out = new PrintStream(new FileOutputStream(path));
        dumpHeader(out);
        String[] allNames = ConfigurationManager.getInstanceNames(Object.class);
        for (String componentName : allNames) {
            dumpComponentAsHTML(out, componentName, ConfigurationManager.getPropertySheet(componentName));
        }
        dumpFooter(out);
        out.close();
    }


    /**
     * Dumps the footer for HTML output
     *
     * @param out the output stream
     */
    private static void dumpFooter(PrintStream out) {
        out.println("</body>");
        out.println("</html>");
    }


    /**
     * Dumps the header for HTML output
     *
     * @param out the output stream
     */
    private static void dumpHeader(PrintStream out) {
        out.println("<html><head>");
        out.println("    <title> Sphinx-4 Configuration</title");
        out.println("</head>");
        out.println("<body>");
    }


    /**
     * Dumps the given component as HTML to the given stream
     *
     * @param out  where to dump the HTML
     * @param name the name of the component to dump
     */
    public static void dumpComponentAsHTML(PrintStream out, String name, PropertySheet properties) {
        out.println("<table border=1>");
        //        out.println("<table border=1 width=\"%80\">");
        out.print("    <tr><th bgcolor=\"#CCCCFF\" colspan=2>");
        //       out.print("<a href="")
        out.print(name);
        out.print("</a>");
        out.println("</td></tr>");

        out.println("    <tr><th bgcolor=\"#CCCCFF\">Property</th><th bgcolor=\"#CCCCFF\"> Value</th></tr>");
        Collection<String> propertyNames = properties.getRegisteredProperties();

        for (String propertyName : propertyNames) {
            out.print("    <tr><th align=\"leftt\">" + propertyName + "</th>");
            Object obj;
            obj = properties.getRaw(propertyName);
            if (obj instanceof String) {
                out.println("<td>" + obj + "</td></tr>");
            } else if (obj instanceof List) {
                List l = (List) obj;
                out.println("    <td><ul>");
                for (Object listElement : l) {
                    out.println("        <li>" + listElement + "</li>");
                }
                out.println("    </ul></td>");
            } else {
                out.println("<td>DEFAULT</td></tr>");
            }
        }
        out.println("</table><br>");
    }


    /**
     * Dumps the given component as GDL to the given stream
     *
     * @param out  where to dump the GDL
     * @param name the name of the component to dump
     */
    public static void dumpComponentAsGDL(ConfigurationManager ConfigurationManager, PrintStream out, String name) {

        out.println("node: {title: \"" + name + "\" color: " + getColor(ConfigurationManager, name)
                + "}");

        PropertySheet properties = ConfigurationManager.getPropertySheet(name);
        Collection<String> propertyNames = properties.getRegisteredProperties();

        for (String propertyName : propertyNames) {
            PropertyType type = null; //todo fixme
//            PropertyType type = registry.lookup(propertyName);
            Object val = properties.getRaw(propertyName);
            if (val != null) {
                if (type == PropertyType.COMPONENT) {
                    out.println("edge: {source: \"" + name
                            + "\" target: \"" + val + "\"}");
                } else if (type == PropertyType.COMPONENT_LIST) {
                    List list = (List) val;
                    for (Object listElement : list) {
                        out.println("edge: {source: \"" + name
                                + "\" target: \"" + listElement + "\"}");
                    }
                }
            }
        }
    }


    /**
     * Dumps the config as a GDL plot
     *
     * @param path where to output the GDL
     * @throws java.io.IOException if an error occurs
     */
    public static void showConfigAsGDL(ConfigurationManager ConfigurationManager, String path) throws IOException {
        PrintStream out = new PrintStream(new FileOutputStream(path));
        dumpGDLHeader(out);
        String[] allNames = ConfigurationManager.getInstanceNames(Object.class);
        for (String componentName : allNames) {
            dumpComponentAsGDL(ConfigurationManager, out, componentName);
        }
        dumpGDLFooter(out);
        out.close();
    }


    /**
     * Outputs the GDL header
     *
     * @param out the output stream
     */
    private static void dumpGDLHeader(PrintStream out) {
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
     * Strips the ${ and } off of a global symbol of the form ${symbol}.
     *
     * @param symbol the symbol to strip
     * @return the stripped symbol
     */
    public static String stripGlobalSymbol(String symbol) {
        Matcher matcher = globalSymbolPattern.matcher(symbol);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return symbol;
        }
    }


    /**
     * Gets the color for the given component
     *
     * @param ConfigurationManager
     * @param componentName        the name of the component @return the color name for the component
     */
    private static String getColor(ConfigurationManager ConfigurationManager, String componentName) {
        try {
            Configurable c = ConfigurationManager.lookup(componentName);
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
     * Dumps the footer for GDL output
     *
     * @param out the output stream
     */
    private static void dumpGDLFooter(PrintStream out) {
        out.println("}");
    }


    public static void editConfig(ConfigurationManager ConfigurationManager, String name) {
        PropertySheet ps = ConfigurationManager.getPropertySheet(name);
        boolean done;

        if (ps == null) {
            System.out.println("No component: " + name);
            return;
        }
        System.out.println(name + ":");

        Collection<String> propertyNames = ps.getRegisteredProperties();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        for (String propertyName : propertyNames) {
            try {
                Object value = ps.getRaw(propertyName);
                String svalue;

                if (value instanceof List) {
                    continue;
                } else if (value instanceof String) {
                    svalue = (String) value;
                } else {
                    svalue = "DEFAULT";
                }
                done = false;

                while (!done) {
                    System.out.print("  " + propertyName + " [" + svalue + "]: ");
                    String in = br.readLine();
                    if (in.length() == 0) {
                        done = true;
                    } else if (in.equals(".")) {
                        return;
                    } else {
                        try {
                            ConfigurationManager.setProperty(name, propertyName, in);
                            done = true;
                        } catch (PropertyException pe) {
                            System.out.println("error setting value " + pe);
                            svalue = in;
                        }
                    }
                }
            } catch (IOException ioe) {
                System.out.println("Trouble reading input");
                return;
            }
        }
    }


    /** Configure the logger */
    public static void configureLogger(ConfigurationManager cm) {
        // apply theb log level (if defined) for the root logger (because we're using package based logging now

        String logLevelName = cm.getGlobalProperty("logLevel");
        Level logLevel;
        if (logLevelName != null)
            logLevel = Level.parse(logLevelName);
        else
            logLevel = Level.WARNING;


        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(logLevel);

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
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            logManager.readConfiguration(bis);
            bis.close();
        } catch (IOException ioe) {
            System.err
                    .println("Can't configure logger, using default configuration");
        }

        String level = cm.getGlobalProperty("logLevel");
        if (level == null)
            level = Level.WARNING.getName();
        rootLogger.setLevel(Level.parse(level));

        // Now we find the SphinxLogFormatter that the log manager created
        // and configure it.
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            handler.setFormatter(new SphinxLogFormatter());
//            if (handler instanceof ConsoleHandler) {
//                if (handler.getFormatter() instanceof SphinxLogFormatter) {
//                    SphinxLogFormatter slf = (SphinxLogFormatter) handler.getFormatter();
//                    slf.setTerse("true".equals(level));
//                }
//            }
        }
    }


    /**
     * converts a configuration manager instance into a xml-string .
     * <p/>
     * Note: This methods will not instantiate configurables.
     */
    public static String toXML(ConfigurationManager cm) {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        sb.append("\n<!--    Sphinx-4 Configuration file--> \n\n");

        sb.append("<config>");

        Pattern pattern = Pattern.compile("\\$\\{(\\w+)\\}");

        Map<String, String> globalProps = cm.getGlobalProperties();
        for (String propName : globalProps.keySet()) {
            String propVal = globalProps.get(propName);

            Matcher matcher = pattern.matcher(propName);
            propName = matcher.matches() ? matcher.group(1) : propName;

            sb.append("\n\t<property name=\"" + propName + "\" value=\"" + propVal + "\"/>");
        }

        List<String> allInstances = cm.getComponentNames();
        for (String instanceName : allInstances)
            sb.append("\n\n").append(propSheet2XML(instanceName, cm.getPropertySheet(instanceName)));

        sb.append("</config>");
        return sb.toString();
    }


    private static String propSheet2XML(String instanceName, PropertySheet ps) {
        StringBuffer sb = new StringBuffer();
        sb.append("<component name=\"" + instanceName + "\" type=\"" + ps.getConfigurableClass().getName() + "\">");

        for (String propName : ps.getRegisteredProperties()) {
            String predec = "\n\t<property name=\"" + propName + "\" ";
            if (ps.getRawNoReplacement(propName) == null)
                continue;  // if the property was net defined within the xml-file

            switch (ps.getType(propName)) {

                case COMPLIST:
                    sb.append("\n\t<propertylist name=\"" + propName + "\">");
                    List<String> compNames = (List<String>) ps.getRawNoReplacement(propName);
                    for (String compName : compNames)
                        sb.append("\n\t\t<item>" + compName + "</item>");
                    sb.append("\n\t</propertylist>");
                    break;
                default:
                    sb.append(predec + "value=\"" + ps.getRawNoReplacement(propName) + "\"/>");
            }
        }

        sb.append("\n</component>\n\n");
        return sb.toString();
    }


    public static void save(ConfigurationManager cm, File cmLocation) {
        assert cm != null;
        try {
            PrintWriter pw = new PrintWriter(new FileOutputStream(cmLocation));
            String configXML = ConfigurationManagerUtils.toXML(cm);
            pw.print(configXML);
            pw.flush();
            pw.close();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
    }


    /** Shows the current configuration */
    public static void showConfig(ConfigurationManager cm) {
        System.out.println(" ============ config ============= ");
        String[] allNames = cm.getInstanceNames(Object.class);
        for (String allName : allNames) {
            showConfig(cm, allName);
        }
    }


    /**
     * Show the configuration for the compnent with the given name
     *
     * @param name the component name
     */
    public static void showConfig(ConfigurationManager cm, String name) {
//        Symbol symbol = cm.getsymbolTable.get(name);

        if (!Arrays.asList(cm.getComponentNames()).contains(name)) {
            System.out.println("No component: " + name);
            return;
        }
        System.out.println(name + ":");

        PropertySheet properties = cm.getPropertySheet(name);

        for (String propertyName : properties.getRegisteredProperties()) {
            System.out.print("    " + propertyName + " = ");
            Object obj;
            obj = properties.getRaw(propertyName);
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
}
