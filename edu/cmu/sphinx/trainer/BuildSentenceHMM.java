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

import edu.cmu.sphinx.knowledge.dictionary.*;

import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;

public class BuildSentenceHMM {
    private Graph wordGraph;
    private Graph phonemeGraph;
    private Graph contextDependentPhoneGraph;
    private Graph sentenceHMMGraph;
    private Dictionary dictionary;
    private Map dictionaryMap;
    private String context;

    /**
     * Constructor for class BuildSentenceHMM.
     * Currently this code sends rockets to the moon,
     * populates remote planets
     * and instantiates time travel.
     * Also provides a cure for SARS.
     */
    public BuildSentenceHMM() {
    }
    
    public void sentenceHMMBuilder (String context, Utterance utterance) {
	Transcript currentTranscript;
	for (utterance.startTranscriptIterator();
	     utterance.hasMoreTranscripts(); ) {
	    /* The transcript object has a pointer to its own dictionary 
	     */
	    Transcript transcript = utterance.nextTranscript();
	    // Well, so that it compiles....
	    this.dictionary = TrainerDictionary.getDictionary(context);
	    wordGraph = buildWordGraph(transcript);
	    phonemeGraph = buildPhonemeGraph(wordGraph);
	    contextDependentPhoneGraph = 
		buildContextDependentPhonemeGraph(phonemeGraph);
	    sentenceHMMGraph = buildHMM(contextDependentPhoneGraph);
	}
    }
    
    /*
     * Build a word graph from this transcript
     */
    private Graph buildWordGraph(Transcript transcript){
        Graph wordGraph;
	// Dictionary dictionary = TrainerDictionary.getDictionary(context);

	transcript.startWordIterator();
        int numWords = transcript.numberOfWords();
	/* Shouldn't node and edge be part of the graph class? */

        /* The wordgraph must always begin with the <s> */
        wordGraph = new SentenceHMMGraph();
	Node initialNode = new SentenceHMMNode(NodeType.UTTERANCE_BEGIN);
	wordGraph.setInitialNode(initialNode);
	
        if (transcript.isExact()) {
	    Node prevNode = initialNode;
	    for (transcript.startWordIterator();
		 transcript.hasMoreWords(); ) {
	        /* create a new node for the next word */
	        Node wordNode = new SentenceHMMNode(NodeType.WORD, 
						    transcript.nextWord());

		/* Link the new node into the graph */
		wordGraph.linkNodes(prevNode, wordNode);
		
		prevNode = wordNode;
	    }
	    /* All words are done. Just add the </s> */
	    Node wordNode = new SentenceHMMNode(NodeType.UTTERANCE_END);
	    wordGraph.linkNodes(prevNode, wordNode);
	    wordGraph.setFinalNode(wordNode);
	} else {
	    /* Begin the utterance with a loopy silence */
	    Node silLoopBack = 
		new SentenceHMMNode(NodeType.SILENCE_WITH_LOOPBACK);
	    wordGraph.linkNodes(initialNode, silLoopBack);
	    
            /* But allow the initial silence to be skipped */
	    Node dummyWordBeginNode = new SentenceHMMNode(NodeType.DUMMY);
	    wordGraph.linkNodes(initialNode, dummyWordBeginNode);
	    
            /* Link the silence to the dummy too */
	    wordGraph.linkNodes(silLoopBack, dummyWordBeginNode);
	    
	    // So that it compiles....
	    for (transcript.startWordIterator();
		 transcript.hasMoreWords(); ) {
	        String word = transcript.nextWord();
		Pronunciation[] pronunciations = 
		    dictionary.getPronunciations(word, null);
		int numberOfPronunciations = pronunciations.length;
		
		Node[] pronNode = new Node[numberOfPronunciations];
		for (int i = 0; i < numberOfPronunciations; i++){
		    // TODO: pronunciationID() method is not correct. Fix
	            pronNode[i] = new 
			SentenceHMMNode(NodeType.WORD, "nada");
					// dictionary.pronunciationID(word,i));
		    wordGraph.linkNodes(dummyWordBeginNode, pronNode[i]);
		}

		/* Add word ending dummy node */
	        Node dummyWordEndNode = new SentenceHMMNode(NodeType.DUMMY);
		for (int i = 0; i < numberOfPronunciations; i++){
	            wordGraph.linkNodes(pronNode[i], dummyWordEndNode);
		}

		/* Add silence */
	        silLoopBack = new
		    SentenceHMMNode(NodeType.SILENCE_WITH_LOOPBACK);
	        wordGraph.linkNodes(dummyWordEndNode, silLoopBack);

                /* But allow the silence to be skipped */
	        dummyWordBeginNode = new SentenceHMMNode(NodeType.DUMMY);
	        wordGraph.linkNodes(dummyWordEndNode, dummyWordBeginNode);

		wordGraph.linkNodes(silLoopBack, dummyWordBeginNode);
            }
	    Node wordNode = new SentenceHMMNode(NodeType.UTTERANCE_END);
	    wordGraph.linkNodes(dummyWordBeginNode, wordNode);
	}
        return wordGraph;
    }


    /**
     * Convert word graph to phoneme graph
     */
    private Graph buildPhonemeGraph(Graph wordGraph) {
	Edge edge;

        for (wordGraph.startNodeIterator(); wordGraph.hasMoreNodes(); ) {
	    Node node = wordGraph.nextNode();
	    if (node.getType().equals(NodeType.WORD)) {
	        String wordAndPronunciation = node.getID();
		// So that it compiles...
	        Graph pronunciationGraph = new SentenceHMMGraph();
		//  dictionary.getPronunciationGraph(wordAndPronunciation);
		//  TODO: Redo as
		//  wordGraph.expandNodeToGraph(node,
		//  pronunciationGraph);
	    	Node source = pronunciationGraph.getInitialNode();
	        Node sink = pronunciationGraph.getFinalNode();

	        for (node.startIncomingEdgeIterator(); 
		     node.hasMoreIncomingEdges(); ) {
	            edge = node.nextIncomingEdge();
		    edge.setDestination(source);
	        }
		// Code for hasMoreOutputEdges not done. Should use iterator
	        for (node.startOutgoingEdgeIterator(); 
		     node.hasMoreOutgoingEdges(); ) {
	            edge = node.nextOutgoingEdge();
		    edge.setSource(sink);
	        }
	    }
	}
	// Is this right? Should we create a new graph inside this method?
        return wordGraph;
    }

    /**
     * Convert phoneme graph to a context sensitive phoneme graph.
     * This graph expands paths out to have separate phoneme nodes for
     * phonemes in different contexts
     */
    public Graph buildContextDependentPhonemeGraph(Graph phonemeGraph) {
	//        int maxLeftContext = dictionary.AcousticModel.maxAcousticLeftContext();
        // int maxRightContext = dictionary.AcousticModel.maxAcousticRightContext();

	// TODO: Dummy stub for now - return the original graph
        return phonemeGraph;
    }

    /**
     * Convert the phoneme graph to an HMM
     */
    public Graph buildHMM (Graph PhonemeGraph)
    {
	// TODO: Much is missing here
         return new SentenceHMMGraph();
    }
}
