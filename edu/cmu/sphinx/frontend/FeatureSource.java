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
 * A FeatureSource produces Features.
 */
public interface FeatureSource extends DataSource {

    /**
     * Returns the next Feature object produced by this FeatureSource.
     * The Feature objects belonging to a single Utterance should be
     * preceded by a Feature object with the Signal.UTTERANCE_START, and
     * ended by a Feature object wtih the Signal.UTTERANCE_END.
     *
     * @return the next available Feature object, returns null if no
     *     Feature object is available
     *
     * @throws java.io.IOException
     */
    public Feature getFeature() throws IOException;
}
