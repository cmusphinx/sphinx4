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


package edu.cmu.sphinx.frontend.endpoint;

import edu.cmu.sphinx.frontend.Audio;
import edu.cmu.sphinx.frontend.AudioSource;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.Signal;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;

import java.util.*;


/**
 * An interface for audio-based endpointers.
 */
public interface AudioEndpointer extends AudioSource {

    /**
     * Initializes an AudioEndpointer with the given name, context,
     * and AudioSource predecessor.
     *
     * @param name the name of this AudioEndpointer
     * @param context the context of the SphinxProperties this
     *    EnergyAudioEndpointer uses
     * @param props the SphinxProperties to read properties from
     * @param predecessor the AudioSource where this AudioEndpointer
     *    gets Audio from
     *
     * @throws java.io.IOException
     */
    public void initialize(String name, String context, 
                           SphinxProperties props,
                           AudioSource predecessor) throws IOException;
}
