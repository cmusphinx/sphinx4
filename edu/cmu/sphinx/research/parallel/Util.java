
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

package edu.cmu.sphinx.research.parallel;

import edu.cmu.sphinx.decoder.search.Token;

/**
 */
public class Util {

    /**
     */
    public static void printTokenReplace(float oldScore, int oldFrameNumber,
                                         float newScore, int newFrameNumber) {
        System.out.println("Replacing oldToken: " + oldScore + ", frame: " +
                           oldFrameNumber);
        System.out.println("     with newToken: " + newScore + ", frame: " +
                           newFrameNumber);
    }
}
