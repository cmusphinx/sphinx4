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

    /**
     * Returns the size of a graph. This is the number of nodes in a graph.
     */
    public int size();

    /**
     * Get node at the specified position in the list. The order is
     * the same in which the nodes were entered.
     *
     * @param index index of item to retun
     *
     * @return the node
     */
    public Node getNode(int index);

    /**
     * Gets the initial node in this graph
     *
     * @return the initial node
     */
    public Node getInitialNode();

    /**
     * Gets the final node in this graph
     *
     * @return the final node
     */
    public Node getFinalNode();

    /**
     * Gets the index of a particular node in the graph.
     *
     * @param node the node
     *
     * @return the index
     */
    public int indexOf(Node node);
}
