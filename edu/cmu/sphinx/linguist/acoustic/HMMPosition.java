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

package edu.cmu.sphinx.linguist.acoustic;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.io.ObjectStreamException;

import java.io.Serializable;

/**
 * Defines possible positions of HMMs. Note that even though the
 * positions are defined to be within words, some recognizers may
 * classify positions in terms of other elements besides words.
 */
public class HMMPosition implements Serializable {
    private static Map map = new LinkedHashMap();
    private String name;
    private int index;


    /**
     * HMM is at the beginning position of the word
     */
    public static HMMPosition BEGIN = new HMMPosition("b", 0);

    /**
     * HMM is at the end position of the word
     */
    public static HMMPosition END = new HMMPosition("e", 1);

    /**
     * HMM is at the beginning and the end of the word
     */
    public static HMMPosition SINGLE = new HMMPosition("s", 2);

    /**
     * HMM is completely internal to the word
     */
    public static HMMPosition INTERNAL  = new HMMPosition("i", 3);

    /**
     * HMM is at an undefined position n the word
     */
    public static HMMPosition UNDEFINED  = new HMMPosition("-", 4);

    /**
     * The maximum number of HMM positions
     */
    public final static int MAX_POSITIONS = 5;


    /**
     * Creates and HMMPosition. No public construction is allowed
     *
     * @param rep the representation for this position
     */
    private HMMPosition(String rep, int index) {
	this.name = rep;
        this.index = index;
	map.put(rep, this);
    }

    /**
     * Looks up an HMMPosition baed upon its representation
     *
     * @param rep the string representation
     *
     * @return the HMMPosition represented by rep or null if not
     *   found
     */
    public static HMMPosition lookup(String rep) {
	return (HMMPosition) map.get(rep);
    }


    /**
     * Returns the index for this position.  Each HMMPosition
     * maintains a unique index. This allows arrays of hmm positions
     * to be easily maintained
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }


    /**
     * Determines if this position is an end word position
     *
     * @return true if this is an end of word position
     */
    public boolean isWordEnd() {
        return this == SINGLE || this == END;
    }

    /**
     * Determines if this position is word beginning position
     *
     * @return true if this is a word beginning position
     */
    public boolean isWordBeginning() {
        return this == SINGLE || this == BEGIN;
    }

    /**
     * Returns the canonical object for the HMMPosition
     */
    private Object readResolve() throws ObjectStreamException {
	return  map.get(name);
    }


    /**
     * Returns an iterator for all HMMPositions
     *
     * @return an iterator that iterates through all positions
     */
    public static Iterator iterator() {
	return map.values().iterator();
    }

    /**
     * Returns a string representation of this object
     *
     * @return the string representation
     */
    public String toString() {
	return name;
    }
}
