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

import java.util.List;
import java.io.Serializable;

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
