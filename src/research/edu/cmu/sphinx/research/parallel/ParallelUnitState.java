
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

package edu.cmu.sphinx.research.parallel;

import edu.cmu.sphinx.linguist.acoustic.Unit;

import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.search.TokenStack;
import edu.cmu.sphinx.decoder.search.ArrayTokenStack;

import edu.cmu.sphinx.linguist.flat.HMMStateState;
import edu.cmu.sphinx.linguist.flat.PronunciationState;
import edu.cmu.sphinx.linguist.flat.SentenceHMMState;
import edu.cmu.sphinx.linguist.flat.UnitState;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Represents an UnitState in one particular feature stream, 
 * in a search graph with states for multiple feature streams.
 */
public class ParallelUnitState extends UnitState implements ParallelState {

    private FeatureStream stream;
    private TokenStack tokenStack;

    /**
     * Creates a ParallelUnitState
     *
     * @param parent the parent of this state
     * @param stream the feature stream of this state
     * @param which the index of the given state
     * @param unit the unit associated with this state
     * @param tokenStackSize the token stack size
     */
    public ParallelUnitState(PronunciationState parent,
                             FeatureStream stream,
                             int which,
                             Unit unit,
                             int tokenStackSize) {
	super(parent, which, unit);
	this.stream = stream;
        if (tokenStackSize > 0) {
            this.tokenStack = new ArrayTokenStack(tokenStackSize);
        } else {
            this.tokenStack = null;
        }
    }


    /**
     * Returns the token stack of this ParallelUnitState.
     *
     * @return the token stack
     */
    public TokenStack getTokenStack() {
        return tokenStack;
    }
        

    /**
     * Returns the FeatureStream of the acoustic model behind this 
     * ParallelUnitState.
     *
     * @return the FeatureStream of the acoustic model
     */
    public FeatureStream getFeatureStream() {
	return stream;
    }

    
    /**
     * Returns the name of this ParallelUnitState.
     * It is in the form of "Hx.y", where y is the index of the
     * parallel branch, and x is which state within the branch.
     * For example, the first state in the first parallel branch
     * might have name "H0.0".
     *
     * @return the name of this ParallelUnitState
     */
    public String getName() {
	String name = super.getName();
	return name + "." + stream.getName();
    }


    /**
     * Gets the pretty name for this unit sate
     *
     * @return the pretty name 
     */
    public String getPrettyName() {
        return super.getPrettyName() + "." + stream.getName();
    }

    /**
     * Returns the title of this state
     *
     * @return the title
     */
    public String getTitle() {
        return super.getTitle() + "." + stream.getName();
    }


    /**
     * Returns the value signature
     *
     * @return the value signature
     */
    public String getValueSignature() {
        return super.getValueSignature() + "." + stream.getName();
    }


    /**
     * Clears/resets any accumulated state or history
     */
    public void clear() {
        // super.clear(); BUG: fix me
        if (tokenStack != null) {
            tokenStack.clear();
        }
    }
}


