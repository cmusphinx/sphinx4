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

package edu.cmu.sphinx.trainer;

import java.io.IOException;

import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FullDictionary;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;

/**
 * Dummy trainer dictionary.
 */
public class TrainerDictionary extends FullDictionary {

    static private Dictionary dictionary = null;

    static final String UTTERANCE_BEGIN_SYMBOL = "<s>";
    static final String UTTERANCE_END_SYMBOL = "</s>";
    static final String SILENCE_SYMBOL = "SIL";

    /**
     * Constructor for class.
     *
     * @param context this class's context
     */
    public TrainerDictionary(String context) throws IllegalArgumentException,
            IOException {
	super(context);
    }

    /**
     * Gets a word pronunciation graph. Dummy initial and final states
     * optional.
     *
     * @param word the word
     * @param hasDummy if true, the graph will have dummy initial and
     * final states
     *
     * @return the graph
     */
    public Graph getWordGraph(String word, boolean hasDummy) {
	Graph wordGraph = new Graph();
	Pronunciation[] pronunciations;
	Unit[] units;
	Node prevNode;
	Node wordNode = null;
	int pronunciationID = 0;
	String wordWithoutParentheses = word.replaceFirst("\\(.*\\)", "");

	if (word.equals(wordWithoutParentheses)) {
	    pronunciationID = 0;
	} else {
	    String number = 
		word.replaceFirst(".*\\(", "").replaceFirst("\\)", "");
	    try {
		pronunciationID = Integer.valueOf(number).intValue();
	    } catch (NumberFormatException nfe) {
		new Error("Word with invalid pronunciation ID", nfe);
	    }
	}
	pronunciations 
            = getWord(wordWithoutParentheses).getPronunciations(null);
	if (pronunciations == null) {
	    System.out.println("Pronunciation not found for word " + 
			       wordWithoutParentheses);
	    return null;
	}
	if (pronunciationID >= pronunciations.length) {
	    System.out.println("Dictionary has only " + 
			       pronunciations.length + 
			       " for word " + word);
	    return null;
	}
	units = pronunciations[pronunciationID].getUnits();
	assert units != null: "units is empty: problem with dictionary?";

	// Now, create the graph, where each node contains a single unit
	if (hasDummy) {
	    Node initialNode = new Node(NodeType.DUMMY);
	    wordGraph.addNode(initialNode);
	    wordGraph.setInitialNode(initialNode);
	    prevNode = initialNode;
	} else {
	    prevNode = null;
	}
	for (int i = 0; i < units.length; i++) {
	    // create a new node for the next unit
	    wordNode = new Node(NodeType.PHONE, units[i].getName());
	    if (prevNode == null) {
		wordGraph.addNode(wordNode);
		wordGraph.setInitialNode(wordNode);
	    } else {
		// Link the new node into the graph
		wordGraph.linkNodes(prevNode, wordNode);
	    }
		prevNode = wordNode;
	}
	// All words are done. Just add the final node
	if (hasDummy) {
	    wordNode = new Node(NodeType.DUMMY);
	    wordGraph.linkNodes(prevNode, wordNode);
	}
	assert wordNode != null;
	wordGraph.setFinalNode(wordNode);

	return wordGraph;
    }



    /**
     * Prints out dictionary as a string.
     */
    public String toString() {
	return "DEFAULT";
    }
}
