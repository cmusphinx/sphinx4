/*
 * 
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.recognizer;
/**
 * Defines the possible states of the recognizer.
 */
public class RecognizerState {
    private String name;
    /**
     * The recognizer is in the deallocated state. No resources are in use by
     * this recognizer.
     */
    public final static RecognizerState DEALLOCATED = new RecognizerState(
            "Deallocated");
    /**
     * The recognizer is allocating resources
     */
    public final static RecognizerState ALLOCATING = new RecognizerState(
            "Allocating");
    /**
     * The recognizer has been allocated
     */
    public final static RecognizerState ALLOCATED = new RecognizerState(
            "Allocated");
    
    /**
     * The recognizer is ready to recognize
     */
    public final static RecognizerState READY = new RecognizerState(
            "Ready");
    /**
     * The recognizer is recognizing speech
     */
    public final static RecognizerState RECOGNIZING = new RecognizerState(
            "Recognizing");
    /**
     * The recognizer is deallocating resources
     */
    public final static RecognizerState DEALLOCATING = new RecognizerState(
            "Deallocating");
    /**
     * The recognizer is in an error state
     */
    public final static RecognizerState ERROR = new RecognizerState("Error");
    private RecognizerState(String name) {
        this.name = name;
    }
    public String toString() {
        return name;
    }
}
