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


package edu.cmu.sphinx.jsapi;

import java.util.Locale;

import javax.speech.EngineCreate;
import javax.speech.EngineException;
import javax.speech.Engine;
import javax.speech.recognition.RecognizerModeDesc;

/**
 * Provides information about a specific operationg mode of a sphinx
 * recognition engine.
 */
public class SphinxRecognizerModeDesc extends RecognizerModeDesc 
			implements EngineCreate {
    public SphinxRecognizerModeDesc() {
        super("Sphinx 4",		// engine name
	"tidigits",			// mode name
        Locale.US,
	Boolean.FALSE,			// running?
	Boolean.TRUE,			// dictationGrammarSupported
	null);				// profile[]
    }

    /**
     * Creats an engine with the properties specified by this
     * SphinxRecognizerModeDesc 
     *
     * @return a SphinxRecognizer
     *
     * @throws IllegalArgumentException if the properties of the
     * SphinxRecognizerModeDesc do not refer to a known engine or
     * engine mode.
     *
     * @throws EngineException if the engine could not be properly
     * created.
     *
     * @throws SecurityException if the caller does not have
     * permission to create an engine.
     *
     */
    public Engine createEngine() 
	throws IllegalArgumentException, EngineException, SecurityException {
        SphinxRecognizer r = new SphinxRecognizer(this);
        if (r == null) {
            throw new EngineException();
        }
	return r;
    }
}
