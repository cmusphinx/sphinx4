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
 * An AudioSource produces Audio objects.
 */
public interface AudioSource {

    /**
     * Returns the next Audio object produced by this AudioSource.
     *
     * @return the next available Audio object, returns null if no
     *     Audio object is available
     *
     * @throws java.io.IOException
     */
    public Audio getAudio() throws IOException;
}
