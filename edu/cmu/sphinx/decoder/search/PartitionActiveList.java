
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
 * An active list that does absolute beam with pruning by partitioning 
 * the token list based on absolute beam width, instead of sorting the
 * token list, and then chopping the list up with the absolute beam width. 
 * The expected run time of this randomized partitioning algorithm is O(n), 
 * instead of O(n log n) for merge sort. 
 *
 * This class is not thread safe and should only be used by  a single
 * thread.
 *
 * Note that all scores are maintained in the LogMath log base.
 */
public class PartitionActiveList implements ActiveList  {

    private SphinxProperties props = null;
    private LogMath logMath;
    private int absoluteBeamWidth;
    private float relativeBeamWidth;

    // when the list is changed these things should be
    // changed/updated as well

    private List tokenList = new ArrayList(absoluteBeamWidth);
    private Partitioner partitioner = new Partitioner();


    /**
     * Creates an empty active list 
     */
    public PartitionActiveList() {
    }

    /**
     * Creates a new relaxed fit active list with the given target
     * size
     *
     * @param props the sphinx properties
     *
     */
    public PartitionActiveList(SphinxProperties props) {
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
        this.logMath = LogMath.getLogMath(props.getContext());

	this.absoluteBeamWidth = props.getInt
            (PROP_ABSOLUTE_BEAM_WIDTH, 
             PROP_ABSOLUTE_BEAM_WIDTH_DEFAULT);

	double linearRelativeBeamWidth  
	    = props.getDouble(PROP_RELATIVE_BEAM_WIDTH, 
			      PROP_RELATIVE_BEAM_WIDTH_DEFAULT);
	
	this.relativeBeamWidth = logMath.linearToLog(linearRelativeBeamWidth);

    }

    /**
     * Sets the absolute beam width of this list.
     *
     * @param absoluteBeamWidth the absolute beam width
     */
    public void setAbsoluteBeamWidth(int absoluteBeamWidth) {
        this.absoluteBeamWidth = absoluteBeamWidth;
    }


    /**
     * Sets the absolute beam width of this list.
     *
     * @param absoluteBeamWidth the absolute beam width
     */
    public void setRelativeBeamWidth(double linearRelativeBeamWidth) {
        this.relativeBeamWidth = logMath.linearToLog(linearRelativeBeamWidth);
    }


    /**
     * Creates a new version of this active list with
     * the same general properties as this list
     *
     * @return the new active list
     */
    public ActiveList createNew() {
	PartitionActiveList newList = new PartitionActiveList(props);
	newList.absoluteBeamWidth = absoluteBeamWidth;
	newList.relativeBeamWidth = relativeBeamWidth;

	return newList;
    }

    /**
     * Determines if a token with the given score
     * is insertable into the list
     *
     * @param logScore the score (in the LogMath log dmain)  
     * of score of the token to insert
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
                System.out.println("PartitionActiveList: replace " +
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

        float highestScore = 0.0f;
        float pruneScore = 0.0f;
        int pruned = 0;

        // if the absolute beam is zero, this means there
        // should be no constraint on the abs beam size at all
        // so we will only be relative beam pruning, which means
        // that we don't have to sort the list

        if (absoluteBeamWidth <= 0) {
            if (tokenList.size() > 0) {
                highestScore = getBestScore();
                pruneScore = highestScore + relativeBeamWidth;
                ArrayList newList = new ArrayList(tokenList.size());

                for (Iterator i = tokenList.iterator(); i.hasNext();) {
                    Token token = (Token) i.next();
                    if (token.getScore() > pruneScore) {
                        newList.add(token);
                    } else {
                        pruned++;
                    }
                }
                tokenList = newList;
            }
        } else {

        // if we have an absolute beam, then we will 
        // need to sort the tokens to apply the beam

            if (tokenList.size() > 0) {

                Token[] tokens = (Token[]) 
                    tokenList.toArray(new Token[tokenList.size()]);

                // "r" is the index of the last element in the partition
                int r = partitioner.partition(tokens, absoluteBeamWidth);

                highestScore = partitioner.getBestToken().getScore();
                pruneScore = highestScore + relativeBeamWidth;

                tokenList = new ArrayList(absoluteBeamWidth);

                for (int i = 0; i <= r; i++) {
                    Token token = tokens[i];
                    if (token.getScore() > pruneScore) {
                        tokenList.add(token);
                    }
                }
            }
        }

	return this;
    }


    /**
     * Returns the best score in the token list
     *
     * @return the best score
     */
    // TODO: it would be nice if the active list could maintain the high
    // score in the list


    private float getBestScore() {
        float bestScore = -Float.MAX_VALUE;
        int count = 0;
        for (Iterator i = tokenList.iterator(); i.hasNext(); ) {
            Token token = (Token) i.next();
            if (token.getScore() > bestScore) {
                bestScore = token.getScore();
            }
            count++;
        }
        return bestScore;
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

