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

import java.util.List;

/**
 * Represents a single state in a language search space
 */
public interface  LanguageState {
    /**
     * Gets a successor to this language state
     *
     * @param the successor index
     *
     * @return a successor
     */
     List getSuccessors();

     /**
      * Determines if this is an emitting state
      *
      * @return <code>true</code> if the state is an emitting state
      */
     boolean isEmitting();

     /**
      * Determines if this is a final state
      *
      * @return <code>true</code> if the state is a final state
      */
     boolean isFinal();

     /**
      * Gets the probability of entering this state
      *
      * @return the log probability
      */
     double getProbability();
}
