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

package edu.cmu.sphinx.decoder.linguist.lextree;

import edu.cmu.sphinx.knowledge.acoustic.HMMState;

/**
 * Represents a single word state in a language search space
 */
public interface  HMMLanguageState {
    /**
     * Gets the hmm state 
     *
     * @return the hmm state
     */
     HMMState getHMMState();

}
