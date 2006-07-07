
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
 * This SameTimeScoreCombiner assumes that there can be more than one token
 * from each feature stream. It will take the highest scoring token
 * from each stream <b>at each last combine time</b>, combine their
 * scores, and choose the one with the highest combined score.
 *
 * For example, if tokens T1 and T2 from feature F1 had time stamps
 * (i.e., the last combine time) t1 and t2, and tokens Ta and Tb from
 * feature F2 had time stampes t1 and t2, we must compare
 * combinedScore(T1,Ta)  and combineScore(T2,Tb), and retain 
 * the one for which the combined score is higher.
 * 
 * All scores are maintained internally in the LogMath logbase
 */
public class SameTimeScoreCombiner implements ScoreCombiner {

    private SameTimeTokensReader reader;

    private static Comparator combineTimeComparator;

    private static Comparator getCombineTimeComparator() {
        if (combineTimeComparator == null) {
            combineTimeComparator = new Comparator() {
                public int compare(Object o1, Object o2) {
                    ParallelToken t1 = (ParallelToken) o1;
                    ParallelToken t2 = (ParallelToken) o2;
                    
                    if (t1.getLastCombineTime() < t2.getLastCombineTime()) {
                        return -1;
                    } else if (t1.getLastCombineTime() == 
                               t2.getLastCombineTime()) {
                        return 0;
                    } else {
                        return 1;
                    }
                }
            };
        }
        return combineTimeComparator;
    }        

    /**
     * Constructs a SameTimeScoreCombiner.
     *
     * @param timeDifference the maximum difference in time between
     *                       tokens for them to be considered approximately
     *                       the 'same time'
     */
    public SameTimeScoreCombiner(int timeDifference) {
	reader = new SameTimeTokensReader(timeDifference);
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
        Collections.sort(tokenList, getCombineTimeComparator());

	// System.out.println("TokenList size: " + tokenList.size());
        
        double logHighestCombinedScore = Double.NEGATIVE_INFINITY;
        List highestList = null;

	reader.reset(tokenList);

        while (reader.hasMoreTokens()) {
            List sameTimeList = reader.readNextSameTimeTokens();
            // checkSameTime(sameTimeList);
            double logCombinedScore = getCombinedScore(sameTimeList);
	    // System.out.println("logCombinedScore = " + logCombinedScore);
            if (logCombinedScore > logHighestCombinedScore ||
		logHighestCombinedScore == Double.NEGATIVE_INFINITY) {
                logHighestCombinedScore = logCombinedScore;
                highestList = sameTimeList;
            }
        }

	assert highestList != null & highestList.size() > 0;
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
     * @param sameTimeTokenList the token list
     *
     * @return the combined log score
     */
    private double getCombinedScore(List sameTimeTokenList) {
        Map uniqueMap = new HashMap();
        int combineTime = -1;

        // first retain the highest scoring token from each stream 
        for (Iterator i = sameTimeTokenList.iterator(); i.hasNext(); ) {
            ParallelToken token = (ParallelToken) i.next();
            /*
	    if (combineTime == -1) {
		combineTime = token.getLastCombineTime();
	    } else {
		assert token.getLastCombineTime() == combineTime;
	    }
            */
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
        sameTimeTokenList.clear();
	assert sameTimeTokenList.size() == 0;

        // now calculate the combinedScore
        double logTotalScore = 0;
        
        for (Iterator i = uniqueMap.values().iterator(); i.hasNext(); ) {
            ParallelToken pToken = (ParallelToken) i.next();
	    // System.out.println("Highest: " + tokenToString(pToken));
            sameTimeTokenList.add(pToken);

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


class SameTimeTokensReader {

    private ListIterator listIterator;
    private int timeDifference;

    SameTimeTokensReader(int timeDiff) {
        timeDifference = timeDiff;
        System.out.println("MaxTimeDiff: " + timeDifference);
    }

    public void reset(List tokenList) {
	this.listIterator = tokenList.listIterator();
    }

    public boolean hasMoreTokens() {
        return listIterator.hasNext();
    }

    public boolean isSameTime(int time1, int time2) {
        return (Math.abs(time1 - time2) <= timeDifference);
    }

    public List readNextSameTimeTokens() {
        List subList = new LinkedList();

	if (listIterator.hasNext()) {

	    // first token

	    ParallelToken token = (ParallelToken) listIterator.next();
	    subList.add(token);
	    int lastCombineTime = token.getLastCombineTime();

	    // then the next token(s) with the same combine time

	    while (listIterator.hasNext()) {
		ParallelToken nextToken = (ParallelToken) listIterator.next();
		if (isSameTime(nextToken.getLastCombineTime(),
                               lastCombineTime)) {
		    subList.add(nextToken);
		} else {
		    // return to the previous element
		    listIterator.previous();
		    break;
		}
	    }
	}

        return subList;
    }
}

