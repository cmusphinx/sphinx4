/**
 * [[[copyright]]]
 */
package tests.search;

import edu.cmu.sphinx.util.SphinxProperties;

import edu.cmu.sphinx.search.Dictionary;
import edu.cmu.sphinx.search.FullDictionary;

import java.io.File;

import java.net.URL;


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
    public AlphaDictionaryTest(String propertiesFile) throws Exception {
        
        String pwd = System.getProperty("user.dir");
        SphinxProperties.initContext
            (context, new URL
             ("file://" + pwd + File.separatorChar + propertiesFile));
        
        Dictionary dictionary = new FullDictionary(context);
        dictionary.dump();
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
