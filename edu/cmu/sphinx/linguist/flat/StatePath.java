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

package edu.cmu.sphinx.linguist.flat;




/**
 * Represents a path in a SentenceHMM. The path includes the
 * particular state, and the states left context.  This is used while
 * building/compiling a SentenceHMM
 */
public class StatePath {
    SentenceHMMState state;
    ContextBucket context;

    /**
     * Creates a StatePath
     *
     * @param state the state for the StatePath
     * @param context the context bucket
     */
    StatePath(SentenceHMMState state, ContextBucket context) {
	this.state = state;
	this.context = context;
    }

    /**
     * Retrieves the state for the state path
     *
     * @return the state
     */
    SentenceHMMState getState() {
	return state;
    }

    /**
     * Retrieves the context for the state path
     *
     * @return the context
     */
    ContextBucket getContext() {
	return context;
    }

    /**
     * Returns the string representation for this StatePath
     *
     * @return the string representation
     */
    public String toString() {
	return state.toString() + " " + context.toString();
    }
}


