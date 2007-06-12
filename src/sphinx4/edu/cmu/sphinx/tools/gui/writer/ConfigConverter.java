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
import edu.cmu.sphinx.tools.gui.RawPropertyData;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.lang.StringBuffer;

/**
 * This is a helper class to convert from ConfigProperties to String format that's ready to be written to .config.XML
 * file
 *
 * @author Ariani
 */
public class ConfigConverter {

    /* this class uses Singleton pattern for the constructor */
    private ConfigConverter() {
    }


    private static class ConfigConverterHolder {

        private static ConfigConverter instance = new ConfigConverter();
    }


    /**
     * Get reference to the <code>ConfigConverter</code>
     *
     * @return <code>ConfigConverter</code>
     */
    public static ConfigConverter getInstance() {
        return ConfigConverterHolder.instance;
    }


    private final static String XML_HEADING = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
    private final static String XML_SUBHEAD = "\n" +
            "<!--    Sphinx-4 Configuration file--> \n" +
            "<!--  ******************************************************** -->\n\n";
    private final static String XML_ROOT = "<config>\n";
    private final static String XML_ROOT_CLOSE = "</config>\n";
    private final static String XML_GLOBAL = "\n" +
            "<!--  *******************************************-->\n" +
            "<!--  frequently tuned properties                -->\n" +
            "<!-- ********************************************-->\n";
    private final static String XML_CONF = "\n" +
            "<!--  ********************************************************-->\n" +
            "<!--  component       configuration                           -->\n" +
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
    private void writeComponent(StringBuffer sb, RawPropertyData rpd){
        String name = rpd.getName();
        String classname = rpd.getClassName();
        Map properties = rpd.getProperties();
        if (name != null && classname != null) // if the rpd is valid
        {
            sb = sb.append(XML_COMPONENT+name+XML_QUOTE+XML_TYPE+classname+XML_CLOSE_TAG);
            
            for(Iterator it = properties.entrySet().iterator(); it.hasNext();)
            {
                Map.Entry entry = (Map.Entry) it.next();            
                String propName = (String)entry.getKey();
                List propList;
                String propVal;

                if (propName != null && (entry.getValue() != null)) {
                    if (entry.getValue() instanceof String) {
                        propVal = (String) entry.getValue();
                        pw.print(XML_PROP + propName + XML_QUOTE + XML_VALUE + propVal + XML_CLOSE_ELEMENT);
                    } else // value is a list
                    {
                        propVal = (String)entry.getValue();
                        sb = sb.append(XML_PROP+propName+XML_QUOTE+XML_VALUE+propVal+XML_CLOSE_ELEMENT);
                    }
                    else // value is a list
                    {
                        sb = sb.append(XML_PROPLIST+propName+XML_CLOSE_TAG);
                        propList = (List)entry.getValue();
                        // iterate the propertyList
                        for (Iterator it2 = propList.iterator(); it2.hasNext();) {
                            String item = (String) it2.next();

                            if(item != null && !(item.trim().equalsIgnoreCase("")))
                            {
                                sb = sb.append(XML_ITEM+item+XML_ITEM_CLOSE);                
                            }

                        }
                        sb = sb.append(XML_PROPLIST_CLOSE);
                    }
                }
            }
            sb = sb.append(XML_COMPONENT_CLOSE);
        }
    }


    /**
     * Write the property name and values into a StringBuffer
     *
     * @param cp <code>ConfigProperties</code> that keeps the property name-values
     * @param sb <code>StringBuffer</code> to write the output
     */
    public StringBuffer writeOutput(ConfigProperties cp){
        
        StringBuffer sb = new StringBuffer();
        Map global = cp.getGlobal();
        Map property = cp.getProperty();
        
        sb = sb.append(XML_HEADING + XML_SUBHEAD + XML_ROOT + '\n');
        sb = sb.append(XML_GLOBAL + '\n');

        // iterate the global properties
        if( cp != null && sb != null)
        {
            if (global != null) {
                for (Iterator it= global.entrySet().iterator(); it.hasNext();)
                {
                    Map.Entry entry = (Map.Entry) it.next();            
                    String name = (String)entry.getKey();
                    if(name != null){                
                        String value = (String)entry.getValue();
                        sb = sb.append(XML_PROP+name+XML_QUOTE+XML_VALUE+value+XML_CLOSE_ELEMENT);                
                    }

                }
            }
            sb = sb.append(XML_CONF);
            
            if( property != null){
                for(Iterator it = property.entrySet().iterator(); it.hasNext();)
                {
                    Map.Entry entry = (Map.Entry)it.next();
                    String name = (String)entry.getKey();
                    RawPropertyData rpd = (RawPropertyData)entry.getValue();
                    if(name != null && rpd != null)
                    {
                        writeComponent(sb,rpd);
                    }
                }
            }
        }
        sb = sb.append(XML_ROOT_CLOSE + '\n');
        return sb;
    }
}
    
