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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * Provides mechanism for handling a simple utterance.
 */
public class SimpleUtterance implements Utterance{
    private String utteranceID;
    private Collection transcriptSet;
    private Iterator transcriptIterator;

    /**
     * Constructor for class SimpleUtterance.
     */
    public SimpleUtterance() {
	transcriptSet  = new LinkedList();
    }

    /** Constructor for class SimpleUtterance.
     *
     * @param utteranceId the utterance ID, usually a file name.
     */
    public SimpleUtterance(String utteranceID) {
	transcriptSet  = new LinkedList();
    }

    /**
     * Initialize with default dictionary and exact flag.
     *
     * @param dictionary the default dictionary name
     * @param isExact the default flag
     */
    public void initialize(String dictionary, boolean isExact) {
    }

    /**
     * Starts the transcript iterator.
     *
     * @return the transcript iterator.
     */
    public void getTranscriptIterator() {
	transcriptIterator = transcriptSet.iterator();
    }

    /**
     * Returns whether there is a next transcript.
     *
     * @return true if there are more transcrips.
     */
    public boolean hasMoreTranscripts() {
	return transcriptIterator.hasNext();
    }

    /**
     * Gets next transcript.
     *
     * @return the next Trasncript.
     */
    public Transcript nextTranscript() {
	return (Transcript) transcriptIterator.next();
    }
}
