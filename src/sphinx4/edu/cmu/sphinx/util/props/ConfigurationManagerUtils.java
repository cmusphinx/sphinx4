package edu.cmu.sphinx.util.props;

import edu.cmu.sphinx.util.SphinxLogFormatter;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
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
public final class ConfigurationManagerUtils {

    // this pattern matches strings of the form '${word}'
    private static final Pattern globalSymbolPattern = Pattern.compile("\\$\\{(\\w+)\\}");

    /** A common property (used by all components) that sets the log level for the component. */
    public final static String GLOBAL_COMMON_LOGLEVEL = "logLevel";

    /** The default file suffix of configuration files. */
    public static final String CM_FILE_SUFFIX = ".sxl";


    // disabled constructor because the class is just a collection of utitilites for handling system configurations
    private ConfigurationManagerUtils() {
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

        Logger rootLogger = Logger.getLogger("");

        // we need to determine the root-level here, beacuse the logManager will reset it
        Level rootLevel = rootLogger.getLevel();

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
            System.err.println("Can't configure logger, using default configuration");
        }

        String level = cm.getGlobalProperty(GLOBAL_COMMON_LOGLEVEL);
        if (level == null)
            level = Level.WARNING.getName();

        Level userLevel = Level.parse(level);
        Level newLevel = userLevel.intValue() < rootLevel.intValue() ? userLevel : rootLevel;

        rootLogger.setLevel(newLevel);

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
     * This method will automatically rename all components of <code>subCM</code> for which there is component named the
     * same in the <code>baseCM</code> .
     * <p/>
     * Note: This is ie. required when merging two system configurations into one.
     *
     * @return A map which maps all renamed component names to their new names.
     */
    public static Map<String, String> fixDuplicateNames(ConfigurationManager baseCM, ConfigurationManager subCM) {
        Map<String, String> renames = new HashMap<String, String>();

        for (String compName : subCM.getComponentNames()) {
            String uniqueName = compName;

            int i = 0;

            while (baseCM.getComponentNames().contains(uniqueName) ||
                    (subCM.getComponentNames().contains(uniqueName) && !uniqueName.equals(compName))) {

                i++;
                uniqueName = compName + i;
            }

            subCM.renameConfigurable(compName, uniqueName);
            renames.put(compName, uniqueName);
        }

        return renames;
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

        GlobalProperties globalProps = cm.getGlobalProperties();
        for (String propName : globalProps.keySet()) {
            String propVal = globalProps.get(propName).toString();

            Matcher matcher = pattern.matcher(propName);
            propName = matcher.matches() ? matcher.group(1) : propName;

            sb.append("\n\t<property name=\"" + propName + "\" value=\"" + propVal + "\"/>");
        }

        Collection<String> allInstances = cm.getComponentNames();
        for (String instanceName : allInstances)
            sb.append("\n\n").append(propSheet2XML(instanceName, cm.getPropertySheet(instanceName)));

        sb.append("\n</config>");
        return sb.toString();
    }


    private static String propSheet2XML(String instanceName, PropertySheet ps) {
        StringBuffer sb = new StringBuffer();
        sb.append("\t<component name=\"" + instanceName + "\" type=\"" + ps.getConfigurableClass().getName() + "\">");

        for (String propName : ps.getRegisteredProperties()) {
            String predec = "\n\t\t<property name=\"" + propName + "\" ";
            if (ps.getRawNoReplacement(propName) == null)
                continue;  // if the property was net defined within the xml-file

            switch (ps.getType(propName)) {

                case COMPLIST:
                    sb.append("\n\t\t<propertylist name=\"" + propName + "\">");
                    List<String> compNames = (List<String>) ps.getRawNoReplacement(propName);
                    for (String compName : compNames)
                        sb.append("\n\t\t\t<item>" + compName + "</item>");
                    sb.append("\n\t\t</propertylist>");
                    break;
                default:
                    sb.append(predec + "value=\"" + ps.getRawNoReplacement(propName) + "\"/>");
            }
        }

        sb.append("\n\t</component>\n\n");
        return sb.toString();
    }


