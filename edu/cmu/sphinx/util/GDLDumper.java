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
package edu.cmu.sphinx.util;

import edu.cmu.sphinx.search.SentenceHMMState;
import edu.cmu.sphinx.search.SentenceHMMStateArc;
import edu.cmu.sphinx.search.LinguistProcessor;
import edu.cmu.sphinx.search.Linguist;
import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cmu.sphinx.search.WordState;
import edu.cmu.sphinx.search.UnitState;
import edu.cmu.sphinx.search.GrammarState;
import edu.cmu.sphinx.search.HMMStateState;
import edu.cmu.sphinx.search.AlternativeState;
import edu.cmu.sphinx.search.PronunciationState;


/**
 *  A linguist processor that dumps out the sentence hmm in GDL
 *  format.
 */
public class GDLDumper extends LinguistDumper  {


    private static final String PROP_SKIP_HMMS = 
        "edu.cmu.sphinx.util.GDLDumper.skipHMMs"; 


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
	out.println("graph: {");
	out.println("    layout_algorithm: minbackward");
	out.println("    orientation: left_to_right");
	out.println("    manhatten_edges: yes");
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
     * Called to dump out a node in the SentenceHMM
     *
     * @param out the output stream.
     * @param state the state to dump
     * @param level the level of the state
     */
    protected void startDumpNode(PrintStream out, 
                                 SentenceHMMState state, int level,
                                 SphinxProperties props) {
        
        boolean skipHMMs = props.getBoolean(PROP_SKIP_HMMS, true);

	if (skipHMMs && (state instanceof HMMStateState)) {
	} else {
	    String color = getColor(state);
	    String shape = "box";

	    if (state.isFinalState()) {
		shape = "circle";
	    }
	    out.println("    node: {" +
		    "title: " + qs(state.getSignature()) + 
		    " label: " + qs(state.getPrettyName()) + 
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

    private String getColor(SentenceHMMState state) {
	String color = "lightred";
	if (state.isFinalState()) {
	    color = "magenta";
	} else if (state instanceof UnitState) {
	    color = "green";
	} else if (state instanceof WordState) {
	    color = "lightblue";
	} else if (state instanceof GrammarState) {
	    color = "lightred";
	} else if (state instanceof HMMStateState) {
	    color = "orange";
	} else if (state instanceof AlternativeState) {
	    color = "purple";
	} else if (state instanceof PronunciationState) {
	    color = "darkcyan";
	}
	return color;
    }

    /**
     * Called to dump out a node in the SentenceHMM
     *
     * @param out the output stream.
     * @param state the state to dump
     * @param level the level of the state
     */
    protected void endDumpNode(PrintStream out, SentenceHMMState state, 
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
    protected void dumpArc(PrintStream out, SentenceHMMState from, 
                           SentenceHMMStateArc arc, int level,
                           SphinxProperties props) {

        boolean skipHMMs = props.getBoolean(PROP_SKIP_HMMS, true);

        String color = getArcColor(arc);
	SentenceHMMState nextState = arc.getNextState();

	if (skipHMMs) {
	    if (nextState instanceof HMMStateState) {
		return;
	    } else if (from instanceof HMMStateState) {
		from = from.getParent();
	    }
	} 
	out.println("   edge: { sourcename: " +
	    qs(from.getSignature()) + " targetname: " +
	    qs(nextState.getSignature()) + 
            " color: " + color + "}");
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
    private String getArcColor(SentenceHMMStateArc arc) {
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
}

