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
 * Defines the basic Node for any graph.
 */
public interface Node {
    /**
     * Method to add an incoming edge.
     */
    public void addIncomingEdge (Edge edge);

    /**
     * Start iterator for incoming edges.
     */
    public void startIncomingEdgeIterator();

    /**
     * Whether there are more incoming edges.
     */
    public boolean hasMoreIncomingEdges();

    /**
     * The next incoming edge.
     */
    public Edge nextIncomingEdge();

    /**
     * Method to add an outgoing edge.
     */
    public void addOutgoingEdge (Edge edge);

    /**
     * Start iterator for outgoing edges.
     */
    public void startOutgoingEdgeIterator();

    /**
     * Whether there are more outgoing edges.
     */
    public boolean hasMoreOutgoingEdges();

    /**
     * The next outgoing edge.
     */
    public Edge nextOutgoingEdge();

    /**
     * Method to check the type of a node.
     */
    public boolean isType (String type);

    /**
     * Returns type of a node.
     */
    public NodeType getType();

    /**
     * Returns the ID of a node.
     */
    public String getID();

}
