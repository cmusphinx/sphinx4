/*
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
package edu.cmu.sphinx.result;

/**
 * Computes confidences for a Result.
 */
public interface ConfidenceScorer {

    /**
     * Computes confidences for a Result and returns a ConfidenceResult,
     * a compact representation of all the hypothesis contained in the
     * result together with their per-word and per-path confidences.
     *
     * @param result the result to compute confidences for
     * @return a confidence result
     */
    public ConfidenceResult score(Result result);
}
