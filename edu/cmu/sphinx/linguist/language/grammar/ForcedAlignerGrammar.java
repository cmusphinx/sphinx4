

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

package edu.cmu.sphinx.linguist.language.grammar;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import edu.cmu.sphinx.util.LogMath;

/**
 * Creates a grammar from a reference sentence. It is a constrained
 * grammar that represents the sentence only.
 *
 * Note that all grammar probabilities are maintained in the LogMath
 * log base
 */
public class ForcedAlignerGrammar extends Grammar {

    private Map nodes = new HashMap();

    /**
     * Create class from reference text (not implemented).
     *
     */
    protected GrammarNode createGrammar()
	{
	GrammarNode initialNode = null;
	initialNode = createGrammarNode(0, true);
	return initialNode;
    }
    /**
     * Creates the grammar
     *
     */
    protected GrammarNode createGrammar(String referenceText)
	throws NoSuchMethodException {
	GrammarNode initialNode = null;
	GrammarNode finalNode = null;
	final float logArcProbability = LogMath.getLogOne();
	String nodeName;

	StringTokenizer tok = new StringTokenizer(referenceText);
	int nodeId = 0;

	// first pass just creates the grammar nodes

	// Create initial node

	int initialID = nodeId++;
        nodeName = "G" + initialID;
	initialNode = createGrammarNode(initialID, false);
	assert initialNode != null;
	nodes.put(nodeName, initialNode);

	// Create a node for each word in the sentence
	while (tok.hasMoreTokens()) {

	    String token;
	    token = tok.nextToken();

	    nodeName = "G" + nodeId;
	    GrammarNode node = (GrammarNode) nodes.get(nodeName);
	    if (node == null) {
		if (false) {
		    System.out.println("Creating "
				       + nodeName + " word is " 
				       + token);
		}
		node = createGrammarNode(nodeId, token);
		nodes.put(nodeName, node);
	    } 
	    nodeId++;
	}

	// create the final node
	finalNode = createGrammarNode(nodeId, true);

	// Now that we have all the grammar nodes, reprocess the nodes

	// Reprocessing is simply adding arcs to nodes, in sequence, with
	// probability of one (linear scale).

	// Second pass, add all of the arcs

	for (int i = 0; i < nodeId; i++) {

	    int thisID = i;
	    int nextID = i + 1;

	    GrammarNode thisNode = get(thisID);
	    GrammarNode nextNode = get(nextID);

	    thisNode.add(nextNode, logArcProbability);
	}


	return initialNode;
    }



    /**
     * Given an id returns the associated grammar node
     *
     * @param id the id of interest
     *
     * @return the grammar node or null if none could be found with
     * the proper id
     */
    private GrammarNode get(int id) {
	String name = "G" + id;
	GrammarNode grammarNode =  (GrammarNode) nodes.get(name);
	if (grammarNode == null) {
	    grammarNode = createGrammarNode(id, false);
	    nodes.put(name, grammarNode);
	}

	return grammarNode;
    }
}
