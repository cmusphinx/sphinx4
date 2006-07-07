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


import javax.speech.AudioException;
import javax.speech.EngineException;
import javax.speech.EngineStateError;

import com.sun.speech.engine.recognition.BaseRecognizer;

/**
 * A SphinxRecognizer provides access to Sphinx speech recognition
 * capabilities.
 */
public class SphinxRecognizer extends BaseRecognizer {
    
    /**
     * Creates the default sphinx recognizer
     */
    public SphinxRecognizer() {
    }

    /**
     * Creates a sphinx 4 recognizer that matches the given mode
     * descriptor
     *
     * @param modeDesc the mode descriptor describing the type of
     * recognizer to create
     */
    public SphinxRecognizer(SphinxRecognizerModeDesc modeDesc) {
    }

    /**
     * Pauses the audio stream for the engine, puts the engine into the
     * PAUSED state
     *
     * @throws EngineStateError - if called for an engine in the
     * DEALLOCATED or DEALLOCATING_RESOURCES states.
     */
    public void pause() throws EngineStateError {
    }
  
    /**
     * Resumes the audio stream for the engine, puts the engine into the
     * RESUMED state
     *
     * @throws EngineStateError - if called for an engine in the
     * DEALLOCATED or DEALLOCATING_RESOURCES states.
     * 
     * @throws AudioException  if unable to gain acces to the audio
     * channel
     *
     */
    public void resume() throws AudioException, EngineStateError {
    }
  
    /**
     * Allocate the resources required for the Engine and put it into
     * the ALLOCATED state
     *
     * @throws EngineException if an allocation error occurred or the
     * engine is not operational
     *
     * @throws EngineStateError if called for an engine in the
     * DEALLOCATING_RESOURCES state.
     */
    public void allocate() throws EngineException, EngineStateError {
    }
    
    /**
     * Free the resoures of the engine that were acquired during
     * allocation and operation, and return the engine to the
     * DEALLOCATED state.
     *
     * @throws EngineException if a deallocation error occurred 
     *
     * @throws EngineStateError if called for an engine in the
     * ALLOCATING_RESOURCES state.
     */
    public void deallocate() throws EngineException, EngineStateError {
    }
}


