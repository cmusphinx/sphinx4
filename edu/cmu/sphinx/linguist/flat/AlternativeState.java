
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

import edu.cmu.sphinx.linguist.dictionary.Word;


import java.io.Serializable;

/**
 * Represents a set of alternatives in an SentenceHMMS
 * 
 */
public class AlternativeState extends SentenceHMMState
	implements Serializable {

    /**
     * Creates a WordState
     *
     */
    public AlternativeState(GrammarState parent, int which) {
	super("A", parent,  which);
    }

    /**
     * Gets the word associated with this state
     *
     * @return the word
     */
    public Word[] getAlternative() {
	return ((GrammarState) getParent())
	    .getGrammarNode().getWords(getWhich());
    }
    /**
     * Retrieves a short label describing the type of this state.
     * Typically, subclasses of SentenceHMMState will implement this
     * method and return a short (5 chars or less) label
     *
     * @return the short label.
     */
    public String getTypeLabel() {
	return "Alt";
    }
}


