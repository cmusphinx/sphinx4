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

import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.parallel.ParallelFrontEnd;

import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;
import edu.cmu.sphinx.knowledge.acoustic.AcousticModelFactory;

import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.SphinxProperties;

import java.util.Iterator;
import java.util.Collection;

/**
 * Test program for the FrontEnd.
 */
public class ParallelFrontEndTest {

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

            FrontEnd fe = new ParallelFrontEnd();
            fe.initialize("ParallelFrontEnd", testName, fet.getAudioSource());
           
	    System.out.println(fe.toString());

            SphinxProperties props = 
                SphinxProperties.getSphinxProperties(testName);
	    Collection namelist = AcousticModelFactory.getNames(props);

	    for (Iterator i = namelist.iterator(); i.hasNext();) {
		String amName = (String) i.next();
		FeatureFrame featureFrame = null;
		do {
		    featureFrame = fe.getFeatureFrame(25, amName);
		    if (featureFrame != null) {
			System.out.println(featureFrame.toString());
		    }
		} while (featureFrame != null);
	    }

            if (fet.getDumpTimes()) {
                Timer.dumpAll();
            }

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
