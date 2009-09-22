
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

package edu.cmu.sphinx.demo.jsapi.cardmatch;


/**
 * An interface that represents a spoken prompt
 */
public class SequencePrompt implements Prompt {
    private String[] prompts;
    private int curPrompt;

    /**
     * Creates a sequence prompt
     *
     * @param an array of prompts
     *
     */
    public SequencePrompt(String[] prompts) {
        this.prompts = prompts;
    }

    /**
     * Gets the next text to be spoken
     */
    public String getText() {
        String prompt = prompts[curPrompt++];
        if (curPrompt >= prompts.length) {
            curPrompt = 0;
        }
        return prompt;
    }


    /**
     * Resets the prompt to its initial state
     */
    public void reset() {
        curPrompt = 0;
    }
}
