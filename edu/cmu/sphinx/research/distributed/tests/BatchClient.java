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

import edu.cmu.sphinx.research.distributed.client.ClientFrontEnd;
import edu.cmu.sphinx.research.distributed.client.ClientFrontEndImpl;

import edu.cmu.sphinx.frontend.DataProcessingException;

import edu.cmu.sphinx.util.BatchFile;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.NISTAlign;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import java.text.DecimalFormat;

import java.util.Iterator;


/**
 * A client-side BatchDecoder. It talks to a ServerDeocder to decode
 * a list of files in a batch file.
 */
public class BatchClient {

    private final static String PROP_PREFIX = 
	"edu.cmu.sphinx.research.distributed.tests.BatchClient.";


    /**
     * The SphinxProperty name for how many files to skip for every decode.
     */
    public final static String PROP_SKIP = PROP_PREFIX + "skip";

    
    /**
     * The default value for the property PROP_SKIP.
     */
    public final static int PROP_SKIP_DEFAULT = 0;


    private static DecimalFormat timeFormat = new DecimalFormat("0.00");

    private ClientFrontEnd clientFrontEnd;

    private Timer decodeTimer;
    private long cumulativeProcessingTime = 0;
    
    private String context;
    private String batchFile;
    private int skip;

    private NISTAlign aligner;


    /**
     * Constructs a BatchClient with the given name and context.
     *
     * @param context the context to use
     * @param batchFile the batch file to decode
     *
     * @throws InstantiationException if an initialization error occurs
     * @throws IOException if an I/O error occurred
     */
    public BatchClient(String context, String batchFile) 
        throws InstantiationException, IOException {
        this.context = context;
        this.batchFile = batchFile;
        SphinxProperties props = SphinxProperties.getSphinxProperties(context);
	skip = props.getInt(PROP_SKIP, PROP_SKIP_DEFAULT);
        clientFrontEnd = new ClientFrontEndImpl();
        clientFrontEnd.initialize("BatchClient", context);
        decodeTimer = Timer.getTimer(context, "BatchClientDecode");
        aligner = new NISTAlign();
    }


    /**
     * Decodes the batch of audio files
     *
     * @throws DataProcessingException if a data processing error occurs
     * @throws IOException if an I/O error occurs
     */
    public void decode() throws DataProcessingException, IOException {
        clientFrontEnd.connect();

	int curCount = skip;
        System.out.println("\nBatchDecoder: decoding files in " + batchFile);
        System.out.println("----------");

	for (Iterator i = BatchFile.getLines(batchFile).iterator();
             i.hasNext();) {
	    String line = (String) i.next();
            String file = BatchFile.getFilename(line);
	    String reference = BatchFile.getReference(line);

	    if (++curCount >= skip) {
		curCount = 0;
		decodeFile(file, reference);
	    }
        }

        System.out.println("\nBatchDecoder: All files decoded\n");
        Timer.dumpAll(context);
	
        clientFrontEnd.close();
    }


    /**
     * Decodes the given file.
     *
     * @param file the file to decode
     * @param reference the reference string (or null if not available)
     *
     * @throws DataProcessingException if a data processing error occurs
     * @throws IOException if an I/O error occurs
     */
    public void decodeFile(String file, String reference)
        throws DataProcessingException, IOException {

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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
