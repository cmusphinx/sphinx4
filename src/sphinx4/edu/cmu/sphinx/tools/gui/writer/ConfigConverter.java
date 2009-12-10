/*
 * ConfigConverter.java
 *
 * Created on November 6, 2006, 9:30 PM
 *
 * Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui.writer;

import edu.cmu.sphinx.tools.gui.ConfigProperties;
import edu.cmu.sphinx.util.props.RawPropertyData;

import java.util.Map;
import java.util.List;

/**
 * This is a helper class to convert from ConfigProperties to String format 
 * that's ready to be written to .config.XML file
 *
 * @author Ariani
 */
public class ConfigConverter {
    
    /* this class uses Singleton pattern for the constructor */
    private ConfigConverter() {}
    
    private static class ConfigConverterHolder {
      private static final ConfigConverter instance = new ConfigConverter();
    }
      
    /**
     * Get reference to the <code>ConfigConverter</code> 
     * 
     * @return <code>ConfigConverter</code>
     */
    public static ConfigConverter getInstance(){
      return ConfigConverterHolder.instance;
    }
    
    private final static String XML_HEADING = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
    private final static String XML_SUBHEAD = '\n' +
        "<!--    Sphinx-4 Configuration file--> \n" +
        "<!--  ******************************************************** -->\n\n";
    private final static String XML_ROOT = "<config>\n";
    private final static String XML_ROOT_CLOSE = "</config>\n";
    private final static String XML_GLOBAL = '\n' +
        "<!--  *******************************************-->\n"+ 
        "<!--  frequently tuned properties                -->\n" +
        "<!-- ********************************************-->\n"; 
    private final static String XML_CONF = '\n' +
     "<!--  ********************************************************-->\n"+
     "<!--  component       configuration                           -->\n"+
     "<!--  ********************************************************-->\n\n"; 

    private final static String XML_PROP = "\t <property name=\"";
    private final static String XML_COMPONENT = "<component name=\"";
    private final static String XML_COMPONENT_CLOSE = "</component>\n\n";
    private final static String XML_TYPE = " type=\"";
    private final static String XML_VALUE = " value=\"";
    private final static String XML_PROPLIST = "\t <propertylist name=\"";
    private final static String XML_PROPLIST_CLOSE = "\t </propertylist>\n";
    private final static String XML_ITEM = "\t\t <item>";
    private final static String XML_ITEM_CLOSE = "</item>\n";
    
    private final static String XML_CLOSE_ELEMENT = "\" /> \n";
    private final static String XML_CLOSE_TAG = "\" > \n";
    private final static String XML_QUOTE = "\"";
            
    // a helper method to write the information from a single RawPropertyData
    private void writeComponent(StringBuilder sb, RawPropertyData rpd) {
        String name = rpd.getName();
        String classname = rpd.getClassName();
        Map<String, Object> properties = rpd.getProperties();
        if (name == null || classname == null) {
            return;
        }
        sb.append(XML_COMPONENT).append(name).append(XML_QUOTE + XML_TYPE)
                .append(classname).append(XML_CLOSE_TAG);

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String propName = entry.getKey();
            Object propVal = entry.getValue();
            if (propName == null && propVal == null) {
                continue;
            }
            if (propVal instanceof String) {
                sb.append(XML_PROP).append(propName).append(
                        XML_QUOTE + XML_VALUE).append(propVal).append(
                        XML_CLOSE_ELEMENT);
            } else if (propVal instanceof List<?>) {
                sb.append(XML_PROPLIST).append(propName).append(XML_CLOSE_TAG);
                // iterate the propertyList
                for (Object obj : (List<?>) propVal) {
                    if (obj instanceof String) {
                        String item = (String) obj;
                        if (item != null && !item.trim().isEmpty()) {
                            sb.append(XML_ITEM).append(item).append(
                                    XML_ITEM_CLOSE);
                        }
                    }
                }
                sb.append(XML_PROPLIST_CLOSE);
            }
        }
        sb.append(XML_COMPONENT_CLOSE);
    }
    
    /**
     * Write the property name and values into a StringBuilder
     * 
     * @param cp <code>ConfigProperties</code> that keeps the property name-values
     * @return StringBuilder to write the output
     */
    public StringBuilder writeOutput(ConfigProperties cp) {

        StringBuilder sb = new StringBuilder();
        Map<String, String> global = cp.getGlobal();
        Map<String, RawPropertyData> property = cp.getProperty();

        sb.append(XML_HEADING + XML_SUBHEAD + XML_ROOT + '\n');
        sb.append(XML_GLOBAL + '\n');
        
        // iterate the global properties
        if (global != null) {
            for (Map.Entry<String, String> entry : global.entrySet()) {
                String name = entry.getKey();
                if (name != null) {
                    String value = entry.getValue();
                    sb.append(XML_PROP).append(name).append(XML_QUOTE + XML_VALUE).append(value).append(XML_CLOSE_ELEMENT);
                }

            }
        }
        sb.append(XML_CONF);

        if (property != null) {
            for (Map.Entry<String, RawPropertyData> entry : property.entrySet()) {
                String name = entry.getKey();
                RawPropertyData rpd = entry.getValue();
                if (name != null && rpd != null) {
                    writeComponent(sb, rpd);
                }
            }
        }
        sb.append(XML_ROOT_CLOSE + '\n');
        return sb;
    }
}
    
