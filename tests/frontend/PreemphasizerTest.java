/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.Audio;
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
            String testName = argv[0];
            String propertiesFile = argv[1];
            String audioFile = argv[2];

            ProcessorTest fet = new ProcessorTest
                (testName, propertiesFile, audioFile);

            Preemphasizer preemphasizer = new Preemphasizer
                ("Preemphasizer", testName, fet.getAudioSource());
            preemphasizer.setDump(fet.getDump());

            Audio audio = null;
            do {
                audio = preemphasizer.getAudio();
            } while (audio != null);

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}


