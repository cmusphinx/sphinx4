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

import edu.cmu.sphinx.frontend.util.StreamAudioSource;
import edu.cmu.sphinx.frontend.util.StreamCepstrumSource;
import edu.cmu.sphinx.frontend.DataSource;

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
import java.util.Iterator;
import java.util.StringTokenizer;


/**
 * Decodes a batch file containing a list of files to decode.
 * The files can be either audio files or cepstral files, but defaults
 * to audio files. To decode cepstral files, set the Sphinx property
 * <code> edu.cmu.sphinx.decoder.BatchDecoder.inputDataType = cepstrum </code>
 */
public class BatchDecoder {


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
    public final static String PROP_INPUT_TYPE = PROP_PREFIX+"inputDataType";


    /**
     * The default value for the property PROP_INPUT_TYPE.
     */
    public final static String PROP_INPUT_TYPE_DEFAULT = "audio";


    private DataSource dataSource;
    private Decoder decoder;
    private String batchFile;
    private String context;
    private String inputDataType;
    private int skip;
    private int whichBatch;
    private int totalBatches;


    /**
     * Constructs a BatchDecoder.
     *
     * @param context the context of this BatchDecoder
     * @param batchFile the file that contains a list of files to decode
     */
    public BatchDecoder(String context, String batchFile) throws IOException {
        SphinxProperties props = SphinxProperties.getSphinxProperties(context);
        init(props, batchFile);
    }


    /**
     * Initialize the SphinxProperties.
     *
     * @param props the SphinxProperties
     */
    private void initSphinxProperties(SphinxProperties props) {
        context = props.getContext();
	inputDataType = props.getString(PROP_INPUT_TYPE, 
                                        PROP_INPUT_TYPE_DEFAULT);
	skip = props.getInt(PROP_SKIP, PROP_SKIP_DEFAULT);
	whichBatch = props.getInt(PROP_WHICH_BATCH, PROP_WHICH_BATCH_DEFAULT);
	totalBatches = props.getInt(PROP_TOTAL_BATCHES, 
                                    PROP_TOTAL_BATCHES_DEFAULT);
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

        initSphinxProperties(props);
        this.batchFile = batchFile;

	if (inputDataType.equals("audio")) {
	    dataSource = new StreamAudioSource
		("batchAudioSource", context, null, null);
	} else if (inputDataType.equals("cepstrum")) {
	    dataSource = new StreamCepstrumSource
		("batchCepstrumSource", context);
	} else {
	    throw new Error("Unsupported data type: " + inputDataType + "\n" +
			    "Only audio and cepstrum are supported\n");
	}

	decoder = new Decoder(context, dataSource);
    }


    /**
     * Decodes the batch of audio files
     */
    public void decode() throws IOException {

	int curCount = skip;
        String file = null;
        String reference = null;

        System.out.println("\nBatchDecoder: decoding files in " + batchFile);
        System.out.println("----------");

	for (Iterator i = getLines(batchFile).iterator(); i.hasNext();) {
	    String line = (String) i.next();


            // skip blank lines
            if (line.length() == 0) {
                continue;
            } else if (line.startsWith("set")) {
                processPropertySetter(line);
            } else if (line.startsWith("reset")) {
                NISTAlign.resetTotals();
            } else if (line.startsWith("go")) {
                NISTAlign.resetTotals();
                if (file != null && reference != null) {
                    decodeFile(file, reference);
                }
            } else {
                file = BatchFile.getFilename(line);
                reference = BatchFile.getReference(line);

                if (++curCount >= skip) {
                    curCount = 0;
                    decodeFile(file, reference);
                }
            }
        }

        System.out.println("\nBatchDecoder: All files decoded\n");
        Timer.dumpAll(context);
	decoder.showSummary();
    }


    /**
     * Sets the sphinx properties given a line of the form
     * set propertyName value
     */
    private void processPropertySetter(String line) {
        StringTokenizer st = new StringTokenizer(line);

        if (st.countTokens() != 3) {
            System.out.println("Bad 'set' in " + line);
        }

        String set = st.nextToken();
        String name = st.nextToken();
        String value = st.nextToken();
        SphinxProperties props = SphinxProperties.getSphinxProperties(context);
        System.out.println("Setting " + name + " to " + value);
        props.setProperty(name, value);
    }


    /**
     * Decodes the given file.
     *
     * @param file the file to decode
     * @param ref the reference string (or null if not available)
     */
    public void decodeFile(String file, String ref) throws IOException {

        System.out.println("\nDecoding: " + file);

	InputStream is = new FileInputStream(file);

	if (inputDataType.equals("audio")) {
	    ((StreamAudioSource) dataSource).setInputStream(is, file);
	} else if (inputDataType.equals("cepstrum")) {
	    boolean bigEndian = Utilities.isCepstraFileBigEndian(file);
	    ((StreamCepstrumSource) dataSource).setInputStream(is, bigEndian);
	}

        decoder.decode(ref);
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
     * Gets the set of lines from the file
     *
     * @param file the name of the file 
     */
    private List getLines(String file) throws IOException {
	List list = BatchFile.getLines(file);

	if (totalBatches > 1) {
	    int linesPerBatch = list.size() / totalBatches;
	    if (linesPerBatch < 1) {
		linesPerBatch = 1;
	    }
	    if (whichBatch >= totalBatches) {
		whichBatch = totalBatches - 1;
	    }
	    int startLine = whichBatch * linesPerBatch;
	    // last batch needs to get all remaining lines
	    if (whichBatch == (totalBatches - 1)) {
		list = list.subList(startLine, list.size());
	    } else {
		list = list.subList(startLine, startLine +
			linesPerBatch);
	    }
	}
	return list;
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
        String pwd = System.getProperty("user.dir");

        try {
            SphinxProperties.initContext
                (context, new URL("file://" + pwd +  "/"
                                   + propertiesFile));

            BatchDecoder decoder = new BatchDecoder(context, batchFile);
            decoder.decode();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
