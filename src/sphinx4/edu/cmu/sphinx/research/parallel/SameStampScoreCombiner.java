
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

package edu.cmu.sphinx.research.parallel;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Combines the various unit feature stream scores in a CombineToken.
 * This SameStampScoreCombiner assumes that there can be more than one token
 * from each feature stream. It will take the highest scoring token
 * from each stream <b>with the same identifying stamp</b>, combine their
 * scores, and choose the one with the highest combined score.
 *
 * For example, if tokens T1 and T2 from feature F1 had stamps
 * s1 and s2, and tokens Ta and Tb from feature F2 had stamps s1 and s2,
 * we must compare combinedScore(s1,Ta)  and combineScore(s2,Tb), and retain 
 * the one for which the combined score is higher.
 * 
 * All scores are maintained internally in the LogMath logbase
 */
public class SameStampScoreCombiner implements ScoreCombiner {

    /**
     * Remove all tokens in the given token list that has the given stamp.
     *
     * @param tokenList the token list
     * @param stamp the stamp of tokens to remove
     *
     * @return a list of the removed tokens
     */
    public List removeTokensByStamp(List tokenList, String stamp) {
        List returnList = new LinkedList();
        for (ListIterator i = tokenList.listIterator(); i.hasNext(); ) {
            ParallelToken token = (ParallelToken) i.next();
            if (token.getLastCombineStamp().equals(stamp)) {
                returnList.add(token);
                i.remove();
            }
        }
        return returnList;
    }

    
    /**
     * Combines the scores from all the feature stream in the given
     * CombineToken. The feature stream(s) are represented in the form
     * of ParallelTokens in the given CombineToken.
     *
     * @param combineToken the CombineToken on which to combine the feature
     *   stream scores
     */
    public void combineScore(CombineToken combineToken) {

        // sort all the parallel tokens in the CombineToken
        // according to their last combine time

	List tokenList = Arrays.asList
            (combineToken.getParallelTokens().toArray());

	// System.out.println("TokenList size: " + tokenList.size());
        
        double logHighestCombinedScore = Double.NEGATIVE_INFINITY;
        List highestList = null;

        while (tokenList.size() > 0) {
            ParallelToken firstToken = (ParallelToken) tokenList.get(0);
            List sameStampList = removeTokensByStamp
                (tokenList, firstToken.getLastCombineStamp());
            
            double logCombinedScore = getCombinedScore(sameStampList);
	    
            // System.out.println("logCombinedScore = " + logCombinedScore);
            if (logCombinedScore > logHighestCombinedScore ||
		logHighestCombinedScore == Double.NEGATIVE_INFINITY) {
                logHighestCombinedScore = logCombinedScore;
                highestList = sameStampList;
            }
        }

	assert highestList != null & highestList.size() > 1;
        // System.out.println("Highest TokenList size: " + highestList.size());

	for (Iterator i = highestList.iterator(); i.hasNext(); ) {
	    ParallelToken token = (ParallelToken) i.next();
	    token.setCombinedScore((float)logHighestCombinedScore);
	}
        
        combineToken.clear();
        combineToken.addAll(highestList);
        combineToken.setCombinedScore((float)logHighestCombinedScore);
    }


    /**
     * Calculates the combined score of the given list of tokens.
     * This method assumes that all tokens have the same last combine time.
     * It will retain only the highest scoring token of each stream
     * and remove all the other lower scoring tokens from the list.
     *
     * @param sameStampTokenList the token list
     *
     * @return the combined log score
     */
    private double getCombinedScore(List sameStampTokenList) {
        Map uniqueMap = new HashMap();
        int combineTime = -1;

        // first retain the highest scoring token from each stream 
        for (Iterator i = sameStampTokenList.iterator(); i.hasNext(); ) {
            ParallelToken token = (ParallelToken) i.next();
            String modelName = token.getModelName();
            ParallelToken tokenInMap = 
                (ParallelToken) uniqueMap.get(modelName);
            if (tokenInMap == null) {
                uniqueMap.put(modelName, token);
            } else {
                if (token.getFeatureScore() > tokenInMap.getFeatureScore()) {
                    uniqueMap.put(modelName, token);
                }
            }
        }

        // clear the list
        sameStampTokenList.clear();
	assert sameStampTokenList.size() == 0;

        // now calculate the combinedScore
        double logTotalScore = 0;
        
        for (Iterator i = uniqueMap.values().iterator(); i.hasNext(); ) {
            ParallelToken pToken = (ParallelToken) i.next();
	    // System.out.println("Highest: " + tokenToString(pToken));
            sameStampTokenList.add(pToken);

            // in linear domain, the following expression is:
            // score = pToken.getFeatureScore()^pToken.getEta()
            
            double logScore = pToken.getFeatureScore() * pToken.getEta();
            
            // in linear domain, the following expression is:
            // totalScore *= score
            
            logTotalScore += logScore;
        }

        return logTotalScore;
    }

    private void checkSameTime(List tokenList) {
	System.out.print("SameTimeList: " );
	for (Iterator i = tokenList.iterator(); i.hasNext(); ) {
	    ParallelToken token = (ParallelToken) i.next();
	    System.out.print(tokenToString(token));
	}
	System.out.println();
    }

    private String tokenToString(ParallelToken token) {
	return (" (" + token.getLastCombineTime() + "," +
		token.getModelName() + "," +
		token.getFeatureScore() + ") ");
    }
}


