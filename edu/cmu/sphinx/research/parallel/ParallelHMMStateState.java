
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

import edu.cmu.sphinx.linguist.acoustic.HMMState;

import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.search.TokenStack;
import edu.cmu.sphinx.decoder.search.ArrayTokenStack;

import edu.cmu.sphinx.linguist.flat.HMMStateState;
import edu.cmu.sphinx.linguist.flat.SentenceHMMState;

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
public class ParallelHMMStateState extends HMMStateState 
implements ParallelState {

    private static List allStates;
    static {
	allStates = new LinkedList();
    }

    private FeatureStream stream;
    private TokenStack tokenStack;


    /**
     * Clear out all the ParallelHMMStateStates in the JVM.
     */
    public static void clearAllStates() {
	for (Iterator i = allStates.iterator(); i.hasNext(); ) {
	    ParallelHMMStateState state = (ParallelHMMStateState)i.next();
	    state.clear();
	}
    }

    
    /**
     * Creates a ParallelHMMStateState
     *
     * @param parent the parent of this state
     * @param stream the name of the feature stream of this HMMState
     * @param hmmState the hmmState associated with this state
     * @param tokenStackSize the height of the token stack
     */
    public ParallelHMMStateState(SentenceHMMState parent,
				 FeatureStream stream,
				 HMMState hmmState,
                                 int tokenStackSize) {
	super(parent, hmmState);
	this.stream = stream;
        if (tokenStackSize > 0) {
            this.tokenStack = new ArrayTokenStack(tokenStackSize);
        } else {
            this.tokenStack = null;
        }
	allStates.add(this);
    }


    /**
     * Returns the token stack of this ParallelHMMStateState.
     *
     * @return the token stack
     */
    public TokenStack getTokenStack() {
        return tokenStack;
    }
        

    /**
     * Returns the FeatureStream of the acoustic model behind this 
     * ParallelHMMStateState.
     *
     * @return the FeatureStream of the acoustic model
     */
    public FeatureStream getFeatureStream() {
	return stream;
    }

    
    /**
     * Returns the name of this ParallelHMMStateState.
     * It is in the form of "Hx.y", where y is the index of the
     * parallel branch, and x is which state within the branch.
     * For example, the first state in the first parallel branch
     * might have name "H0.0".
     *
     * @return the name of this ParallelHMMStateState
     */
    public String getName() {
	return (super.getName() + "." + stream.getName());
    }



    /**
     * Clears/resets any accumulated state or history
     */
    public void clear() {
        // super.clear();  BUG: fix me
        if (tokenStack != null) {
            tokenStack.clear();
        }
    }
}


