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

/**
 * Provides mechanisms for accessing an utterance.
 */
public interface Utterance {

    /**
     * Prefix for trainer.Utterance SphinxProperties
     */
    String PROP_PREFIX = "edu.cmu.sphinx.trainer.Utterance.";

    /**
     * Initialize with default dictionary and exact flag.
     *
     * @param dictionary the default dictionary name
     * @param isExact the default flag
     */
    public void initialize(String dictionary, boolean isExact);

    /**
     * Gets the transcript iterator.
     */
    public void getTranscriptIterator();

    /**
     * Returns whether there is a next transcript.
     */
    public boolean hasMoreTranscripts();

    /**
     * Returns next transcript.
     */
    public Transcript nextTranscript();

}
