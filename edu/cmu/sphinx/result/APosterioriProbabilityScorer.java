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

import edu.cmu.sphinx.decoder.search.Token;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Measures the confidence of any token in the token tree as its
 * a posteriori probability. The a posteriori probability of any token 
 * in the token tree is defined as the ratio of the total likelihood scores 
 * of all paths through the token tree that pass through the token,
 * to the total likelihood score of all paths through the token tree.
 */
public class APosterioriProbabilityScorer {

    private Result result;
    private float totalLikelihoodScore;
    private Map pathScoreMap;

    /**
     * Constructs a APosterioriProbabilityScorer for the given Result
     * (which contains the token tree).
     *
     * @param result the Result to calculate a posteriori probabilities on
     */
    public APosterioriProbabilityScorer(Result result) {
	this.result = result;
	this.pathScoreMap = new HashMap();
	totalLikelihoodScore = calculateTotalLikelihoodScore(result);
    }

    /**
     * Calculates the a posteriori probability of the given token
     * in its token tree. The a posteriori probability of any token 
     * in the token tree is computed as the ratio of the total likelihood 
     * scores of all paths through the token tree that pass through the token,
     * to the total likelihood score of all paths through the token tree.
     *
     * @param token the token which a posteriori probability is returned
     *
     * @return the a posteriori probability of the given token
     */
    public float getScore(Token token) {
	float tokenPathScores = calculateTokenPathScores(token);
	System.out.println("Token paths: " + tokenPathScores);
	return tokenPathScores / totalLikelihoodScore;
    }

    /**
     * Calculates the total likelihood scores of all paths through the
     * token tree that pass through the given token.
     *
     * @param token the token
     *
     * @return the total likelihood scores of all paths through the token
     * tree that pass through the given token
     */
    private float calculateTokenPathScores(Token token) {
	float totalPathScore = 0.0f;

	// iterator through all paths to check if the path passes through
	// the given token
	for (Iterator i = result.getResultTokens().iterator(); i.hasNext(); ) {
	    Token path = (Token) i.next();
	    if (isTokenOnPath(token, path)) {
		// System.out.println("token on path");
		Float pathScore = (Float) pathScoreMap.get(path);
		totalPathScore += pathScore.floatValue();
	    } else {
		// System.out.println("token not on path");
	    }
	}
	return totalPathScore;
    }

    /**
     * Returns true if the given token is on the given token path.
     *
     * @param token the token to check if its on the path
     * @param path the last token of the path to check
     *
     * @return true if the token is on the path, false otherwise
     */
    private boolean isTokenOnPath(Token token, Token path) {
	if (token == null) {
	    return false;
	}
	while (path != null) {
	    if (token == path) {
		return true;
	    } else {
		path = path.getPredecessor();
	    }
	}
	return false;
    }

    /**
     * Calculates the total log likelihood score of all the paths through
     * the token tree of the given Result.
     *
     * @param result the Result
     *
     * @return the total log likelihood score of all paths through the
     * token tree of the given Result
     */
    private float calculateTotalLikelihoodScore(Result result) {
	float totalScore = 0.0f;
	for (Iterator i = result.getResultTokens().iterator(); i.hasNext(); ) {
	    Token token = (Token) i.next();
	    float pathScore = calculatePathLikelihoodScore(token);
	    totalScore += pathScore;
	    pathScoreMap.put(token, new Float(pathScore));
	}
	System.out.println("All Paths (" + result.getResultTokens().size() +
			   "): " + totalScore);
	return totalScore;
    }

    /**
     * Calculates the total log likelihood score of the path ending 
     * at the given token. The path likelihood score is computed using
     * the acoustic likelihoods of the tokens in the paths.
     *
     * @param token the ending Token of the path
     *
     * @return the total log likelihood score of the path ending at the
     * given token
     */
    private float calculatePathLikelihoodScore(Token token) {
	float totalAcousticScore = 0.0f;
	while (token != null) {
	    totalAcousticScore += token.getAcousticScore();
	    token = token.getPredecessor();
	}
	return totalAcousticScore;
    }
}
