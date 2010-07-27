/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package linguist.dictionary;

import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FullDictionary;
import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * A test for FullDictionary class that reads in the connected-digits
 * dictionaries and dumps out the dictionary.
 */
public class AlphaDictionaryTest {


    private String context = "AlphaDictionaryTest";


    /**
     * Construct a AlphaDictionaryTest with the given SphinxProperties file.
     *
     * @param propertiesFile a SphinxProperties file
     */
    public AlphaDictionaryTest(String configFile) throws Exception {
        
        ConfigurationManager cm = new ConfigurationManager (configFile);    
        Dictionary dictionary = (FullDictionary)cm.lookup("dictionary");
        System.out.println (dictionary);
    }


    /**
     * Main method of this Test.
     */
    public static void main(String[] argv) {
        try {
            AlphaDictionaryTest test = new AlphaDictionaryTest(argv[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
