/**
 * [[[copyright]]]
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
