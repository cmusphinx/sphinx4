/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.Audio;
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
            String testName = argv[0];
            String propertiesFile = argv[1];
            String audioFile = argv[2];

            FrontEndTest fet = new FrontEndTest
                (testName, propertiesFile, audioFile);

            Preemphasizer preemphasizer = new Preemphasizer
                ("Preemphasizer", testName, fet.getAudioSource());
            Windower windower = new Windower
                ("HammingWindow", testName, preemphasizer);
            windower.setDump(fet.getDump());

            Audio audio = null;
            do {
                audio = windower.getAudio();
            } while (audio != null);

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
