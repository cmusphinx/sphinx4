/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package linguist.dictionary;

import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FullDictionary;
import edu.cmu.sphinx.linguist.dictionary.FastDictionary;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.TimerPool;
import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * A test for FullDictionary class that reads in the connected-digits
 * dictionaries and dumps out the dictionary.
 */
public class FullDictionaryTest {

	private Dictionary fastDictionary;
    private Dictionary fullDictionary;


    /**
     * Construct a FullDictionaryTest with the given SphinxProperties file.
     *
     * @param configFile a SphinxProperties file
     */
    public FullDictionaryTest(String configFile) throws Exception {
        
     ConfigurationManager cm = new ConfigurationManager (configFile);

	Timer fullTimer = TimerPool.getTimer(this, "fullTimer");
	Timer fastTimer = TimerPool.getTimer(this, "fastTimer");
        
	// some loading timings

	for (int i = 0; i < 3; i++) {
	    fastTimer.start();
	    fastDictionary = (FastDictionary)cm.lookup("fastdictionary");
	    fastTimer.stop();

	    fullTimer.start();
	    fullDictionary = (FullDictionary)cm.lookup("fulldictionary");
	    fullTimer.stop();

	    TimerPool.dumpAll();
	}

	// some lookup comparisons

	comparePronunciations("cat");
	comparePronunciations("dog");
	comparePronunciations("tomato");
    }

    private void comparePronunciations(String word) {
	Pronunciation p1[] = 
            fastDictionary.getWord(word).getPronunciations(null);
	Pronunciation p2[] = 
            fullDictionary.getWord(word).getPronunciations(null);

	if (p1.length != p2.length) {
	    System.out.println("Different # pronunciations for " + word);
	} else {
	    for (int i = 0; i < p1.length; i++) {
    		System.out.println(p1[i]);
            System.out.println(p2[i]);
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
                String errorMessage = "";
                if (u1[i] == null) {
                    errorMessage = ("Unit " + i + " of word `" + word + 
                                    "' in FastDictionary is null. ");
                }
                if (u2[i] == null) {
                    errorMessage += ("Unit " + i + " of word `" + word + 
                                     "' in FullDictionary is null.");
                }
                if (!errorMessage.isEmpty()) { 
                    throw new Error(errorMessage);
                }
                String name1 = u1[i].getName();
                String name2 = u2[i].getName();
                if (name1 == null) {
                    throw new Error("No name for " + u1);
                }
                if (name2 == null) {
                    throw new Error("No name for " + u2);
                }
                if (!name1.equals(name2)) {
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
