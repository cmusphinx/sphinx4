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


package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.model.acoustic.AcousticModel;

import edu.cmu.sphinx.util.SphinxProperties;

import java.util.Enumeration;
import java.util.Properties;


/**
 * Resolves the differences in properties between the SphinxProperties
 * file and the AcousticModel properties. The need for this class arises
 * because the FrontEnd retrieves most of its properties from the
 * AcousticModel, but at the same time, retrieves the rest of the properties
 * from the SphinxProperties file. As a result, there can be overlapping
 * properties between the two sets.
 *
 * <p>The current policy for selecting the properties is:
 * <ol>
 * <li>The AcousticModel properties are the default.
 * <li>The SphinxProperties file properties can override the AcousticModel
 *     properties.
 * </ol>
 *
 * Therefore, if the property "edu.cmu.sphinx.frontend.sampleRate"
 * is only in the AcousticModel properties, that will be used. However,
 * if it is in both, then the one in the SphinxProperties file will be used.
 * If it is only in the SphinxProperties file, that one will be used.
 */
public class PropertiesResolver {

    /**
     * Resolve the differences in properties between the
     * SphinxProperties object (from the SphinxProperties file), and
     * the given AcousticModel properties object.
     *
     * @param sphinxProperties the SphinxProperties from the properties file
     * @param acousticProperties the properties from the AcousticModel
     *
     * @return a new SphinxProperties containing the resolved properties
     */
    public static SphinxProperties resolve
	(SphinxProperties sphinxProperties,
	 SphinxProperties acousticProperties, String newContext) {

	Properties sphinxProps = sphinxProperties.getProperties();
	Properties acousticProps = acousticProperties.getProperties();

        // now put in all System properties to override the ones in the
        // sphinxProps object
        sphinxProps.putAll(System.getProperties());

	// Now merge all acoustic properties with the sphinx file properties
	// according to the policy defined above.

	for (Enumeration e = acousticProps.propertyNames(); 
	     e.hasMoreElements();) {

	    String propName = (String) e.nextElement();
	    String key = propName;
	    
	    // Reconstruct the key if the acoustic properties start
	    // with "edu.cmu.sphinx.model.acoustic.". The new key
	    // should start with "edu.cmu.sphinx.frontend.", otherwise
	    // throws an Error.

	    if (propName.startsWith(AcousticModel.PROP_PREFIX)) {
		String suffix = propName.substring
		    (AcousticModel.PROP_PREFIX.length());
		key = FrontEnd.PROP_PREFIX + suffix;
	    } else if (!propName.startsWith(FrontEnd.PROP_PREFIX)) {
		throw new Error("Acoustic property name does not start with " +
				FrontEnd.PROP_PREFIX + ": " + propName);
	    }

	    // If the property is already in Sphinx property, then it is 
	    // overriden by that in Sphinx property.

	    if (sphinxProps.containsKey(key)) {
		System.out.println
		    ("WARNING: replacing acoustic model property " + propName +
		     "\n         with Sphinx property " + key);
	    } else {
		String value = (String) acousticProps.getProperty(propName);
		sphinxProps.setProperty(key, value);
	    }
	}

	// now remove all properties that don't start with FrontEnd.PROP_PREFIX
	for (Enumeration e = sphinxProps.propertyNames();
	     e.hasMoreElements();) {
	    String property = (String) e.nextElement();
	    if (!property.startsWith(FrontEnd.PROP_PREFIX)) {
		sphinxProps.remove(property);
	    }
	}

        System.out.println("After pruning");
        sphinxProps.list(System.out);

	// create and return the new SphinxProperties
	SphinxProperties.initContext(newContext, sphinxProps);
	return SphinxProperties.getSphinxProperties(newContext);
    }
}
