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

package edu.cmu.sphinx.linguist.flat;

import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;

/**
 * Represents a transition in a sentence HMM. Each transition is
 * described by the next state and the associated acoustic and
 * language probability for the transition.
 *
 * All probabilities are in the LogMath log domain
 */
public class SentenceHMMStateArc implements SearchStateArc {
    private SentenceHMMState nextState;
    private float logAcousticProbability;
    private float logLanguageProbability;
    private float logInsertionProbability;
    private int hashCode;

    /**
     * Creates a SentenceHMMStateArc
     *
     * @param nextState the next state
     * @param logAcousticProbability the log acoustic probability
     * @param logLanguageProbability the log language probability
     * @param logInsertionProbability the log insertion probability
     *
     */
    public SentenceHMMStateArc(SentenceHMMState nextState,
			       float logAcousticProbability,
			       float logLanguageProbability,
			       float logInsertionProbability) {
	this.nextState = nextState;
	this.logAcousticProbability = logAcousticProbability;
	this.logLanguageProbability = logLanguageProbability;
	this.logInsertionProbability = logInsertionProbability;

        hashCode = 111 + nextState.hashCode() +
            3 * Float.floatToIntBits(logAcousticProbability) +
            17 * Float.floatToIntBits(logLanguageProbability) +
            23 * Float.floatToIntBits(logInsertionProbability);

    }


    /**
     * Determines if the given object is equal to this object
     *
     * @param o the object to compare to
     *
     * @return <code>true</code> if the objects are equal
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof SentenceHMMStateArc) {
            SentenceHMMStateArc other = (SentenceHMMStateArc) o;
            return  nextState == other.nextState &&
                logAcousticProbability == other.logAcousticProbability &&
                logLanguageProbability == other.logLanguageProbability &&
                logInsertionProbability == other.logInsertionProbability;
                   
        } else {
            return false;
        }
    }

    /**
     * Returns a hashCode for this object
     *
     * @return the hashCode
     */
    public int hashCode() {
        return hashCode;
    }


    /**
     * Retrieves the next state
     *
     * @return the next state
     */
    public SearchState getState() {
	return nextState;
    }


    /**
     * For backwards compatibility
     *
     * Returns the next state as a SentenceHMSMtate
     *
     * @return the next state
     */
    public SentenceHMMState getNextState() {
        return (SentenceHMMState) getState();
    }

    /**
     * Retrieves the acoustic transition probablility for this
     * transition.
     *
     * @return the acoustic transition probability in the logmath log
     * domain
     */
    public float getAcousticProbability() {
	return logAcousticProbability;
    }

    /**
     * Retrieves the language transition probability for this
     * transition
     *  
     * @return the language  transition probability in the logmath log
     * domain
     */
    public float getLanguageProbability() {
	return logLanguageProbability;
    }

    /**
     * Retrieves the insertaion probability for this
     * transition
     *  
     * @return the insertion probability  in the logmath log domain
     */
    public float getInsertionProbability() {
	return logInsertionProbability;
    }


     /**
      * Gets the composite probability of entering this state
      *
      * @return the log probability
      */
     public float getProbability() {
         return logLanguageProbability + logAcousticProbability +
             logInsertionProbability;
     }
}
