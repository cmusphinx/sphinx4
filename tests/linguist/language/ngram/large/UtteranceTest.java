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
package linguist.language.ngram.large;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.ngram.large.LargeTrigramModel;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.util.TimerPool;
import edu.cmu.sphinx.util.props.ConfigurationManager;

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
     * @param configPath the properties file to use
     * @param testFile the N-gram test file
     * @param outFile the output file
     */
    public UtteranceTest(String context, String configPath, 
			 String testFile, String outFile) throws IOException, Exception {
	
        ConfigurationManager cm = new ConfigurationManager(configPath);
     
        lm = (LargeTrigramModel) cm.lookup ("trigramModel");
        printScores = Boolean.getBoolean("printScores");

        InputStream stream = new FileInputStream(testFile);

        reader = new BufferedReader(new InputStreamReader(stream));
	if (outFile != null) {
	    outStream = new PrintStream(new FileOutputStream(outFile));
	}
	
        timer = TimerPool.getTimer(this, "lmLookup");
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
        
        List<WordSequence> wordSequences = new LinkedList<WordSequence>();
        
        while ((input = reader.readLine().trim()) != null &&
	       !input.equals("<END_UTT>")) {

            StringTokenizer st = new StringTokenizer(input);
            List<Word> list = new ArrayList<Word>();
            while (st.hasMoreTokens()) {
                String tok = st.nextToken().toLowerCase();
                list.add(dictionary.getWord(tok));
            }
            WordSequence wordSequence = new WordSequence(list);
            wordSequences.add(wordSequence);
        }

        int[] logScores = new int[wordSequences.size()];
        int s = 0;

        timer.start();

	    lm.start();

        for (WordSequence ws : wordSequences) {
            logScores[s++] = (int)lm.getProbability(ws);
        }

	    totalQueries += s;
	
	    lm.stop();
	
        timer.stop();
	
        if (printScores) {
            s = 0;
            for (WordSequence ws : wordSequences) {
                outStream.println(Utilities.pad(logScores[s++], 10) + ' ' + ws.toString().toUpperCase());
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
	System.out.println("NGram misses: " + lm.getNGramMisses());
	System.out.println("NGram hits: " + lm.getNGramHits());
	TimerPool.dumpAll();
    }


    /**
     * The main program
     */
    public static void main(String[] args) throws Exception {

        String configPath;
        String testFile = null;
	String outFile = null;
	
        if (args.length == 0) {
            System.out.println
                ("Usage: java UtteranceTest <config_file> " +
                 "<testFile> <output_file>");
        }
        
        configPath = args[0];
        if (args.length >= 2) {
            testFile = args[1];
        }
        if (args.length >= 3) {
	    outFile = args[2];
	}

	try {
	    UtteranceTest test = new UtteranceTest
		("test", configPath, testFile, outFile);
	    
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

