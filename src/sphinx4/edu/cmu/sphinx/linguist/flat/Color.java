

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
import java.io.Serializable;


/**
 * A Color is used to tag SentenceHMM nodes
 */
public class Color implements Serializable {

    /**
     * The Color red
     */
    public final static Color RED = new Color("red");

    /**
     * The Color Green
     */
    public final static Color GREEN = new Color("green");

    private String name;

    /**
     * Constructs a color.  This is private to ensure that the set of
     * colors are defined as constants.
     *
     * @param name the name of the color.
     */
    private Color(String name) {
	this.name = name;
    }

    /**
     * Returns a string representation of this color
     *
     * @return the string representation
     */
    public String toString() {
	return name;
    }
}
