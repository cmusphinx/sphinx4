/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.BatchFileAudioSource;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.StreamAudioSource;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Test program for the Preemphasizer.
 */
public class FrontEndTest {

    FrontEnd frontend;
    boolean batchMode;
    boolean dumpTimes;


    /**
     * Constructs a FrontEndTest.
     */
    public FrontEndTest(String testName, String propertiesFile,
                        String audioSourceFile) throws Exception {

        batchMode = Boolean.getBoolean
            ("test.frontend." + testName + ".batchMode");
        dumpTimes = Boolean.getBoolean
            ("test.frontend." + testName + ".dumpTimes");

        String pwd = System.getProperty("user.dir");
        SphinxProperties.initContext
            (testName, new URL
             ("file://" + pwd + File.separatorChar + audioSourceFile));
        
	frontend = new FrontEnd();
	
        if (batchMode) {
            frontend.setAudioSource
                (new BatchFileAudioSource(audioSourceFile));
        } else {
            frontend.setAudioSource
                (new StreamAudioSource(new FileInputStream(audioSourceFile)));
        }
    }


    /**
     * Returns the FrontEnd to be tested.
     */
    public FrontEnd getFrontEnd() {
        return frontend;
    }


    /**
     * Runs the FrontEnd test.
     */
    public void run() {
        frontend.run();
        if (dumpTimes) {
            Timer.dumpAll("");
        }
    }
}
