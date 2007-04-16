package edu.cmu.sphinx.util.props.newconman;

import edu.cmu.sphinx.util.SphinxLogFormatter;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertyType;

import java.io.*;
import java.util.*;
import java.util.logging.*;
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
     * Outputs the pretty header for a component
     *
     * @param indent the indentation level
     * @param writer where to write the header
     * @param name   the component name
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
     * Generate a string of the given character
     *
     * @param c     the character
     * @param count the length of the string
     * @return the padded string
     */
    private String pad(char c, int count) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }


    /**
     * Saves the current configuration to the given file
     *
     * @param file place to save the configuration
     * @throws java.io.IOException if an error occurs while writing to the file
     */
    public void save(ConMan conMan, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        PrintWriter writer = new PrintWriter(fos);

        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println();
        outputHeader(0, writer, "Sphinx-4 Configuration File");
        writer.println();

        writer.println("<config>");

        // save global symbols
        Map<String, String> globalProperties = conMan.getGlobalProperties();

        outputHeader(2, writer, "Global Properties");
        for (String globalProperty : globalProperties.keySet()) {
            String value = globalProperties.get(globalProperty);
            value = encodeValue(value);
            writer.println("        <property name=\"" +
                    stripGlobalSymbol(globalProperty) + "\" value=\"" + value + "\"/>");
        }
        writer.println();

        outputHeader(2, writer, "Components");

        String[] allNames = conMan.getInstanceNames(Object.class);
        for (String componentName : allNames) {
            PropSheet ps = conMan.getPropertySheet(componentName);
            String[] names = ps.getNames();

            outputHeader(4, writer, componentName);

            try {
                writer.println("    <component name=\"" + componentName + "\"" +
                        "\n          type=\"" + ps.getOwner().getClass().getName()
                        + "\">");
            } catch (InstantiationException e) {
                e.printStackTrace();
            }

            for (String propertyName : names) {
                Object obj = ps.getRawNoReplacement(propertyName);
                if (obj instanceof String) {
                    String value = (String) obj;
                    value = encodeValue(value);
                    String pad = (value.length() > 25) ? "\n        " : "";
                    writer.println("        <property name=\"" + propertyName
                            + "\"" + pad + " value=\"" + value + "\"/>");
                } else if (obj instanceof List) {
                    List list = (List) obj;
                    writer.println("        <propertylist name=\"" + propertyName
                            + "\">");
                    for (Object listElement : list) {
                        writer.println("            <item>" +
                                encodeValue(listElement.toString()) + "</item>");
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
     * Encodes a value so that it is suitable for an xml property
     *
     * @param value the value to be encoded
     * @return the encoded value
     */
    private String encodeValue(String value) {
        value = value.replaceAll("<", "&lt;");
        value = value.replaceAll(">", "&gt;");
        return value;
    }


    /**
     * Dumps the config as a set of HTML tables
     *
     * @param path where to output the HTML
     * @throws java.io.IOException if an error occurs
     */
    public static void showConfigAsHTML(ConMan conMan, String path) throws IOException {
        PrintStream out = new PrintStream(new FileOutputStream(path));
        dumpHeader(out);
        String[] allNames = conMan.getInstanceNames(Object.class);
        for (String componentName : allNames) {
            dumpComponentAsHTML(out, componentName, conMan.getPropertySheet(componentName));
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
    public static void dumpComponentAsHTML(PrintStream out, String name, PropSheet properties) {
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
    public void dumpComponentAsGDL(ConMan conMan, PrintStream out, String name) {

        out.println("node: {title: \"" + name + "\" color: " + getColor(conMan, name)
                + "}");

        PropSheet properties = conMan.getPropertySheet(name);
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
    public void showConfigAsGDL(ConMan conMan, String path) throws IOException {
        PrintStream out = new PrintStream(new FileOutputStream(path));
        dumpGDLHeader(out);
        String[] allNames = conMan.getInstanceNames(Object.class);
        for (String componentName : allNames) {
            dumpComponentAsGDL(conMan, out, componentName);
        }
        dumpGDLFooter(out);
        out.close();
    }


    /**
     * Outputs the GDL header
     *
     * @param out the output stream
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
     * Strips the ${ and } off of a global symbol of the form ${symbol}.
     *
     * @param symbol the symbol to strip
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
     * Gets the color for the given component
     *
     * @param conMan
     * @param componentName the name of the component @return the color name for the component
     */
    private String getColor(ConMan conMan, String componentName) {
        try {
            SimpleConfigurable c = conMan.lookup(componentName);
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
    private void dumpGDLFooter(PrintStream out) {
        out.println("}");
    }


    /** Shows the current configuration */
    public void showConfig(ConMan conMan) {
        System.out.println(" ============ config ============= ");
        String[] allNames = conMan.getInstanceNames(Object.class);
        for (String allName : allNames) {
            showConfig(conMan, allName);
        }
    }


    /**
     * Show the configuration for the compnent with the given name
     *
     * @param name the component name
     */
    public void showConfig(ConMan conMan, String name) {
        PropSheet ps = conMan.getPropertySheet(name);
        if (ps == null) {
            System.out.println("No component: " + name);
            return;
        }
        System.out.println(name + ":");


        Collection<String> propertyNames = ps.getRegisteredProperties();

        for (String propertyName : propertyNames) {
            System.out.print("    " + propertyName + " = ");
            Object obj;
            obj = ps.getRaw(propertyName);
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


    public void editConfig(ConMan conMan, String name) {
        PropSheet ps = conMan.getPropertySheet(name);
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
                            conMan.setProperty(name, propertyName, in);
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
    public static void configureLogger(ConMan conMan) {
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
        for (Handler handler : handlers) {
            if (handler instanceof ConsoleHandler) {
                if (handler.getFormatter() instanceof SphinxLogFormatter) {
                    SphinxLogFormatter slf = (SphinxLogFormatter) handler.getFormatter();

                    String level = conMan.getGlobalProperties().get("logLevel");
                    if (level == null) {
                        level = Level.WARNING.getName();
                    }

                    slf.setTerse("true".equals(level));
                }
            }
        }
    }


    /**
     * converts a configuration manager instance into a xml-string .
     * <p/>
     * Note: This methods will not instantiate configurables.
     */
    public static String toXML(ConMan cm) {
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


    private static String propSheet2XML(String instanceName, PropSheet ps) {
        StringBuffer sb = new StringBuffer();
        sb.append("<component name=\"" + instanceName + "\" type=\"" + ps.getConfigurableClass().getName() + "\">");

        for (String propName : ps.getRegisteredProperties()) {
            String predec = "\n\t<property name=\"" + propName + "\" ";
            if (ps.getRaw(propName) == null)
                continue;

            switch (ps.getType(propName)) {

                case COMPLIST:
                    sb.append("\n\t<propertylist name=\"" + propName + "\">");
                    List<String> compNames = (List<String>) ps.getRaw(propName);
                    for (String compName : compNames)
                        sb.append("\n\t\t<item>" + compName + "</item>");
                    sb.append("\n\t</propertylist>");
                    break;
                default:
                    sb.append(predec + "value=\"" + ps.getRaw(propName) + "\"/>");
            }
        }

        sb.append("\n</component>\n\n");
        return sb.toString();
    }


    public static void toConfigFile(ConMan cm, File cmLocation) {
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
}
