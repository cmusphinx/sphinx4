/**
 * [[[copyright]]]
 */

package tests.frontend;

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumSource;
import edu.cmu.sphinx.frontend.EnergyEndpointer;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.MelCepstrumProducer;
import edu.cmu.sphinx.frontend.MelFilterbank;
import edu.cmu.sphinx.frontend.NonSpeechFilter;
import edu.cmu.sphinx.frontend.Preemphasizer;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.SpectrumAnalyzer;
import edu.cmu.sphinx.frontend.Utterance;
import edu.cmu.sphinx.frontend.Windower;

import java.util.*;

/**
 * Test program for the MelFilterbank.
 */
public class EnergyEndpointerTest {

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
            Windower windower = new Windower
                ("HammingWindow", testName, preemphasizer);
            SpectrumAnalyzer spectrumAnalyzer = new SpectrumAnalyzer
                ("FFT", testName, windower);
            MelFilterbank melFilterbank = new MelFilterbank
                ("MelFilter", testName, spectrumAnalyzer);
            MelCepstrumProducer melCepstrum = new MelCepstrumProducer
                ("MelCepstrumProducer", testName, melFilterbank);
	    EnergyEndpointer endpointer = new EnergyEndpointer
                ("EnergyEndpointer", testName, melCepstrum);
            NonSpeechFilter nonSpeechFilter = new NonSpeechFilter
                ("NonSpeechFilter", testName, endpointer);

            CepstrumSource finalSource = endpointer;
            if (Boolean.getBoolean("removeNonSpeech")) {
                finalSource = nonSpeechFilter;
            }

            CepstraViewer cepstraViewer = new CepstraViewer("EndpointerTest");
            cepstraViewer.show();

            List cepstraGroupList = new LinkedList();
            List cepstraList = new LinkedList();

            Cepstrum cepstrum = null;

            do {
                cepstrum = finalSource.getCepstrum();
                if (cepstrum != null) {
                    cepstraList.add(cepstrum);
                    Signal signal = cepstrum.getSignal();

                    if (signal != null && 
                        signal.equals(Signal.UTTERANCE_END)) {

                        // an Utterance has ended
                        Cepstrum[] cepstra = new Cepstrum[cepstraList.size()];
                        cepstraList.toArray(cepstra);
                        String name = "no name";

                        // last Cepstrum with cepstral data
                        Utterance utterance = 
                            cepstra[cepstra.length-2].getUtterance();
                        if (utterance != null) {
                            name = utterance.getName();
                        }

                        // create the CepstraGroup and add it to the viewer
                        CepstraGroup cepstraGroup = new CepstraGroup
                            (cepstra, name);
                        cepstraViewer.addCepstraGroup(cepstraGroup);
                        cepstraList = new LinkedList();
                    }
                }
            } while (cepstrum != null);

            System.out.println("no more cepstrum");

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
