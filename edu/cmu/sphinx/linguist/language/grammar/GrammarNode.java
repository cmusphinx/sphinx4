
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


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.sphinx.linguist.dictionary.Word;



/**
 * Represents a grammar node in a grammar. A {@link Grammar grammar} is
 * represented as a graph of grammar nodes and {@link GrammarArc arcs}.
 * A grammar node usually represents a word or words, but it can also be
 * a transition point or simply silence.
 *
 * Note that all probabilties are maintained in the LogMath log base
 */
public class GrammarNode implements Serializable {

    private int identity;			// the node id
    private boolean isFinal;			// is this the final node?

    private Word[][] alternatives;              // ordered words at this node
    private List arcList = new ArrayList();  	// arcs to successors


    /**
     * Creates a GrammarNode with the given ID, Words.
     * A GrammarNode with words is, by default, neither a silence 
     * nor a final node. 
     *
     * @param id the identity of this GrammarNode
     * @param alternatives the set of Words in this GrammarNode. This
     * is a two dimensional array, the first index corresponds to the
     * set of alternative choices, the second index corresponds to a
     * particular word for the alternative
     */
    public GrammarNode(int id, Word[][] alternatives) {
        this(id, false);
        this.alternatives = alternatives;
    }
    

    /**
     * Creates a GrammarNode with the given ID and silence or final
     * attributes. A silence or final node does not have any
     * words by default.
     *
     * @param id the identity of this GrammarNode
     * @param isFinal if true this is a final node
     */
    protected GrammarNode(int id, boolean isFinal) {
        this.identity = id;
        this.isFinal = isFinal;
        this.alternatives = new Word[0][0];
    }


    /**
     * Returns the ID of this GrammarNode.
     *
     * @return the ID of this GrammarNode
     */
    public int getID() {
        return identity;
    }


    /**
     * Retrieves the words associated with this grammar node
     *
     * @return the words associated with this grammar node
     */
    public Word[][] getAlternatives() {
        return alternatives;
    }


    /**
     * Optimize this grammar node.
     */
    void optimize() {
        for (int i = 0; i < arcList.size(); i++) {
            GrammarArc arc = (GrammarArc) arcList.get(i);
            arcList.set(i, optimizeArc(arc));
        }
    }

    /**
     * Optimize the given arc. If an arc branches to an empty node
     * that has only one exit, the node can be bypassed by making a
     * new arc that skips the nodes. This can happen multiple times.
     *
     * @param arc the arc to optimize
     *
     * @return the optimized arc
     */
    GrammarArc optimizeArc(GrammarArc arc) {
        GrammarNode nextNode = arc.getGrammarNode();
        while (nextNode.isEmpty() && nextNode.arcList.size() == 1) {
            GrammarArc nextArc = (GrammarArc) nextNode.arcList.get(0);
            arc = new GrammarArc(nextArc.getGrammarNode(),
                arc.getProbability() + nextArc.getProbability());
            nextNode = arc.getGrammarNode();
        }
        return arc;
    }

    /**
     * Retrieves the words associated with a specific alternative 
     *
     * @param alternative the index of the alternative
     *
     * @return the words associated with this grammar node
     */
    public Word[] getWords(int alternative) {
        return alternatives[alternative];
    }

    /**
     * Retrieve the single word associated with this grammar
     *
     * @return the word associated with this grammar node
     */
    public Word getWord() {
        return alternatives[0][0];
    }

    /**
     * Gets the number of alternatives
     *
     * @return the number of alternatives
     */
    public int getNumAlternatives() {
	return alternatives.length;
    }

    /**
     * Determines if this grammar node is empty (that is, has no
     * words).
     *
     * @return <code>true</code> if the node is empty, otherwise
     * <code>false</code>.
     */
    public boolean isEmpty() {
        return getNumAlternatives() == 0;
    }


    /**
     * Retrieves the set of transitions out of this node
     *
     * @return the transitions to the successors for this node.
     */
    public GrammarArc[] getSuccessors() {
        return (GrammarArc[]) arcList.toArray(new GrammarArc[arcList.size()]);
    }


    /**
     * Determines if this grammar node is a final node
     * in the grammar
     *
     * @return true if the node is a final node in the grammar
     */
    public boolean isFinalNode() {
        return isFinal;
    }


    /**
     * Sets the 'final' state of the grammar node.  A 'final' state
     * grammar marks the end of a grammar
     *
     * @param isFinal if <code>true</code> the grammar node is a final
     * node.
     */

    public void setFinalNode(boolean isFinal) {
        this.isFinal = isFinal;
    }


    /**
     * Adds an arc to the given node 
     *
     * @param node the node that this new arc goes to
     * @param logProbability the log probability of the transition occuring
     */
    public void add(GrammarNode node, float logProbability) {
        arcList.add(new GrammarArc(node, logProbability));
    }


