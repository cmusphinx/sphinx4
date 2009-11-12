/*
 * Copyright 1999-2002 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.result;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;

/**
 * Class rescore the lattice with the new Language model.
 */

public class LatticeRescorer {

    protected final Lattice lattice;
    protected final LanguageModel model;
    private int depth;
    private float languageWeigth = 8.0f;

    /**
     * Create a new Lattice optimizer
     *
     * @param lattice
     */
    public LatticeRescorer(Lattice lattice, LanguageModel model) {
        this.lattice = lattice;
        this.model = model;
        depth = model.getMaxDepth();
    }

    private boolean isFillerNode (Node node) {
    	return node.getWord().getSpelling().equals("<sil>");
    }
    
    // Ensures each node has unique context
    public void expand() {
    	List<Node> nodeQueue = lattice.sortNodes();
    	
    	while (nodeQueue.size() > 0) {

    		Node node = nodeQueue.remove(0);
    		
    		if (node.getLeavingEdges().size() < 1) {
    			continue;
    		}
    		
    		Collection<Edge> enteringEdges = node.getCopyOfEnteringEdges();
    		if (enteringEdges.size() > 1) {
    			for (Edge e : enteringEdges) {
    				Node newNode = new Node(node.getWord(), node.getBeginTime(), node.getEndTime());
    				lattice.addNode(newNode);
    				lattice.addEdge(e.getFromNode(), newNode, e.getAcousticScore(), e.getLMScore());
    				for (Edge e1 : node.getCopyOfLeavingEdges()) {
    					Node toNode = e1.getToNode();
    					if (!nodeQueue.contains(toNode)) {
   						nodeQueue.add(toNode);
    					}
    					lattice.addEdge(newNode, toNode, e1.getAcousticScore(), e1.getLMScore());
    					lattice.removeEdge(e1);
    				}
    				nodeQueue.add(newNode);
    			}
    			lattice.removeNodeAndEdges(node);
    		}
    	}
    }
    
    public void removeFillers () {    	
    	for (Node node : lattice.sortNodes()) {
    		if (isFillerNode(node)) {
    			lattice.removeNodeAndCrossConnectEdges(node);
    			assert lattice.checkConsistency();
    		}
    	}
    }

    private void rescoreEdges() {
    	for (Edge edge : lattice.edges) {
    		
    		if (isFillerNode(edge.getToNode()))
    			continue;

    		ArrayList<Word> wordList = new ArrayList<Word> ();
    		wordList.add (edge.getToNode().getWord());
    		Node node = edge.fromNode;
    		    		
    		int i = 1;
    		while (true) {
    			if (!isFillerNode(node)) {
    				wordList.add(0, node.getWord());
    				i++;
    			}
    			
    			Collection<Edge> enteringEdges = node.getEnteringEdges();

    			assert enteringEdges.size() <= 1;

    			if (i == depth)
    				break;
    			if (enteringEdges.isEmpty())
    				break;

    			Edge prevEdge = enteringEdges.iterator().next();
    			node = prevEdge.fromNode;
    		}
    		WordSequence seq = new WordSequence (wordList);
    		float prob = model.getProbability(seq) * languageWeigth;
    		System.out.println ("Changing prob of sequence " + seq + " from " + edge.getLMScore() + " to " + prob);
    		edge.setLMScore(prob);
    	}    	
    }
    public void rescore () {
    	
    	removeFillers();
    
        lattice.dumpAISee("before.gdl", "before");

        expand();
    	
        lattice.dumpAISee("after.gdl", "after");

        rescoreEdges ();
    }
}
