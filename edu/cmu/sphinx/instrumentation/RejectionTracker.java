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
package edu.cmu.sphinx.instrumentation;

import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.RecognizerState;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.ResultListener;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.Resetable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * Tracks and reports rejection accuracy.
 */
public class RejectionTracker implements Configurable,
                                         ResultListener,
                                         Resetable {

    /**
     * A Sphinx property that defines which recognizer to monitor
     */
    public final static String PROP_RECOGNIZER = "recognizer";

    /**
     * A sphinx property that define whether summary accuracy information is
     * displayed
     */
    public final static String PROP_SHOW_SUMMARY = "showSummary";

    /**
     * The default setting of PROP_SHOW_SUMMARY
     */
    public final static boolean PROP_SHOW_SUMMARY_DEFAULT = true;

    /**
     * A sphinx property that define whether detailed accuracy information is
     * displayed
     */
    public final static String PROP_SHOW_DETAILS = "showDetails";

    /**
     * The default setting of PROP_SHOW_DETAILS
     */
    public final static boolean PROP_SHOW_DETAILS_DEFAULT = true;


    // ------------------------------
    // Configuration data
    // ------------------------------
    private String name;
    private Recognizer recognizer;
    private boolean showSummary;
    private boolean showDetails;

    /**
     * total number of utterances
     */
    private int numUtterances;

    /**
     * actual number of out-of-grammar utterance
     */
    private int numOutOfGrammarUtterances;

    /**
     * number of correctly classified in-grammar utterances
     */
    private int numCorrectOutOfGrammarUtterances;

    /**
     * number of in-grammar utterances misrecognized as out-of-grammar
     */
    private int numFalseOutOfGrammarUtterances;

    /**
     * number of correctly classified out-of-grammar utterances
     */
    private int numCorrectInGrammarUtterances;

    /**
     * number of out-of-grammar utterances misrecognized as in-grammar
     */
    private int numFalseInGrammarUtterances;


    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        this.name = name;
        registry.register(PROP_RECOGNIZER, PropertyType.COMPONENT);
        registry.register(PROP_SHOW_SUMMARY, PropertyType.BOOLEAN);
        registry.register(PROP_SHOW_DETAILS, PropertyType.BOOLEAN);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        Recognizer newRecognizer = (Recognizer)
            ps.getComponent(PROP_RECOGNIZER, Recognizer.class);
        
        if (recognizer == null) {
            recognizer = newRecognizer;
            recognizer.addResultListener(this);
        } else if (recognizer != newRecognizer) {
            recognizer.removeResultListener(this);
            recognizer = newRecognizer;
            recognizer.addResultListener(this);
        }
        
        showSummary = ps.getBoolean(PROP_SHOW_SUMMARY,
                                    PROP_SHOW_SUMMARY_DEFAULT);
        showDetails = ps.getBoolean(PROP_SHOW_DETAILS,
                                    PROP_SHOW_DETAILS_DEFAULT);
    }


    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.instrumentation.Resetable
     */
    public void reset() {
        numUtterances = 0;
        numOutOfGrammarUtterances = 0;
        numCorrectOutOfGrammarUtterances = 0;
        numFalseOutOfGrammarUtterances = 0;
        numCorrectInGrammarUtterances = 0;
        numFalseInGrammarUtterances = 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.result.ResultListener#newResult(edu.cmu.sphinx.result.Result)
     */
    public void newResult(Result result) {
        String ref = result.getReferenceText();
        if (result.isFinal() && ref != null) {
            numUtterances++;
            String hyp = result.getBestResultNoFiller();
            if (ref.equals("<unk>")) {
                numOutOfGrammarUtterances++;
                if (hyp.equals("<unk>")) {
                    numCorrectOutOfGrammarUtterances++;
                } else {
                    numFalseInGrammarUtterances++;
                }
            } else {
                if (hyp.equals("<unk>")) {
                    numFalseOutOfGrammarUtterances++;
                } else {
                    numCorrectInGrammarUtterances++;
                }
            }
            if (showSummary) {
                float correctPercent = ((float)
                    (numCorrectOutOfGrammarUtterances +
                     numCorrectInGrammarUtterances)) /
                    ((float) numUtterances) * 100f;
                System.out.println
                    ("   Rejection Accuracy: " + correctPercent + "%");
            }
            if (showDetails) {
                System.out.println
                    ("   Correct OOG: " + numCorrectOutOfGrammarUtterances +
                     "   False OOG: " + numFalseOutOfGrammarUtterances +
                     "   Correct IG: " + numCorrectInGrammarUtterances +
                     "   False IG: " + numFalseInGrammarUtterances);
            }
        }
    }
}
