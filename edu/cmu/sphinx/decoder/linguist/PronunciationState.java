
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

package edu.cmu.sphinx.decoder.linguist;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.knowledge.acoustic.LeftRightContext;
import edu.cmu.sphinx.knowledge.dictionary.Pronunciation;


/**
 * Represents a pronunciation in an SentenceHMMS
 * 
 */
public class PronunciationState extends SentenceHMMState {
    private Pronunciation pronunciation;
    private String prettyLabel;

    /**
     * Creates a PronunciationState
     *
     * @param pronunciation the pronunciation associated with this state
     */
    public PronunciationState(WordState parent, int which) {
	super("P", parent,  which);

	pronunciation = parent.getWord().getPronunciations()[which];
    }

    /**
     * Creates a PronunciationState
     *
     * @param pronunciation the pronunciation associated with this state
     */
    public PronunciationState(String name, Pronunciation p, int which) {
	super(name, null,  which);
	pronunciation = p;
    }

    /**
     * Gets the pronunciation associated with this state
     *
     * @return the pronunciation
     */
    public Pronunciation getPronunciation() {
	return pronunciation;
    }

    /**
     * Retrieves a short label describing the type of this state.
     * Typically, subclasses of SentenceHMMState will implement this
     * method and return a short (5 chars or less) label
     *
     * @return the short label.
     */
    public String getTypeLabel() {
	return "Pron";
    }

    /**
     * empty contructor
     */
    private PronunciationState() {
    }
}


