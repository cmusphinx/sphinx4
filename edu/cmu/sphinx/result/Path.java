

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

package edu.cmu.sphinx.result;

import edu.cmu.sphinx.result.WordResult;

/**
 * Represents a path of words through the recognition result lattice.
 * 
 * All scores are maintained in the logMath log domain
 */
public abstract class Path {


    /**
     * No constructor defined yet
     */
    private  Path() {}
    /**
     * Gets the total score for this path. Scores are in the LogMath
     * log domain
     *
     * @return the score for the path in the LogMath log domaain.
     */
    public abstract float getScore();

    /**
     * Gets the ordered set of words for this path
     *
     * @return an array containing zero or more words 
     */
    public abstract WordResult[] getWords();


    /**
     * Gets the transcription of the path. 
     *
     * @return the transcription of the path.
     */
    public abstract String getTranscription();

    /**
     * Returns a string representation of this object
     */
    public abstract String toString();
}

