package edu.cmu.sphinx.decoder.adaptation.regtree;

public class Node {
	
	private Node parent;
	private Node leftChild;
	private Node rightChild;
	private Cluster cluster;
	
	
	public Node(Node parent, Node leftChild, Node rightChild, Cluster cluster) {
		this.parent = parent;
		this.leftChild = leftChild;
		this.rightChild = rightChild;
		this.cluster = cluster;
	}
	
	public Cluster getCluster() {
		return cluster;
	}
	public void setCluster(Cluster cluster) {
		this.cluster = cluster;
	}
	public Node getParent() {
		return parent;
	}
	public Node getLeftChild() {
		return leftChild;
	}
	public Node getRightChild() {
		return rightChild;
	}

	
}
