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
import edu.cmu.sphinx.frontend.FrontEnd;

import edu.cmu.sphinx.result.Result;

import edu.cmu.sphinx.util.BatchFile;
import edu.cmu.sphinx.util.GapInsertionDetector;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.NISTAlign;
import edu.cmu.sphinx.util.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileWriter;

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

    /**
     * Prefix for the properties of this class.
     */
    public final static String PROP_PREFIX = 
	"edu.cmu.sphinx.decoder.LivePretendDecoder.";

    /**
     * SphinxProperty specifying the transcript file. If a transcript file
     * is specified, it will be created. Otherwise, it is not created.
     */
    public final static String PROP_HYPOTHESIS_TRANSCRIPT = 
        PROP_PREFIX + "hypothesisTranscript";

    /**
     * The default value of PROP_TRANSCRIPT.
     */
    public final static String PROP_HYPOTHESIS_TRANSCRIPT_DEFAULT = null;

    /**
     * SphinxProperty specifying the number of files to decode before
     * alignment is performed.
     */
    public final static String PROP_ALIGN_INTERVAL = 
        PROP_PREFIX + "alignInterval";

    /**
     * The default value of PROP_ALIGN_INTERVAL.
     */
    public final static int PROP_ALIGN_INTERVAL_DEFAULT = -1;


    private int sampleRate;
    private int alignInterval;
    private String context;
    private String batchFile;
    private String hypothesisFile;
    private Decoder decoder;
    private FileWriter hypothesisTranscript;
    private SphinxProperties props;
    private ConcatFileAudioSource dataSource;
    private GapInsertionDetector gapInsertionDetector;


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
        hypothesisFile 
            = props.getString(PROP_HYPOTHESIS_TRANSCRIPT,
                              PROP_HYPOTHESIS_TRANSCRIPT_DEFAULT);
        if (hypothesisFile != null) {
            sampleRate = props.getInt(FrontEnd.PROP_SAMPLE_RATE,
                                      FrontEnd.PROP_SAMPLE_RATE_DEFAULT);
            hypothesisTranscript = new FileWriter(hypothesisFile);
        }
        alignInterval = props.getInt(PROP_ALIGN_INTERVAL, 
                                     PROP_ALIGN_INTERVAL_DEFAULT);
    }

    /**
     * Decodes the batch of audio files
     */
    public void decode() throws IOException {
        int numUtterances = 0;
        List resultList = new LinkedList();
        Result result = null;
        int startReference = 0;

        while ((result = decoder.decode()) != null) {
            numUtterances++;
            decoder.calculateTimes(result);
            String resultText = result.getBestResultNoFiller();

            System.out.println("\nHYP: " + resultText);
            System.out.println("   Sentences: " + numUtterances);
            decoder.showAudioUsage();
            decoder.showMemoryUsage();
            resultList.add(resultText);
            
            if (hypothesisTranscript != null) {
                hypothesisTranscript.write
                    (result.getTimedBestResult(false, true, sampleRate) +"\n");
                hypothesisTranscript.flush();
            }

            if (alignInterval > 0 && (numUtterances % alignInterval == 0)) {
                // perform alignment
                List references = dataSource.getReferences();
                List section =
                    references.subList(startReference, references.size());
                alignResults(resultList, section);
                resultList = new LinkedList();
                startReference = references.size();
            }
        }

        List references = dataSource.getReferences();
        List section = references.subList(startReference, references.size());
        
        if (resultList.size() > 0 || section.size() > 0) {
            alignResults(resultList, section);
        }
        
        detectGapInsertionErrors();

        Timer.dumpAll(context);
        decoder.showSummary();
    }

    /**
     * Detect gap insertion errors.
     */
    private void detectGapInsertionErrors() throws IOException {
        Timer gapTimer = Timer.getTimer(context, "GapInsertionDetector");
        gapTimer.start();
        GapInsertionDetector gid = new GapInsertionDetector
            (dataSource.getTranscriptFile(), hypothesisFile);
        System.out.println();
        System.out.println("# of gap insertion errors: " + gid.detect());
        System.out.println();
        gapTimer.stop();
    }

    /**
     * Align the list of results with reference text.
     */
    private void alignResults(List hypothesisList, List referenceList) {
        System.out.println("Aligning results...");
        System.out.println("   Actual utterances: " + referenceList.size());
        
        String hypothesis = listToString(hypothesisList);
        String reference = listToString(referenceList);
        saveAlignedText(hypothesis, reference);

        getAlignTimer().start();
        NISTAlign aligner = decoder.getNISTAlign();
        aligner.align(reference, hypothesis);
        getAlignTimer().stop();

        System.out.println("...done aligning");
        aligner.printTotalSummary();

    }

    /**
     * Saves the aligned hypothesis and reference text to the aligned
     * text file.
     *
     * @param hypothesis the aligned hypothesis text
     * @param reference the aligned reference text
     */
    private void saveAlignedText(String hypothesis, String reference) {
        try {
            FileWriter writer = new FileWriter("align.txt");
            writer.write(hypothesis);
            writer.write("\n");
            writer.write(reference);
            writer.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
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
     * Returns the time in seconds given the sample number and sample rate.
     *
     * @param sampleNumber the sample number
     * @param sampleRate the sample rate
     *
     * @return the time in seconds
     */
    private static float getSeconds(long sampleNumber, int sampleRate) {
        return ((float) (sampleNumber / sampleRate));
    }

    /**
     * Do clean up
     */
    public void close() throws IOException {
        if (hypothesisTranscript != null) {
            hypothesisTranscript.close();
        }
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
            decoder.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
