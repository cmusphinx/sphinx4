/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.BatchFileAudioSource;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Preemphasizer;
import edu.cmu.sphinx.frontend.StreamAudioSource;
import edu.cmu.sphinx.frontend.Windower;
import edu.cmu.sphinx.util.Timer;

import java.io.FileInputStream;


/**
 * Test program for the HammingWindow
 */
public class HammingWindowerTest {

    public static void main(String[] argv) {
	if (argv.length < 1) {
	    System.out.println("Usage: java FrontEnd <filename>");
	}

        boolean batchMode = Boolean.getBoolean
            ("tests.frontend.HammingWindowerTest.batchMode");
        boolean dumpValues = Boolean.getBoolean
            ("tests.frontend.HammingWindowerTest.dumpValues");
        boolean dumpTimes = Boolean.getBoolean
            ("tests.frontend.HammingWindowerTest.dumpTimes");

        Preemphasizer preemphasizer = new Preemphasizer();

        Windower hammingWindow = new Windower();
        hammingWindow.setDump(dumpValues);
        
	FrontEnd frontend = new FrontEnd();
      	frontend.addProcessor(preemphasizer);
        frontend.addProcessor(hammingWindow);

	try {
            if (batchMode) {
                frontend.setAudioSource(new BatchFileAudioSource(argv[0]));
            } else {
                frontend.setAudioSource
                    (new StreamAudioSource(new FileInputStream(argv[0])));
            }

	    frontend.run();
            
            if (dumpTimes) {
                Timer.dumpAll();
            }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
