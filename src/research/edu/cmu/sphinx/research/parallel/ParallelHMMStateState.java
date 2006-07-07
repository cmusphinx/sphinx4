
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
 * Represents an HMMStateState in a feature stream.
 */
public class ParallelHMMStateState extends HMMStateState 
implements ParallelState {

    private FeatureStream stream;
    private TokenStack tokenStack;

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
     * Returns the FeatureStream of this ParallelHMMStateState.
     *
     * @return the FeatureStream of this ParallelHMMStateState
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


