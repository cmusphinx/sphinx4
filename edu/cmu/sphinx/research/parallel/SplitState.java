
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

package edu.cmu.sphinx.research.parallel;

import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.decoder.linguist.SentenceHMMState;


/**
 * Represents a state where it splits into multiple streams.
 */
public class SplitState extends edu.cmu.sphinx.decoder.linguist.SentenceHMMState {

    /**
     * Creates a SplitState
     *
     */
    public SplitState(edu.cmu.sphinx.decoder.linguist.SentenceHMMState parent, int which) {
	super("SP", parent,  which);
    }

    /**
     * Retrieves a short label describing the type of this state.
     * Typically, subclasses of SentenceHMMState will implement this
     * method and return a short (5 chars or less) label
     *
     * @return the short label.
     */
    public String getTypeLabel() {
	return "Split";
    }
}


