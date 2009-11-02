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

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;

/**
 * Class used to collapse all equivalent paths in a Lattice.  Results in a Lattices that is deterministic (no Node has
 * Edges to two or more equivalent Nodes), and minimal (no Node has Edge from two or more equivalent Nodes).
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
    
    public void rescore () {
    	
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
    			System.out.println ("Checking node '" + node + "'");
    			
    			Collection<Edge> enteringEdges = node.getEnteringEdges();
    			
    			if (enteringEdges.size() > 1) {
    				System.err.println ("Too many " + enteringEdges.size() + " entering edges to " + node);
    			}

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
}