    /**
     * Returns the string representation of this object
     */
    public String toString() {
	return "G" + getID();
    }


    
    /**
     * Dumps this GrammarNode as a String.
     *
     * @param level the indent level
     * @param visitedNodes the set of visited nodes
     * @param logProb the probability of the transition (in logMath
     * log domain)
     */
    private String traverse(int level, Set visitedNodes, float logProb) {
        String dump = "";
        
        for (int i = 0; i < level; i++) {
            dump += ("    ");
        }
        
        dump += "N("+ getID() +"):";
	dump += "p:" + logProb;
        
        if (isFinalNode()) {
            dump += (" !");
        }
        
        Word[][] alternatives = getAlternatives();
        for (int i = 0; i < alternatives.length; i++) {
	    for (int j = 0; j < alternatives[i].length; j++) {
		dump += (" " + alternatives[i][j].getSpelling());
	    }
	    if (i < alternatives.length - 1) {
		dump += "|";
	    }
        }
        
	System.out.println(dump);
        
        // Visit the children nodes if this node has never been visited.
        
        if (!isFinalNode() && !(visitedNodes.contains(this))) {
            
            visitedNodes.add(this);
            GrammarArc[] arcs = getSuccessors();
            
            for (int i = 0; i < arcs.length; i++) {
                GrammarNode child = arcs[i].getGrammarNode();
                child.traverse(level+1, visitedNodes,
			arcs[i].getProbability());
            }
        } else if (isFinalNode()) {
            
            // this node has no children, so just add it to the visitedNodes
            visitedNodes.add(this);
        }
        
        return dump;
    }

    /**
     * Traverse the grammar and dump out the nodes and arcs in GDL
     *
     * @param out print the gdl to this file
     * @param visitedNodes the set of visited nodes
     *
     * @throws IOException if an error occurs while writing the file
     */
    private void traverseGDL(PrintWriter out, Set visitedNodes) 
	    throws IOException {
        
        // Visit the children nodes if this node has never been visited.
        
        if (!(visitedNodes.contains(this))) {
            visitedNodes.add(this);
	    out.println("   node: { title: "  + getGDLID(this) +  
			" label: "+ getGDLLabel(this) + 
                        " shape: " + getGDLShape(this) +
			" color: " + getGDLColor(this) + "}");
            GrammarArc[] arcs = getSuccessors();
            for (int i = 0; i < arcs.length; i++) {
                GrammarNode child = arcs[i].getGrammarNode();
		float prob = arcs[i].getProbability();
	        out.println("   edge: { source: " 
				+ getGDLID(this) +
				" target: " + getGDLID(child) + 
				 " label: \"" + prob + "\"}");
                child.traverseGDL(out, visitedNodes);
            }
        } 
    }

    /**
     * Gvien a node, return a GDL ID for the node
     *
     * @param node the node
     *
     * @return the GDL id
     */
    String getGDLID(GrammarNode node) {
        return "\"" + node.getID() + "\"";
    }

    /**
     * Given a node, returns a GDL Label for the node
     *
     * @param node the node 
     *
     * @return a gdl label for the node
     */
    String getGDLLabel(GrammarNode node) {
	String label = node.isEmpty() ? "" : node.getWord().getSpelling();
        return "\"" + label + "\"";
    }

    /**
     * Given a node, returns a GDL shape for the node
     *
     * @param node the node 
     *
     * @return a gdl shape for the node
     */
    String getGDLShape(GrammarNode node) {
	return node.isEmpty() ? "circle" : "box";
    }


    /**
     * Gets the color for the grammar node
     *
     * @param node the node of interest
     *
     * @return the gdl label for the color
     */
    String getGDLColor(GrammarNode node) {
	String color = "lightgrey";
	if (node.isFinalNode()) {
	    color = "red";
	} else if (!node.isEmpty()) {
	    color = "lightgreen";
	}
	return color;
    }

    /**
     * Dumps the grammar in GDL form
     *
     * @param path the path to write the gdl file to
     */
    public void dumpGDL(String path) {
	try  {
	    PrintWriter out = new PrintWriter(new FileOutputStream(path));
	    out.println("graph: {");
	    out.println("    orientation: left_to_right");
	    out.println("    layout_algorithm: dfs");
	    traverseGDL(out, new HashSet());
	    out.println("}");
	    out.close();
	} catch (FileNotFoundException fnfe) {
	    System.out.println("Can't write to " + path + " " + fnfe);
	} catch (IOException ioe) {
	    System.out.println("Trouble writing to " + path + " " + ioe);
	}
    }

    /**
     * Dumps the grammar
     */
    public void dump() {
	 System.out.println(traverse(0, new HashSet(), 1.0f));
    }
}
