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

package edu.cmu.sphinx.frontend.plp;

/**
 * FrequencyWarper.java
 * Defines various frequency warping functions
 *
 * Created: Tue Dec 24 17:56:39 2002
 *
 * @author <a href="mailto:rsingh@cs.cmu.edu">rsingh</a>
 * @version 1.0
 */

public class FrequencyWarper {
    

    /**
     * Default constructor. No arguments are needed.
     */
    public FrequencyWarper (){}


   /**
     * Compute Bark frequency from linear frequency in hertz.
     * The function is:
     * bark = 6.0*log(hertz/600 + sqrt((hertz/600)^2 + 1))
     *
     * @param inputFreq the input frequency in Hertz
     *
     * @return the frequency in a Bark scale
     *
     */
    public double hertzToBark(double hertz) {
	double x = hertz/600;
	return (6.0*Math.log(x + Math.sqrt(x*x + 1)));
    }


    /**
     * Compute linear frequency in hertz from Bark frequency.
     * The function is:
     * hertz = 300*(exp(bark/6.0) - exp(-bark/6.0))
     *
     * @param inputFreq the input frequency in Barks
     *
     * @return the frequency in Hertz
     *
     */
    public double barkToHertz(double bark) {
	double x = bark/6.0;
	return (300.0*(Math.exp(x)-Math.exp(-x)));
    }
    
}// FrequencyWarper
