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

package tests.search;

import edu.cmu.sphinx.util.SphinxProperties;

import edu.cmu.sphinx.search.Dictionary;
import edu.cmu.sphinx.search.FullDictionary;
import edu.cmu.sphinx.search.FastDictionary;
import edu.cmu.sphinx.search.Pronunciation;
import edu.cmu.sphinx.model.acoustic.Unit;
import edu.cmu.sphinx.util.Timer;

import java.io.File;

import java.net.URL;


/**
 * A test for FullDictionary class that reads in the connected-digits
 * dictionaries and dumps out the dictionary.
 */
public class FullDictionaryTest {


    private String context = "FullDictionaryTest";
    private Dictionary fastDictionary;
    private Dictionary fullDictionary;


    /**
     * Construct a FullDictionaryTest with the given SphinxProperties file.
     *
     * @param propertiesFile a SphinxProperties file
     */
    public FullDictionaryTest(String propertiesFile) throws Exception {
        
        String pwd = System.getProperty("user.dir");
        SphinxProperties.initContext
            (context, new URL
             ("file://" + pwd + File.separatorChar + propertiesFile));
	Timer fullTimer = Timer.getTimer("FullDictionaryTest", "fullTimer");
	Timer fastTimer = Timer.getTimer("FullDictionaryTest", "fastTimer");
        
	// some loading timings

	for (int i = 0; i < 3; i++) {
	    fastTimer.start();
	    fastDictionary = new FastDictionary(context);
	    fastTimer.stop();

	    fullTimer.start();
	    fullDictionary = new FullDictionary(context);
	    fullTimer.stop();

	    Timer.dumpAll();
	}

	// some lookup comparisons

	comparePronunciations("cat");
	comparePronunciations("dog");
	comparePronunciations("tomato");
    }

    private void comparePronunciations(String word) {
	Pronunciation p1[] = fastDictionary.getPronunciations(word, null);
	Pronunciation p2[] = fullDictionary.getPronunciations(word, null);

	if (p1.length != p2.length) {
	    System.out.println("Different # pronunciations for " + word);
	} else {
	    for (int i = 0; i < p1.length; i++) {
		compareUnits(word, p1[i].getUnits(), p2[i].getUnits());
	    }
	}
    }

    private void compareUnits(String word, Unit[] u1,
	    Unit[] u2) {
	if (u1.length != u2.length) {
	    System.out.println("Different # units for " + word);
	} else {
	    for (int i = 0; i < u1.length; i++) {
		if (!u1[i].getName().equals(u2[i].getName())) {
		    System.out.println("Mismatched units " +
			    u1[i].getName() + " and " +
			    u2[i].getName());
		}
	    }
	}
    }




    /**
     * Main method of this Test.
     */
    public static void main(String[] argv) {
        try {
            FullDictionaryTest test = new FullDictionaryTest(argv[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
