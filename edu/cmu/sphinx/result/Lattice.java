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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import edu.cmu.sphinx.decoder.search.AlternateHypothesisManager;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;

/**
 * Provides recognition lattice results. Lattices are created from Results
 * which can be partial or final.
 *
 * Lattices describe all theories considered by the Recognizer that have not
 * been pruned out.  Lattices are a directed graph containing Nodes and Edges.
 * A Node that correponda to a theory that a word was spoken over a particular
 * period of time.  An Edge that corresponds to the score of one word following
 * another.  The usual result transcript is the sequence of Nodes though the
 * Lattice with the best scoring path.
 *
 * Lattices are a useful tool for analyzing "alternate results".
 *
 */
public class Lattice {

    protected Node initialNode;
    protected double logBase;
    protected Node terminalNode;
    protected Set edges;
    protected Map nodes;
    AlternateHypothesisManager loserManager;

    {
        edges = new HashSet();
        nodes = new HashMap();
        logBase = Math.exp(1);
    }

    /**
     * Create an empty Lattice.
     */
    public Lattice() {
    }

    /**
     * Create a Lattice from a Result.
     *
     * The Lattice is created from the Token tree referenced by the Result.
     * The Lattice is then optimized to all collapse equivalent paths.
     *
     * @param result the result to convert into a lattice
     * @param logBase the logbase for the probabilities
     */
    public Lattice(Result result, double logBase) {
        initialNode = addNode("0",Dictionary.SENTENCE_START_SPELLING, 0, 0);
        terminalNode = addNode("-1",Dictionary.SENTENCE_END_SPELLING, 0, 0);

        loserManager = result.getAlternateHypothesisManager();

        if (loserManager != null) {
            loserManager.purge();
        }

        for (Iterator it = result.getResultTokens().iterator(); it.hasNext();) {
            Token token = (Token) (it.next());
            processToken(terminalNode, token);
        }

        if (result == null) {
            throw new Error("result is null");
        }
        if (result.getAlternateHypothesisManager() == null) {
            throw new Error("ahm is null");
        }

        this.logBase = logBase;
    }

    protected void processToken(Node thisNode, Token token) {

        assert hasNode(thisNode.getId());
        //assert token.isWord();
        assert thisNode != null;
        assert token != null;

        double thisAcousticScore = token.getScore();
        double thisLMScore = 0.0;

        // test to see if token is processed via a previous node path
        if (hasNode(token)) {
            assert getNode(token).getId().equals( Integer.toString(token.hashCode()) );
            addEdge(getNode(token), thisNode, thisAcousticScore, thisLMScore);
        } else {
            Node newNode = addNode(token);
            addEdge(newNode, thisNode, thisAcousticScore, thisLMScore);

            if (loserManager != null) {
                List list = loserManager.getAlternatePredecessors(token);
                if (list != null) {
                    for (Iterator iter = list.iterator(); iter.hasNext();) {
                        Token predecessor = (Token) iter.next();
                        processToken(newNode, predecessor);
                    }
                }
            }
            Token predecessor = token.getPredecessor();
            if (predecessor != null) {
                processToken(newNode, predecessor);
            }
            else {
                addEdge( initialNode,newNode,thisAcousticScore, thisLMScore );
            }
        }
    }

    /**
     * Create a Lattice from a LAT file.  LAT files are created by
     * the method Lattice.dump()
     *
     * @param fileName
     */
    public Lattice(String fileName) {
        try {
            System.err.println("Loading from " + fileName);

            // load the nodes
            LineNumberReader in = new LineNumberReader(new FileReader(fileName));
            String line;
            while ((line = in.readLine()) != null) {
                StringTokenizer tokens = new StringTokenizer(line);
                if (tokens.hasMoreTokens()) {
                    String type = tokens.nextToken();

                    if (type.equals("edge:")) {
                        Edge.load(this, tokens);
                    } else if (type.equals("node:")) {
                        Node.load(this, tokens);
                    } else if (type.equals("initialNode:")) {
                        setInitialNode(getNode(tokens.nextToken()));
                    } else if (type.equals("terminalNode:")) {
                        setTerminalNode(getNode(tokens.nextToken()));
                    } else if (type.equals("logBase:")) {
                        setLogBase(Double.parseDouble(tokens.nextToken()));
                    } else {
                        throw new Error("SYNTAX ERROR: " + fileName +
                                "[" + in.getLineNumber() + "] " + line);
                    }
                }
            }
            in.close();
        } catch (Exception e) {
            throw new Error(e.toString());
        }
    }

    /**
     * Add an edge from fromNode to toNode.  This method creates the Edge
     * object and does all the connecting
     *
     * @param fromNode
     * @param toNode
     * @param acousticScore
     * @param lmScore
     * @return the new Edge
     */
    public Edge addEdge(Node fromNode, Node toNode,
                        double acousticScore, double lmScore) {
        Edge e = new Edge(fromNode, toNode, acousticScore, lmScore);
        fromNode.addToEdge(e);
        toNode.addFromEdge(e);
        edges.add(e);
        return e;
    }

