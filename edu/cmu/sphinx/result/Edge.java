
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
package edu.cmu.sphinx.result;

import edu.cmu.sphinx.result.Lattice;

import java.io.IOException;
import java.io.FileWriter;
import java.io.Writer;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/**
 * Edges are part of Lattices.  They connect Nodes, and contain the score associated with that sequence.
 */
public class Edge {
    protected double acousticScore;
    protected double lmScore;
    protected Node fromNode;
    protected Node toNode;

    /**
     * Create an Edge from fromNode to toNode with acoustic and Language Model scores.
     * @param fromNode
     * @param toNode
     * @param acousticScore
     * @param lmScore
     */
    protected Edge( Node fromNode, Node toNode, double acousticScore, double lmScore ) {
        this.acousticScore = acousticScore;
        this.lmScore = lmScore;
        this.fromNode = fromNode;
        this.toNode = toNode;
    }

    public String toString() {
        return "Edge(" + fromNode + "-->" + toNode + "[" + acousticScore + "," + lmScore + "])";
    }

    /**
     * Internal routine used when creating a Lattice from a .LAT file
     * @param lattice
     * @param tokens
     */
    static void load( Lattice lattice, StringTokenizer tokens ) {

        String from = tokens.nextToken();
        String to = tokens.nextToken();
        int score = Integer.parseInt( tokens.nextToken() );

        Node fromNode = lattice.getNode(from);
        if( fromNode == null ) {
            throw new Error( "Edge fromNode \"" + from + "\" does not exist" );
        }

        Node toNode = lattice.getNode(to);
        if( fromNode == null ) {
            throw new Error( "Edge toNode \"" + to + "\" does not exist" );
        }

        lattice.addEdge(fromNode,toNode,score,0.0);
    }

    /**
     * Internal routine used when dumping a Lattice as a .LAT file
     * @param f
     * @throws IOException
     */
    void dump(PrintWriter f) throws IOException {
        f.println( "edge: " + fromNode.getId() + " " + toNode.getId() + " " + acousticScore + " " + lmScore );
    }

    /**
     * Internal routine used when dumping a Lattice as an AiSee file
     * @param f
     * @throws IOException
     */
    void dumpAISee(FileWriter f) throws IOException {
        f.write( "edge: { sourcename: \"" + fromNode.getId() + "\" targetname: \"" + toNode.getId() + "\" }\n" );
    }

    /**
     * Get the acoustic score associated with an Edge
     * @return the score
     */
    public double getAcousticScore() {
        return acousticScore;
    }

    /**
     * Get the language model score associated with an Edge
     * @return the score
     */
    public double getLMScore() {
        return lmScore;
    }

    /**
     * Get the "from" Node associated with an Edge
     * @return the Node
     */
    public Node getFromNode() {
        return fromNode;
    }

    /**
     * Get the "to" Node associated with an Edge
     * @return the Node
     */
    public Node getToNode() {
        return toNode;
    }

}
