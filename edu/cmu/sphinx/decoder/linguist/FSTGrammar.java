
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

package edu.cmu.sphinx.decoder.linguist;

import edu.cmu.sphinx.util.ExtendedStreamTokenizer;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.decoder.linguist.Grammar;
import edu.cmu.sphinx.decoder.linguist.GrammarArc;
import edu.cmu.sphinx.decoder.linguist.GrammarNode;
import edu.cmu.sphinx.decoder.linguist.GrammarWord;
import edu.cmu.sphinx.knowledge.dictionary.Dictionary;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads a grammar from a file in 'arpa' grammar format.
 * The ARPA format is like so: <br>
 <code> <br>
 I 2 <br>
 F 0 2.30259 <br>
 T 0 1 <unknown> <unknown> 2.30259 <br>
 T 0 4 wood wood 1.60951 <br>
 T 0 5 cindy cindy 1.60951 <br>
 T 0 6 pittsburgh pittsburgh 1.60951 <br>
 T 0 7 jean jean 1.60951 <br>
 F 1 2.89031 <br>
 T 1 0 , , 0.587725 <br>
 T 1 4 wood wood 0.58785 <br>
 F 2 3.00808 <br>
 T 2 0 , , 0.705491 <br>
 T 2 1 <unknown> <unknown> 0.58785 <br>
 F 3 2.30259 <br>
 T 3 0 <br>
 F 4 2.89031 <br>
 T 4 0 , , 0.587725 <br>
 T 4 6 pittsburgh pittsburgh 0.58785 <br>
 F 5 2.89031 <br>
 T 5 0 , , 0.587725 <br>
 T 5 7 jean jean 0.58785 <br>
 F 6 2.89031 <br>
 T 6 0 , , 0.587725 <br>
 T 6 5 cindy cindy 0.58785 <br>
 F 7 1.28093 <br>
 T 7 0 , , 0.454282 <br>
 T 7 4 wood wood 1.28093  <br>
 </code>
 *
 *
 * Probabilities read in from the FST file are in negative natural log
 * format and are converted to the internal logMath log base
 *
 */
public class FSTGrammar extends Grammar {

    
    /**
     * The SphinxProperty for the location of the FST n-gram file.
     */
    public final static String PROP_PATH
	= "edu.cmu.sphinx.decoder.linguist.FSTGrammar.path";


    /**
     * The default value for PROP_PATH.
     */
    public final static String PROP_PATH_DEFAULT = "default.arpa_gram";


    // TODO: If this property turns out to be worthwhile, turn this
    // into a full fledged sphinx property
    private boolean addInitialSilenceNode = false;

    // TODO: If this property turns out to be worthwhile, turn this
    // into a full fledged sphinx property
    private boolean addOptionalSilence = false;

    private boolean ignoreUnknownTransitions = true;

    private Map nodes = new HashMap();
    private Set expandedNodes = new HashSet();

    /**
     * Create class from reference text (not implemented).
     *
     * @param bogusText dummy variable
     *
     * @throws NoSuchMethogException if called with reference sentence
     */
    protected GrammarNode createGrammar(String bogusText)
	throws NoSuchMethodException {
	throw new NoSuchMethodException("Does not create "
				       + "grammar with reference text");
    }

    /**
     * Creates the grammar.
     *
     * @return the initial node for the grammar.
     */
    protected GrammarNode createGrammar()
	throws IOException, NoSuchMethodException {

	GrammarNode initialNode = null;
	GrammarNode finalNode = null;

	String path = props.getString(PROP_PATH, PROP_PATH_DEFAULT);

	// first pass create the FST nodes
	int maxNodeId = createNodes(path);

	// create the final node:
	finalNode = createGrammarNode(++maxNodeId, Dictionary.SILENCE_SPELLING);
        finalNode.setFinalNode(true);

	// replace each word node with a pair of nodes, which
	// consists of the word node and a new dummy node
	maxNodeId = expandWordNodes(maxNodeId);

	ExtendedStreamTokenizer tok = new ExtendedStreamTokenizer(path, true);

	// Second pass, add all of the arcs

	while (!tok.isEOF()) {
	    String token;
	    tok.skipwhite();
	    token = tok.getString();

	    // System.out.println(token);

	    if (token == null) {
		break;

	    } else if (token.equals("I")) {
		assert initialNode == null;
		int initialID = tok.getInt("initial ID");
		String nodeName = "G" + initialID;

            // TODO: SimpleLinguist requires the initial grammar node
            // to contain a single silence.  We'll do that for now,
            // but once the SimpleLinguist is fixed, this should be
            // returned to its former method of creating an empty
            // initial grammar node
            //          initialNode = createGrammarNode(initialID, false);

		initialNode = createGrammarNode(initialID,
                        Dictionary.SILENCE_SPELLING);
		nodes.put(nodeName, initialNode);

		// optionally add a silence node
		if (addInitialSilenceNode) {
		    GrammarNode silenceNode =
			createGrammarNode(++maxNodeId, 
                                Dictionary.SILENCE_SPELLING);
		    initialNode.add(silenceNode, getLogMath().getLogOne());
		    silenceNode.add(initialNode, getLogMath().getLogOne());
		}

	    } else if (token.equals("T")) {
		int thisID = tok.getInt("this id");
		int nextID = tok.getInt("next id");

		GrammarNode thisNode = get(thisID);
		GrammarNode nextNode = get(nextID);
		
		// if the source node is an FSTGrammarNode, we want
		// to join the endNode to the destination node
		
                if (hasEndNode(thisNode)) {
                    thisNode = getEndNode(thisNode);
                }
		
		float lnProb = 0f;        // negative natural log
		String output = tok.getString();
		
		if (output == null || output.equals(",")) {
		    
		    // these are epsilon (meaning backoff) transitions
		    
		    if (output != null && output.equals(",")) {
			tok.getString(); // skip the word
			lnProb = tok.getFloat("probability");
		    }

		    // if the destination node has been expanded
		    // we actually want to add the backoff transition
		    // to the endNode

		    if (hasEndNode(nextNode)) {
			nextNode = getEndNode(nextNode);
		    }
		    
		} else {
		    String word = tok.getString();     // skip words
		    lnProb = tok.getFloat("probability");
		    
		    if (ignoreUnknownTransitions && word.equals("<unknown>")) {
			continue;
		    }
		    /*
		    System.out.println(nextNode.toString() + ": " + output);
		    */
		    assert hasWord(nextNode);
		}
		
		thisNode.add(nextNode, convertProbability(lnProb));
		
	    } else if (token.equals("F")) {
		int thisID = tok.getInt("this id");
		float lnProb = tok.getFloat("probability");
		
		GrammarNode thisNode = get(thisID);
		GrammarNode nextNode = finalNode;

		if (hasEndNode(thisNode)) {
		    thisNode = getEndNode(thisNode);
		}
		
		thisNode.add(nextNode, convertProbability(lnProb));
	    }
	}
	tok.close();

	assert initialNode != null;

	return initialNode;
    }


