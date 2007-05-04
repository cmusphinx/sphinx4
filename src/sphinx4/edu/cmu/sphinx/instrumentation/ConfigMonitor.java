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
package edu.cmu.sphinx.instrumentation;


import edu.cmu.sphinx.util.props.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Shows the configuration currently in use. This monitor is typically added as a recognition monitor such that the
 * configuration is shown immediately after the recognizer is allocated.
 */
public class ConfigMonitor implements Configurable, Runnable, Monitor {

    /** Sphinx property that is used to indicate whether or not this monitor should show the current configuration. */
    public final static String PROP_SHOW_CONFIG = "showConfig";

    /** The default value for PROP_SHOW_CONFIG */
    public final static boolean PROP_SHOW_CONFIG_DEFAULT = false;

    /**
     * Sphinx property that is used to indicate whether or not this monitor should dump the configuration in an HTML
     * document
     */
    @S4Boolean(defaultValue = false)
    public final static String PROP_SHOW_CONFIG_AS_HTML = "showConfigAsHTML";

    /** The default value for PROP_SHOW_CONFIG_AS_HTML_DEFAULT */
    public final static boolean PROP_SHOW_CONFIG_AS_HTML_DEFAULT = false;

    /**
     * Sphinx property that is used to indicate whether or not this monitor should dump the configuration in an GDL
     * document
     */
    @S4Boolean(defaultValue = false)
    public final static String PROP_SHOW_CONFIG_AS_GDL = "showConfigAsGDL";

    /** The default value for PROP_SHOW_CONFIG_AS_GDL_DEFAULT */
    public final static boolean PROP_SHOW_CONFIG_AS_GDL_DEFAULT = false;

    /**
     * Sphinx property that is used to indicate whether or not this monitor should save the configuration in an XML
     * document
     */
    @S4Boolean(defaultValue = false)
    public final static String PROP_SAVE_CONFIG_AS_XML = "saveConfigAsXML";

    /** The default value for PROP_SAVE_CONFIG_AS_XML */
    public final static boolean PROP_SAVE_CONFIG_AS_XML_DEFAULT = false;

    // -------------------------
    // Configuration data
    // -------------------------
    private String name;
    private boolean showConfig;
    private boolean showHTML = true;
    private boolean saveXML = false;
    private boolean showGDL = true;
    private Logger logger;
    private ConfigurationManager cm;
    private String htmlPath = "config.html";
    private String gdlPath = "config.gdl";
    private String xmlPath = "config.xml";


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#getConfigurationInfo()
    */
    public static Map getConfigurationInfo() {
        Map info = new HashMap();
        info.put(new String("PROP_SHOW_CONFIG_TYPE"), new String("BOOLEAN"));
        info.put(new String("PROP_SHOW_CONFIG_AS_HTML_TYPE"), new String("BOOLEAN"));
        info.put(new String("PROP_SHOW_CONFIG_AS_GDL_TYPE"), new String("BOOLEAN"));
        info.put(new String("PROP_SAVE_CONFIG_AS_XML_TYPE"), new String("BOOLEAN"));
        return info;
    }


    /* (non-Javadoc)
    * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String, edu.cmu.sphinx.util.props.Registry)
    */
    public void register(String name, Registry registry)
            throws PropertyException {
        this.name = name;
        registry.register(PROP_SHOW_CONFIG, PropertyType.BOOLEAN);
        registry.register(PROP_SHOW_CONFIG_AS_HTML, PropertyType.BOOLEAN);
        registry.register(PROP_SHOW_CONFIG_AS_GDL, PropertyType.BOOLEAN);
        registry.register(PROP_SAVE_CONFIG_AS_XML, PropertyType.BOOLEAN);
    }


    /* (non-Javadoc)
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    public void newProperties(PropertySheet ps) throws PropertyException {
        logger = ps.getLogger();
        cm = ps.getPropertyManager();
        showConfig = ps.getBoolean(PROP_SHOW_CONFIG);
        showHTML = ps.getBoolean(PROP_SHOW_CONFIG_AS_HTML
        );
        showGDL = ps.getBoolean(PROP_SHOW_CONFIG_AS_GDL
        );
        saveXML = ps.getBoolean(PROP_SAVE_CONFIG_AS_XML
        );
    }


    /* (non-Javadoc)
    * @see edu.cmu.sphinx.util.props.Configurable#getName()
    */
    public String getName() {
        return name;
    }


    /* (non-Javadoc)
    * @see java.lang.Runnable#run()
    */
    public void run() {
        if (showConfig) {
            ConfigurationManagerUtils.showConfig(cm);
//            cm.showConfig();
        }

        if (showHTML) {
            try {
                ConfigurationManagerUtils.showConfigAsHTML(cm, "foo.html");
//                cm.showConfigAsHTML("foo.html");
            } catch (IOException e) {
                logger.warning("Can't open " + htmlPath + " " + e);
            }
        }

        if (showGDL) {
            try {
                ConfigurationManagerUtils.showConfigAsGDL(cm, gdlPath);
//                cm.showConfigAsGDL(gdlPath);
            } catch (IOException e) {
                logger.warning("Can't open " + gdlPath + " " + e);
            }
        }

        if (saveXML) {
            ConfigurationManagerUtils.save(cm, new File(xmlPath));
//                cm.save(new File(xmlPath));
        }
    }

}
