
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

package edu.cmu.sphinx.decoder.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.LinkedList;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.decoder.scorer.Scoreable;

/**
 * An active list that tries to be simple and correct. This type of
 * active list will be slow, but should exhibit correct behavior.
 * Faster versions of the ActiveList exist (HeapActiveList,
 * TreeActiveList).  
 *
 * This class is not thread safe and should only be used by  a single
 * thread.
 *
 * Note that all scores are maintained in the LogMath log base.
 */
public class SortingActiveList implements ActiveList  {

    private SphinxProperties props = null;
    private int absoluteBeamWidth;
    private float relativeBeamWidth;
    private boolean showSameScoreEdgeTokens = false;
    private Token bestToken;

    // when the list is changed these things should be
    // changed/updated as well

    private List tokenList = new ArrayList(absoluteBeamWidth);


    /**
     * Creates an empty active list 
     */
    public SortingActiveList() {
    }

    /**
     * Creates a new relaxed fit active list with the given target
     * size
     *
     * @param props the sphinx properties
     *
     */
    public SortingActiveList(SphinxProperties props) {
	setProperties(props);
    }

    /**
     * Returns the SphinxProperties of this list.
     *
     * @return the properties of this list
     */
    public SphinxProperties getProperties() {
        return this.props;
    }


    /**
     * Sets the properties for this list
     *
     * @param props the properties for this list
     */
    public void setProperties(SphinxProperties props) {
	this.props = props;
	this.absoluteBeamWidth = props.getInt
            (PROP_ABSOLUTE_BEAM_WIDTH, 
             PROP_ABSOLUTE_BEAM_WIDTH_DEFAULT);

	double linearRelativeBeamWidth  
	    = props.getDouble(PROP_RELATIVE_BEAM_WIDTH, 
			      PROP_RELATIVE_BEAM_WIDTH_DEFAULT);
        
        setRelativeBeamWidth(linearRelativeBeamWidth);
    }


    /**
     * Sets the absolute beam width.
     *
     * @param absoluteBeamWidth the absolute beam width
     */
    public void setAbsoluteBeamWidth(int absoluteBeamWidth) {
        this.absoluteBeamWidth = absoluteBeamWidth;
    }


    /**
     * Sets the relative beam width.
     *
     * @param relativeBeamWidth the linear relative beam width
     */
    public void setRelativeBeamWidth(double relativeBeamWidth) {
        LogMath logMath = LogMath.getLogMath(props.getContext());
        this.relativeBeamWidth = logMath.linearToLog(relativeBeamWidth);
    }


    /**
     * Creates a new version of this active list with
     * the same general properties as this list
     *
     * @return the new active list
     */
    public ActiveList createNew() {
	SortingActiveList newList = new SortingActiveList();
	newList.props = props;
	newList.absoluteBeamWidth = absoluteBeamWidth;
	newList.relativeBeamWidth = relativeBeamWidth;

	return newList;
    }


    /**
     * Adds the given token to the list
     *
     * @param token the token to add
     */
    public void add(Token token) {
	token.setLocation(tokenList.size());
	tokenList.add(token);
    }


    /**
     * Replaces an old token with a new token
     *
     * @param oldToken the token to replace (or null in which case,
     * replace works like add).
     *
     * @param newToken the new token to be placed in the list.
     *
     */
    public void replace(Token oldToken, Token newToken) {
	if (oldToken != null) {
	    int location = oldToken.getLocation();

            // BUG:CHeck:

            if (tokenList.get(location) != oldToken) {
                System.out.println("SortingActiveList: replace " +
                        oldToken + " not where it should have been.  New " 
                        + newToken + " location is " + location 
                        + " found " + tokenList.get(location));
            }
	    tokenList.set(location, newToken);
	    newToken.setLocation(location);
	} else {
	    add(newToken);
	}
    }


    /**
     * Purges excess members. Reduce the size of the token list to the
     * absoluteBeamWidth
     *
     * @return a (possible new) active list
     */
    public ActiveList purge() {
        // if the absolute beam is zero, this means there
        // should be no constraint on the abs beam size at all
        // so we will only be relative beam pruning, which means
        // that we don't have to sort the list

        if (absoluteBeamWidth > 0 && tokenList.size() > absoluteBeamWidth ) {
            Collections.sort(tokenList, Token.COMPARATOR);
            tokenList = tokenList.subList(0, absoluteBeamWidth);
        }
	return this;
    }




    /**
     * gets the beam threshold best upon the best scoring token
     *
     * @return the beam threshold
     */
    public float getBeamThreshold() {
        return getBestScore() + relativeBeamWidth;
    }

    /**
     * gets the best score in the list
     *
     * @return the best score
     */
    public float getBestScore() {
        float bestScore = -Float.MAX_VALUE;
        if (bestToken != null) {
            bestScore = bestToken.getScore();
        }
        return bestScore;
    }

    /**
     * Sets the best scoring token for this active list
     *
     * @param token the best scoring token
     */
    public void setBestToken(Token token) {
        bestToken = token;
    }

    /**
     * Gets the best scoring token for this active list
     *
     * @return the best scoring token
     */
    public Token getBestToken() {
        return bestToken;
    }

    /**
     * Retrieves the iterator for this tree. 
     *
     * @return the iterator for this token list
     */
    public Iterator iterator() {
	return tokenList.iterator();
    }

    /**
     * Gets the list of all tokens
     *
     * @return the list of tokens
     */
    public List getTokens() {
        return tokenList;
    }


    /**
     * Returns the number of tokens on this active list
     *
     * @return the size of the active list
     */
    public final int size() {
	return tokenList.size();
    }
}

