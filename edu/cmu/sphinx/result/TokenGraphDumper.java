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

import java.io.FileWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dumps out the GDL graph of all the result token chains in a Result,
 * as well as all the alternate hypotheses along those chains.
 */
public class TokenGraphDumper {

    private AlternateHypothesisManager loserManager;
    private Result result;
    private Map tokenIDMap;
    private Set dumpedTokens;
    private int ID = 0;

    /**
     * Constructs a TokenGraphDumper from the given result.
     *
     * @param result The result which search space we want to dump.
     */
    public TokenGraphDumper(Result result) {
        this.result = result;
        this.loserManager = result.getAlternateHypothesisManager();
        tokenIDMap = new HashMap();
        dumpedTokens = new HashSet();
    }

    /**
     * Dumps the GDL output of the search space to the given file.
     *
     * @param title the title of the GDL graph
     * @param fileName
     */
    public void dumpGDL(String title, String fileName) {
        try {
            System.err.println("Dumping " + title + " to " + fileName);
            FileWriter f = new FileWriter(fileName);
            f.write(dumpGDL(title));
            f.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Dumps the GDL output.
     *
     * @param title the title of the GDL graph
     *
     * @return the GDL output string
     */
    public String dumpGDL(String title) {
        String gdl = "graph: {\n";
        gdl += ("title: \"" + title + "\"\n");
        gdl += ("display_edge_labels: yes\n");

        for (Iterator i = result.getResultTokens().iterator(); i.hasNext();) {
            Token token = (Token) i.next();
            gdl += dumpTokenGDL(token);
        }

        gdl += ("}\n");
        return gdl;
    }

    /**
     * Dumps the GDL output for a token, and any of its predecessors
     * or alternate hypotheses.
     *
     * @param token the token to dump
     *
     * @return the GDL output string
     */
    private String dumpTokenGDL(Token token) {
        
        if (dumpedTokens.contains(token)) {
            return "";
        } else {
            String label = ("[" + token.getAcousticScore() + "," +
                            token.getLanguageScore() + "]");
            if (token.isWord()) {
                label = token.getWord().getSpelling() + label;
            }
            
            String color = null;

            if (token.getSearchState() != null) {
                color = getColor(token.getSearchState());
            }

            String gdl = 
                ("node: { title: \"" + getTokenID(token).intValue() +
                 "\" label: \"" + label + "\" color: ");
            if (color != null) {
                gdl += (color + " }");
            } else {
                gdl += " }";
            }
            gdl += "\n";

            dumpedTokens.add(token);
            
            if (token.getPredecessor() != null) {
                gdl +=
                    ("edge: { sourcename: \"" + getTokenID(token) +
                     "\" targetname: \"" + getTokenID(token.getPredecessor()) 
                     + "\" }");
                gdl += "\n";
                gdl += dumpTokenGDL(token.getPredecessor());
            }
            
            if (loserManager != null) {
                List list = loserManager.getAlternatePredecessors(token);
                if (list != null) {
                    for (Iterator i = list.iterator(); i.hasNext();) {
                        Token loser = (Token) i.next();
                        gdl += ("edge: { sourcename: \"" + getTokenID(token)
                                + "\" targetname: \"" + getTokenID(loser) +
                                "\" }"); 
                        gdl += "\n";
                        gdl += dumpTokenGDL(loser);
                    }
                }
            }
            return gdl;
        }
    }

    /**
     * Gets the color for a particular state
     *
     * @param state the state
     *
     * @return its color
     */
    private String getColor(SearchState state) {
        String color = "lightred";
        if (state.isFinal()) {
            color = "magenta";
        } else if (state instanceof UnitSearchState) {
            color = "green";
        } else if (state instanceof WordSearchState) {
            color = "lightblue";
        } else if (state instanceof HMMSearchState) {
            color = "orange";
        }
        return color;
    }

    /**
     * Returns the next available token ID.
     *
     * @param token the token for which we want an ID
     *
     * @return the next available token ID
     */
    private Integer getTokenID(Token token) {
        Integer id = (Integer) tokenIDMap.get(token);
        if (id == null) {
            id = new Integer(ID++);
            tokenIDMap.put(token, id);
        }
        return id;
    }
}