    /**
     * Reads the FST file in the given path, and creates the
     * nodes in the FST file.
     *
     * @param path the path of the FST file to read
     *
     * @return the highest ID of all nodes
     */
    private int createNodes(String path) throws
            IOException, NoSuchMethodException {
        ExtendedStreamTokenizer tok = new ExtendedStreamTokenizer(path, true);
        int maxNodeId = 0;

        while (!tok.isEOF()) {
            tok.skipwhite();
            String token = tok.getString();

            if (token == null) {
                break;

            } else if (token.equals("T")) {
		tok.getInt("src id");                   // toss source node
                int id = tok.getInt("dest id");         // dest node numb
		if (id > maxNodeId) {
		    maxNodeId = id;
		}

                String word1 = tok.getString();         // get word

                if (word1 == null) {
                    continue;
                }

                String word2 = tok.getString();         // get word
                tok.getString();                        // toss probability

                String nodeName = "G" + id;
                GrammarNode node = (GrammarNode) nodes.get(nodeName);
                if (node == null) {
                    if (word2.equals(",")) {
                        node = createGrammarNode(id, false);
                    } else {
                        node = createGrammarNode(id, word2.toLowerCase());
                    }
                    nodes.put(nodeName, node);
                } else {
		    if (!word2.equals(",")) {
                        /*
                        if (!word2.toLowerCase().equals(getWord(node))) {
                            System.out.println(node.toString() + ": " +
                                               word2 + " " + getWord(node));
                        }
                        */
			assert (word2.toLowerCase().equals(getWord(node)));
		    }
		}
            }
        }
        tok.close();
	return maxNodeId;
    }


    /**
     * Expand each of the word nodes into a pair of nodes, as well as
     * adding an optional silence node between the grammar node and its
     * end node.
     *
     * @param maxNodeId the node ID to start with for the new nodes
     *
     * @return the last (or maximum) node ID
     */
    private int expandWordNodes(int maxNodeID) {
	Collection allNodes = nodes.values();
	String[][] silence = {{Dictionary.SILENCE_SPELLING}};

	for (Iterator i = allNodes.iterator(); i.hasNext();) {
	    GrammarNode node = (GrammarNode) i.next();
	    // if it has at least one word, then expand the node
	    if (node.getNumAlternatives() > 0) {
		GrammarNode endNode = createGrammarNode(++maxNodeID, false);
		node.add(endNode, getLogMath().getLogOne());

                // add an optional silence
                if (addOptionalSilence) {
                    GrammarNode silenceNode = createGrammarNode
                        (++maxNodeID, silence);
                    node.add(silenceNode, getLogMath().getLogOne());
                    silenceNode.add(endNode, getLogMath().getLogOne());
                }
		expandedNodes.add(node);
	    }
	}
	return maxNodeID;
    }


    /**
     * Converts the probability from -ln to logmath
     *
     * @param prob the probability to convert. Probabilities in the
     * arpa format in negative natural log format. We convert them to
     * logmath.
     * 
     * @return the converted probability in logMath log base
     */
    private float convertProbability(float lnProb) {
        return getLogMath().lnToLog(-lnProb);
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

    /**
     * Determines if the node has a word
     *
     * @param node the grammar node of interest
     *
     * @return true if the node has a word
     *
     */
    private boolean hasWord(GrammarNode node) {
	return (node.getNumAlternatives() > 0);
    }

    /**
     * Gets the word from the given grammar ndoe
     *
     * @param node the node of interest
     *
     * @return the word (or null if the node has no word)
     */
    private String getWord(GrammarNode node) {
	String word = null;
	if (node.getNumAlternatives() > 0) {
	    GrammarWord[][] alternatives = node.getAlternatives();
	    word = alternatives[0][0].getSpelling();
	}
	return word;
    }

    /**
     * Determines if the given node has an end node associated with
     * it. 
     *
     * @param node the node of interest
     *
     * @return <code>true</code> if the given node has an end node.
     */
    private boolean hasEndNode(GrammarNode node) {
	return (expandedNodes.contains(node));
    }

    /**
     * Retrieves the end node associated with the given node
     *
     * @param node the node of interest
     *
     * @return the ending node or null if no end node is available
     */
    private GrammarNode getEndNode(GrammarNode node) {
	GrammarArc[] arcs = node.getSuccessors();
	assert arcs != null && arcs.length > 0;
	return arcs[0].getGrammarNode();
    }

}
