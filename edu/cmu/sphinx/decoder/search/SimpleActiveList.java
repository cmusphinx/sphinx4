
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

import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StatisticsVariable;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * An active list that tries to be simple and correct. This type of
 * active list will be slow, but should exhibit correct behavior.
 * Faster versions of the ActiveList exist (HeapActiveList,
 * TreeActiveList).  
 *
 * This class is not thread safe and should only be used by  a single
 * thread.
 *
 * Note that all scores are maintained in the LogMath log domain
 */
public class SimpleActiveList implements ActiveList  {
    private SphinxProperties props = null;
    public int absoluteBeamWidth = 2000;
    public float relativeBeamWidth;
    private StatisticsVariable tokens;

    // when the list is changed these things should be
    // changed/updated as well

    private List tokenList = new LinkedList();


    /**
     * Creates an empty active list
     */
    public SimpleActiveList() {
    }

    /**
     * Creates a new relaxed fit active list with the given target
     * size
     *
     * @param props the sphinx properties
     *
     */
    public SimpleActiveList(SphinxProperties props) {
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
	this.absoluteBeamWidth = props.getInt(PROP_ABSOLUTE_BEAM_WIDTH,
                PROP_ABSOLUTE_BEAM_WIDTH_DEFAULT);
	double linearRelativeBeamWidth  
	    = props.getDouble(PROP_RELATIVE_BEAM_WIDTH,
                    PROP_RELATIVE_BEAM_WIDTH_DEFAULT);

        setRelativeBeamWidth(linearRelativeBeamWidth);

	tokens = StatisticsVariable.getStatisticsVariable(props.getContext(),
		    "tokensCreated");
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
	SimpleActiveList newList = new SimpleActiveList();
	newList.props = props;
	newList.absoluteBeamWidth = absoluteBeamWidth;
	newList.relativeBeamWidth = relativeBeamWidth;
	newList.tokens = tokens;

	return newList;
    }

    /**
     * Determines if a token with the given score
     * is insertable into the list
     *
     * @param logScore the entry score of the token to insert. The
     * score is in the log math domain
     * 
     * @return <code>true</code>  if its insertable
     */
    public boolean isInsertable(float logScore) {
	return true;
    }

    /**
     * Adds the given token to the list
     *
     * @param token the token to add
     */
    public void add(Token token) {
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
	tokenList.add(newToken);
	if (oldToken != null) {
	    if (!tokenList.remove(oldToken)) {
                // Some optional debugging code here to dump out the paths
                // when this "should never happen" error happens
                if (false) {
                    System.out.println("SimpleActiveList: remove " +
                            oldToken + " missing, but replaced by " +
                            newToken);
                        oldToken.dumpTokenPath(true);
                        newToken.dumpTokenPath(true);
                }
            }
	}
    }

    /**
     * Returns true if the token is scored high enough to grow.
     *
     * @param token the token to check
     *
     * @return <code>true</code> if the token is worth growing
     */
    public boolean isWorthGrowing(Token token) {
	return true;
    }


    /**
     * Purges excess members. Remove all nodes that fall below the
     * relativeBeamWidth
     *
     * @return a (possible new) active list
     */
    public ActiveList purge() {
	int count = 0;
	Collections.sort(tokenList, Token.COMPARATOR);

	if (tokenList.size() > 0) {
	    Token bestToken = (Token) tokenList.get(0);
	    float highestScore = bestToken.getScore();
	    float pruneScore = highestScore + relativeBeamWidth;

	    for (Iterator i = tokenList.iterator();
		    i.hasNext() && count < absoluteBeamWidth; count++) {
		Token token = (Token) i.next();
		if (token.getScore() <= pruneScore) {
		    break;
		}
	    }
	    tokenList = tokenList.subList(0, count);
	}
	return this;
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
     * Gets the set of all tokens
     *
     * @return the set of tokens
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

