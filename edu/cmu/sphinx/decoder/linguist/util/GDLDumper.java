/*
 * Copyright 1999-2003 Carnegie Mellon University.  
 * Portions Copyright 2003 Sun Microsystems, Inc.  
 * Portions Copyright 2003 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.decoder.linguist.util;

import edu.cmu.sphinx.util.Utilities;

import edu.cmu.sphinx.decoder.linguist.SearchStateArc;
import edu.cmu.sphinx.decoder.linguist.SearchState;
import edu.cmu.sphinx.decoder.linguist.WordSearchState;
import edu.cmu.sphinx.decoder.linguist.UnitSearchState;
import edu.cmu.sphinx.decoder.linguist.HMMSearchState;
import edu.cmu.sphinx.decoder.linguist.LinguistProcessor;
import edu.cmu.sphinx.decoder.linguist.Linguist;

import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Iterator;

import edu.cmu.sphinx.util.LogMath;




/**
 *  A linguist processor that dumps out the sentence hmm in GDL
 *  format.
 */
public class GDLDumper extends LinguistDumper  {

    private static final String PROP_PREFIX =
        "edu.cmu.sphinx.decoder.linguist.util.GDLDumper.";


    /**
     * The SphinxProperty specifying whether to skip HMMs during dumping.
     */
    public static final String PROP_SKIP_HMMS = PROP_PREFIX + "skipHMMs"; 

    
    /**
     * The default value for PROP_SKIP_HMMS.
     */
    public static final boolean PROP_SKIP_HMMS_DEFAULT = true;


    /**
     * The SphinxProperty to specify whether to use vertical graph layout.
     */
    public static final String PROP_VERTICAL_LAYOUT
        = PROP_PREFIX + "verticalLayout"; 
   

    /**
     * The default value for PROP_VERTICAL_LAYOUT.
     */
    public static final boolean PROP_VERTICAL_LAYOUT_DEFAULT = false;

 
    /**
     * The SphinxProperty to specify whether to dump arc labels.
     */
    public static final String PROP_DUMP_ARC_LABELS
        = PROP_PREFIX + "dumpArcLabels";


    /**
     * The default value for PROP_DUMP_ARC_LABELS.
     */
    public static final boolean PROP_DUMP_ARC_LABELS_DEFAULT = true;


    /**
     * Creates a GDL dumper
     */
    public GDLDumper() {
	setDepthFirst(false); // breadth first traversal
    }

    /**
     * Retreives the default name for the destination dump. This
     * method is typically overridden by derived classes
     *
     *
     * @return the default name for the file.
     */
    protected String getDefaultName() {
	return "linguistDump.gdl";
    }

    /**
     * Called at the start of the dump
     *
     * @param out the output stream.
     */
    protected void startDump(PrintStream out) {
        boolean verticalLayout =
            getProperties().getBoolean(PROP_VERTICAL_LAYOUT, 
                                       PROP_VERTICAL_LAYOUT_DEFAULT);

	out.println("graph: {");
	out.println("    layout_algorithm: minbackward");
        if (verticalLayout) {
            out.println("    orientation: top_to_bottom");
            out.println("    manhatten_edges: no");
            out.println("    splines: yes");
        } else {
            out.println("    orientation: left_to_right");
            out.println("    manhatten_edges: yes");
            out.println("    splines: no");
        }
    }


    /**
     * Called at the end of the dump
     *
     * @param out the output stream.
     */
    protected void endDump(PrintStream out) {
	out.println("}");
    }

