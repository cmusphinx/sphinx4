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
 * Interface to the UtteranceGraph, a graph of an utterance.
 */
public interface UtteranceGraph {

    /**
     * Add a transcript graph to the current utterance graph.
     *
     * @param transcriptGraph the transcript graph to add
     */
    public void add(Graph transcriptGraph);

}
