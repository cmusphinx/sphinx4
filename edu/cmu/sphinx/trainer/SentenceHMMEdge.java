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
 * Defines the basic Edge for any graph
 * A generic graph edge must have a destination Node
 * and an identifier.
 */

public class SentenceHMMEdge implements Edge {
    /*
     * The identifier for this edge
     */
    public String id;

    /*
     * The source node for this edge
     */
    public Node sourceNode;

    /*
     * The destination node for this edge
     */
    public Node destinationNode;

    /*
     * Default Constructor
     */
    SentenceHMMEdge (Node source, Node destination, String id) {
        this.sourceNode = source;
        this.destinationNode = destination;
	this.id = id;
    }

    /*
     * Constructor given no id.
     */
    SentenceHMMEdge (Node source, Node destination) {
	this(source, destination, (String) null);
    }

    /**
     * Sets the destination node for a given edge.
     */
    public void setDestination(Node node) {
	this.destinationNode = node;
    }

    /**
     * Sets source node for a given edge.
     */
    public void setSource(Node node) {
	this.sourceNode = node;
    }
}
