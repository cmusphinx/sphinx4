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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Defines the basic Node for any graph
 * A generic graph Node must have a list of outgoing edges
 * and an identifier.
 */
public class SentenceHMMNode implements Node {

    /**
     * The identifier for this Node
     */
    private String nodeId;

    /**
     * The type of node, such as a dummy node or node represented by a
     * specific type of symbol
     */
    private NodeType nodeType;

    /**
     * The list of incoming edges to this node.
     */
    private List incomingEdges;
    private Iterator incomingEdgeIterator;

    /**
     * The list of outgoing edges from this node
     */
    private List outgoingEdges;
    private Iterator outgoingEdgeIterator;

    /**
     * Constructor for sentence HMM node when a type and symbol are given.
     *
     * @param nodeType the type of node.
     * @param nodeSymbol the symbol for this type.
     */
    SentenceHMMNode(NodeType nodeType, String nodeSymbol) {
        incomingEdges = new ArrayList();
        outgoingEdges = new ArrayList();
	this.nodeId = nodeSymbol;
	this.nodeType = nodeType;
    }

    /**
     * Constructor for sentence HMM node when a type is given.
     *
     * @param nodeType the type of node.
     */
    SentenceHMMNode(NodeType nodeType) {
        this(nodeType, (String)null);
    }

    /**
     * Default constructor
     */
    SentenceHMMNode (){
    }

    /**
     * Method to add an incoming edge.
     * Note that we do not check if the destination node of the
     * incoming edge is identical to this node
     */
    public void addIncomingEdge (Edge edge){
        incomingEdges.add(edge);
    }

    /**
     * Start iterator for incoming edges.
     */
    public void startIncomingEdgeIterator() {
	incomingEdgeIterator = incomingEdges.iterator();
    }

    /**
     * Whether there are more incoming edges.
     */
    public boolean hasMoreIncomingEdges() {
	return incomingEdgeIterator.hasNext();
    }

    /**
     * The next incoming edge.
     */
    public Edge nextIncomingEdge() {
	return (Edge) incomingEdgeIterator.next();
    }

    /**
     * Method to add an outgoing edge.
     * Note that we do not check if the source node of the outgoing
     * edge is identical to this node
     */
    public void addOutgoingEdge (Edge edge){
        outgoingEdges.add(edge);
    }

    /**
     * Start iterator for outgoing edges.
     */
    public void startOutgoingEdgeIterator() {
	outgoingEdgeIterator = outgoingEdges.iterator();
    }

    /**
     * Whether there are more outgoing edges.
     */
    public boolean hasMoreOutgoingEdges() {
	return outgoingEdgeIterator.hasNext();
    }

    /**
     * The next outgoing edge.
     */
    public Edge nextOutgoingEdge() {
	return (Edge) outgoingEdgeIterator.next();
    }

    /**
     * Method to check the type of a node.
     */
    public boolean isType (String type){
        return (type.equals(this.nodeType));
    }

    /**
     * Returns type of a node.
     */
    public NodeType getType() {
        return nodeType;
    }

    /**
     * Returns the ID of a node.
     */
    public String getID() {
        return nodeId;
    }
}
