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

import edu.cmu.sphinx.frontend.Audio;
import edu.cmu.sphinx.frontend.Preemphasizer;


/**
 * Test program for the Preemphasizer.
 */
public class PreemphasizerTest {

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
                ("Preemphasizer", testName, fet.getSphinxProperties(),
		 fet.getAudioSource());
            preemphasizer.setDump(fet.getDump());

            Audio audio = null;
            do {
                audio = preemphasizer.getAudio();
            } while (audio != null);

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}


