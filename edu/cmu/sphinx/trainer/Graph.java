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
 * Implementation of a graph Graph
 */

public class Graph {

    private List edges; // The list of edges.
    private List nodes; // The list of nodes.
    private Iterator edgeIterator; // The iterator for the list of edges.
    private Iterator nodeIterator; // The iterator for the list of nodes.

    /**
     * The initial node in the graph. This has no incoming edges.
     */
    private Node initialNode;

    /*
     * The final node in the graph. This has no outgoing edges.
     */
    private Node finalNode;

    /**
     * Constructor for class. Creates lists of edges and nodes.
     */
    public Graph() {
        edges = new ArrayList();
        nodes = new ArrayList();
    }

    /**
     * Set the initial node
     */
    public void setInitialNode(Node node) throws IllegalArgumentException {
	if (isNodeInGraph(node)) {
	    initialNode = node;
	} else {
	    throw new IllegalArgumentException("Initial node not in graph");
	}
    }

    /**
     * Set the final node
     */
    public void setFinalNode(Node node) throws IllegalArgumentException {
	if (isNodeInGraph(node)) {
	    finalNode = node;
	} else {
	    throw new IllegalArgumentException("Final node not in graph");
	}
    }

    /**
     * Get the initial node
     */
    public Node getInitialNode(){
        return initialNode;
    }

    /**
     * Get the final node
     */
    public Node getFinalNode(){
        return finalNode;
    }

    /**
     * Returns whether the given node is the initial node in this graph.
     *
     * @param node the node we want to compare
     *
     * @returns if true, the node is the initial node
     */
    public boolean isInitialNode(Node node) {
	return node == initialNode;
    }

    /**
     * Returns whether the given node is the final node in this graph.
     *
     * @param node the node we want to compare
     *
     * @returns if true, the node is the final node
     */
    public boolean isFinalNode(Node node) {
	return node == finalNode;
    }

    /**
     * Link two nodes.
     * If the source or destination nodes are not in the graph,
     * they are added to it. No check is performed to ensure that
     * the nodes are linked to other nodes in the graph.
     */
    public Edge linkNodes(Node sourceNode, Node destinationNode){
        Edge newLink = new Edge(sourceNode, destinationNode);

	sourceNode.addOutgoingEdge(newLink);
	destinationNode.addIncomingEdge(newLink);

        if (!isNodeInGraph(sourceNode)){
	    addNode(sourceNode);
	}

        if (!isNodeInGraph(destinationNode)){
	    addNode(destinationNode);
	}

	addEdge(newLink);

	return newLink;
    }

    /**
     * Add node to list of nodes.
     */
    public void addNode(Node node) {
	nodes.add(node);
    }

    /**
     * Add edge to list of nodes.
     */
    public void addEdge(Edge edge) {
	edges.add(edge);
    }

    /**
     * Check if a node is in the graph.
     */
    public boolean isNodeInGraph(Node node){
	return nodes.contains(node);
    }

    /**
     * Check if an edge is in the graph.
     */
    public boolean isEdgeInGraph(Node edge){
	return edges.contains(edge);
    }

    /**
     * Start iterator for nodes.
     */
    public void startNodeIterator() {
	nodeIterator = nodes.iterator();
    }

    /**
     * Whether there are more nodes.
     */
    public boolean hasMoreNodes() {
	return nodeIterator.hasNext();
    }

    /**
     * Returns next node.
     */
    public Node nextNode() {
	return (Node) nodeIterator.next();
    }

    /**
     * Start iterator for edges.
     */
    public void startEdgeIterator() {
	edgeIterator = edges.iterator();
    }

    /**
     * Whether there are more edges.
     */
    public boolean hasMoreEdges() {
	return edgeIterator.hasNext();
    }

    /**
     * Returns next edge.
     */
    public Edge nextEdge() {
	return (Edge) edgeIterator.next();
    }

    /**
     * Copy a graph to the current graph object.
     *
     * @param graph the graph to copy from
     */
    public void copyGraph(Graph graph) {
	// Make sure the current graph is empty
	assert ((nodes.size() == 0) & (edges.size() == 0));
	for (graph.startNodeIterator();
	     graph.hasMoreNodes(); ) {
	    addNode(graph.nextNode());
	}
	for (graph.startEdgeIterator();
	     graph.hasMoreEdges(); ) {
	    addEdge(graph.nextEdge());
	}
	setInitialNode(graph.getInitialNode());
	setFinalNode(graph.getFinalNode());
    }

    /**
     * Insert a graph in the current graph, replacing a particular node.
     *
     * @param graph the graph to insert
     * @param node the node that this graph will replace
     */
    public void insertGraph(Graph graph, Node node) {
	// Make sure the node belongs to the graph
	assert isNodeInGraph(node) : "Node not in graph";
	for (graph.startNodeIterator();
	     graph.hasMoreNodes(); ) {
	    addNode(graph.nextNode());
	}
	for (graph.startEdgeIterator();
	     graph.hasMoreEdges(); ) {
	    addEdge(graph.nextEdge());
	}
	Node initialNode = graph.getInitialNode();
	for (node.startIncomingEdgeIterator();
	     node.hasMoreIncomingEdges(); ) {
	    Edge edge = node.nextIncomingEdge();
	    edge.setDestination(initialNode);
	    initialNode.addIncomingEdge(edge);
	}
	Node finalNode = graph.getFinalNode();
	for (node.startOutgoingEdgeIterator();
	     node.hasMoreOutgoingEdges(); ) {
	    Edge edge = node.nextOutgoingEdge();
	    edge.setSource(finalNode);
	    finalNode.addOutgoingEdge(edge);
	}
	int index = nodes.indexOf(node);
	nodes.remove(index);
    }

    /**
     * Validate the graph. It checks out basics about the graph, such
     * as whether all nodes have at least one incoming and outgoing
     * edge, except for the initial and final.
     *
     * @return if true, graph validation passed
     */
    public boolean validate() {
	boolean passed = true;
	for (startNodeIterator();
	     hasMoreNodes(); ) {
	    Node node = nextNode();
	    passed &= node.validate();
	    int incoming = node.incomingEdgesSize();
	    int outgoing = node.outgoingEdgesSize();
	    if (incoming < 1) {
		if (!isInitialNode(node)) {
		    System.out.println("No incoming edge: " + node.toString());
		    passed = false;
		}
	    }
	    if (outgoing < 1) {
		if (!isFinalNode(node)) {
		    System.out.println("No outgoing edge: " + node.toString());
		    passed = false;
		}
	    }
	}
	for (startEdgeIterator();
	     hasMoreEdges(); ) {
	    Edge edge = nextEdge();
	    passed &= edge.validate();
	}
	return passed;
    }

    /**
     * Prints out the graph. For debugging purposes.
     */
    public void printGraph() {
	for (startNodeIterator();
	     hasMoreNodes(); ) {
	    Node node = nextNode();
	    if (isInitialNode(node)) {
		System.out.println("Initial Node");
	    }
	    if (isFinalNode(node)) {
		System.out.println("Final Node");
	    }
	    System.out.println(node);
	    node.print();
	}
	for (startEdgeIterator();
	     hasMoreEdges(); ) {
	    Edge edge = nextEdge();
	    System.out.println(edge);
	    edge.print();
	}
    }
}
