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

package tests.search;

import edu.cmu.sphinx.util.SphinxProperties;

import edu.cmu.sphinx.search.Dictionary;
import edu.cmu.sphinx.search.FullDictionary;
import edu.cmu.sphinx.search.Grammar;
import edu.cmu.sphinx.search.ArpaGrammar;

import java.io.File;

import java.net.URL;


/**
 * A test for the ArpaGrammar class 
 */
public class ArpaGrammarTest {
    private static String context = "ArpaGrammar";

    /**
     * Main method of this Test.
     */
    public static void main(String[] argv) {
        try {
	    String pwd = System.getProperty("user.dir");
	    SphinxProperties.initContext (context, new URL ("file://" 
			       + pwd + File.separatorChar + argv[0])); 
	    Dictionary dictionary = new FullDictionary(context);


            Grammar grammar = new ArpaGrammar();

	    grammar.initialize(context, dictionary);
	    grammar.dump();
	    System.out.println("Num nodes loaded is " +
		    grammar.getNumNodes());

	    System.out.println("done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
