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

package edu.cmu.sphinx.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.text.DecimalFormat;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.result.Result;


/**
 * A utility that assists in optimizing relative and absolute beam
 */
public class BeamFinder {
    /**
     * Sphinx property that defines whether the beam finder is enabled
     */
    public final static String PROP_ENABLED  =
	"edu.cmu.sphinx.util.BeamFinder.enable";

    /**
     * The default value for the PROP_ENABLED property
     */
    public final static boolean PROP_ENABLED_DEFAULT = false;

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
    int totalUtterances;

    private final static DecimalFormat logFormatter =
        new DecimalFormat("0.#E0");

    /**
     * Creates the beam finder
     *
     * @param context the context of this Beamfinder
     */
    public BeamFinder(String context) {
	SphinxProperties props = SphinxProperties.getSphinxProperties(context);
	this.logMath = LogMath.getLogMath(context);
	enabled = props.getBoolean(PROP_ENABLED, PROP_ENABLED_DEFAULT);
    }

    /**
     * Determines if the beam finder is enabled
     *
     * @return true if the beam finder is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Ranks the given set of tokens
     *
     * @param result the result to process
     */
    public void process(Result result) {
        if (enabled) {
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
    }


    /**
     * Checks to make sure that all upstream tokens are ranked.
     * Primarily used fro debugging
     *
     * @param token the token to check
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
        if (enabled) {
            System.out.print(
                "   Beam Abs Max: " + maxAbsoluteBeam
                + "  Avg: " + avgAbsoluteBeam);
            System.out.println( "   Rel Max: " + 
                logFormatter.format(logMath.logToLinear(maxRelativeBeam))
                + "  Avg: " + 
                logFormatter.format(logMath.logToLinear(avgRelativeBeam)));
        }
    }

    /**
     * show the summary result
     */
    public void showSummary() {
        if (enabled) {
            System.out.print(
                "   Summary Beam Abs Max: " + totMaxAbsoluteBeam
                + "  Avg: " + sumAbsoluteBeam / totalUtterances );
            System.out.println( "   Rel Max: " + 
                logFormatter.format(logMath.logToLinear(totMaxRelativeBeam))
                + "  Avg: " + 
                logFormatter.format(logMath.logToLinear(
                        sumRelativeBeam/ totalUtterances)));
        }
    }


    /**
     * Collect statistics from the collected beam data
     *
     * @param result the result of interest
     */
    private void collectStatistics(Result result) {
        totalUtterances++;
        collectAbsoluteBeamStatistics(result);
        collectRelativeBeamStatistics(result);
    }

    /**
     * Collects the absolute beam statistics
     *
     * @param result the result of interest
     */
    private  void collectAbsoluteBeamStatistics(Result result) {
        Token  token = result.getBestToken();
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
     * Returns the maximum relative beam for a the chain of 
     * tokens reachable from the given token
     *
     * @param result the result of interest
     *
     */
    private void collectRelativeBeamStatistics(Result result) {
        Token  token = result.getBestToken();
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
 * A token application object that keeps track of the absolute and
 * relative rank of a token
 */
class TokenRank {
    private int absoluteRank;
    private float relativeRank;

    /**
     * Creates a token rank object 
     *
     * @param abs the absolute rank
     * @param rel the relative rank
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
        return "Rank[" + absoluteRank +"," + relativeRank + "]";
    }
}
