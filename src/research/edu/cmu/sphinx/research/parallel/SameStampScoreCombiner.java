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

import java.util.*;

/**
 * Combines the various unit feature stream scores in a CombineToken. This SameStampScoreCombiner assumes that there can
 * be more than one token from each feature stream. It will take the highest scoring token from each stream <b>with the
 * same identifying stamp</b>, combine their scores, and choose the one with the highest combined score.
 * <p/>
 * For example, if tokens T1 and T2 from feature F1 had stamps s1 and s2, and tokens Ta and Tb from feature F2 had
 * stamps s1 and s2, we must compare combinedScore(s1,Ta)  and combineScore(s2,Tb), and retain the one for which the
 * combined score is higher.
 * <p/>
 * All scores are maintained internally in the LogMath logbase
 */
public class SameStampScoreCombiner implements ScoreCombiner {

    /**
     * Remove all tokens in the given token list that has the given stamp.
     *
     * @param tokenList the token list
     * @param stamp     the stamp of tokens to remove
     * @return a list of the removed tokens
     */
    public List<ParallelToken> removeTokensByStamp(List<ParallelToken> tokenList, String stamp) {
        List<ParallelToken> returnList = new LinkedList<ParallelToken>();
        for (Iterator<ParallelToken> i = tokenList.iterator(); i.hasNext();) {
            ParallelToken token = i.next();
            if (token.getLastCombineStamp().equals(stamp)) {
                returnList.add(token);
                i.remove();
            }
        }
        return returnList;
    }


    /**
     * Combines the scores from all the feature stream in the given CombineToken. The feature stream(s) are represented
     * in the form of ParallelTokens in the given CombineToken.
     *
     * @param combineToken the CombineToken on which to combine the feature stream scores
     */
    public void combineScore(CombineToken combineToken) {

        // sort all the parallel tokens in the CombineToken
        // according to their last combine time

        List<ParallelToken> tokenList = new ArrayList<ParallelToken>(combineToken.getParallelTokens());

        // System.out.println("TokenList size: " + tokenList.size());

        double logHighestCombinedScore = Double.NEGATIVE_INFINITY;
        List<ParallelToken> highestList = null;

        while (!tokenList.isEmpty()) {
            ParallelToken firstToken = tokenList.get(0);
            List<ParallelToken> sameStampList = removeTokensByStamp
                    (tokenList, firstToken.getLastCombineStamp());

            double logCombinedScore = getCombinedScore(sameStampList);

            // System.out.println("logCombinedScore = " + logCombinedScore);
            if (logCombinedScore > logHighestCombinedScore ||
                    logHighestCombinedScore == Double.NEGATIVE_INFINITY) {
                logHighestCombinedScore = logCombinedScore;
                highestList = sameStampList;
            }
        }

        assert highestList != null && highestList.size() > 1;
        // System.out.println("Highest TokenList size: " + highestList.size());

        for (ParallelToken token : highestList) {
            token.setCombinedScore((float)logHighestCombinedScore);
        }

        combineToken.clear();
        combineToken.addAll(highestList);
        combineToken.setCombinedScore((float) logHighestCombinedScore);
    }


    /**
     * Calculates the combined score of the given list of tokens. This method assumes that all tokens have the same last
     * combine time. It will retain only the highest scoring token of each stream and remove all the other lower scoring
     * tokens from the list.
     *
     * @param sameStampTokenList the token list
     * @return the combined log score
     */
    private double getCombinedScore(List<ParallelToken> sameStampTokenList) {
        Map<String, ParallelToken> uniqueMap = new HashMap<String, ParallelToken>();

        // first retain the highest scoring token from each stream 
        for (ParallelToken token : sameStampTokenList) {
            String modelName = token.getModelName();
            ParallelToken tokenInMap = uniqueMap.get(modelName);
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
        assert sameStampTokenList.isEmpty();

        // now calculate the combinedScore
        double logTotalScore = 0;

        for (ParallelToken pToken : uniqueMap.values()) {
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
}


