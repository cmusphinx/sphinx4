/*
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.instrumentation;

import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.result.*;
import edu.cmu.sphinx.util.NISTAlign;
import edu.cmu.sphinx.util.props.*;

/**
 * Tracks and reports recognition accuracy using the "confidenceScorer" component specified in the ConfigurationManager.
 * The "confidenceScorer" component is typically configured to be edu.cmu.sphinx.result.SausageMaker.
 *
 * @see edu.cmu.sphinx.result.SausageMaker
 */
public class BestConfidenceAccuracyTracker extends AccuracyTracker {

    /** Defines the class to use for confidence scoring. */
    @S4Component(type = ConfidenceScorer.class)
    public final static String PROP_CONFIDENCE_SCORER = "confidenceScorer";

    /** The confidence scorer */
    protected ConfidenceScorer confidenceScorer;


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        confidenceScorer = (ConfidenceScorer) ps.getComponent(
                PROP_CONFIDENCE_SCORER
        );
    }


    /** Gets the transcription with no fillers and no "<unk>". */
    protected String getTranscriptionNoFiller(Path path) {
        StringBuffer buf = new StringBuffer();
        WordResult[] words = path.getWords();
        for (int i = 0; i < words.length; i++) {
            Word word = words[i].getPronunciation().getWord();
            if (!word.isFiller() && !word.getSpelling().equals("<unk>")) {
                buf.append(word.getSpelling()).append(" ");
            }
        }
        return (buf.toString().trim());
    }


    /** Gets the raw transcription */
    protected String getTranscriptionRaw(Path path) {
        StringBuffer buf = new StringBuffer();
        WordResult[] words = path.getWords();
        for (int i = 0; i < words.length; i++) {
            Word word = words[i].getPronunciation().getWord();
            buf.append(word.getSpelling()).append(" ");
        }
        return (buf.toString().trim());
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.result.ResultListener#newResult(edu.cmu.sphinx.result.Result)
    */
    public void newResult(Result result) {
        NISTAlign aligner = getAligner();
        String ref = result.getReferenceText();
        if (result.isFinal() && (ref != null)) {
            try {
                Path bestPath = null;
                String hyp = "";
                if (result.getBestFinalToken() != null) {
                    ConfidenceResult confidenceResult =
                            confidenceScorer.score(result);
                    bestPath = confidenceResult.getBestHypothesis();
                    hyp = getTranscriptionNoFiller(bestPath);
                }
                aligner.align(ref, hyp);
                if (bestPath != null) {
                    showDetails(getTranscriptionRaw(bestPath));
                } else {
                    showDetails("");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
