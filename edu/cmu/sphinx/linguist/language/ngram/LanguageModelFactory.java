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

package edu.cmu.sphinx.linguist.language.ngram;

import java.io.IOException;

import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;



/**
 * This class provides a static method for creating a language model
 * for a particular context.
 */
public class LanguageModelFactory {

    /**
     * Prefix string for properties.
     */
    public final static String PROP_PREFIX
        = "edu.cmu.sphinx.linguist.language.ngram.LanguageModelFactory";

    /**
     * The SphinxProperty for the language model class.
     */
    public final static String PROP_CLASS = PROP_PREFIX + ".languageClass";


    /**
     * The default value of PROP_CLASS.
     */
    public final static String PROP_CLASS_DEFAULT = null;


    /**
     * Creates a language model based upon the particular set of
     * sphinx properties
     *
     * @param props the sphinx properties
     * @param dictionary the dictionary to be used by the model
     *
     * @return a language model (or null)
     *
     * @throws InstantiationException if the model could not be created
     * @throws IOException if the model could not be loaded
     */
    public static LanguageModel getModel(
            SphinxProperties props, Dictionary dictionary)
            throws IOException, InstantiationException {
	String path =  "";
	try {

           Timer.start("createLanguageModel");
	   LanguageModel lm = null;
	   path = props.getString(PROP_CLASS, null);

	   if (path != null) {
	       lm = (LanguageModel) Class.forName(path).newInstance();
	       lm.initialize(props, dictionary);
	   }
           Timer.stop("createLanguageModel");
	   return lm;
	} catch (ClassNotFoundException fe) {
	    throw new InstantiationException(
                    "CNFE:Can't create language model " + path);
	} catch (IllegalAccessException iea) {
	    throw new InstantiationException(
                    "IEA: Can't create language model " + path);
	} 
    }
}

