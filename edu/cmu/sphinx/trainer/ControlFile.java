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

package edu.cmu.sphinx.trainer;



/**
 * Provides mechanisms for reading a control file (or a pair control
 * file plus transcription file).
 */
public interface ControlFile {

    /**
     * Prefix for trainer.ControlFile SphinxProperties
     */
    String PROP_PREFIX = "edu.cmu.sphinx.trainer.ControlFile.";

    /**
     * Simple control file containing audio file names only.
     */
    String PROP_AUDIO_FILE = PROP_PREFIX + "audioFile";

    /**
     * The default value for the property PROP_AUDIO_FILE.
     */
    String PROP_AUDIO_FILE_DEFAULT = "train.ctl";

    /**
     * Transcription file containing transcriptions, simple or full.
     */
    String PROP_TRANSCRIPT_FILE = PROP_PREFIX + "transcriptFile";

    /**
     * The default value for the property PROP_TRANSCRIPT_FILE.
     */
    String PROP_TRANSCRIPT_FILE_DEFAULT = "train.trans";

    /**
     * The SphinxProperty name for which batch partition to process.
     */
    public final static String PROP_WHICH_BATCH = PROP_PREFIX + "whichBatch";

    /**
     * The default value for the whichBatch SphinxProperty.
     */
    public final static int PROP_WHICH_BATCH_DEFAULT = 1;

    /**
     * The SphinxProperty name for the total number of batch partitions.
     */
    public final static String PROP_TOTAL_BATCHES 
	= PROP_PREFIX + "totalBatches";

    /**
     * The default value of the totalBatches SphinxProperty.
     */
    public final static int PROP_TOTAL_BATCHES_DEFAULT = 1;

    /**
     * Initializes the ControlFile with the proper context.
     *
     * @param context the context to use
     */
    public void initialize(String context);

    /**
     * Gets an iterator for utterances.
     */
    public void startUtteranceIterator();

    /**
     * Returns whether there is a next utterance.
     */
    public boolean hasMoreUtterances();

    /**
     * Returns next utterance.
     */
    public Utterance nextUtterance();

}
