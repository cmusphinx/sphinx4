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

import edu.cmu.sphinx.knowledge.dictionary.Dictionary;
import edu.cmu.sphinx.knowledge.dictionary.Pronunciation;

import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;

public class BuildTranscriptHMM {
    private Graph wordGraph;
    private Graph phonemeGraph;
    private Graph contextDependentPhoneGraph;
    private Graph hmmGraph;
    private Map dictionaryMap;
    private String context;
    private TrainerDictionary dictionary;

    /**
     * Constructor for class BuildTranscriptHMM. When called, this
     * method creates graphs for the transcript at several levels of
     * detail, subsequently mapping from a word graph to a phone
     * graph, to a state graph.
     */
    public BuildTranscriptHMM(String context, Transcript transcript) {

	wordGraph = buildWordGraph(transcript);
	assert wordGraph.validate() : "Word graph not validated";
	phonemeGraph = buildPhonemeGraph(wordGraph);
	assert phonemeGraph.validate() : "Phone graph not validated";
	contextDependentPhoneGraph = 
	    buildContextDependentPhonemeGraph(phonemeGraph);
	assert contextDependentPhoneGraph.validate() : 
	    "Context dependent graph not validated";
	hmmGraph = buildHMM(contextDependentPhoneGraph);
	//	assert hmmGraph.validate() : "HMM graph not validated";
    }
    
    /**
     * Returns the graph.
     *
     * @return the graph.
     */
    public Graph getGraph() {
	return wordGraph;
    }

    /*
     * Build a word graph from this transcript
     */
    private Graph buildWordGraph(Transcript transcript){
        Graph wordGraph;
	dictionary = (TrainerDictionary) transcript.getDictionary();

	transcript.startWordIterator();
        int numWords = transcript.numberOfWords();
	/* Shouldn't node and edge be part of the graph class? */

        /* The wordgraph must always begin with the <s> */
        wordGraph = new Graph();
	Node initialNode = new Node(NodeType.UTTERANCE_BEGIN);
	wordGraph.addNode(initialNode);
	wordGraph.setInitialNode(initialNode);
	
        if (transcript.isExact()) {
	    Node prevNode = initialNode;
	    for (transcript.startWordIterator();
		 transcript.hasMoreWords(); ) {
	        /* create a new node for the next word */
	        Node wordNode = new Node(NodeType.WORD, 
						    transcript.nextWord());
		/* Link the new node into the graph */
		wordGraph.linkNodes(prevNode, wordNode);
		
		prevNode = wordNode;
	    }
	    /* All words are done. Just add the </s> */
	    Node wordNode = new Node(NodeType.UTTERANCE_END);
	    wordGraph.linkNodes(prevNode, wordNode);
	    wordGraph.setFinalNode(wordNode);
	} else {
	    /* Begin the utterance with a loopy silence */
	    Node silLoopBack = 
		new Node(NodeType.SILENCE_WITH_LOOPBACK);
	    wordGraph.linkNodes(initialNode, silLoopBack);
	    
	    Node prevNode = initialNode;

	    // Create links with words from the transcript
	    for (transcript.startWordIterator();
		 transcript.hasMoreWords(); ) {
	        String word = transcript.nextWord();
		Pronunciation[] pronunciations = 
		    dictionary.getPronunciations(word, null);
		int numberOfPronunciations = pronunciations.length;
		
		Node[] pronNode = new Node[numberOfPronunciations];

		// Create node at the beginning of the word
		Node dummyWordBeginNode = new Node(NodeType.DUMMY);
		// Allow the silence to be skipped
		wordGraph.linkNodes(prevNode, dummyWordBeginNode);
		// Link the latest silence to the dummy too
		wordGraph.linkNodes(silLoopBack, dummyWordBeginNode);
		// Add word ending dummy node
	        Node dummyWordEndNode = new Node(NodeType.DUMMY);
		// Update prevNode
		prevNode = dummyWordEndNode;
		for (int i = 0; i < numberOfPronunciations; i++){
		    String wordAlternate = pronunciations[i].getWord();
		    if (i > 0) {
			wordAlternate += "(" + i + ")";
		    }
	            pronNode[i] = new Node(NodeType.WORD, wordAlternate);
		    wordGraph.linkNodes(dummyWordBeginNode, pronNode[i]);
	            wordGraph.linkNodes(pronNode[i], dummyWordEndNode);
		}

		/* Add silence */
	        silLoopBack = new
		    Node(NodeType.SILENCE_WITH_LOOPBACK);
	        wordGraph.linkNodes(dummyWordEndNode, silLoopBack);

            }
	    Node wordNode = new Node(NodeType.UTTERANCE_END);
	    // Link previous node, a dummy word end node
	    wordGraph.linkNodes(prevNode, wordNode);
	    // Link also the previous silence node
	    wordGraph.linkNodes(silLoopBack, wordNode);
	    wordGraph.setFinalNode(wordNode);
	}
        return wordGraph;
    }


    /**
     * Convert word graph to phoneme graph
     */
    private Graph buildPhonemeGraph(Graph wordGraph) {
	Edge edge;
	boolean notTraversed = false;
	Graph phonemeGraph = new Graph();
	phonemeGraph.copyGraph(wordGraph);

	// We have to go through all this convoluted thing to avoid a
	// "ConcurrentModificationException". The way we avoid is, we
	// loop until we find a node that we want to replace with
	// something more detailed. We get out of the loop, replace
	// it, and start again. We're careful to mark what had already
	// been done. In this case, we mark it by replace the node
	// type from "WORD" to "PHONE".
	do {
	    // Repeat the loop until we encounter a WORD.
	    for (phonemeGraph.startNodeIterator(); 
		 phonemeGraph.hasMoreNodes(); ) {
		Node node = phonemeGraph.nextNode();
		// If a word already got converted, the node type is
		// PHONE.
		if (node.getType().equals(NodeType.WORD)) {
		    String word = node.getID();
		    Graph pronunciationGraph = dictionary.getWordGraph(word);
		    phonemeGraph.insertGraph(pronunciationGraph, node);
		    notTraversed = true;
		    break;
		}
		// If we traversed everything, we'll reach this and
		// then leave.
		notTraversed = false; 
	    }
	} while (notTraversed);
        return phonemeGraph;
    }

    /**
     * Convert phoneme graph to a context sensitive phoneme graph.
     * This graph expands paths out to have separate phoneme nodes for
     * phonemes in different contexts
     */
    public Graph buildContextDependentPhonemeGraph(Graph phonemeGraph) {
	// TODO: Dummy stub for now - return a copy of the original graph
	Graph cdGraph = new Graph();
	cdGraph.copyGraph(phonemeGraph);
        return cdGraph;
    }

    /**
     * Convert the phoneme graph to an HMM
     */
    public Graph buildHMM (Graph PhonemeGraph)
    {
	// TODO: Much is missing here
         return new Graph();
    }
}
