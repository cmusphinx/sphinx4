

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

package edu.cmu.sphinx.decoder.search;

import edu.cmu.sphinx.knowledge.acoustic.HMMState;
import edu.cmu.sphinx.decoder.search.Token;

/**
 * Represents a state in the token tree
 */
public class StateToken extends Token {
    private HMMState hmmState;

    /**
     * Creates a new state token
     *
     * @param stateToken the token to be turned into a StateToken
     * @param word the word represented by this token
     */
     public StateToken(Token stateToken, HMMState hmmState) {
	 super( stateToken.getPredecessor(),
		 stateToken.getSentenceHMMState(),
		 stateToken.getScore(),
		 stateToken.getLanguageScore(),
		 stateToken.getInsertionProbability(),
		 stateToken.getFrameNumber());
	 this.hmmState = hmmState;
     }

     /**
      * Returns the grammar word for this token
      *
      * @return the word associated with this token
      */
     public HMMState getHMMState() {
	 return hmmState;
     }
}

