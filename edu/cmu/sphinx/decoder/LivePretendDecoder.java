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

package edu.cmu.sphinx.decoder;

import edu.cmu.sphinx.frontend.util.ConcatFileAudioSource;
import edu.cmu.sphinx.frontend.DataSource;

import edu.cmu.sphinx.result.Result;

import edu.cmu.sphinx.util.BatchFile;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.NISTAlign;
import edu.cmu.sphinx.util.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.StringTokenizer;


/**
 * Decodes a batch file containing a list of files to decode.
 * The files can be either audio files or cepstral files, but defaults
 * to audio files. To decode cepstral files, set the Sphinx property
 * <code> edu.cmu.sphinx.decoder.LiveDecoder.inputDataType = cepstrum </code>
 */
public class LivePretendDecoder {

    private final static String PROP_PREFIX = 
	"edu.cmu.sphinx.decoder.LivePretendDecoder.";

    private String context;
    private String batchFile;
    private Decoder decoder;
    private SphinxProperties props;
    private ConcatFileAudioSource dataSource;


    /**
     * Constructs a LivePretendDecoder.
     *
     * @param context the context of this LivePretendDecoder
     * @param batchFile the file that contains a list of files to decode
     */
    public LivePretendDecoder(String context, String batchFile) 
        throws IOException {
        SphinxProperties props = SphinxProperties.getSphinxProperties(context);
        this.context = context;
        init(props, batchFile);
    }

    /**
     * Common intialization code
     *
     * @param props the sphinx properties
     * 
     * @param batchFile the batch file
     */
    private void init(SphinxProperties props, String batchFile) 
        throws IOException {
        this.batchFile = batchFile;
        dataSource = new ConcatFileAudioSource
            ("ConcatFileAudioSource", context, props, batchFile);
        decoder = new Decoder(context, dataSource);
    }

    /**
     * Decodes the batch of audio files
     */
    public void decode() throws IOException {
        int numUtterances = 0;
        List resultList = new LinkedList();
        Result result = null;

        while ((result = decoder.decode()) != null) {
            numUtterances++;
            decoder.calculateTimes(result);
            String resultText = result.getBestResultNoFiller();

            System.out.println("\nHYP: " + resultText);
            System.out.println("   Sentences: " + numUtterances);
            decoder.showAudioUsage();
            decoder.showMemoryUsage();
            resultList.add(resultText);
        }

        alignResults(resultList, dataSource.getReferences());
    }

    /**
     * Align the list of results with reference text.
     */
    private void alignResults(List hypothesisList, List referenceList) {
        System.out.println("Aligning results...");
        System.out.println("   Actual utterances: " + referenceList.size());
        
        getAlignTimer().start();
        String hypothesis = listToString(hypothesisList);
        String reference = listToString(referenceList);
        NISTAlign aligner = decoder.getNISTAlign();
        aligner.align(reference, hypothesis);
        getAlignTimer().stop();

        aligner.printTotalSummary();
    }
    
    /**
     * Converts the given list of strings into one string, putting a space
     * character in between the strings.
     *
     * @param resultList the list of strings
     *
     * @return a string which is a concatenation of the strings in the list,
     *    separated by a space character
     */
    private String listToString(List resultList) {
        StringBuffer sb = new StringBuffer();
        for (Iterator i = resultList.iterator(); i.hasNext(); ) {
            String result = (String) i.next();
            sb.append(result + " ");
        }
        return sb.toString();
    }

    /**
     * Return the timer for alignment.
     */
    private Timer getAlignTimer() {
        return Timer.getTimer(context, "Align");
    }

    /**
     * Main method of this LivePretendDecoder.
     *
     * @param argv argv[0] : SphinxProperties file
     *             argv[1] : a file listing all the audio files to decode
     */
    public static void main(String[] argv) {

        if (argv.length < 2) {
            System.out.println
                ("Usage: LivePretendDecoder propertiesFile batchFile");
            System.exit(1);
        }

        String context = "batch";
        String propertiesFile = argv[0];
        String batchFile = argv[1];

        try {
            URL url = new File(propertiesFile).toURI().toURL();
            SphinxProperties.initContext (context, url);
            LivePretendDecoder decoder = 
                new LivePretendDecoder(context, batchFile);
            decoder.decode();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
