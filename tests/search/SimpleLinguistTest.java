/**
 * [[[copyright]]]
 */
package tests.search;

import edu.cmu.sphinx.model.acoustic.AcousticModel;

import edu.cmu.sphinx.search.Dictionary;
import edu.cmu.sphinx.search.Linguist;
import edu.cmu.sphinx.search.SimpleDigitLinguist;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.File;

import java.net.URL;


/**
 * A test for SimpleLinguist class that reads in the connected-digits
 * dictionaries and dumps out the dictionary.
 */
public class SimpleLinguistTest {


    private String context = "SimpleLinguistTest";
    private SimpleDigitLinguist simpleLinguist;


    /**
     * Construct a SimpleLinguistTest with the given SphinxProperties file.
     *
     * @param propertiesFile a SphinxProperties file
     */
    public SimpleLinguistTest(String propertiesFile) throws Exception {
        
        String pwd = System.getProperty("user.dir");
        SphinxProperties.initContext
            (context, new URL
             ("file://" + pwd + File.separatorChar + propertiesFile));
        
        simpleLinguist = new SimpleDigitLinguist
            (context, AcousticModel.getAcousticModel(context));
	dumpGrammar();
	dumpSentenceHMM();

    }



    /**
     * Dumps the SentenceHMM
     */
    public void dumpSentenceHMM() {
	simpleLinguist.getInitialState().dump();
    }

    
    /**
     * Dumps the grammar 
     */
    public void dumpGrammar() {
        System.out.print
            (((SimpleDigitLinguist) simpleLinguist).getGrammar().toString());
    }



    /**
     * Main method of this Test.
     */
    public static void main(String[] argv) {
        try {
            SimpleLinguistTest test = new SimpleLinguistTest(argv[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
