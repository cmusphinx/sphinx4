/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.frontend.util.PropertiesResolver;

import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;
import edu.cmu.sphinx.knowledge.acoustic.AcousticModelFactory;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.InputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Identical to BaseFrontEnd except that it uses the acoustic model
 * properties when initializing the front-end processors.
 *
 * @see BaseFrontEnd
 */
public class SimpleFrontEnd extends BaseFrontEnd {


    /**
     * Constructs a default SimpleFrontEnd.
     *
     * @param name the name of this SimpleFrontEnd
     * @param context the context of this SimpleFrontEnd
     * @param dataSource the place to pull data from
     *
     * @throws IOException if an I/O error occurs
     */
    public void initialize(String name, String context, DataSource dataSource)
        throws IOException {
	initialize(name, context, null, dataSource);
    }


    /**
     * Constructs a SimpleFrontEnd with the given name, context, acoustic 
     * model name, and DataSource.
     *
     * @param name the name of this SimpleFrontEnd
     * @param context the context of interest
     * @param amName the name of the acoustic model
     * @param dataSource the source of data
     *
     * @throws IOException if an I/O error occurs
     */
    public void initialize(String name, String context, String amName,
                           DataSource dataSource) throws IOException {
	
	SphinxProperties props = SphinxProperties.getSphinxProperties(context);
	boolean useAcousticModelProperties = props.getBoolean
	    (FrontEnd.PROP_USE_ACOUSTIC_MODEL_PROPERTIES,
             FrontEnd.PROP_USE_ACOUSTIC_MODEL_PROPERTIES_DEFAULT);

        if (useAcousticModelProperties) {
	    props = (PropertiesResolver.resolve
                     (props, getAcousticProperties(context, amName), 
                      context + "." + name));
        }

        super.initialize(name, context, props, amName, dataSource);
    }


    /**
     * Returns the properties of the relevant acoustic model.
     *
     * @param context the context of the acoustic properties
     * @param amName  the acoustic model name of the returned acoustic 
     *                properties
     *
     * @return the properties of the relevant acoustic model
     *
     * @throws java.io.IOException if an I/O error occurred
     */
    public SphinxProperties getAcousticProperties(String context,
                                                  String amName) 
        throws IOException {

	SphinxProperties props = SphinxProperties.getSphinxProperties(context);
	AcousticModel am;
        am = AcousticModelFactory.getModel(props, amName);
	if (am != null) {
	    return am.getProperties();
	} else {
	    return null;
	}
    }
}
