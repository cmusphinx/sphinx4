
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

import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.search.HeapActiveList;
import edu.cmu.sphinx.decoder.linguist.HMMStateState;
import edu.cmu.sphinx.decoder.linguist.PronunciationState;
import edu.cmu.sphinx.decoder.linguist.SentenceHMMState;
import edu.cmu.sphinx.decoder.linguist.UnitState;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Represents an HMMStateState in a parallel SentenceHMM.
 * The only difference this class and the HMMStateState class is
 * in the getName() method, which tells you which of the
 * parallel branches this HMMStateState is in. See the getName()
 * method for details.
 */
public class ParallelUnitState extends edu.cmu.sphinx.decoder.linguist.UnitState implements ParallelState {

    private static List allStates;
    static {
	allStates = new LinkedList();
    }

    private String modelName;
    private TokenStack tokenStack;


    /**
     * Clear out all the ParallelUnitStates in the JVM.
     */
    public static void clearAllStates() {
	for (Iterator i = allStates.iterator(); i.hasNext(); ) {
	    ParallelUnitState state = (ParallelUnitState)i.next();
	    state.clear();
	}
    }

    
    /**
     * Creates a ParallelUnitState
     *
     * @param parent the parent of this state
     * @param modelName the name of the acoustic model behind this HMMState
     * @param hmmState the hmmState associated with this state
     */
    public ParallelUnitState(edu.cmu.sphinx.decoder.linguist.PronunciationState parent,
                             String modelName,
                             int which,
                             Unit unit,
                             int tokenStackSize) {
	super(parent, which, unit);
	this.modelName = modelName;
        if (tokenStackSize > 0) {
            this.tokenStack = new TokenStackImpl(tokenStackSize);
        } else {
            this.tokenStack = null;
        }
	allStates.add(this);
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
     * Returns the name of the acoustic model behind this 
     * ParallelUnitState.
     *
     * @return the name of the acoustic model
     */
    public String getModelName() {
	return modelName;
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
	return name + "." + modelName;
    }


    /**
     * Gets the pretty name for this unit sate
     *
     * @return the pretty name 
     */
    public String getPrettyName() {
        return super.getPrettyName() + "." + modelName;
    }


    public String getTitle() {
        return super.getTitle() + "." + modelName;
    }

    public String getValueSignature() {
        return super.getValueSignature() + "." + modelName;
    }


    /**
     * Clears/resets any accumulated state or history
     */
    public void clear() {
        super.clear();
        if (tokenStack != null) {
            tokenStack.clear();
        }
    }
}


