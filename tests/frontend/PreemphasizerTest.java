/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.BatchFileAudioSource;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Preemphasizer;
import edu.cmu.sphinx.frontend.StreamAudioSource;
import edu.cmu.sphinx.util.Timer;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Test program for the Preemphasizer.
 */
public class PreemphasizerTest {

    public static void main(String[] argv) {
	if (argv.length < 1) {
	    System.out.println("Usage: java FrontEnd <filename>");
	}

        boolean batchMode = Boolean.getBoolean
            ("test.frontend.PreemphasizerTest.batchMode");
        boolean dumpValues = Boolean.getBoolean
            ("test.frontend.PreemphasizerTest.dumpValues");
        boolean dumpTimes = Boolean.getBoolean
            ("test.frontend.PreemphasizerTest.dumpTimes");

	Preemphasizer preemphasizer = new Preemphasizer();
	preemphasizer.setDump(dumpValues);

	FrontEnd frontend = new FrontEnd();

	frontend.addProcessor(preemphasizer);
	
	try {
            if (batchMode) {
                frontend.setAudioSource(new BatchFileAudioSource(argv[0]));
            } else {
                frontend.setAudioSource
                    (new StreamAudioSource(new FileInputStream(argv[0])));
            }
	    frontend.run();
	} catch (Exception e) {
	    e.printStackTrace();
	}

        if (dumpTimes) {
            Timer.dumpAll("");
        }
    }
}
