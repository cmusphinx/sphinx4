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

import java.io.File;
import java.net.URL;

import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FullDictionary;
import edu.cmu.sphinx.linguist.language.grammar.FSTGrammar;
import edu.cmu.sphinx.linguist.language.grammar.Grammar;
import edu.cmu.sphinx.util.SphinxProperties;


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


       Grammar grammar = new FSTGrammar();

	    grammar.initialize(context, null, dictionary);
	    grammar.dumpGrammar("fst.grammar");
	    System.out.println("Num nodes loaded is " +
		    grammar.getNumNodes());

	    System.out.println("done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
