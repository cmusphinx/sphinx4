/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.CepstralMeanNormalizer;
import edu.cmu.sphinx.frontend.FeatureExtractor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.MelCepstrumProducer;
import edu.cmu.sphinx.frontend.MelFilterbank;
import edu.cmu.sphinx.frontend.SpectrumAnalyzer;
import edu.cmu.sphinx.frontend.Windower;
import edu.cmu.sphinx.frontend.Preemphasizer;


/**
 * Test program for the FeatureExtractor.
 */
public class FeatureExtractorTest {

    public static void main(String[] argv) {

	if (argv.length < 3) {
	    System.out.println
                ("Usage: java testClass <testName> " +
                 "<propertiesFile> <audiofilename>");
	}

        try {
            String testName = argv[0];
            String propertiesFile = argv[1];
            String audioFileName = argv[2];

            FrontEndTest fet = new FrontEndTest
                (testName, propertiesFile, audioFileName);

            // create the processors
            Preemphasizer preemphasizer = new Preemphasizer(testName);
            Windower hammingWindow = new Windower(testName);
            SpectrumAnalyzer spectrumAnalyzer = new SpectrumAnalyzer(testName);
            MelFilterbank melFilterbank = new MelFilterbank(testName);
	    MelCepstrumProducer melCepstrum = new MelCepstrumProducer(testName);
            CepstralMeanNormalizer cmn = new CepstralMeanNormalizer(testName);
            FeatureExtractor featureExtractor = new FeatureExtractor(testName);

            // add the processors
            FrontEnd fe = fet.getFrontEnd();
            fe.addProcessor(preemphasizer);
            fe.addProcessor(hammingWindow);
            fe.addProcessor(spectrumAnalyzer);
	    fe.addProcessor(melFilterbank);
	    fe.addProcessor(melCepstrum);
            fe.addProcessor(cmn);
            fe.addProcessor(featureExtractor);

            fet.run();

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
