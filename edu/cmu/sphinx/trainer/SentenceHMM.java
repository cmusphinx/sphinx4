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

package edu.cmu.sphinx.trainer;


import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.knowledge.acoustic.*;
import edu.cmu.sphinx.decoder.search.*;

/**
 * Provides mechanisms for building and maintaining a sentence HMM.
 */
public interface  SentenceHMM {

    /**
     * Initializes the sentenceHMM. This performs the same function in
     * the trainer as the linguist does in the decoder.
     *
     * @param context the context to use
     * @param models the set of HMMs to be associated with the sentence HMMs
     */
    public void initialize(String context, AcousticModel[] models);

    /**
     * Starts the SentenceHMM.
     */
    public void start();

    /**
     * Stops the SentenceHMM.
     */
    public void stop();

    /**
     * Creates the sentenceHMM from a given transcription sentence.
     *
     * @param transcriptSentence the transcript sentence from where
     * the sentence HMM will be created.
     */
    public void buildSentenceHMM(String transcriptSentence);

    /**
     * Gets prob for a given state.
     *
     * @param hmmState the HMM state
     * @param feature the current incoming feature
     */
    public Double getScore(HMMState hmmState, Feature feature);

    /**
     * Accumulates counts into pool.
     *
     * @param models the set of acoustic models
     * @param unit the HMM Unit
     * @param state the HMM state whose accumulators are to be updated.
     * @param probability the posterior probability
     */
    public void accumulateCount(AcousticModel[] models, Unit unit, 
				HMMState state, Double probability);
}
