/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.AudioSource;
import edu.cmu.sphinx.frontend.BatchFileAudioSource;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.FeatureSource;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.StreamAudioSource;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Test program for the Preemphasizer.
 */
public class FrontEndTest {

    AudioSource audioSource;
    boolean batchMode;
    boolean dumpTimes;
    boolean dumpValues;
    String context;

    /**
     * Constructs a FrontEndTest.
     */
    public FrontEndTest(String testName, String propertiesFile,
                        String audioSourceFile) throws Exception {
                            
        context = testName;

        batchMode = Boolean.getBoolean
            ("tests.frontend." + testName + ".batchMode");
        dumpTimes = Boolean.getBoolean
            ("tests.frontend." + testName + ".dumpTimes");
        dumpValues = Boolean.getBoolean
            ("tests.frontend." + testName + ".dumpValues");

        String pwd = System.getProperty("user.dir");
        SphinxProperties.initContext
            (testName, new URL
             ("file://" + pwd + File.separatorChar + audioSourceFile));
        
        if (batchMode) {
            audioSource = 
                (new BatchFileAudioSource
                 ("BatchFileAudioSource", context, audioSourceFile));
        } else {
            audioSource =
                (new StreamAudioSource
                 ("StreamAudioSource", context,
                  (new FileInputStream(audioSourceFile))));
        }
    }


    public AudioSource getAudioSource() {
        return audioSource;
    }


    public boolean getDump() {
        return dumpValues;
    }
}
