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

package tests.linguist.language.ngram.large;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FastDictionary;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModelFactory;
import edu.cmu.sphinx.linguist.language.ngram.large.LargeTrigramModel;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.Utilities;


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
 * java UtteranceTest <propertiesFile> <testFile> <outputFile>
 * </code>
 */
public class UtteranceTest {

    private BufferedReader reader;
    private Dictionary dictionary;
    private LargeTrigramModel lm;
    private Timer timer;
    private PrintStream outStream = System.out;
    private int totalQueries;
    private boolean printScores;
    

    /**
     * Constructs an UtteranceTest.
     *
     * @param context the context to use
     * @param propsPath the properties file to use
     * @param testFile the N-gram test file
     * @param outFile the output file
     */
    public UtteranceTest(String context, String propsPath, 
			 String testFile, String outFile) throws IOException, Exception {
	
        SphinxProperties.initContext(context, new URL(propsPath));
        SphinxProperties props = SphinxProperties.getSphinxProperties(context);
        dictionary = new FastDictionary(context);
        lm = (LargeTrigramModel) LanguageModelFactory.getModel(props, dictionary);
        printScores = Boolean.getBoolean("printScores");

        InputStream stream = new FileInputStream(testFile);

        reader = new BufferedReader(new InputStreamReader(stream));
	if (outFile != null) {
	    outStream = new PrintStream(new FileOutputStream(outFile));
	}
	
        timer = Timer.getTimer(context, "lmLookup");
    }


    public void close() {
        outStream.close();
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
        
        List wordSequences = new LinkedList();
        
        while ((input = reader.readLine().trim()) != null &&
	       !input.equals("<END_UTT>")) {

            StringTokenizer st = new StringTokenizer(input);
            List list = new ArrayList();
            while (st.hasMoreTokens()) {
                String tok = (String) st.nextToken().toLowerCase();
                list.add(dictionary.getWord(tok));
            }
            WordSequence wordSequence = WordSequence.getWordSequence(list);
	    wordSequences.add(wordSequence);
        }

        int[] logScores = new int[wordSequences.size()];
        int s = 0;

        timer.start();

	lm.start();

	for (Iterator i = wordSequences.iterator(); i.hasNext(); ) {
	    WordSequence ws = (WordSequence) i.next();
            logScores[s++] = (int)lm.getProbability(ws);
	}

	totalQueries += s;
	
	lm.stop();
	
        timer.stop();
	
        if (printScores) {
            s = 0;
            for (Iterator i = wordSequences.iterator(); i.hasNext(); ) {
                WordSequence ws = (WordSequence) i.next();
                outStream.println(Utilities.pad(logScores[s++], 10) + " "+
                                  ws.toText().toUpperCase());
            }
        }
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
                ("Usage: java UtteranceTest <props_file> " +
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
	    UtteranceTest test = new UtteranceTest
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

