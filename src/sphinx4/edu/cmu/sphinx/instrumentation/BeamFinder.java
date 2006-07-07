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

package edu.cmu.sphinx.instrumentation;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.RecognizerState;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.ResultListener;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.Resetable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * Monitors the absolute and relative beam sizes required to achieve the
 * optimum recognition results and reports this data.
 */
public class BeamFinder implements Configurable, ResultListener,
    Resetable, StateListener {
    /**
     * A Sphinx property that defines which recognizer to monitor
     */
    public final static String PROP_RECOGNIZER = "recognizer";

    /**
     * A Sphinx property that defines which recognizer to monitor
     */
    public final static String PROP_LOG_MATH = "logMath";

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
     * A sphinx property that define whether this beam tracker is enabled
     */
    public final static String PROP_ENABLED = "enable";
    /**
     * The default setting of PROP_ENABLED
     */
    public final static boolean PROP_ENABLED_DEFAULT = true;
    // ------------------------------
    // Configuration data
    // ------------------------------
    private String name;
    private Recognizer recognizer;
    private boolean showSummary;
    private boolean showDetails;
    private boolean enabled;
    private LogMath logMath;

    private int maxAbsoluteBeam;
    private int avgAbsoluteBeam;
    private float maxRelativeBeam;
    private float avgRelativeBeam;

    private int totMaxAbsoluteBeam;
    private int sumAbsoluteBeam;
    private float totMaxRelativeBeam;
    private float sumRelativeBeam;
    private int totalUtterances;

    private final static DecimalFormat logFormatter = new DecimalFormat("0.#E0");

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
        registry.register(PROP_LOG_MATH, PropertyType.COMPONENT);
        registry.register(PROP_SHOW_SUMMARY, PropertyType.BOOLEAN);
        registry.register(PROP_SHOW_DETAILS, PropertyType.BOOLEAN);
        registry.register(PROP_ENABLED, PropertyType.BOOLEAN);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        Recognizer newRecognizer = (Recognizer) ps.getComponent(
                PROP_RECOGNIZER, Recognizer.class);

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

        logMath = (LogMath) ps.getComponent(PROP_LOG_MATH, LogMath.class);

        showSummary = ps.getBoolean(PROP_SHOW_SUMMARY,
                PROP_SHOW_SUMMARY_DEFAULT);
        showDetails = ps.getBoolean(PROP_SHOW_DETAILS,
                PROP_SHOW_DETAILS_DEFAULT);
        enabled = ps.getBoolean(PROP_ENABLED, PROP_ENABLED_DEFAULT);
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
     * Resets the beam statistics
     */
    public void reset() {
        maxAbsoluteBeam = 0;
        avgAbsoluteBeam = 0;
        maxRelativeBeam = 0;
        avgRelativeBeam = 0;

        totMaxAbsoluteBeam = 0;
        sumAbsoluteBeam = 0;
        totMaxRelativeBeam = 0;
        sumRelativeBeam = 0;
        totalUtterances = 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.result.ResultListener#newResult(edu.cmu.sphinx.result.Result)
     */
    public void newResult(Result result) {
        if (enabled) {
            process(result);
            if (result.isFinal() && showDetails) {
                showLatestResult();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.recognizer.StateListener#statusChanged(edu.cmu.sphinx.recognizer.RecognizerState)
     */
    public void statusChanged(RecognizerState status) {
        if (enabled && status.equals(RecognizerState.DEALLOCATED)) {
            if (showSummary) {
                showSummary();
            }
        }
    }

    /**
     * Ranks the given set of tokens
     * 
     * @param result
     *                the result to process
     */
    private void process(Result result) {
        if (result.isFinal()) {
            collectStatistics(result);
        } else {
            List tokenList = result.getActiveTokens().getTokens();
            if (tokenList.size() > 0) {
                Collections.sort(tokenList, Token.COMPARATOR);
                Token bestToken = (Token) tokenList.get(0);
                int rank = 0;
                for (Iterator i = tokenList.iterator(); i.hasNext(); ) {
                    Token token = (Token) i.next();
                    float scoreDiff = bestToken.getScore() -
                        token.getScore();
                    assert scoreDiff >= 0;
                    token.setAppObject(new TokenRank(rank++, scoreDiff));
                    // checkRank(token);
                }
            }
        }
    }
    /**
     * Checks to make sure that all upstream tokens are ranked. Primarily used
     * fro debugging
     * 
     * @param token
     *                the token to check
     */
    private void checkRank(Token token) {
        while (token != null) {
            if (token.isEmitting()) {
                if (token.getAppObject() == null) {
                    if (token.getFrameNumber() != 0) {
                        System.out.println("MISSING " + token);
                    }
                } else {
                }
            }
            token = token.getPredecessor();
        }
    }

    /**
     * show the latest result
     */
    public void showLatestResult() {
        System.out.print("   Beam Abs Max: " + maxAbsoluteBeam + "  Avg: "
                + avgAbsoluteBeam);
        System.out.println("   Rel Max: "
                + logFormatter.format(logMath.logToLinear(maxRelativeBeam))
                + "  Avg: "
                + logFormatter.format(logMath.logToLinear(avgRelativeBeam)));
    }

    /**
     * show the summary result
     */
    public void showSummary() {
        System.out.print("   Summary Beam Abs Max: " + totMaxAbsoluteBeam
                + "  Avg: " + sumAbsoluteBeam / totalUtterances);
        System.out.println("   Rel Max: "
                + logFormatter.format(logMath.logToLinear(totMaxRelativeBeam))
                + "  Avg: "
                + logFormatter.format(logMath.logToLinear(sumRelativeBeam
                        / totalUtterances)));
    }

    /**
     * Collect statistics from the collected beam data
     * 
     * @param result
     *                the result of interest
     */
    private void collectStatistics(Result result) {
        totalUtterances++;
        collectAbsoluteBeamStatistics(result);
        collectRelativeBeamStatistics(result);
    }

    /**
     * Collects the absolute beam statistics
     * 
     * @param result
     *                the result of interest
     */
    private void collectAbsoluteBeamStatistics(Result result) {
        Token token = result.getBestToken();
        int count = 0;
        int sumBeam = 0;
        maxAbsoluteBeam = 0;
        while (token != null) {
            if (token.isEmitting()) {
                TokenRank rank = (TokenRank) token.getAppObject();
                if (rank != null) {
                    if (rank.getAbsoluteRank() > maxAbsoluteBeam) {
                        maxAbsoluteBeam = rank.getAbsoluteRank();
                    }
                    sumBeam += rank.getAbsoluteRank();
                    count++;
                } else {
                    if (token.getFrameNumber() > 0) {
                        System.out.println("Null rank! for " + token);
                    }
                }
            }
            token = token.getPredecessor();
        }

        if (count > 0) {
            avgAbsoluteBeam = sumBeam / count;
            if (maxAbsoluteBeam > totMaxAbsoluteBeam) {
                totMaxAbsoluteBeam = maxAbsoluteBeam;
            }
            sumAbsoluteBeam += avgAbsoluteBeam;
        }
    }

    /**
     * Returns the maximum relative beam for a the chain of tokens reachable
     * from the given token
     * 
     * @param result
     *                the result of interest
     *  
     */
    private void collectRelativeBeamStatistics(Result result) {
        Token token = result.getBestToken();
        int count = 0;
        double sumBeam = 0.0;

        maxRelativeBeam = -Float.MAX_VALUE;

        while (token != null) {
            if (token.isEmitting()) {
                TokenRank rank = (TokenRank) token.getAppObject();
                if (rank != null) {
                    if (rank.getRelativeRank() > maxRelativeBeam) {
                        maxRelativeBeam = rank.getRelativeRank();
                    }
                    sumBeam += rank.getRelativeRank();
                    count++;
                } else {
                    if (token.getFrameNumber() > 0) {
                        System.out.println("Null rank! for " + token);
                    }
                }
            }
            token = token.getPredecessor();
        }

        if (count > 0) {
            avgRelativeBeam = (float) (sumBeam / count);
            if (maxRelativeBeam > totMaxRelativeBeam) {
                totMaxRelativeBeam = maxRelativeBeam;
            }
            sumRelativeBeam += avgRelativeBeam;
        }
    }
}

/**
 * A token application object that keeps track of the absolute and relative
 * rank of a token
 */

class TokenRank {
    private int absoluteRank;
    private float relativeRank;

    /**
     * Creates a token rank object
     * 
     * @param abs
     *                the absolute rank
     * @param rel
     *                the relative rank
     */
    TokenRank(int abs, float rel) {
        absoluteRank = abs;
        relativeRank = rel;
    }

    /**
     * Gets the absolute rank
     * 
     * @return the absolute rank
     */
    int getAbsoluteRank() {
        return absoluteRank;
    }

    /**
     * Gets the relative rank
     * 
     * @return the relative rank
     */
    float getRelativeRank() {
        return relativeRank;
    }

    /**
     * Returns the string representation of this object
     * 
     * @return the string representation of this object
     */
    public String toString() {
        return "Rank[" + absoluteRank + "," + relativeRank + "]";
    }
}
