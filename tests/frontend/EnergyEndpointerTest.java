/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package tests.frontend;

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumSource;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.mfc.MelCepstrumProducer;
import edu.cmu.sphinx.frontend.mfc.MelFilterbank;
import edu.cmu.sphinx.frontend.Preemphasizer;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.SpectrumAnalyzer;
import edu.cmu.sphinx.frontend.Utterance;
import edu.cmu.sphinx.frontend.Windower;

import edu.cmu.sphinx.frontend.endpoint.EnergyEndpointer;
import edu.cmu.sphinx.frontend.endpoint.NonSpeechFilter;

import edu.cmu.sphinx.util.*;

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
	    SphinxProperties props = fet.getSphinxProperties();

            Preemphasizer preemphasizer = new Preemphasizer
                ("Preemphasizer", testName, props, fet.getAudioSource());
            Windower windower = new Windower
                ("HammingWindow", testName, props, preemphasizer);
            SpectrumAnalyzer spectrumAnalyzer = new SpectrumAnalyzer
                ("FFT", testName, props, windower);
            MelFilterbank melFilterbank = new MelFilterbank
                ("MelFilter", testName, props, spectrumAnalyzer);
            MelCepstrumProducer melCepstrum = new MelCepstrumProducer
		("MelCepstrumProducer", testName, props, melFilterbank);
	    EnergyEndpointer endpointer = new EnergyEndpointer
                ("EnergyEndpointer", testName, props, melCepstrum);
            NonSpeechFilter nonSpeechFilter = new NonSpeechFilter
                ("NonSpeechFilter", testName, props, endpointer);

            CepstrumSource finalSource = endpointer;
            if (Boolean.getBoolean("removeNonSpeech")) {
                finalSource = nonSpeechFilter;
            }

            final CepstraViewer cepstraViewer = 
                new CepstraViewer("EndpointerTest");
            cepstraViewer.show();

            CepstraGroupProducer groupProducer = new CepstraGroupProducer
                ("CepstraGroupProducer", testName, finalSource) {
                
                public void cepstraGroupProduced(CepstraGroup cepstraGroup) {
                    cepstraViewer.addCepstraGroup(cepstraGroup);
                }
            };

            Cepstrum cepstrum = null;
            do {
                cepstrum = groupProducer.getCepstrum();
            } while (cepstrum != null);

            System.out.println("no more cepstrum");

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
