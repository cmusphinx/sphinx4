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

package edu.cmu.sphinx.trainer;


import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.model.acoustic.*;


/**
 * Provides mechanisms for accessing a next utterance's file name
 * and transcription.
 */
public interface Transcript {

    /**
     * Prefix for trainer.Transcript SphinxProperties
     */
    String PROP_PREFIX = "edu.cmu.sphinx.trainer.Transcript.";

    /**
     * Simple control file containing file names only.
     */
    String PROP_CONTROL_FILE = PROP_PREFIX + "controlFile";

    /**
     * The default value for the property PROP_CONTROL_FILE.
     */
    String PROP_CONTROL_FILE_DEFAULT = "./input.ctl";

    /**
     * Transcription file containing transcriptions, simple or full.
     */
    String PROP_TRANSCRIPT_FILE = PROP_PREFIX + "transcriptFile";

    /**
     * The default value for the property PROP_TRANSCRIPT_FILE.
     */
    String PROP_TRANSCRIPT_FILE_DEFAULT = "./input.trans";

    /**
     * Initializes the Transcript with the proper context.
     *
     * @param context the context to use
     */
    public void initialize(String context);

    /**
     * Initializes the SimpleTranscript with the proper context.
     *
     * @param context the context to use
     * @param thisPartition the current partition of the transcript file
     * @param numberOfPartitions the total number of partitions
     */
    public void initialize(String context, int thisPartition, 
			   int numberOfPartitions);

    /**
     * Starts the Transcript.
     */
    public void start();

    /**
     * Stops the Transcript.
     */
    public void stop();

    /**
     * Gets the next utterance's full path file name.
     */
    public String getNextUttId();

    /**
     * Gets the next utterance's transcription.
     */
    public String getNextTranscription();

    /**
     * Gets the next utterance's dictionary.
     */
    public String getNextDictionary();

    /**
     * Gets the word separator for the utterance.
     */
    public String wordSeparator();
}
