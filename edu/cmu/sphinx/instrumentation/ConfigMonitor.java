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

import java.io.IOException;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import java.util.logging.Logger;
import java.io.File;

/**
 * Shows the configuration currently in use. This monitor is typically added
 * as a recognition monitor such that the configuration is shown immediately
 * after the recognizer is allocated.
 */
public class ConfigMonitor implements Configurable, Runnable {
    
    /**
     * Sphinx property that is used to indicate whether or not this
     * monitor should show the current configuration.
     */
    public final static String PROP_SHOW_CONFIG = "showConfig";
    
    /**
     * The default value for PROP_SHOW_CONFIG
     */
    public final static boolean PROP_SHOW_CONFIG_DEFAULT = false;

    /**
     * Sphinx property that is used to indicate whether or not this
     * monitor should dump the configuration in an HTML document
     */
    public final static String PROP_SHOW_CONFIG_AS_HTML=
        "showConfigAsHTML";
    
    /**
     * The default value for PROP_SHOW_CONFIG_AS_HTML_DEFAULT
     */
    public final static boolean PROP_SHOW_CONFIG_AS_HTML_DEFAULT = false;

    /**
     * Sphinx property that is used to indicate whether or not this
     * monitor should dump the configuration in an GDL document
     */
    public final static String PROP_SHOW_CONFIG_AS_GDL =
        "showConfigAsGDL";
    
    /**
     * The default value for PROP_SHOW_CONFIG_AS_GDL_DEFAULT
     */
    public final static boolean PROP_SHOW_CONFIG_AS_GDL_DEFAULT = false;

    /**
     * Sphinx property that is used to indicate whether or not this
     * monitor should save the configuration in an XML document
     */
    public final static String PROP_SAVE_CONFIG_AS_XML  =
        "saveConfigAsXML";
    
    /**
     * The default value for PROP_SAVE_CONFIG_AS_XML
     */
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
        showConfig = ps.getBoolean(PROP_SHOW_CONFIG, PROP_SHOW_CONFIG_DEFAULT);
        showHTML = ps.getBoolean(PROP_SHOW_CONFIG_AS_HTML,
                PROP_SHOW_CONFIG_AS_HTML_DEFAULT);
        showGDL = ps.getBoolean(PROP_SHOW_CONFIG_AS_GDL,
                PROP_SHOW_CONFIG_AS_GDL_DEFAULT);
        saveXML = ps.getBoolean(PROP_SAVE_CONFIG_AS_XML,
                PROP_SAVE_CONFIG_AS_XML_DEFAULT);
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
            cm.showConfig();
        }
        
        if (showHTML) {
            try {
                cm.showConfigAsHTML("foo.html");
            } catch (IOException e) {
                logger.warning("Can't open " + htmlPath + " " + e);
            }
        }
        
        if (showGDL) {
            try {
                cm.showConfigAsGDL(gdlPath);
            } catch (IOException e) {
                logger.warning("Can't open " + gdlPath + " " + e);
            }
        }

        if (saveXML) {
            try {
                cm.save(new File(xmlPath));
            } catch (IOException e) {
                logger.warning("Can't save " + xmlPath + " " + e);
            }
        }
    }

}