    /**
     * Add a Node that represents the theory that a given word was spoken
     * over a given period of time.
     *
     * @param word
     * @param beginTime
     * @param endTime
     * @return the new Node
     */
    public Node addNode(String word, int beginTime, int endTime) {
        Node n = new Node(word, beginTime, endTime);
        addNode(n);
        return n;
    }

    /**
     * Add a Node with a given ID that represents the theory that a
     * given word was spoken over a given period of time.
     * This method is used when loading Lattices from .LAT files.
     *
     * @param word
     * @param beginTime
     * @param endTime
     * @return the new Node
     */
    protected Node addNode(String id, String word, int beginTime, int endTime) {
        Node n = new Node(id, word, beginTime, endTime);
        addNode(n);
        return n;
    }

    /**
     * Add a Node corresponding to a Token from the result Token tree.
     * Usually, the Token should reference a search state that is a
     * WordSearchState, although other Tokens may be used for debugging.
     * @param token
     * @return the new Node
     */
    protected Node addNode(Token token) {
        String word;
        if (token.getSearchState() instanceof WordSearchState) {
            word = ((WordSearchState) (token.getSearchState()))
                    .getPronunciation().getWord().getSpelling();
        } else {
            word = token.getSearchState().toString();
        }
        return addNode(Integer.toString(token.hashCode()), word, 0, 0);
    }

    /**
     * Test to see if the Lattice contains a Node
     *
     * @param node
     * @return true if yes
     */
    boolean hasNode(Node node) {
        return nodes.containsValue(node);
    }

    /**
     * Test to see if the Lattice contains an Edge
     *
     * @param edge
     * @return true if yes
     */
    boolean hasEdge(Edge edge) {
        return edges.contains(edge);
    }
    /**
     * Test to see if the Lattice already contains a Node corresponding
     * to a given Token.
     *
     * @param token
     * @return true if yes
     */
    protected boolean hasNode(Token token) {
        return hasNode(Integer.toString(token.hashCode()));
    }

    /**
     * Test to see if the Lattice already has a Node with a given ID
     *
     * @param id
     * @return true if yes
     */
    protected boolean hasNode(String id) {
        return nodes.containsKey(id);
    }


    /**
     * Get the Node associated with an ID
     *
     * @param id
     * @return the Node
     */
    protected Node getNode(String id) {
        return (Node) (nodes.get(id));
    }

    /**
     * Get the node associated with a Token
     *
     * @param token
     * @return the Node
     */
    protected Node getNode(Token token) {
        return getNode(Integer.toString(token.hashCode()));
    }

    /**
     * Add a Node to the set of all Nodes
     *
     * @param n
     */
    protected void addNode(Node n) {
        assert !hasNode(n.getId());
        //System.out.println("Lattice adding node " + n);
        nodes.put(n.getId(), n);
    }

    /**
     * Remove a Node from the set of all Nodes
     *
     * @param n
     */
    protected void removeNode(Node n) {
        assert hasNode(n.getId());
        nodes.remove(n.getId());
    }

    /**
     * Get a copy of the Collection of all Nodes.
     * Used by LatticeOptimizer to avoid Concurrent modification of the
     * nodes list.
     *
     * @return a copy of the collection of Nodes
     */
    protected Collection getCopyOfNodes() {
        return new Vector(nodes.values());
    }

    /**
     * Get the Collection of all Nodes.
     *
     * @return the colllection of all Nodes
     */
    public Collection getNodes() {
        return nodes.values();
    }

    /**
     * Remove an Edge from the set of all Edges.
     * @param e
     */
    protected void removeEdge(Edge e) {
        edges.remove(e);
    }

    /**
     * Get the set of all Edges.
     *
     * @return the set of all edges
     */
    public Collection getEdges() {
        return edges;
    }

    /**
     * Dump the Lattice in the form understood by AiSee
     * (a graph visualization tool).  See http://www.AbsInt.com
     *
     * @param fileName
     * @param title
     */
    public void dumpAISee(String fileName, String title) {
        try {
            System.err.println("Dumping " + title + " to " + fileName);
            FileWriter f = new FileWriter(fileName);
            f.write("graph: {\n");
            f.write("title: \"" + title + "\"\n");
            f.write("display_edge_labels: yes\n");
            /*
            f.write( "colorentry 32: 25 225 0\n");
            f.write( "colorentry 33: 50 200 0\n");
            f.write( "colorentry 34: 75 175 0\n");
            f.write( "colorentry 35: 100 150 0\n");
            f.write( "colorentry 36: 125 125 0\n");
            f.write( "colorentry 37: 150 100 0\n");
            f.write( "colorentry 38: 175 75 0\n");
            f.write( "colorentry 39: 200 50 0\n");
            f.write( "colorentry 40: 225 25 0\n");
            f.write( "colorentry 41: 250 0 0\n");
            f.write( "color: black\n");
            f.write( "orientation: left_to_right\n");
            f.write( "xspace: 10\n");
            f.write( "yspace: 10\n");
            */

            for (Iterator i = nodes.values().iterator(); i.hasNext();) {
                ((Node) (i.next())).dumpAISee(f);
            }
            for (Iterator i = edges.iterator(); i.hasNext();) {
                ((Edge) (i.next())).dumpAISee(f);
            }
            f.write("}\n");
            f.close();
        } catch (IOException e) {
            throw new Error(e.toString());
        }
    }


