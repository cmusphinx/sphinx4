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


package edu.cmu.sphinx.frontend;

import java.io.IOException;


/**
 * A FeatureFrameSource produces FeatureFrames.
 */
public interface FeatureFrameSource {

    /**
     * Returns the next N FeatureFrames produced by this FeatureFrameSource.
     *
     * @return the next N FeatureFrames, returns null if no
     *     FeatureFrame object is available
     *
     * @throws java.io.IOException
     */
    public FeatureFrame getFeatureFrame(int numberFrames) throws IOException;
}
