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


import edu.cmu.sphinx.frontend.LiveCMN;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.mfc.MelCepstrumProducer;
import edu.cmu.sphinx.frontend.mfc.MelFilterbank;
import edu.cmu.sphinx.frontend.SpectrumAnalyzer;
import edu.cmu.sphinx.frontend.Windower;
import edu.cmu.sphinx.frontend.Preemphasizer;

import edu.cmu.sphinx.util.*;

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
		("MelCepstrum", testName, props, melFilterbank);
            
	    LiveCMN cmn = new LiveCMN
                ("CMN", testName, props, melCepstrum);
            DeltasFeatureExtractor extractor = new DeltasFeatureExtractor
                ("FeatureExtractor", testName, props, cmn);

            extractor.setDump(fet.getDump());

            Feature feature = null;
            do {
                feature = extractor.getFeature();
            } while (feature != null);

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
