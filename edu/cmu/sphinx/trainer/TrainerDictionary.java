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
import edu.cmu.sphinx.knowledge.dictionary.FullDictionary;
import edu.cmu.sphinx.knowledge.dictionary.Pronunciation;
import edu.cmu.sphinx.knowledge.dictionary.WordClassification;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import java.io.IOException;

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
     * Gets a word pronunciation graph.
     *
     * @param word the word
     *
     * @return the graph
     */
    public Graph getWordGraph(String word) {
	Graph wordGraph = new Graph();
	Pronunciation[] pronunciations;
	Unit[] units;
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
	pronunciations = getPronunciations(wordWithoutParentheses, null);
	units = pronunciations[pronunciationID].getUnits();

	// Now, create the graph, where each node contains a single unit
	Node initialNode = new Node(NodeType.DUMMY);
	wordGraph.addNode(initialNode);
	wordGraph.setInitialNode(initialNode);
	
	Node prevNode = initialNode;
	for (int i = 0; i < units.length; i++) {
	    // create a new node for the next unit
	    Node wordNode = new Node(NodeType.PHONE, units[i].getName());
	    // Link the new node into the graph
	    wordGraph.linkNodes(prevNode, wordNode);
	    prevNode = wordNode;
	}
	// All words are done. Just add the final node
	Node wordNode = new Node(NodeType.DUMMY);
	wordGraph.linkNodes(prevNode, wordNode);
	wordGraph.setFinalNode(wordNode);

	return wordGraph;
    }



    // Below here, dummy implementations.
    // We want it to compile :-).

    /**
     * Returns the set of all possible word classifications for this
     * dictionary.
     *
     * @return the set of all possible word classifications
     */
    public WordClassification[] getPossibleWordClassifications() {
	return null;
    }


    /**
     * Dumps out a dictionary
     *
     */
    public void dump() {
    }

    /**
     * Prints out dictionary as a string.
     */
    public String toString() {
	return "DEFAULT";
    }
}