    public static void save(ConfigurationManager cm, File cmLocation) {
        if (!cmLocation.getName().endsWith(CM_FILE_SUFFIX))
            System.err.println("WARNING: Serialized s4-configuration should have the suffix '" + CM_FILE_SUFFIX + "'");

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
    static void applySystemProperties(Map<String, RawPropertyData> rawMap, GlobalProperties global)
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
                    throw new InternalConfigurationException(compName, param,
                            "System property attempting to set parameter "
                                    + " for unknown component " + compName
                                    + " (" + param + ")");
                }
            }

            // look for params of the form foo=fum
            // these go in the global map

            else if (param.indexOf('.') == -1) {
//                String symbolName = "${" + param + "}";
                global.setValue(param, value);
            }
        }
    }


    /**
     * Renames a given <code>Configurable</code>. The configurable component named <code>oldName</code> is assumed to be
     * registered to the CM. Renaming does not only affect the configurable itself but possibly global property values
     * and properties of other components.
     */
    static void renameComponent(ConfigurationManager cm, String oldName, String newName) {
        assert cm != null;
        assert oldName != null && newName != null;
        if (cm.getPropertySheet(oldName) == null) {
            throw new RuntimeException("no configurable (to be renamed) named " + oldName + " is contained in the CM");
        }

        // this iteration is a little hacky. It would be much better to maintain the links to a configurable in a special table
        for (String instanceName : cm.getComponentNames()) {
            PropertySheet propSheet = cm.getPropertySheet(instanceName);

            for (String propName : propSheet.getRegisteredProperties()) {
                if (propSheet.getRawNoReplacement(propName) == null)
                    continue;  // if the property was net defined within the xml-file

                switch (propSheet.getType(propName)) {

                    case COMPLIST:
                        List<String> compNames = (List<String>) propSheet.getRawNoReplacement(propName);
                        for (int i = 0; i < compNames.size(); i++) {
                            String compName = compNames.get(i);
                            if (compName.equals(oldName)) {
                                compNames.set(i, newName);
                            }
                        }

                        break;
                    case COMP:
                        if (propSheet.getRawNoReplacement(propName).equals(oldName)) {
                            propSheet.setRaw(propName, newName);
                        }
                }
            }
        }

        PropertySheet ps = cm.getPropertySheet(oldName);
        ps.setInstanceName(newName);

        // it might be possible that the component is the value of a global property
        GlobalProperties globalProps = cm.getGlobalProperties();
        for (String propName : globalProps.keySet()) {
            String propVal = globalProps.get(propName).toString();

            if (propVal.equals(oldName))
                cm.setGlobalProperty(propName, newName);
        }
    }


    /**
     * Gets a resource associated with the given parameter name given an property sheet.
     *
     * @param name the parameter name
     * @param ps   The property sheet which contains the property
     * @return the resource associated with the name or NULL if it doesn't exist.
     * @throws PropertyException if the resource cannot be found
     */
    public static URL getResource(String name, PropertySheet ps) throws PropertyException {
        URL url;
        String location = ps.getString(name);
        if (location == null) {
            throw new InternalConfigurationException(name, name, "Required resource property '" + name + "' not set");
        }

        Matcher jarMatcher = Pattern.compile("resource:/([.\\w]+?)!(.*)", Pattern.CASE_INSENSITIVE).matcher(location);
        if (jarMatcher.matches()) {
            String className = jarMatcher.group(1);
            String resourceName = jarMatcher.group(2);

            try {
                Class cls = Class.forName(className);
                url = cls.getResource(resourceName);
                if (url == null) {
                    // getResource doesn't usually find directories
                    // If the resource is a directory and we
                    // can't find it, we will instead try to  find the class
                    // anchor and backup to the top level and try again
                    String classPath = className.replaceAll("\\.", "/") + ".class";
                    url = cls.getClassLoader().getResource(classPath);
                    if (url != null) {
                        // we should have something like this, so replace everything
                        // jar:file:/foo.jar!/a/b/c/HelloWorld.class
                        // after the ! with the resource name
                        String urlString = url.toString();
                        urlString = urlString.replaceAll("/" + classPath, resourceName);
                        try {
                            url = new URL(urlString);
                        } catch (MalformedURLException mfe) {
                            throw new InternalConfigurationException(mfe, name, name, "Bad URL " + urlString + mfe.getMessage());
                        }
                    }
                }
                if (url == null) {
                    throw new InternalConfigurationException(name, name, "Can't locate resource " + resourceName);
                } else {
                    // System.out.println("URL FOUND " + url);
                }
            } catch (ClassNotFoundException cnfe) {
                throw new InternalConfigurationException(cnfe, name, name, "Can't locate resource:/" + className);
            }
        } else {
            if (location.indexOf(":") == -1) {
                location = "file:" + location;
            }

            try {
                url = new URL(location);
            } catch (MalformedURLException e) {
                throw new InternalConfigurationException(e, name, name, "Bad URL " + location + e.getMessage());
            }
        }
        return url;
    }


    /**
     * @return <code>true</code> if <code>aClass</code> is either equal to <code>poosibleParent</code>, a subclass of
     *         it, or implementing if <code>possibl\</code> is an interface.
     */
    public static boolean isDerivedClass(Class aClass, Class possibleParent) {
        return aClass.equals(possibleParent)
                || (possibleParent.isInterface() && ConfigurationManagerUtils.isImplementingInterface(aClass, possibleParent))
                || ConfigurationManagerUtils.isSubClass(aClass, possibleParent);
    }


    public static boolean isImplementingInterface(Class aClass, Class interfaceClass) {
        assert interfaceClass.isInterface();

        Class<?> superClass = aClass.getSuperclass();
        if (superClass != null && isImplementingInterface(superClass, interfaceClass))
            return true;

        for (Class curInterface : aClass.getInterfaces()) {
            if (curInterface.equals(interfaceClass) || isImplementingInterface(curInterface, interfaceClass))
                return true;
        }

        return false;
    }


    public static boolean isSubClass(Class aClass, Class possibleSuperclass) {
        while (aClass != null && !aClass.equals(Object.class)) {
            aClass = aClass.getSuperclass();

            if (aClass != null && aClass.equals(possibleSuperclass))
                return true;
        }

        return false;
    }


    /**
     * Why do we need this method? The reason is, that we would like to avoid this method ot be part of the
     * <code>PropertySheet</code>-API. In some circumstances it is nevertheless required to get access to the managing
     * <code>ConfigurationManager</code>.
     */
    public static ConfigurationManager getPropertyManager(PropertySheet ps) {
        return ps.getPropertyManager();
    }
}
