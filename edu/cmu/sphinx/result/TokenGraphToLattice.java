/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.result;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.decoder.search.AlternateHypothesisManager;

import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.UnitSearchState;
import edu.cmu.sphinx.linguist.WordSearchState;

import edu.cmu.sphinx.linguist.dictionary.Dictionary;

import java.io.FileWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts a token graph into a lattice. A token graph is obtained from
 * a Result object, using the Result.getBestTokens() method, and the
 * Result.getAlternateHypothesisManager() method.
 */
public class TokenGraphToLattice {

    private Set edges;
    private Set visitedWordTokens;
    private Map tokenNodeMap;
    private Node terminalNode;
    private Node initialNode;
    private AlternateHypothesisManager loserManager;


    /**
     * Constructs a lattice out of a token graph.
     */
    public TokenGraphToLattice(Result result) {
        edges = new HashSet();
        visitedWordTokens = new HashSet();
        tokenNodeMap = new HashMap();
        loserManager = result.getAlternateHypothesisManager();
        terminalNode = new Node(Dictionary.SENTENCE_END_SPELLING, -1, -1);

        for (Iterator i = result.getResultTokens().iterator(); i.hasNext();) {
            Token token = (Token) i.next();
            while (token != null && !token.isWord()) {
                token = token.getPredecessor();
            }
            assert token.getWord().isSentenceEndWord();
            collapseWordToken(token);
        }
    }


    /**
     * Returns the node corresponding to the given word token.
     *
     * @param token the token which we want a node of
     *
     * @return the node of the given token
     */
    private Node getNode(Token token) {
        if (token.getWord().isSentenceEndWord()) {
            return terminalNode;
        }
        Node node = (Node) tokenNodeMap.get(token);
        if (node == null) {
            node = new Node(token.getWord().getSpelling(), -1,
                            token.getFrameNumber());
            tokenNodeMap.put(token, node);
        }
        return node;
    }

    
    /**
     * Collapse the given word-ending token. This means collapsing all
     * the unit and HMM tokens that correspond to the word represented
     * by this token into an edge of the lattice.
     *
     * @param token the word-ending token to collapse
     */
    public void collapseWordToken(Token token) {
        if (visitedWordTokens.contains(token)) {
            return;
        }
        visitedWordTokens.add(token);
        collapseWordPath(getNode(token), token.getPredecessor(),
                         token.getAcousticScore(), token.getLanguageScore());
        if (loserManager != null) {
            List list = loserManager.getAlternatePredecessors(token);
            if (list != null) {
                for (Iterator i = list.iterator(); i.hasNext();) {
                    Token loser = (Token) i.next();
                    collapseWordPath(getNode(token), loser,
                                     token.getAcousticScore(),
                                     token.getLanguageScore());
                }
            }
        }
    }

    /**
     * @param parentWordNode the 'toNode' of the returned edge
     * @param token the predecessor token of the token represented by
     *              the parentWordNode
     * @param acousticScore the acoustic score until and including the
     *                      parent of token
     * @param languageScore the language score until and including the
     *                      parent of token
     */
    private void collapseWordPath(Node parentWordNode, Token token,
                                  float acousticScore, float languageScore) {
        if (token.isWord()) {
            /*
             * If this is a word, create a Node for it, and then create an
             * edge from the Node to the parentWordNode
             */
            Node fromNode = getNode(token);
            edges.add(new Edge(fromNode, parentWordNode,
                               (double)acousticScore, (double)languageScore));

            if (token.getPredecessor() != null) {
                /* Collapse the token sequence ending in this token. */
                collapseWordToken(token);
            } else {
                /* we've reached the sentence start token */
                assert token.getWord().isSentenceStartWord();
                initialNode = fromNode;
            }
        } else {
            /*
             * If a non-word token, just add the acoustic and language
             * scores to the current totals, and then move on to the
             * predecessor token.
             */
            acousticScore += token.getAcousticScore();
            languageScore += token.getLanguageScore();
            collapseWordPath(parentWordNode, token.getPredecessor(),
                             acousticScore, languageScore);
            
            /* Traverse the path(s) for the loser token(s). */
            if (loserManager != null) {
                List list = loserManager.getAlternatePredecessors(token);
                if (list != null) {
                    for (Iterator i = list.iterator(); i.hasNext();) {
                        Token loser = (Token) i.next();
                        collapseWordPath(parentWordNode, loser,
                                         acousticScore, languageScore);
                    }
                }
            }
        }
    }

    /**
     * Dump the Lattice in the form understood by AiSee
     * (a graph visualization tool).  See http://www.AbsInt.com
     *
     * @param fileName  the name of the file to dump to
     * @param title     the title of the lattice
     */
    public void dumpAISee(String fileName, String title) {
        try {
            System.err.println("Dumping " + title + " to " + fileName);
            FileWriter f = new FileWriter(fileName);
            f.write("graph: {\n");
            f.write("title: \"" + title + "\"\n");
            f.write("display_edge_labels: yes\n");

            terminalNode.dumpAISee(f);
            
            for (Iterator i = tokenNodeMap.values().iterator(); i.hasNext();) {
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
}
