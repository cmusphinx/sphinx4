/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Preemphasizer;


/**
 * Test program for the Preemphasizer.
 */
public class PreemphasizerTest {

    public static void main(String[] argv) {

	if (argv.length < 3) {
	    System.out.println
                ("Usage: java testClass <testName> " +
                 "<propertiesFile> <audiofilename>");
	}

        try {
            FrontEndTest fet = new FrontEndTest(argv[0], argv[1], argv[2]);

            Preemphasizer preemphasizer = new Preemphasizer(argv[0]);

            FrontEnd fe = fet.getFrontEnd();
            fe.addProcessor(preemphasizer);

            fet.run();

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}


