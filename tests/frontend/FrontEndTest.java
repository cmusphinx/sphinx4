/**
 * [[[copyright]]]
 */

package tests.frontend;

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

    FrontEnd frontend;
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
        
	frontend = new FrontEnd("FrontEnd", context);
	
        if (batchMode) {
            frontend.setAudioSource
                (new BatchFileAudioSource
                 ("BatchFileAudioSource", context, audioSourceFile));
        } else {
            frontend.setAudioSource
                (new StreamAudioSource
                 ("StreamAudioSource", context,
                  (new FileInputStream(audioSourceFile))));
        }
    }


    /**
     * Returns the FrontEnd to be tested.
     */
    public FrontEnd getFrontEnd() {
        return frontend;
    }


    public boolean getDump() {
        return dumpValues;
    }


    /**
     * Runs the FrontEnd test.
     */
    public void run() {

        frontend.getTimer().start();

        FeatureSource featureSource = frontend.getFeatureSource();
        
        if (featureSource != null) {

            Feature feature = null;
            
            try {
                do {
                    feature = featureSource.getFeature();
                } while (feature != null);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        frontend.getTimer().stop();
        
        if (dumpTimes) {
            Timer.dumpAll(context);
        }
    }
}
