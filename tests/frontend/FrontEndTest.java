/**
 * [[[copyright]]]
 */

package tests.frontend;


import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.util.Timer;


/**
 * Test program for the FrontEnd.
 */
public class FrontEndTest {

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

            FrontEnd fe = new FrontEnd("FrontEnd", testName, 
                                       fet.getAudioSource());
            
            FeatureFrame featureFrame = null;
            do {
                featureFrame = fe.getFeatureFrame(25);
            } while (featureFrame != null);

            if (fet.getDumpTimes()) {
                Timer.dumpAll();
            }

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
