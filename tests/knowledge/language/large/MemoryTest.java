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

package tests.knowledge.language.large;

import edu.cmu.sphinx.knowledge.dictionary.*;

import edu.cmu.sphinx.knowledge.language.*;
import edu.cmu.sphinx.knowledge.language.large.*;

import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Utilities;

import java.io.*;

import java.net.URL;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import edu.cmu.sphinx.util.Timer;


/**
 * Reads in a file of n-grams, with each utterance bounded by <START_UTT>
 * and <END_UTT> markers. A sample test file looks like:
 *
 * <code>
 * <START_UTT>
 * ngram_0
 * ngram_1
 * ...
 * <END_UTT>
 * <START_UTT>
 * ngram_0
 * ngram_1
 * <END_UTT>
 * ...
 * </code>
 *
 * This test will then query the dynamic language model loader for all
 * the ngrams in an utterance, and clears the language model cache
 * after the utterance. This is intended to simulate what happens during
 * a real decoding.
 *
 * To run this test:
 * <code>
 * java MemoryTest <propertiesFile> <testFile> <outputFile>
 * </code>
 */
public class MemoryTest {

    private BufferedReader reader;
    private Dictionary dictionary;
    private LargeTrigramModel lm;
    private Timer timer;
    private PrintStream outStream = System.out;
    private int totalQueries = 0;
    private boolean printScores;
    

    /**
     * Constructs an MemoryTest.
     *
     * @param context the context to use
     * @param propsPath the properties file to use
     * @param testFile the N-gram test file
     * @param outFile the output file
     */
    public MemoryTest(String context, String propsPath, 
                      String testFile, String outFile) throws IOException {
	
        SphinxProperties.initContext(context, new URL(propsPath));
        dictionary = new FastDictionary(context);
        lm = new LargeTrigramModel(context, dictionary);
        printScores = Boolean.getBoolean("printScores");

        InputStream stream = new FileInputStream(testFile);

        reader = new BufferedReader(new InputStreamReader(stream));
	if (outFile != null) {
	    outStream = new PrintStream(new FileOutputStream(outFile));
	}
	
        timer = Timer.getTimer(context, "lmLookup");
    }


    /**
     * Close the results output stream
     */
    public void close() {
        // outStream.close();
    }


    /**
     * Returns true if there are more utterances to test.
     *
     * @return true if there are more utterances to test
     */
    public boolean hasMoreUtterances() throws IOException {
	String input = reader.readLine();
	return (input != null && input.trim().equals("<START_UTT>"));
    }
        

    /**
     * Test the next available utterance.
     */
    public void testUtterance() throws IOException {
	
        String input;

        timer.start();
        lm.start();
        
        while ((input = reader.readLine()) != null &&
               !input.equals("<END_UTT>")) {

            input = input.toLowerCase();
            StringTokenizer st = new StringTokenizer(input);
            List list = new ArrayList();
            while (st.hasMoreTokens()) {
                String tok = (String) st.nextToken();
                list.add(dictionary.getWord(tok));
            }
            WordSequence ws = WordSequence.getWordSequence(list);
            lm.getProbability(ws);
            totalQueries++;
        }

        lm.stop();
        timer.stop();
    }


    /**
     * Prints out statistics
     */
    public void printStats() {
	long usedMemory = Runtime.getRuntime().totalMemory() - 
	    Runtime.getRuntime().freeMemory();
	System.out.println("Total queries: " + totalQueries + " ngrams");
	System.out.println("Used memory: " + usedMemory + " bytes");
	System.out.println("Bigram misses: " + lm.getBigramMisses());
	System.out.println("Trigram misses: " + lm.getTrigramMisses());
	System.out.println("Trigram hits: " + lm.getTrigramHits());
	Timer.dumpAll();
    }


    /**
     * The main program
     */
    public static void main(String[] args) throws Exception {

        String propsPath;
        String testFile = null;
	String outFile = null;
	
        if (args.length == 0) {
            System.out.println
                ("Usage: java MemoryTest <props_file> " +
                 "<testFile> <output_file>");
        }
        
        propsPath = args[0];
        if (args.length >= 2) {
            testFile = args[1];
        }
        if (args.length >= 3) {
	    outFile = args[2];
	}

	try {
	    MemoryTest test = new MemoryTest
		("test", propsPath, testFile, outFile);
	    
	    while (test.hasMoreUtterances()) {
		test.testUtterance();
	    }
	    
	    test.printStats();
            test.close();
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}
    }
}

