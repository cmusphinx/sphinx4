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


package edu.cmu.sphinx.research.distributed.tests;

import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.research.distributed.client.ClientFrontEndImpl;
import edu.cmu.sphinx.util.BatchFile;
import edu.cmu.sphinx.util.NISTAlign;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.TimerPool;
import edu.cmu.sphinx.util.props.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;


/** A client-side BatchDecoder. It talks to a ServerDeocder to decode a list of files in a batch file. */
public class BatchClient implements Configurable {

    @S4String
    private static final String BATCH_FILE = "batchFile";
    private String batchFile;

    @S4Integer(defaultValue = 0)
    public static final String SKIP = "skip";
    private int skip;


    @S4Component(type = ClientFrontEndImpl.class)
    private final static String CLIENT = "client";
    private ClientFrontEndImpl clientFrontEnd;


    private static DecimalFormat timeFormat = new DecimalFormat("0.00");

    private Timer decodeTimer;
    private long cumulativeProcessingTime;

    private NISTAlign aligner;


    public void newProperties(PropertySheet ps) throws PropertyException {
        this.batchFile = ps.getString(BATCH_FILE);

        skip = ps.getInt(SKIP);
        clientFrontEnd = (ClientFrontEndImpl) ps.getComponent(CLIENT);

        decodeTimer = TimerPool.getTimer(this, "BatchClientDecode");
        aligner = new NISTAlign(true, true);
    }


    /**
     * Decodes the batch of audio files
     *
     * @throws DataProcessingException if a data processing error occurs
     * @throws IOException             if an I/O error occurs
     */
    public void decode() throws DataProcessingException, IOException {
        clientFrontEnd.connect();

        int curCount = skip;
        System.out.println("\nBatchDecoder: decoding files in " + batchFile);
        System.out.println("----------");

        for (String line : BatchFile.getLines(batchFile)) {
            String file = BatchFile.getFilename(line);
            String reference = BatchFile.getReference(line);

            if (++curCount >= skip) {
                curCount = 0;
                decodeFile(file, reference);
            }
        }

        System.out.println("\nBatchDecoder: All files decoded\n");
        TimerPool.dumpAll();

        clientFrontEnd.close();
    }


    /**
     * Decodes the given file.
     *
     * @param file      the file to decode
     * @param reference the reference string (or null if not available)
     * @throws DataProcessingException if a data processing error occurs
     * @throws IOException             if an I/O error occurs
     */
    public void decodeFile(String file, String reference)
            throws DataProcessingException, IOException {

        InputStream is = new FileInputStream(file);

        decodeTimer.start();
        String result = clientFrontEnd.decode(is, file);
        decodeTimer.stop();

        aligner.align(reference, result);
        aligner.printSentenceSummary();
        aligner.printTotalSummary();

        long processingTime = decodeTimer.getCurTime();
        cumulativeProcessingTime += processingTime;

        System.out.println("   This Time Proc: " +
                timeFormat.format(processingTime / 1000.f));
        System.out.println("   Total Time Proc: " +
                timeFormat.format(cumulativeProcessingTime / 1000.f));
        System.out.println("----------");
    }


    /**
     * Main method of this BatchDecoder.
     *
     * @param argv argv[0] : SphinxProperties file argv[1] : a file listing all the audio files to decode
     */
    public static void main(String[] argv) {

        if (argv.length < 2) {
            System.out.println
                    ("Usage: BatchClient propertiesFile batchFile");
            System.exit(1);
        }

        String batchFile = argv[1];

        try {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(BATCH_FILE, batchFile);
            BatchClient client = ConfigurationManager.getInstance(BatchClient.class, props);

            client.decode();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
