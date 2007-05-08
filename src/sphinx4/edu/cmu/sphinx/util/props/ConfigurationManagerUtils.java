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
 * Some static utitity methods which ease the handling of system configurations.
 *
 * @author Holger Brandl
 */
public class ConfigurationManagerUtils {

    // this pattern matches strings of the form '${word}'
    private static Pattern globalSymbolPattern = Pattern.compile("\\$\\{(\\w+)\\}");

    /** A common property (used by all components) that sets the tersness of the log output */
    public final static String PROP_COMMON_LOG_TERSE = "logTerse";


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


    public static void editConfig(ConfigurationManager cm, String name) {
        PropertySheet ps = cm.getPropertySheet(name);
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

                        cm.getPropertySheet(name).setRaw(propertyName, in);
                        done = true;
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

        Collection<String> allInstances = cm.getComponentNames();
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
        for (String allName : cm.getInstanceNames(Configurable.class)) {
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

        if (!cm.getComponentNames().contains(name)) {
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


    public static boolean isImplementingInterface(Class aClass, Class interfaceClass) {
        Class<?> superClass = aClass.getSuperclass();
        if (superClass != null && isImplementingInterface(superClass, interfaceClass))
            return true;

        for (Class curInterface : aClass.getInterfaces()) {
            if (curInterface.equals(interfaceClass) || isImplementingInterface(curInterface, interfaceClass))
                return true;
        }

        return false;
    }


    /**
     * Applies the system properties to the raw property map. System properties should be of the form
     * compName[paramName]=paramValue
     * <p/>
     * List types cannot currently be set from system properties.
     *
     * @param rawMap the map of raw property values
     * @param global global properies
     * @throws PropertyException if an attempt is made to set a parameter for an unknown component.
     */
    static void applySystemProperties(Map<String, RawPropertyData> rawMap, Map<String, String> global)
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
                RawPropertyData rpd = rawMap.get(compName);
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
}
