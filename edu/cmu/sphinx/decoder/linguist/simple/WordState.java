
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

package edu.cmu.sphinx.decoder.linguist.simple;

import edu.cmu.sphinx.decoder.linguist.GrammarWord;

/**
 * Represents a word in an SentenceHMMS
 * 
 */
public class WordState extends SentenceHMMState {

    /**
     * Creates a WordState
     *
     */
    public WordState(AlternativeState parent, int which) {
	super("W", parent,  which);
    }

    /**
     * Gets the word associated with this state
     *
     * @return the word
     */
    public GrammarWord getWord() {
	return ((AlternativeState) getParent()).getAlternative()[getWhich()];
    }


    /**
     * Returns a pretty name for this state
     *
     * @return a pretty name for this state
     */
    public String getPrettyName() {
	return getName() + "(" + getWord().getSpelling() + ")";
    }
    /**
     * Retrieves a short label describing the type of this state.
     * Typically, subclasses of SentenceHMMState will implement this
     * method and return a short (5 chars or less) label
     *
     * @return the short label.
     */
    public String getTypeLabel() {
	return "Word";
    }

}


