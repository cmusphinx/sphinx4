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

package edu.cmu.sphinx.knowledge.acoustic;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

/**
 * Represents the generic interface to the Acoustic 
 * Model for sphinx4
 */
public class AcousticModelFactory {

    static Map classMap = null;
    static Map modelMap = new HashMap();
    static String defaultModelName = null;

    /**
     * Prefix for acoustic model SphinxProperties.
     */
    public final static String PROP_PREFIX
	= "edu.cmu.sphinx.knowledge.acoustic.AcousticModelFactory.";

    /**
     * the prefix for the model specific properties
     */
    public final static String PROP_MODELS = PROP_PREFIX + "models";


    /**
     * the prefix for the model specific properties
     */
    public final static String PROP_CLASS = PROP_PREFIX + "class";


    /**
     * Return the names of all acoustic models in the given context.
     *
     * @param props the  sphinxproperties
     *
     * @return a list of all names of acoustic models in the given context;
     *    if there are no names, it will return a list with no elements.
     */
    public static Collection getNames(SphinxProperties props) {
        loadClassMap(props);
	return classMap.keySet();
    }

     /**
      * Retrieves the acoustic model with the given name. If no name
      * is given the first model defined in the sphinx properties
      * sheet is returned
      *
      * @param props the sphinx properties
      * @param name the name of the acoustic model (or null if the
      * first model should be returned)
      *
      * @return the acoustic model associated with the context or null
      * if no model could be found
      *
      * @throws IOException if the model could not be loaded
      */
    public static AcousticModel getModel(SphinxProperties props, String name) 
	throws IOException {

        if (name == null) {
            name = defaultModelName;
        } 
        AcousticModel am = (AcousticModel) modelMap.get(name);

        if (am == null) {
            loadClassMap(props);

            String className = (String) classMap.get(name);

            try {
               Timer.start(name);

               if (className != null) {
                   am = (AcousticModel) Class.forName(className).newInstance();
                   am.initialize(name, props.getContext());
                   modelMap.put(name, am);
               }
               Timer.stop(name);
            } catch (ClassNotFoundException fe) {
                throw new Error("CNFE:Can't create acoustic model " 
                        + className, fe);
            } catch (InstantiationException ie) {
                throw new Error("IE: Can't create acoustic model " 
                        + className, ie);
            } catch (IllegalAccessException iea) {
                throw new Error("IEA: Can't create acoustic model " 
                        + className, iea);
            } catch (IOException ioe) {
                throw new Error("IOE: Can't create acoustic model " 
                        + className, ioe);
            } 
        }
       return am;
    }


    /**
     * loads the class map. The class map is map that relates acoustic
     * model names to classnames
     *
     * @param props the sphinx properties
     */
    private static void loadClassMap(SphinxProperties props) {
        if (classMap == null) {
            classMap = new HashMap();
            String modelNames = props.getString(PROP_MODELS,"");
            StringTokenizer st = new StringTokenizer(modelNames);

            while (st.hasMoreTokens()) {
                String name = st.nextToken();
                String className = props.getString(name,  PROP_CLASS, null);

                if (className != null) {
                    if (defaultModelName == null) {
                        defaultModelName = name;
                    }
                    classMap.put(name, className);
                }  else {
                    System.err.println(
                      "AcousticModelFactory: Bad config for model " + name);
                }
            }
        }
    }
}

