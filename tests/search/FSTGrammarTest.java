/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on u and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package tests.search;

import edu.cmu.sphinx.util.SphinxProperties;

import edu.cmu.sphinx.knowledge.dictionary.Dictionary;
import edu.cmu.sphinx.knowledge.dictionary.FullDictionary;
import edu.cmu.sphinx.decoder.linguist.Grammar;
import edu.cmu.sphinx.decoder.linguist.FSTGrammar;

import java.io.File;

import java.net.URL;


/**
 * A test for the FSTGrammar class 
 */
public class FSTGrammarTest {
    private static String context = "FSTGrammar";

    /**
     * Main method of this Test.
     */
    public static void main(String[] argv) {
        try {
	    String pwd = System.getProperty("user.dir");
	    SphinxProperties.initContext (context, new URL ("file://" 
			       + pwd + File.separatorChar + argv[0])); 
	    Dictionary dictionary = new FullDictionary(context);


            edu.cmu.sphinx.decoder.linguist.Grammar grammar = new edu.cmu.sphinx.decoder.linguist.FSTGrammar();

	    grammar.initialize(context, null, dictionary);
	    grammar.dump();
	    System.out.println("Num nodes loaded is " +
		    grammar.getNumNodes());

	    System.out.println("done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
