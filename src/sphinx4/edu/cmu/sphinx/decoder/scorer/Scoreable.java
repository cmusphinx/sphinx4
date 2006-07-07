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

package edu.cmu.sphinx.decoder.scorer;

import edu.cmu.sphinx.frontend.Data;

/**
 * Represents an entity that can be scored against a data
 */
public interface  Scoreable {

    /**
     * Calculates a score against the given data. The score can be
     * retreived with get score
     *
     * @param data the data to be scored
     * @param keepData whether this Scoreable should keep a reference
     *    to the given data
     * @param gain the gain to apply to the acoustic score
     *
     * @return the score for the data
     */
    public float calculateScore(Data data, boolean keepData, float gain);

    
    /**
     * Retrieves a previously calculated (and possibly normalized) score
     *
     * @return the score
     */
    public float getScore();

    /**
     * Normalizes a previously calculated score
     *
     * @return the normalized score
     */
    public float normalizeScore(float maxScore);


    /**
     * Returns the frame number that this Scoreable should be scored against.
     *
     * @return the frame number that this Scoreable should be scored against.
     */
    public int getFrameNumber();
}
