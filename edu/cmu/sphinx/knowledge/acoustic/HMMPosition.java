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


    /**
     * HMM is at the beginning position of the word
     */
    public static HMMPosition BEGIN = new HMMPosition("b");

    /**
     * HMM is at the end position of the word
     */
    public static HMMPosition END = new HMMPosition("e");

    /**
     * HMM is at the beginning and the end of the word
     */
    public static HMMPosition SINGLE = new HMMPosition("s");

    /**
     * HMM is completely internal to the word
     */
    public static HMMPosition INTERNAL  = new HMMPosition("i");

    /**
     * HMM is at an undefined position n the word
     */
    public static HMMPosition UNDEFINED  = new HMMPosition("-");


    /**
     * Creates and HMMPosition. No public construction is allowed
     *
     * @param rep the representation for this position
     */
    private HMMPosition(String rep) {
	this.name = rep;
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
