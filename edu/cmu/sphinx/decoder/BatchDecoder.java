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

import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.util.StreamDataSource;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.NISTAlign;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.util.BatchItem;
import edu.cmu.sphinx.util.BatchManager;
import edu.cmu.sphinx.util.SimpleBatchManager;
import edu.cmu.sphinx.util.PooledBatchManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import java.util.List;
import java.util.Iterator;
import java.util.StringTokenizer;


/**
 * Decodes a batch file containing a list of files to decode.
 * The files can be either audio files or cepstral files, but defaults
 * to audio files.
 */
public class BatchDecoder {

    /**
     *  prefix string for sphinx properties
     */
    private final static String PROP_PREFIX = 
	"edu.cmu.sphinx.decoder.BatchDecoder.";


    /**
     * The SphinxProperty name for how many files to skip for every decode.
     */
    public final static String PROP_SKIP = PROP_PREFIX + "skip";

    
    /**
     * The default value for the property PROP_SKIP.
     */
    public final static int PROP_SKIP_DEFAULT = 0;

    
    /**
     * The SphinxProperty that specified which batch job is to be run.
     *
     */
    public final static String PROP_WHICH_BATCH = PROP_PREFIX + "whichBatch";


    /**
     * The default value for the property PROP_WHICH_BATCH.
     */
    public final static int PROP_WHICH_BATCH_DEFAULT = 0;
    

    /**
     * The SphinxProperty for the total number of batch jobs the decoding 
     * run is being divided into.
     *
     * The BatchDecoder supports running a subset of a batch. 
     * This allows a test to be distributed among several machines.
     *
     */
    public final static String PROP_TOTAL_BATCHES 
	= PROP_PREFIX + "totalBatches";


    /**
     * The default value for the property PROP_TOTAL_BATCHES.
     */
    public final static int PROP_TOTAL_BATCHES_DEFAULT = 1;


    /**
     * The SphinxProperty name for the input data type.
     */
    public final static String PROP_SHOW_PROPS_AT_START = 
        PROP_PREFIX + "showPropertiesAtStart";

    /**
     * The SphinxProperty that defines whether or not the decoder
     * should use the pooled batch manager
     */
    public final static String PROP_USE_POOLED_BATCH_MANAGER = 
        PROP_PREFIX + "usePooledBatchManager";


    /**
     * The default value for the property * PROP_USE_POOLED_BATCH_MANAGER.
     */
    public final static boolean PROP_USE_POOLED_BATCH_MANAGER_DEFAULT = false;


    /**
     * The default value for the property PROP_SHOW_PROPS_AT_START.
     */
    public final static boolean PROP_SHOW_PROPS_AT_START_DEFAULT = false;

    private StreamDataSource dataSource;
    private Decoder decoder;
    private String context;
    private int skip;
    private int whichBatch;
    private int totalBatches;


    private SphinxProperties props;
    private boolean usePooledBatchManager;
    private boolean showPropertiesAtStart;
    private BatchManager batchManager;


    /**
     * Constructs a BatchDecoder.
     *
     * @param context the context of this BatchDecoder
     * @param batchFile the file that contains a list of files to decode
     *
     * @throws IOException if an I/O error is encountered while
     * preparing the batch file
     * @throws InstantiationException if the decoder could not be
     * fully instantiated
     */
    public BatchDecoder(String context, String batchFile) 
        throws IOException, InstantiationException {
        props = SphinxProperties.getSphinxProperties(context);
        context = props.getContext();
        skip = props.getInt(PROP_SKIP, PROP_SKIP_DEFAULT);
        whichBatch = props.getInt(PROP_WHICH_BATCH, PROP_WHICH_BATCH_DEFAULT);
        totalBatches = props.getInt(PROP_TOTAL_BATCHES, 
                                    PROP_TOTAL_BATCHES_DEFAULT);
        usePooledBatchManager = props.getBoolean
            (PROP_USE_POOLED_BATCH_MANAGER, 
             PROP_USE_POOLED_BATCH_MANAGER_DEFAULT);
	showPropertiesAtStart = props.getBoolean(PROP_SHOW_PROPS_AT_START,
                                    PROP_SHOW_PROPS_AT_START_DEFAULT);
        if (usePooledBatchManager) {
            batchManager = new PooledBatchManager(batchFile, skip);
        } else {
            batchManager = new SimpleBatchManager(batchFile, skip,
                                                  whichBatch, totalBatches);
        }

        dataSource = new StreamDataSource();
        dataSource.initialize("StreamDataSource", null, props, null);

	decoder = new Decoder(context);
        decoder.initialize();
        decoder.getRecognizer().getFrontEnd().setDataSource(dataSource);
    }


    /**
     * Decodes the batch of audio files
     *
     * @throws IOException if there is an I/O error processing the
     * batch file
     */
    public void decode() throws IOException {
        BatchItem batchItem;

        batchManager.start();

        if (showPropertiesAtStart) {
            props.list(System.out);
        }

        System.out.println("\nBatchDecoder: decoding files in " +
                           batchManager.getFilename());
        System.out.println("----------");

        while ((batchItem = batchManager.getNextItem()) != null) {
            InputStream is = new FileInputStream(batchItem.getFilename());
            dataSource.setInputStream(is, batchItem.getFilename());
            decoder.decode(batchItem.getTranscript());
        }
        System.out.println("\nBatchDecoder: All files decoded\n");
        Timer.dumpAll(context);
	decoder.showSummary();

        batchManager.stop();
    }


    /**
     * Returns the set of batch results
     *
     * @return a batch result representing the set of runs for this
     * batch decoder.
     */

    public BatchResults getBatchResults() {
        NISTAlign align = decoder.getNISTAlign();
        return new BatchResults(align.getTotalWords(),
                                align.getTotalSentences(),
                                align.getTotalSubstitutions(),
                                align.getTotalInsertions(),
                                align.getTotalDeletions(),
                                align.getTotalSentencesWithErrors());
    }


    /**
     * Main method of this BatchDecoder.
     *
     * @param argv argv[0] : SphinxProperties file
     *             argv[1] : a file listing all the audio files to decode
     */
    public static void main(String[] argv) {

        if (argv.length < 2) {
            System.out.println
                ("Usage: BatchDecoder propertiesFile batchFile");
            System.exit(1);
        }

        String context = "batch";
        String propertiesFile = argv[0];
        String batchFile = argv[1];
        BatchDecoder decoder;

        try {
            URL url = new File(propertiesFile).toURI().toURL();
            SphinxProperties.initContext (context, url);
            decoder = new BatchDecoder(context, batchFile);

        } catch (IOException ioe) {
            System.err.println("I/O error during initialization: \n   " 
                    + ioe.getMessage());
            return;
        } catch (InstantiationException ie) {
            System.err.println("Error during initialization: \n   " 
                    + ie.getMessage());
            return;
        }
        try {
            decoder.decode();
        } catch (IOException ioe) {
            System.err.println("I/O error during decoding: " + 
                    ioe.getMessage());
        }
    }
}