    /**
     * Dump the Lattice as a .LAT file
     *
     * @param out
     * @throws IOException
     */
    protected void dump(PrintWriter out) throws IOException {
        //System.err.println( "Dumping to " + out );
        for (Iterator i = nodes.values().iterator(); i.hasNext();) {
            ((Node) (i.next())).dump(out);
        }
        for (Iterator i = edges.iterator(); i.hasNext();) {
            ((Edge) (i.next())).dump(out);
        }
        out.println("initialNode: " + initialNode.getId());
        out.println("terminalNode: " + terminalNode.getId());
        out.println("logBase: " + logBase);
        out.flush();
    }

    /**
     * Dump the Lattice as a .LAT file.  Used to save Lattices as
     * ASCII files for testing and experimentation.
     *
     * @param file
     */
    public void dump(String file) {
        try {
            dump(new PrintWriter(new FileWriter(file)));
        } catch (IOException e) {
            throw new Error(e.toString());
        }
    }

    /**
     * Remove a Node and all Edges connected to it.  Also remove those
     * Edges from all connected Nodes.
     *
     * @param n
     */
    protected void removeNodeAndEdges(Node n) {

        //System.err.println("Removing node " + n + " and associated edges");
        for (Iterator i = n.getToEdges().iterator(); i.hasNext();) {
            Edge e = (Edge) (i.next());
            e.getToNode().removeFromEdge(e);
            //System.err.println( "\tRemoving " + e );
            edges.remove(e);
        }
        for (Iterator i = n.getFromEdges().iterator(); i.hasNext();) {
            Edge e = (Edge) (i.next());
            e.getFromNode().removeToEdge(e);
            //System.err.println( "\tRemoving " + e );
            edges.remove(e);
        }
        //System.err.println( "\tRemoving " + n );
        nodes.remove(n.getId());

        assert checkConsistancy();
    }

    /**
     * Remove a Node and cross connect all Nodes with Edges to it.
     *
     * For example given
     *
     * Nodes A, B, X, M, N
     * Edges A-->X, B-->X, X-->M, X-->N
     *
     * Removing and cross connecting X would result in
     *
     * Nodes A, B, M, N
     * Edges A-->M, A-->N, B-->M, B-->N
     *
     * @param n
     */
    protected void removeNodeAndCrossConnectEdges(Node n) {
        System.err.println("Removing node " + n + " and cross connecting edges");
        for (Iterator i = n.getFromEdges().iterator(); i.hasNext();) {
            Edge ei = (Edge) (i.next());
            for (Iterator j = n.getToEdges().iterator(); j.hasNext();) {
                Edge ej = (Edge) (j.next());
                addEdge(ei.getFromNode(), ej.getToNode(),
                        ei.getAcousticScore(), ei.getLMScore());
            }
        }
        removeNodeAndEdges(n);

        assert checkConsistancy();
    }

    /**
     * Get the initialNode for this Lattice.  This corresponds usually to
     * the <s> symbol
     *
     * @return the initial Node
     */
    public Node getInitialNode() {
        return initialNode;
    }

    /**
     * Set the initialNode for this Lattice.  This corresponds usually to
     * the <s> symbol
     *
     * @param p_initialNode
     */
    public void setInitialNode(Node p_initialNode) {
        initialNode = p_initialNode;
    }

    /**
     * Get the terminalNode for this Lattice.  This corresponds usually to
     * the </s> symbol
     *
     * @return the initial Node
     */
    public Node getTerminalNode() {
        return terminalNode;
    }

    /**
     * Set the terminalNode for this Lattice.  This corresponds usually to
     * the </s> symbol
     *
     * @param p_terminalNode
     */
    public void setTerminalNode(Node p_terminalNode) {
        terminalNode = p_terminalNode;
    }

    /**
     * Edge scores are usually log-likelyhood.  Get the log base.
     *
     * @return the log base
     */
    public double getLogBase() {
        return logBase;
    }

