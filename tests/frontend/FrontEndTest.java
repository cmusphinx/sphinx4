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


import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.frontend.SimpleFrontEnd;
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

            FrontEnd fe = new SimpleFrontEnd("FrontEnd", testName, 
					     fet.getAudioSource());
            
            FeatureFrame featureFrame = null;
            do {
                featureFrame = fe.getFeatureFrame(25, null);
            } while (featureFrame != null);

            if (fet.getDumpTimes()) {
                Timer.dumpAll();
            }

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
