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


package edu.cmu.sphinx.research.distributed.tests;

import edu.cmu.sphinx.decoder.BatchDecoder;

import edu.cmu.sphinx.research.distributed.client.ClientFrontEnd;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.NISTAlign;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import java.text.DecimalFormat;


/**
 * A client-side BatchDecoder. It talks to a ServerDeocder to decode
 * a list of files in a batch file.
 */
public class BatchClient extends BatchDecoder {

    private static DecimalFormat timeFormat = new DecimalFormat("0.00");

    private ClientFrontEnd clientFrontEnd;

    private Timer decodeTimer;
    private long cumulativeProcessingTime = 0;

    private NISTAlign aligner;


    /**
     * Constructs a BatchClient with the given name and context.
     */
    public BatchClient(String context, String batchFile) throws IOException {
        super(context, batchFile, false);
        SphinxProperties props = SphinxProperties.getSphinxProperties(context);
        clientFrontEnd = new ClientFrontEnd();
        clientFrontEnd.initialize("BatchClient", context);
        decodeTimer = Timer.getTimer(context, "BatchClientDecode");
        aligner = new NISTAlign();
    }


    /**
     * Decodes the batch of audio files
     */
    public void decode() throws IOException {
        clientFrontEnd.connect();
        super.decode();
        clientFrontEnd.close();
    }


    /**
     * Decodes the given file.
     *
     * @param file the file to decode
     * @param ref the reference string (or null if not available)
     */
    public void decodeFile(String file, String reference) throws IOException {

        InputStream is = new FileInputStream(file);

        decodeTimer.start();
        String result = clientFrontEnd.decode(is, file);        
        decodeTimer.stop();

        boolean match = aligner.align(reference, result);
        aligner.printSentenceSummary();
        aligner.printTotalSummary();

        long processingTime = decodeTimer.getCurTime();
        cumulativeProcessingTime += processingTime;

        System.out.println("   This Time Proc: " + 
                           timeFormat.format(processingTime/1000.f));
        System.out.println("   Total Time Proc: " + 
                           timeFormat.format(cumulativeProcessingTime/1000.f));
        System.out.println("----------");
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
                ("Usage: BatchClient propertiesFile batchFile");
            System.exit(1);
        }

        String context = "batch";
        String propertiesFile = argv[0];
        String batchFile = argv[1];
        String pwd = System.getProperty("user.dir");

        try {
            SphinxProperties.initContext
                (context, new URL("file://" + pwd + "/" + propertiesFile));

            BatchClient client = new BatchClient(context, batchFile);
            client.decode();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}