    /**
     * Edge scores are usually log-likelyhood.  Set the log base.
     *
     * @param p_logBase
     */
    public void setLogBase(double p_logBase) {
        logBase = p_logBase;
    }

    /**
     * Dump all paths through this Lattice.  Used for debugging.
     */
    public void dumpAllPaths() {
        for (Iterator i = allPaths().iterator(); i.hasNext();) {
            System.out.println(i.next());
        }
    }

    /**
     * Generate a List of all paths through this Lattice.
     *
     * @return a lists of lists of Nodes
     */
    public List allPaths() {
        return allPathsFrom("", initialNode);
    }

    /**
     * Internal routine used to generate all paths starting at a given node.
     *
     * @param path
     * @param n
     * @return a list of lists of Nodes
     */
    protected List allPathsFrom(String path, Node n) {
        String p = path + " " + n.getWord();
        List l = new LinkedList();
        if (n == terminalNode) {
            l.add(p);
        } else {
            for (Iterator i = n.getToEdges().iterator(); i.hasNext();) {
                Edge e = (Edge) i.next();
                l.addAll(allPathsFrom(p, e.getToNode()));
            }
        }
        return l;
    }

    boolean checkConsistancy() {
        for (Iterator i = nodes.values().iterator(); i.hasNext();) {
            Node n = (Node) i.next();
            for (Iterator j = n.fromEdges.iterator(); j.hasNext();) {
                Edge e = (Edge) j.next();
                if (!hasEdge(e)) {
                    throw new Error("Lattice has NODE with missing FROM edge: " + n + "," + e);
                }
            }
            for (Iterator j = n.toEdges.iterator(); j.hasNext();) {
                Edge e = (Edge) j.next();
                if (!hasEdge(e)) {
                    throw new Error("Lattice has NODE with missing TO edge: " + n + "," + e);
                }
            }
        }
        for (Iterator i = edges.iterator(); i.hasNext();) {
            Edge e = (Edge) i.next();
            if (!hasNode(e.getFromNode())) {
                throw new Error("Lattice has EDGE with missing FROM node: " + e);
            }
            if (!hasNode(e.getToNode())) {
                throw new Error("Lattice has EDGE with missing TO node: " + e);
            }
            if(!e.getToNode().hasEdgeFromNode(e.getFromNode())) {
                throw new Error("Lattice has EDGE with TO node with no corresponding FROM edge: " + e);
            }
            if(!e.getFromNode().hasEdgeToNode(e.getToNode())) {
                throw new Error("Lattice has EDGE with FROM node with no corresponding TO edge: " + e);
            }
        }
        return true;
    }


    /**
     * Self test for Lattices.  Test loading, saving, dynamically creating
     * and optimizing Lattices
     *
     * @param args
     */
    public static void main(String[] args) {

        Lattice lattice = null;

        if (args.length > 0) {
            System.err.println("Loading lattice from " + args[0]);
            lattice = new Lattice(args[0]);
        } else {
            System.err.println("Building test Lattice");

            lattice = new Lattice();

            /*
            1 --> 2 -
            /         \
            0 --> 1 --> 4
            \     \   /
            2 --> 3 -
            */

            Node n0 = lattice.addNode("0", "0", 0, 0);
            Node n1 = lattice.addNode("1", "1", 0, 0);
            Node n1a = lattice.addNode("1a", "1", 0, 0);
            Node n2 = lattice.addNode("2", "2", 0, 0);
            Node n2a = lattice.addNode("2a", "2", 0, 0);
            Node n3 = lattice.addNode("3", "3", 0, 0);
            Node n4 = lattice.addNode("4", "4", 0, 0);

            Edge e01 = lattice.addEdge(n0, n1, -1, 0);
            Edge e01a = lattice.addEdge(n0, n1a, -1, 0);
            Edge e14 = lattice.addEdge(n1, n4, -1, 0);
            Edge e1a2a = lattice.addEdge(n1a, n2a, -1, 0);
            Edge e2a4 = lattice.addEdge(n2a, n4, -1, 0);
            Edge e02 = lattice.addEdge(n0, n2, -1, 0);
            Edge e23 = lattice.addEdge(n2, n3, -1, 0);
            Edge e13 = lattice.addEdge(n1, n3, -1, 0);
            Edge e34 = lattice.addEdge(n3, n4, -1, 0);

            lattice.setInitialNode(n0);
            lattice.setTerminalNode(n4);
        }

        System.err.println("Lattice has " + lattice.getNodes().size() + " nodes and " + lattice.getEdges().size() + " edges");

        System.err.println("Testing Save/Load .LAT file");
        lattice.dump("test.lat");

        lattice.dumpAllPaths();

        LatticeOptimizer lo = new LatticeOptimizer(lattice);
        lo.optimize();

        /*
        2
        /   \
        0 --> 1 --> 4
        \     \   /
        2 -->  3
        */

        lattice.dumpAllPaths();


    }
}
