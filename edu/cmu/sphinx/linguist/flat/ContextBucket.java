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

import edu.cmu.sphinx.linguist.acoustic.LeftRightContext;
import edu.cmu.sphinx.linguist.acoustic.Unit;

/**
 * A ContextBucket manages left context history when an
 * SentenceHMM is being built
 */
class ContextBucket  {
    private int maxSize;
    private Unit[] units;

    /**
     * Creates a context bucket with the given maximum size
     *
     * @param maxSize the maximum size of the context bucket
     */
    ContextBucket(int maxSize) {
	units = new Unit[0];
	this.maxSize = maxSize;
    }


    /**
     * Creates a new context bucket by appending the unit to the
     * context in the previous context bucket
     *
     * @param prev the previous context bucket
     * @param unit the unit to add to this context
     */
    ContextBucket(ContextBucket prev, Unit unit) {
	int newSize;
	Unit[] prevUnits = prev.getUnits();

	this.maxSize = prev.maxSize;
	newSize = Math.min(prevUnits.length + 1, maxSize);
	this.units = new Unit[newSize];

	if (newSize > 0) {
	    units[0] = unit;

	    for (int i = 1; i < newSize; i++) {
		units[i] = prevUnits[i - 1];
	    }
	}
    }

    /**
     * Returns the set of units for this context bucket
     *
     * @return the set of units
     */
    Unit[] getUnits() {
	return units;
    }

    /**
     * Returns the string representation for this StatePath
     *
     * @return the string representation
     */
    public String toString() {
	return LeftRightContext.getContextName(units);
    }
}
