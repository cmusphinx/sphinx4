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
package edu.cmu.sphinx.instrumentation;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

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
    
    // -------------------------
    // Configuration data
    // -------------------------
    private String name;
    private boolean showConfig;
    private ConfigurationManager cm;

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String, edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        this.name = name;
        registry.register(PROP_SHOW_CONFIG, PropertyType.BOOLEAN);
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        cm = ps.getPropertyManager();
        showConfig = ps.getBoolean(PROP_SHOW_CONFIG, PROP_SHOW_CONFIG_DEFAULT);
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
    }

}
