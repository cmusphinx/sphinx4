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
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.ResultListener;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.util.NISTAlign;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.Resetable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * Tracks and reports recognition accuracy
 */
public class AccuracyTracker
        implements
            Configurable,
            ResultListener,
            Resetable,
            StateListener {
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
    /**
     * A sphinx property that define whether the full token path is
     * displayed
     */
    public final static String PROP_SHOW_FULL_PATH = "showFullPath";
    /**
     * The default setting of PROP_SHOW_FULL_PATH
     */
    public final static boolean PROP_SHOW_FULL_PATH_DEFAULT = false;
    /**
     * A sphinx property that define whether recognition results
     * should be displayed.
     */
    public final static String PROP_SHOW_RESULTS = "showResults";
    /**
     * The default setting of PROP_SHOW_DETAILS
     */
    public final static boolean PROP_SHOW_RESULTS_DEFAULT = true;

    
    /**
     * A sphinx property that define whether recognition results
     * should be displayed.
     */
    public final static String PROP_SHOW_ALIGNED_RESULTS = "showAlignedResults";
    /**
     * The default setting of PROP_SHOW_ALIGNED_RESULTS
     */
    public final static boolean PROP_SHOW_ALIGNED_RESULTS_DEFAULT = true;
    
    /**
     * A sphinx property that define whether recognition results
     * should be displayed.
     */
    public final static String PROP_SHOW_RAW_RESULTS = "showRawResults";
    /**
     * The default setting of PROP_SHOW_RAW_RESULTS
     */
    public final static boolean PROP_SHOW_RAW_RESULTS_DEFAULT = true;
    
    // ------------------------------
    // Configuration data
    // ------------------------------
    private String name;
    private Recognizer recognizer;
    private boolean showSummary;
    private boolean showDetails;
    private boolean showResults;
    private boolean showAlignedResults;
    private boolean showRaw;
    private boolean showFullPath;
    
    private NISTAlign aligner = new NISTAlign(false, false);

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
        registry.register(PROP_SHOW_RESULTS, PropertyType.BOOLEAN);
        registry.register(PROP_SHOW_ALIGNED_RESULTS, PropertyType.BOOLEAN);
        registry.register(PROP_SHOW_RAW_RESULTS, PropertyType.BOOLEAN);       
        registry.register(PROP_SHOW_FULL_PATH, PropertyType.BOOLEAN);       

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        Recognizer newRecognizer = (Recognizer) ps.getComponent(PROP_RECOGNIZER,
                Recognizer.class);
        
        if (recognizer == null) {
            recognizer = newRecognizer;
            recognizer.addResultListener(this);
            recognizer.addStateListener(this);
        } else if (recognizer != newRecognizer) {
            recognizer.removeResultListener(this);
            recognizer.removeStateListener(this);
            recognizer = newRecognizer;
            recognizer.addResultListener(this);
            recognizer.addStateListener(this);
        }
        
        showSummary = ps.getBoolean(PROP_SHOW_SUMMARY,
                PROP_SHOW_SUMMARY_DEFAULT);
        showDetails = ps.getBoolean(PROP_SHOW_DETAILS,
                PROP_SHOW_DETAILS_DEFAULT);
        showResults = ps.getBoolean(PROP_SHOW_RESULTS,
                PROP_SHOW_RESULTS_DEFAULT);
        showAlignedResults = ps.getBoolean(PROP_SHOW_ALIGNED_RESULTS,
                PROP_SHOW_ALIGNED_RESULTS_DEFAULT);
        showFullPath = ps.getBoolean(PROP_SHOW_FULL_PATH,
                PROP_SHOW_FULL_PATH_DEFAULT);
        
        showRaw = ps.getBoolean(PROP_SHOW_RAW_RESULTS,
                PROP_SHOW_RAW_RESULTS_DEFAULT);
        aligner.setShowResults(showResults);
        aligner.setShowAlignedResults(showAlignedResults);
    }


    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.instrumentation.Resetable
     */
    public void reset() {
        aligner.resetTotals();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }
    
    /**
     * Retrieves the aligner used to track the accuracy stats
     * @return the aligner
     */
    public NISTAlign getAligner() {
        return aligner;
    }


    /**
     * Dumps the best path 
     *
     * @param result the result to dump
     */
    private void dumpBestPath(Result result) {
        System.out.println();
        Token bestToken = result.getBestToken();
        if (bestToken != null) {
            bestToken.dumpTokenPath();
        } else {
            System.out.println("Null result");
        }
        System.out.println(); 
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.result.ResultListener#newResult(edu.cmu.sphinx.result.Result)
     */
    public void newResult(Result result) {
        String ref = result.getReferenceText();
        if (result.isFinal() && ref != null) {
            String hyp = result.getBestResultNoFiller();
            aligner.align(ref, hyp);
            if (showFullPath) {
                dumpBestPath(result);
            }
            if (showDetails) {
                System.out.println();
                aligner.printSentenceSummary();
                if (showRaw) {
                    System.out.println("RAW     " + result.toString());
                }
                System.out.println(); 
                aligner.printTotalSummary();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.recognizer.StateListener#statusChanged(edu.cmu.sphinx.recognizer.RecognizerState)
     */
    public void statusChanged(RecognizerState status) {
        if (status.equals(RecognizerState.DEALLOCATED)) {
            if (showSummary) {
                System.out.println("\n# --------------- Summary statistics ---------");
                aligner.printTotalSummary();
            }
        }
    }
}
