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
public class FullDictionaryTest {


    private String context = "FullDictionaryTest";


    /**
     * Construct a FullDictionaryTest with the given SphinxProperties file.
     *
     * @param propertiesFile a SphinxProperties file
     */
    public FullDictionaryTest(String propertiesFile) throws Exception {
        
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
            FullDictionaryTest test = new FullDictionaryTest(argv[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
