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
 * This is a dummy implementation of Graph just so that we can compile.
 */
public interface Graph {

    /**
     * Set the initial node
     */
    public void setInitialNode(Node node) throws IllegalArgumentException;

    /**
     * Set the final node
     */
    public void setFinalNode(Node node);

    /**
     * Get the initial node
     */
    public Node getInitialNode();

    /**
     * get the final node
     */
    public Node getFinalNode();

    /**
     * Link two nodes.
     */
    public Edge linkNodes(Node sourceNode, Node destinationNode);

    /**
     * Check if a node is in the graph.
     */
    public boolean isNodeInGraph(Node node);

    /**
     * Check if an edge is in the graph.
     */
    public boolean isEdgeInGraph(Node edge);

    /**
     * Start iterator for nodes.
     */
    public void startNodeIterator();

    /**
     * Whether there are more nodes.
     */
    public boolean hasMoreNodes();

    /**
     * Returns next node.
     */
    public Node nextNode();

    /**
     * Start iterator for edges.
     */
    public void startEdgeIterator();

    /**
     * Whether there are more edges.
     */
    public boolean hasMoreEdges();

    /**
     * Returns next edge.
     */
    public Edge nextEdge();
}
