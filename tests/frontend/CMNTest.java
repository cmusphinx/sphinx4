/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstralMeanNormalizer;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.MelCepstrumProducer;
import edu.cmu.sphinx.frontend.MelFilterbank;
import edu.cmu.sphinx.frontend.SpectrumAnalyzer;
import edu.cmu.sphinx.frontend.Windower;
import edu.cmu.sphinx.frontend.Preemphasizer;


/**
 * Test program for the FeatureExtractor.
 */
public class CMNTest {

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
            SpectrumAnalyzer spectrumAnalyzer = new SpectrumAnalyzer
                ("FFT", testName, windower);
            MelFilterbank melFilterbank = new MelFilterbank
                ("MelFilter", testName, spectrumAnalyzer);
	    MelCepstrumProducer melCepstrum = new MelCepstrumProducer
                ("MelCepstrum", testName, melFilterbank);
            CepstralMeanNormalizer cmn = new CepstralMeanNormalizer
                ("CMN", testName, melCepstrum);

            cmn.setDump(fet.getDump());

            Cepstrum cepstrum = null;
            do {
                cepstrum = cmn.getCepstrum();
            } while (cepstrum != null);

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
