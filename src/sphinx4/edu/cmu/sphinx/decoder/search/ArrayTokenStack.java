
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
 * An array implementation of TokenStack.
 */
public class ArrayTokenStack implements TokenStack {

    private Token[] stack;


    /**
     * Constructs a ArrayTokenStack object with the given capacity.
     *
     * @param capacity the height of the stack
     */
    public ArrayTokenStack(int capacity) {
        this.stack = new Token[capacity];
        clear();
    }


    /**
     * Adds the given Token to this TokenStack.
     *
     * @param newToken the new Token to add
     *
     * @return the replaced Token, or null if no Token was replaced
     */
    public Token add(Token newToken) {
        Token lowestToken = null;
        int lowestTokenIndex = -1;

        // first look for the lowest scoring token
        for (int i = 0; i < stack.length; i++) {
            Token token = stack[i];
	    if (token == null) {
		stack[i] = newToken;
		return null;
	    } else {
                if (token.getFrameNumber() != newToken.getFrameNumber()) {
                    stack[i] = newToken;
                    return token; 
                }
                if (lowestToken == null) {
                    lowestToken = token;
                    lowestTokenIndex = i;
                } else if (token.getScore() <= lowestToken.getScore()) {
                    lowestToken = token;
                    lowestTokenIndex = i;
                }
            }
        }

        // if found the lowest scoring token
        if (lowestTokenIndex != -1) {
            stack[lowestTokenIndex] = newToken;
            return lowestToken;
        } else {
            // if no tokens at all
            stack[0] = newToken;
            return null;
        }
    }
            

    /**
     * Returns true if the given score is higher than the lowest scoring
     * token in this TokenStack.
     *
     * @param score the score the check
     *
     * @return true if the given score is higher than the lowest scoring
     *    token, false otherwise
     */
    public boolean isInsertable(float score, int frameNumber) {
        for (int i = 0; i < stack.length; i++) {
	    Token token = stack[i];
            if (token == null || 
                token.getFrameNumber() != frameNumber ||
		token.getScore() <= score) {
                return true;
            }
        }
        return false;
    }


    /**
     * Returns the capacity of this TokenStack, that is, the maximum
     * number of elements allowed in this TokenStack.
     *
     * @return the capacity of this TokenStack
     */
    public int getCapacity() {
        return stack.length;
    }



    /**
     * Removes all of the Tokens from this TokenStack
     */
    public void clear() {
        for (int i = 0; i < stack.length; i++) {
            stack[i] = null;
        }
    }
}



