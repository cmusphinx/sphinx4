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
import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;
import edu.cmu.sphinx.knowledge.acoustic.HMM;
import edu.cmu.sphinx.knowledge.acoustic.HMMPosition;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.util.LogMath;

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
    private AcousticModel acousticModel;

    /**
     * Constructor for class BuildTranscriptHMM. When called, this
     * method creates graphs for the transcript at several levels of
     * detail, subsequently mapping from a word graph to a phone
     * graph, to a state graph.
     */
    public BuildTranscriptHMM(String context, Transcript transcript, 
			      AcousticModel acousticModel) {

	this.acousticModel = acousticModel;
	wordGraph = buildWordGraph(transcript);
	assert wordGraph.validate() : "Word graph not validated";
	phonemeGraph = buildPhonemeGraph(wordGraph);
	assert phonemeGraph.validate() : "Phone graph not validated";
	contextDependentPhoneGraph = 
	    buildContextDependentPhonemeGraph(phonemeGraph);
	assert contextDependentPhoneGraph.validate() : 
	    "Context dependent graph not validated";
	hmmGraph = buildHMMGraph(contextDependentPhoneGraph);
	assert hmmGraph.validate() : "HMM graph not validated";
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
        Graph graph;
	Dictionary transcriptDict = transcript.getDictionary();
	// Make sure the dictionary is a TrainerDictionary before we cast
	assert 
	    transcriptDict.getClass().getName().endsWith("TrainerDictionary");
	dictionary = (TrainerDictionary) transcriptDict;

	transcript.startWordIterator();
        int numWords = transcript.numberOfWords();
	/* Shouldn't node and edge be part of the graph class? */

        /* The wordgraph must always begin with the <s> */
        graph = new Graph();
	Node initialNode = new Node(NodeType.UTTERANCE_BEGIN);
	graph.addNode(initialNode);
	graph.setInitialNode(initialNode);
	
        if (transcript.isExact()) {
	    Node prevNode = initialNode;
	    for (transcript.startWordIterator();
		 transcript.hasMoreWords(); ) {
	        /* create a new node for the next word */
	        Node wordNode = new Node(NodeType.WORD, 
						    transcript.nextWord());
		/* Link the new node into the graph */
		graph.linkNodes(prevNode, wordNode);
		
		prevNode = wordNode;
	    }
	    /* All words are done. Just add the </s> */
	    Node wordNode = new Node(NodeType.UTTERANCE_END);
	    graph.linkNodes(prevNode, wordNode);
	    graph.setFinalNode(wordNode);
	} else {
	    /* Begin the utterance with a loopy silence */
	    Node silLoopBack = 
		new Node(NodeType.SILENCE_WITH_LOOPBACK);
	    graph.linkNodes(initialNode, silLoopBack);
	    
	    Node prevNode = initialNode;

	    // Create links with words from the transcript
	    for (transcript.startWordIterator();
		 transcript.hasMoreWords(); ) {
	        String word = transcript.nextWord();
		Pronunciation[] pronunciations = 
		    dictionary.getWord(word).getPronunciations(null);
		int numberOfPronunciations = pronunciations.length;
		
		Node[] pronNode = new Node[numberOfPronunciations];

		// Create node at the beginning of the word
		Node dummyWordBeginNode = new Node(NodeType.DUMMY);
		// Allow the silence to be skipped
		graph.linkNodes(prevNode, dummyWordBeginNode);
		// Link the latest silence to the dummy too
		graph.linkNodes(silLoopBack, dummyWordBeginNode);
		// Add word ending dummy node
	        Node dummyWordEndNode = new Node(NodeType.DUMMY);
		// Update prevNode
		prevNode = dummyWordEndNode;
		for (int i = 0; i < numberOfPronunciations; i++){
		    String wordAlternate 
                        = pronunciations[i].getWord().getSpelling();
		    if (i > 0) {
			wordAlternate += "(" + i + ")";
		    }
	            pronNode[i] = new Node(NodeType.WORD, wordAlternate);
		    graph.linkNodes(dummyWordBeginNode, pronNode[i]);
	            graph.linkNodes(pronNode[i], dummyWordEndNode);
		}

		/* Add silence */
	        silLoopBack = new
		    Node(NodeType.SILENCE_WITH_LOOPBACK);
	        graph.linkNodes(dummyWordEndNode, silLoopBack);

            }
	    Node wordNode = new Node(NodeType.UTTERANCE_END);
	    // Link previous node, a dummy word end node
	    graph.linkNodes(prevNode, wordNode);
	    // Link also the previous silence node
	    graph.linkNodes(silLoopBack, wordNode);
	    graph.setFinalNode(wordNode);
	}
        return graph;
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
		    // "false" means graph won't have additional dummy
		    // nodes surrounding the word
		    Graph pronunciationGraph = 
			dictionary.getWordGraph(word, false);
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
    public Graph buildHMMGraph(Graph cdGraph)
    {
	boolean notTraversed = false;
	Graph hmmGraph = new Graph();

	hmmGraph.copyGraph(cdGraph);

	// We have to go through all this shebang again to avoid a
	// "ConcurrentModificationException".We mark what had already
	// been done by replacing the node type from "PHONE" to
	// "STATE".
	do {
	    // Repeat the loop until we encounter a PHONE. Should do
	    // the same for SILENCE?
	    for (cdGraph.startNodeIterator(); 
		 cdGraph.hasMoreNodes(); ) {
		Node node = cdGraph.nextNode();
		// If a word already got converted, the node type is
		// STATE.
		System.out.println(node.getID());
		if (node.getType().equals(NodeType.PHONE)) {
		    Unit unit = acousticModel.lookupUnit(node.getID());
		    HMM hmm = acousticModel.lookupNearestHMM(unit, 
			     HMMPosition.UNDEFINED);
		    Graph modelGraph = buildModelGraph(hmm);
		    modelGraph.printGraph();
		    modelGraph.validate();
		    cdGraph.insertGraph(modelGraph, node);
		    notTraversed = true;
		    break;
		}
		// If we traversed everything, we'll reach this and
		// then leave.
		notTraversed = false; 
	    }
	} while (notTraversed);
	return hmmGraph;
    }

    /**
     * Build a graph given an HMM.
     *
     * @param hmm the HMM
     *
     * @return the graph
     */
    private Graph buildModelGraph(HMM hmm) {
	Graph graph = new Graph();
	Node prevNode;
	Node stateNode;
	float[][] tmat = hmm.getTransitionMatrix();

	prevNode = new Node(NodeType.DUMMY);
	graph.addNode(prevNode);
	graph.setInitialNode(prevNode);

	for (int i = 0; i < hmm.getOrder(); i++) {
	    /* create a new node for the next hmmState */
	    stateNode = new Node(NodeType.STATE, hmm.getUnit().getName());
	    stateNode.setObject(hmm.getState(i));
	    graph.addNode(stateNode);
	    /* Link the new node into the graph */
	    if (i == 0) {
		graph.linkNodes(prevNode, stateNode);
	    }
	    for (int j = 0; j <= i; j++) {
		System.out.println("TMAT: " + j + " " + i + " " + tmat[j][i]);
		if (tmat[j][i] != LogMath.getLogZero()) {
		    // j + 1 to account for the initial dummy node
		    graph.linkNodes(graph.getNode(j + 1), stateNode);
		}
	    }
	    prevNode = stateNode;
	}
	/* All words are done. Just add the final dummy */
	stateNode = new Node(NodeType.DUMMY);
	graph.linkNodes(prevNode, stateNode);
	graph.setFinalNode(stateNode);

	return graph;
    }
}
