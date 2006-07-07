
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

package edu.cmu.sphinx.decoder.search;


/**
 * A stack of tokens at a SentenceHMMState, allowing more than one path
 * through the state.
 */
public interface TokenStack {


    /**
     * Adds the given Token to this TokenStack.
     *
     * @return the replaced Token, or null if no Token was replaced
     */
    public Token add(Token token);


    /**
     * Returns true if the given score is higher than the lowest scoring
     * token in this TokenStack.
     *
     * @param score the score the check
     *
     * @return true if the given score is higher than the lowest scoring
     *    token, false otherwise
     */
    public boolean isInsertable(float score, int frameNumber);


    /**
     * Returns the capacity of this TokenStack, that is, the maximum
     * number of elements allowed in this TokenStack.
     *
     * @return the capacity of this TokenStack
     */
    public int getCapacity();


    /**
     * Removes all of the Tokens from this TokenStack
     */
    public void clear();
}


