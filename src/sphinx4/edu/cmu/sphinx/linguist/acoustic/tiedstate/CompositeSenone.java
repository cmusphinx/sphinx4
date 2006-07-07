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

package edu.cmu.sphinx.linguist.acoustic.tiedstate;

import edu.cmu.sphinx.frontend.Data;

import java.util.Collection;

import java.io.Serializable;


/**
 * Represents a composite senone. A composite senone consists of a set
 * of all possible {@link Senone senones} for a given state.
 * CompositeSenones are
 * used when the exact context of a senone is not known. The
 * CompositeSenone represents all the possible senones.
 * <p>
 * This class currently only needs to be public for testing purposes.
 * <p>
 * Note that all scores are maintained in LogMath log base
 */
public class CompositeSenone implements Senone, Serializable {
    private final static int MAX_SENONES = 20000;
    private final static boolean wantMaxScore = true;
    private Senone[] senones;
    private float weight;

    transient volatile private Data logLastDataScored;
    transient volatile private float logLastScore;

   /**
    * a factory method that creates a CompositeSenone from a list of
    * senones.
    *
    * @param senoneCollection the Collection of senones
    *
    * @return a composite senone
    */
    public static  CompositeSenone create(Collection senoneCollection,
            float weight) {
	 return new CompositeSenone(
	    (Senone[]) senoneCollection.toArray(
		new Senone[senoneCollection.size()]), weight);
     }

    /**
     * Constructs a CompositeSenone given the set of constiuent
     * senones
     *
     * @param senones the set of constiuent senones
     *
     */
    public CompositeSenone(Senone[] senones, float weight) {
	this.senones = senones;
        this.weight = weight;
        System.out.print(" " + senones.length);
    }


    /**
     * Dumps this senone
     *
     * @param msg annotatin for the dump
     */
    public void dump(String msg) {
	System.out.println("   CompositeSenone " + msg + ": ");
	for (int i = 0; i < senones.length; i++) {
	    senones[i].dump("   ");
	}
    }

    /**
     * Calculates the composite senone score. Typically this is the
     * best score for all of the constituent senones
     *
     * @param feature the feature to score
     *
     * @return the score for the feature in logmath log base
     */
    public float getScore(Data feature) {
	float logScore = -Float.MAX_VALUE;

	if (feature == logLastDataScored) {
	    logScore = logLastScore;
	} else {
            if (wantMaxScore) {
                for (int i = 0; i < senones.length; i++) {
                    float newScore = senones[i].getScore(feature);
                    if (newScore > logScore) {
                        logScore = newScore;
                    }
                }
            } else { // average score
                float logCumulativeScore = 0.0f;
                for (int i = 0; i < senones.length; i++) {
                    logCumulativeScore += senones[i].getScore(feature);
                }
                logScore = logCumulativeScore / senones.length;
            }


	    logLastScore = logScore;
	    logLastDataScored = feature;
	}
	return logScore + weight;
    }


    /**
     * Calculate scores for each component in the senone's
     * distribution. Not yet implemented.
     *
     * @param feature the current feature
     *
     * @return the score for the feature in LogMath
     */
    public float[] calculateComponentScore(Data feature) {
	assert false: "Not implemented!";
	return null;
    }

    /**
     * Returns the set of senones that compose this composite senone.
     * This method is only needed for unit testing.
     *
     * @return the array of senones.
     */
    public Senone[] getSenones() {
	return senones;
    }

    /**
     * Determines if two objects are equal
     *
     * @param o the object to compare to this.
     *
     * @return true if the objects are equal
     */
    public boolean equals(Object o) {
        if (!(o instanceof Senone)) {
            return false;
        }
        Senone other = (Senone) o;
        return this.getID() == other.getID();
    }


    /**
     * Returns the hashcode for this object
     *
     * @return the hashcode
     */
    public int hashCode() {
        long id = getID();
        int high = (int) ((id >> 32) & 0xffffffff);
        int low = (int) (id & 0xffffffff);
        return high + low;
    }

    /**
     * Gets the ID for this senone
     *
     * @return the senone id
     */
    public long getID() {
        long factor = 1L;
        long id = 0L;
        for (int i = 0; i < senones.length; i++) {
            id += senones[i].getID() * (factor);
            factor = factor * MAX_SENONES;
        }
        return id;
    }

    /**
     * Retrieves a string form of this object
     *
     * @return the string representation of this object
     */
    public String toString() {
        return "senone id: " + getID();
    }
}
