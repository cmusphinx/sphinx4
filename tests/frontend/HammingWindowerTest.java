/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Windower;
import edu.cmu.sphinx.frontend.Preemphasizer;


/**
 * Test program for the HammingWindower.
 */
public class HammingWindowerTest {

    public static void main(String[] argv) {

	if (argv.length < 3) {
	    System.out.println
                ("Usage: java testClass <testName> " +
                 "<propertiesFile> <audiofilename>");
	}

        try {
            FrontEndTest fet = new FrontEndTest(argv[0], argv[1], argv[2]);

            Preemphasizer preemphasizer = new Preemphasizer(argv[0]);
            Windower hammingWindow = new Windower(argv[0]);
            hammingWindow.setDump(true);

            FrontEnd fe = fet.getFrontEnd();
            fe.addProcessor(preemphasizer);
            fe.addProcessor(hammingWindow);

            fet.run();

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