    /**
     * Called to dump out a node in the search space
     *
     * @param out the output stream.
     * @param state the state to dump
     * @param level the level of the state
     */
    protected void startDumpNode(PrintStream out, 
                                 SearchState state, int level) {
        
        boolean skipHMMs = getProperties().getBoolean(PROP_SKIP_HMMS,
                                                      PROP_SKIP_HMMS_DEFAULT);

	if (skipHMMs && (state instanceof HMMSearchState)) {
	} else {
	    String color = getColor(state);
	    String shape = "box";

	    if (state.isFinal()) {
		shape = "circle";
	    }
	    out.println("    node: {" +
		    "title: " + qs(getUniqueName(state)) + 
		    " label: " + qs(state.toPrettyString()) + 
		    " color: " + color +
		    " shape: " + shape + 
		    " vertical_order: " + level +
		    "}");
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
     * Called to dump out a node in the search space
     *
     * @param out the output stream.
     * @param state the state to dump
     * @param level the level of the state
     */
    protected void endDumpNode(PrintStream out, SearchState state,
	    int level) {
    }

    /**
     * Dumps an arc
     *
     * @param out the output stream.
     * @param from arc leaves this state
     * @param arc the arc to dump
     * @param level the level of the state
     */
    protected void dumpArc(PrintStream out, SearchState from,
                           SearchStateArc arc, int level) {

        List arcList = new ArrayList();
        boolean skipHMMs = getProperties().getBoolean(PROP_SKIP_HMMS,
                                                      PROP_SKIP_HMMS_DEFAULT);
        boolean dumpArcLabels = 
            getProperties().getBoolean(PROP_DUMP_ARC_LABELS,
                                       PROP_DUMP_ARC_LABELS_DEFAULT);
        
        LogMath logMath = LogMath.getLogMath(getProperties().getContext());


	if (skipHMMs) {
            if (from instanceof HMMSearchState) {
                return;
            } else if (arc.getState()  instanceof HMMSearchState) {
                findNextNonHMMArc(arc, arcList);
	    }  else {
                arcList.add(arc);
            }
	} else {
            arcList.add(arc);
        }

        for (Iterator i = arcList.iterator(); i.hasNext(); ) {
            SearchStateArc nextArc = (SearchStateArc) i.next();
            String label = "";
            String color = getArcColor(nextArc);
            if (dumpArcLabels) {
                double acoustic = logMath.logToLinear
                    (nextArc.getAcousticProbability());
                double language = logMath.logToLinear
                    (nextArc.getLanguageProbability());
                double insert = logMath.logToLinear
                    (nextArc.getInsertionProbability());

                label = " label: " +
                    qs("(" + formatEdgeLabel(acoustic) +
                       "," + formatEdgeLabel(language) +
                       "," + formatEdgeLabel(insert) +
                       ")");
            }
            out.println("   edge: { sourcename: " + qs(getUniqueName(from)) + 
                    " targetname: " + qs(getUniqueName(nextArc.getState())) + 
                    label + " color: " + color + "}");
        }
    }



    /**
     * Given an arc to an HMMSearchState, find a downstream arc to the
     * first non-HMM state
     *
     * @param arc the arc to start the search at
     * @param results the resulting arcs are placed on this list
     */
    private void findNextNonHMMArc(SearchStateArc arc, List results) {
        Set visited = new HashSet();
        List queue = new ArrayList();

        queue.add(arc);

        while (queue.size() > 0) {
            SearchStateArc nextArc = (SearchStateArc) queue.remove(0);
            if (visited.contains(nextArc)) {
                continue;
            } else {
                visited.add(nextArc);
                if (! (nextArc.getState()  instanceof HMMSearchState)) {
                    results.add(nextArc);
                } else {
                    SearchStateArc[] nextArcs =
                        nextArc.getState().getSuccessors();
                    for (int i = 0; i < nextArcs.length; i++) {
                        queue.add(nextArcs[i]);
                    }
                }
            }
        }
    }


    /**
     * Formats the given floating point number for edge labels.
     *
     * @param value the floating point value to format
     */
    private String formatEdgeLabel(double value) {
        if (value == 1.0) {
            return "1";
        } else if (value == 0.0) {
            return "0";
        } else {
            int maxStringLength = 5;
            String stringValue = String.valueOf(value);
            if (stringValue.length() > maxStringLength) {
                stringValue = Utilities.doubleToScientificString(value, 3);
            }
            return stringValue;
        }
    }


    /**
     * Returns a color based upon the type of arc
     *
     * @param arc the arc
     *
     * @return the color of the arc based on weather it is a language
     * arc (green), acoustic arc (red), insertion arc(blue), flat arc
     * (black) or a combo (purple).
     */
    private String getArcColor(SearchStateArc arc) {
        String color = null;
        if (arc.getLanguageProbability() != 0.0) {
            color = "green";
        }
        if (arc.getAcousticProbability() != 0.0) {
            if (color == null) {
                color = "red";
            } else {
                color = "purple";
            }
        }
        if (arc.getInsertionProbability() != 0.0) {
            if (color == null) {
                color = "blue";
            } else {
                color = "purple";
            }
        }

        if (color == null) {
            color = "black";
        }

        return color;
    }


    /**
     * Returns a quoted string version of its argument. This method
     * mainly is used to hide the ugliness caused by trying to esape a
     * quote character in certain syntax higlighting editors such as
     * vim.
     *
     * @param s the string to quote.
     *
     * @return the quoted string
     */
    private String qs(String s) {
	return "\"" + s + "\"";
    }

    /**
     * returns a guaranteed unique name for the state
     *
     * @param state the state of interest
     *
     * @return the name
     */
    private String getUniqueName(SearchState state) {
        return  state.getSignature();
    }
}

