
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

package edu.cmu.sphinx.knowledge.acoustic;

import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.util.LogMath;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

/**
 * Represents a single state in an HMM
 */
public class HMMState implements Serializable {
    private HMM hmm;
    private int state;
    HMMStateArc[] arcs;
    private boolean isEmitting;
    private Senone senone;

    private static int objectCount;


    /**
     * Constructs an HMMState
     *
     * @param hmm the hmm for this state
     * @param which the index for this particular state
     */
    HMMState(HMM hmm, int which) {
	this.hmm = hmm;
	this.state = which;
	this.isEmitting = ((hmm.getTransitionMatrix().length - 1) != state);
        if (isEmitting) {
            SenoneSequence ss = hmm.getSenoneSequence();
            senone = ss.getSenones()[state];
        }
        Utilities.objectTracker("HMMState", objectCount++);
    }

    /**
     * Gets the HMM associated with this state
     *
     * @return the HMM
     */
    public HMM getHMM() {
	return hmm;
    }

    /**
     * Gets the state 
     *
     * @return the state
     */
    public int getState() {
	return state;
    }


    /**
     * Gets the score for this HMM state
     *
     * @param feature the feature to be scored
     *
     * @return the acoustic score for this state.
     */
    public float getScore(Feature feature) {
        return senone.getScore(feature);
    }


    /**
     * Gets the scores for each mixture component in this HMM state
     *
     * @param feature the feature to be scored
     *
     * @return the acoustic scores for the components of this state.
     */
    public float[] calculateComponentScore(Feature feature) {
	SenoneSequence ss = hmm.getSenoneSequence();
	return senone.calculateComponentScore(feature);
    }


    /**
     * Gets the senone for this HMM state
     *
     * @return the senone for this state.
     */
    public Senone getSenone() {
        return senone;
    }


    /**
     * Determines if two HMMStates are equal
     *
     * @param other the state to compare this one to
     *
     * @return true if the states are equal
     */
    public boolean equals(Object other) {
	if (this == other) {
	    return true;
	} else if (!(other instanceof HMMState)) {
	    return false;
	} else {
	    HMMState otherState = (HMMState) other;
	    return this.hmm == otherState.hmm && 
		this.state == otherState.state;
	}
    }

    /**
     * Returns the hashcode for this state
     *
     * @return the hashcode
     */
    public int hashCode() {
	return hmm.hashCode() + state;
    }


    /**
     * Determines if this HMMState is an emittting state
     *
     * @return true if the state is an emitting state
     */
    // TODO: We may have non-emitting entry states as well.
    public final boolean isEmitting() {
	return isEmitting;
    }


    /**
     * Retrieves the state of successor states for this state
     *
     * @return the set of successor state arcs
     */
    public HMMStateArc[] getSuccessors() {
	if (arcs == null) {
	    List list = new ArrayList();
	    float[][] transitionMatrix = hmm.getTransitionMatrix();

            // dumpMatrix("arc", transitionMatrix);
	    for (int i = 0; i < transitionMatrix.length; i++) {
		 if (transitionMatrix[state][i] > LogMath.getLogZero()) {
		    HMMStateArc arc = new HMMStateArc(hmm.getState(i),
				    transitionMatrix[state][i]);
		    list.add(arc);
		}
	    }
	    arcs = (HMMStateArc[]) list.toArray(new HMMStateArc[list.size()]);  
	}
	return arcs;
    }


    /**
     * Dumps out a matrix
     *
     * @param title the title for the dump
     * @param matrix the matrix to dump
     */
    private void dumpMatrix(String title, float[][] matrix) {
        System.out.println(" -- " + title + " --- ");
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(" " + matrix[i][j]);
            }
            System.out.println();
        }
    }

    /**
     * Determines if this state is an exit state of the HMM
     *
     * @return true if the state is an exit state
     */
    public boolean isExitState() {
	// return (hmm.getTransitionMatrix().length - 1) == state;
	return !isEmitting;
    }

    /**
     * returns a string represntation of this object
     * 
     * @return a string representation
     */
    public String toString() {
	return "HMMS " + hmm + " state " + state;
    }
}

