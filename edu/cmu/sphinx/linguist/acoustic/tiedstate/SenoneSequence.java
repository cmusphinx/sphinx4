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

import java.io.Serializable;
import java.util.List;

/**
 * Contains an ordered list of senones. 
 */
public class SenoneSequence implements Serializable {
    private Senone[] senones;

   /**
    * a factory method that creates a SeononeSequence from a list of
    * senones.
    *
    * @param senoneList the list of senones
    *
    * @return a composite senone
    */
    public static  SenoneSequence create(List senoneList) {
	 return new SenoneSequence(
	     (Senone[]) senoneList.toArray(new Senone[senoneList.size()]));
     }

    /**
     * Constructs a senone sequence
     *
     * @param sequence the ordered set of senones for this sequence
     */
    public SenoneSequence(Senone[] sequence) {
	this.senones = sequence;
    }

    /**
     * Returns the ordered set of senones for this sequence
     *
     * @return	 the ordered set of senones for this sequence
     */
    public Senone[] getSenones() {
	return senones;
    }

    /**
     * Returns the hashCode for this object
     *
     * @return  the object hashcode
     */
    public int hashCode() {
        int hashCode = 31;
        for (int i = 0; i < senones.length; i++) {
            hashCode =  hashCode * 91 + senones[i].hashCode();
        }
        return hashCode;
    }

    /**
     * Returns true if the objects are equal
     *
     * @return true  if the objects are equal
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            if (o instanceof SenoneSequence) {
                SenoneSequence other = (SenoneSequence) o;
                if (senones.length != other.senones.length) {
                    return false;
                } else {
                    for (int i = 0; i < senones.length; i++) {
                        if (!senones[i].equals(other.senones[i])) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }
    }


    /**
     * Dumps this senone sequence
     *
     * @param msg a string annotation
     */
    public void dump(String msg) {
	System.out.println(" SenoneSequence " + msg + ":");
	for (int i = 0; i < senones.length; i++) {
	    senones[i].dump("  seq:");
	}
    }
}
