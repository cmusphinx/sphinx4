
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

package edu.cmu.sphinx.research.parallel;

import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.search.Pruner;
import edu.cmu.sphinx.decoder.search.Token;

import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Prunes an ActiveList of ParallelTokens based on their scores.
 * Pruning by this Pruner simply means performing a
 * <code>Token.setPruned(true)</code>.
 */
public abstract class TokenScorePruner implements edu.cmu.sphinx.decoder.search.Pruner {

    private int absoluteBeamWidth;
    private float relativeBeamWidth;
    private boolean doRelativePruning = false;

    private static Comparator tokenComparator = null;


    /**
     * Starts the pruner
     */
    public void start() {}


    /**
     * Returns the score that we use to compare this Token with
     * other Tokens.
     *
     * @param token the Token to compare
     */
    protected abstract float getTokenScore(edu.cmu.sphinx.decoder.search.Token token);


    /**
     * Returns a token comparator that is used to order ParallelTokens in
     * descending order of their combined score.
     *
     * @return a ParallelTokenComparator
     */
    protected Comparator getTokenComparator() {
        if (tokenComparator == null) {
            tokenComparator = new Comparator() {
                public int compare(Object o1, Object o2) {
                    edu.cmu.sphinx.decoder.search.Token t1 = (edu.cmu.sphinx.decoder.search.Token) o1;
		    edu.cmu.sphinx.decoder.search.Token t2 = (edu.cmu.sphinx.decoder.search.Token) o2;
                    
                    if (getTokenScore(t1) > getTokenScore(t2)) {
                        return -1;
                    } else if (getTokenScore(t1) == getTokenScore(t2)) {
                        return 0;
                    } else {
                        return 1;
                    }
                }
            };
        }
        return tokenComparator;
    }


    /**
     * Prunes the set of tokens in the given ActiveList. It is 
     * assumed that all token in the ActiveList are ParallelTokens.
     * Pruning is performed according to the absolute and relative
     * beam widths of the given ActiveList.
     *
     * @param activeList a activeList of tokens
     */
    public ActiveList prune(ActiveList activeList) {

	ActiveList newList = activeList.createNew();

	edu.cmu.sphinx.decoder.search.Token[] tokens = activeList.getTokens();
        List tokenList = Arrays.asList(tokens);
        Collections.sort(tokenList, getTokenComparator());

        if (tokenList.size() > 0) {
            edu.cmu.sphinx.decoder.search.Token bestToken = (edu.cmu.sphinx.decoder.search.Token) tokenList.get(0);
            float highestScore = getTokenScore(bestToken);
            float pruneScore = highestScore + relativeBeamWidth;

            int count = 0;  // the number of tokens included so far
            float lastScore = highestScore;
            float thisScore = highestScore;
            
            Iterator i = tokenList.iterator();
           
            // do the pruning
            while (i.hasNext() && newList.size() < absoluteBeamWidth) {

                edu.cmu.sphinx.decoder.search.Token token = (edu.cmu.sphinx.decoder.search.Token) i.next();
                thisScore = getTokenScore(token);

		if (doRelativePruning) {
		    if (thisScore > pruneScore) {
			newList.add(token);
			lastScore = thisScore;
		    } else {
			// token.setPruned(true);
			break;
		    }
		} else {
		    newList.add(token);
		    lastScore = thisScore;
		}
            }

            // include the next token(s) that have the same score
            // as the last included token
            /*
            while (i.hasNext()) {
                Token token = (Token) i.next();
                thisScore = getTokenScore(token);
		if (thisScore == lastScore) {
		    newList.add(token);
		} else {
		    // token.setPruned(true);
		    break;
		}
            }
            */
        }

	return newList;
    }


    /**
     * Returns the absolute beam width of this TokenScorePruner.
     *
     * @return the absolute beam width (in the linear domain)
     */
    public int getAbsoluteBeamWidth() {
        return this.absoluteBeamWidth;
    }


    /**
     * Sets the absolute beam width of this TokenScorePruner
     *
     * @param the absolute beam width
     */
    public void setAbsoluteBeamWidth(int absoluteBeamWidth) {
        this.absoluteBeamWidth = absoluteBeamWidth;
    }


    /**
     * Returns the relative beam width (in the linear domain) of this
     * TokenScorePruner
     *
     * @return the relative beam width (in the linear domain)
     */
    public float getRelativeBeamWidth() {
        return relativeBeamWidth;
    }


    /**
     * Sets the relative beam width (log domain) of this TokenScorePruner.
     *
     * @param the relative beam width in log domain
     */
    public void setRelativeBeamWidth(float relativeBeamWidth) {
        this.relativeBeamWidth = relativeBeamWidth;
    }


    /**
     * Performs post-recognition cleanup. 
     */
    public void stop() {}
}


