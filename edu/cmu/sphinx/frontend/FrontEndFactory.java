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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.sphinx.util.SphinxProperties;


/**
 * A factory class for FrontEnd objects.
 */
public class FrontEndFactory {

    /**
     * The prefix for the SphinxProperties names of this class.
     */
    public static final String PROP_PREFIX
        = "edu.cmu.sphinx.frontend.FrontEndFactory.";


    /**
     * The SphinxProperty name for sample rate in Hertz (i.e.,
     * number of times per second).
     */
    public static final String PROP_SAMPLE_RATE = PROP_PREFIX + "sampleRate";


    /**
     * The default value for PROP_SAMPLE_RATE.
     */
    public static final int PROP_SAMPLE_RATE_DEFAULT = 16000;


    /**
     * A map for storing the front ends.
     */
    private static Map frontends = new HashMap();
    

    /**
     * Returns a front end with the given name.
     *
     * @param props the SphinxProperty to use
     * @param name the name of the front end to get
     *
     * @return the last DataProcessor of the front end with the given name
     *
     * @throws InstantiationException if there is an error initializing
     *                                a new front end
     */
    public static FrontEnd getFrontEnd(SphinxProperties props, String name)
        throws InstantiationException {
        String[] frontEndNames = getFrontEndNames(props);
        boolean exists = false;
        for (int i = 0; i < frontEndNames.length; i++) {
            if (frontEndNames[i].equals(name)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            debugMessage("Front end \"" + name + "\" does not exist.");
            return null;
        } else {
            return loadFrontEnd(name, props);
        }
    }


    /**
     * Returns the names of all available front ends.
     *
     * @param props the SphinxProperty to use
     *
     * @return the names of all available front ends
     */
    public static Collection getNames(SphinxProperties props) {
        return Arrays.asList(getFrontEndNames(props));
    }


    /**
     * Returns as a string array the name(s) of the front end(s),
     * as specified in the SphinxProperties by the following:
     * <code>
     * edu.cmu.sphinx.frontend.pipelines = ... ... (e.g., "mfc plp");
     * </code>
     *
     * @param props the SphinxProperty to use
     *
     * @return the name of all available front ends
     */
    private static String[] getFrontEndNames(SphinxProperties props) {
        String pipelines = props.getString(PROP_PREFIX + "pipelines", "");
        return pipelines.split(" ");
    }


    /**
     * Loads the given front end, and returns its last DataProcessor.
     *
     * @param name the name of the front end to load
     * @param props the SphinxProperty to use
     *
     * @return the FrontEnd with the given name
     */
    private static FrontEnd loadFrontEnd(String frontEndName,
                                         SphinxProperties props)
        throws InstantiationException {
        FrontEnd frontend = (FrontEnd) frontends.get(frontEndName);

        if (frontend == null) {
            int nStages = props.getInt
                (frontEndName, PROP_PREFIX + "nStages", 0);
            debugMessage
                ("Loading " + frontEndName + " with " + nStages +" stages...");

            DataProcessor firstProcessor = null;
            DataProcessor lastProcessor = null;

            // initialize the pipeline
            for (int i = 1; i <= nStages; i++) {
                String prefix = 
                    frontEndName + ";" + PROP_PREFIX + "stage." + i + ".";
                String className = props.getString(prefix + "class", null);
		String processorName = props.getString(prefix + "name", null);
                
                if (className == null) {
                    throw new Error
                        ("FrontEnd " + frontEndName +" has no processor " + i);
                } else {
                    try {
                        DataProcessor processor = (DataProcessor)
                            Class.forName(className).newInstance();
                        processor.initialize
                            (getProcessorName(processorName, className),
			     frontEndName, props, lastProcessor);
                        if (firstProcessor == null) {
                            firstProcessor = processor;
                        }
                        lastProcessor = processor;
                    } catch (ClassNotFoundException cnfe) {
                        throw new InstantiationException
                            ("Class " + className + " not found");
                    } catch (IllegalAccessException iae) {
                        throw new InstantiationException
                            ("Illegal access: " + className);
                    }
                }
            }
            
            frontend = new FrontEnd(firstProcessor, lastProcessor);
            frontend.initialize(frontEndName, frontEndName, props, null);
            frontends.put(frontEndName, frontend);
        }
        
        return frontend;
    }
    
    /**
     * Returns the processor name (the first argument) if it is
     * not null. If it is null,
     * returns the class name from a fully-qualified class name.
     * For example, given "edu.cmu.sphinx.frontend.FrontEndFactory",
     * will return "FrontEndFactory".
     *
     * @param processorName the given name of the processor, if any
     * @param fullClassName the fully-qualified class name
     *
     * @return a name for the front end processor
     */
    private static String getProcessorName(String processorName, 
					   String fullClassName) {
	if (processorName == null) {
	    int periodIndex = fullClassName.lastIndexOf(".");
	    if (periodIndex > -1) {
		return fullClassName.substring(periodIndex + 1);
	    } else {
		return fullClassName;
	    }
	} else {
	    return processorName;
	}
    }

    /**
     * Print debug messages.
     *
     * @param message the message to print
     */
    private static void debugMessage(String message) {
        if (false) {
            System.out.println("FrontEndFactory.getFrontEnd(): " + message);
        }
    }
}
