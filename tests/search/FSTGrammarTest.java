/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on u and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package search;

import edu.cmu.sphinx.linguist.language.grammar.FSTGrammar;
import edu.cmu.sphinx.linguist.language.grammar.Grammar;
import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * A test for the FSTGrammar class 
 */
public class FSTGrammarTest {

    /**
     * Main method of this Test.
     */
    public static void main(String[] argv) {
        try {
	    ConfigurationManager cm = new ConfigurationManager(argv[0]); 

        Grammar grammar = (FSTGrammar)cm.lookup("grammar");
        grammar.allocate();
	    grammar.dumpGrammar("fst.grammar");
	    
	    System.out.println("Num nodes loaded is " + grammar.getNumNodes());
	    System.out.println("done");
	    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
