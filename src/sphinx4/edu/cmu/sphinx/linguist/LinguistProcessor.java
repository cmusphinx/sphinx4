/*
 * Copyright 1999-2003 Carnegie Mellon University.  
 * Portions Copyright 2002-2003 Sun Microsystems, Inc.  
 * Portions Copyright 2002-2003 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.linguist;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * A standard interface for a linguist processor
 *
 */
public class LinguistProcessor implements Configurable, Runnable {

    /**
     * The sphinx property that defines the name of the linguist to process
     */
    
    public final static String PROP_LINGUIST = "linguist";

    
    // ----------------------------
    // Configuration data
    // ----------------------------
    private String name;
    private Linguist linguist;
    
    
    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String, edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry) throws PropertyException {
        registry.register(PROP_LINGUIST, PropertyType.COMPONENT);
        this.name = name;
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        linguist = (Linguist) ps.getComponent(PROP_LINGUIST, Linguist.class);
        
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
        
    }

    
    /**
     * Returns the configured lingust
     * @return the linguist
     */
    protected Linguist getLinguist() {
        return linguist;
    }
}


