
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
import java.util.Comparator;
import java.util.TreeSet;
import java.util.Random;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.decoder.search.ActiveList;

/**
 * An active list that tries to be fast by not keeping the elements in
 * any particular order, but still contrains the list to the
 * approximate target size. By being relaxed about the size we can
 * prune much faster without having to sort things. This relaxed
 * approach may not always be appropriate however.
 *
 * This class is not thread safe and should only be used by  a single
 * thread.
 *
 * All scores are maintained in LogMath log base
 */
public class TreeActiveList implements ActiveList  {
    private SphinxProperties props = null;
    private int absoluteBeamWidth = 2000;
    private float relativeBeamWidth;
    private float logLowestScore = Float.MAX_VALUE;
    private float logPruneScore;
    private StatisticsVariable tokens;
    private boolean strictPruning = false;

    // when the list is changed these things should be
    // changed/updated as well

    private Token head;
    private int size = 0;
    private List listForIterator;

    private static Random shuffleSeed = new Random(0xBABEL);

    /**
     * Creates an active list with the given max size
     * 
     * @param props the sphinx properties
     */
    public TreeActiveList() {
    }

    /**
     * Creates a new relaxed fit active list with the given target
     * size
     *
     * @param props the sphinx properties
     *
     */
    public TreeActiveList(SphinxProperties props) {
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
	this.absoluteBeamWidth = props.getInt(PROP_ABSOLUTE_BEAM_WIDTH, 2000);
	double linearRelativeBeamWidth  
	    = props.getDouble(PROP_RELATIVE_BEAM_WIDTH, 0);
	LogMath logMath = LogMath.getLogMath(props.getContext());
	this.relativeBeamWidth = 
	    (float) logMath.linearToLog(linearRelativeBeamWidth);

	tokens =
	    StatisticsVariable.getStatisticsVariable(props.getContext(),
		    "tokensCreated");

	strictPruning  = props.getBoolean(PROP_STRICT_PRUNING, false);

	if (false) {
	    System.out.println("target size is " + absoluteBeamWidth);
	}
    }

    /**
     * Creates a new version of this active list with
     * the same general properties as this list
     */
    public ActiveList createNew() {
	TreeActiveList newTree = new TreeActiveList();
	newTree.props = props;
	newTree.absoluteBeamWidth = absoluteBeamWidth;
	newTree.relativeBeamWidth = relativeBeamWidth;
	newTree.strictPruning = strictPruning;
	newTree.tokens = tokens;

	return newTree;
    }

    /**
     * Determines if a token with the given score
     * is insertable into the list
     *
     * @param float score the entry score of the token to insert (in
     * LogMath log base)
     * 
     * @return true if its insertable
     */
    public boolean isInsertable(float logScore) {
	if (false) {
	    System.out.println("Size " + size() + " score: " +
                    logScore + 
		    " ls " + getLowestScore());
    	}

	// if we are 'strictPruning' then all tokens are insertable
	// we only prune after all tokens are added to the ist

	if (strictPruning) {
	    return true;
	} else if (size() < absoluteBeamWidth)  {
	    return true;
	} else {
	    return logScore > getLowestScore();
	}
    }

    /**
     * Adds the given token to the list, keeping track of the lowest
     * scoring token
     *
     * @param token the token to add
     */
    public void add(Token token) {
	float logScore = token.getScore();

	tokens.value++;

        int depth = 0;
        
	if (head == null) {
	    head = token;
	} else {
	    Token cur = head;
	    while (true) {
		if (logScore < cur.getScore()) {
		    if (cur.getLeft() == null) {
			cur.setLeft(token);
			break;
		    } else {
			cur = cur.getLeft();
		    }
		} else {
		    if (cur.getRight() == null) {
			cur.setRight(token);
			break;
		    } else {
			cur = cur.getRight();
		    }
		}
	    }
        }
	size++;

	if (false) {
	System.out.println("ADD: lw " + logLowestScore + " is " +
                logScore +
		" size " + size);
        } 

	if (logScore < logLowestScore) {
	    logLowestScore = logScore;
	}

	if (!strictPruning && size > absoluteBeamWidth) {
	     removeLowest();
	}

        this.logPruneScore = Float.MIN_VALUE;

	listForIterator = null;
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
	// BUG: Doesn't replace yet. Just adds.
	add(newToken);
    }


    /**
     * Removes the lowest scoring token
     */
    private void removeLowest() {
	Token parent;

	if (head == null) {
	    return;
	}

	if (head.getLeft() == null) {
	    parent = head = head.getRight();
	} else {
	    parent = head;
	    Token cur = head.getLeft();

	    while (cur.getLeft() != null) {
		parent = cur;
		cur = cur.getLeft();
	    }
	    parent.setLeft(cur.getRight());
	}

	if (false) {
	System.out.println("Remove: lw " + logLowestScore + " is " +
		parent.getScore());
	}
	logLowestScore = findLowestScore(parent);
	size--;
    }


    private void remove(Token token) {
    }

    /**
     * Gets the highest logScore in the list.
     *
     * TODO: Fix this method. It currently might not actually return
     * the highest score, since it takes the rightmost node as the
     * highest scoring token, but the current topology was based on
     * the growing stage of the previous iteration. The scores were
     * changed during the scoring stage of this iteration. Therefore,
     * we should an exhaustive search to find the highest score.
     *
     * @return the highest score, or 0.0f if no tokens in the list (in
     * LogMath log base)
     */
    public float getHighestScore() {
	Token node = head;

	if (head == null) {
	    return 0.0f;
	}

	while (true) {
	    if (node.getRight() == null) {
		break;
	    }
	    node = node.getRight();
	}
	return node.getScore();
    }

    /**
     * Finds the lowest score in the tree starting at the given token
     *
     * @param node the token to start at
     *
     * @return the lowest score in the tree (in LogMath log base)
     */
    private float findLowestScore(Token node) {
	while (true) {
	    if (node.getLeft() == null) {
		break;
	    }
	    node = node.getLeft();
	}
	return node.getScore();
    }

    /**
     * Returns the lowest score on the list
     *
     * @return the lowest score (in LogMath log base)
     */
    private final float getLowestScore() {
	return logLowestScore;
    }

    /**
     * Returns true if the token is scored high enough to grow.
     *
     * @param token the token to check, false otherwise
     */
    public boolean isWorthGrowing(Token token) {
        if (this.logPruneScore == Float.MIN_VALUE) {
            this.logPruneScore = getHighestScore() + relativeBeamWidth;
        }
        return (token.getScore() >= logPruneScore);
    }

    /**
     * Purges excess members. Remove all nodes that fall below the
     * relativeBeamWidth
     *
     * @return a (possible new) active list
     */
    public ActiveList purge() {
        /*
	int beforeSize = size();
	this.logPruneScore =  getHighestScore()  + relativeBeamWidth;

	//  now prune all tokens that score lower than the pruneScore
	head = prune(head, logPruneScore);
	size = -1;	// invalidate size
	if (false) {
	    System.out.println("Prune " +  beforeSize + " " + size() +
		    " score " + logPruneScore);
	}
	listForIterator = null;
        */

	// this is easier to do when we get the iterator


	return this;
    }

    /**
     * Count the nodes in the tree
     *
     * @param head the head of the tree to count
     *
     * @return the size of the subtree
     */
    private int countNodes(Token head) {
	if (head == null) {
	    return 0;
	} else {
	    return 1 + countNodes(head.getLeft()) + 
		       countNodes(head.getRight());
	}
    }

    /**
     * Given a tree/subtree return the subtree that contains
     * just the nodes that are greater-than or equal to pruneScore
     *
     * @param top the top of the tree/subtree
     * @param logPruneScore the score used to prune (in LogMath log
     * base)
     *
     * @return the head of the new subtree
     */
    private Token prune(Token top, float logPruneScore) {

	if (top == null) {
	    return null;
	} else if (top.getScore() >= logPruneScore) {
	    top.setLeft(prune(top.getLeft(), logPruneScore));
	} else {
	   top = prune(top.getRight(), logPruneScore);
	}
	return top;
    }

    /**
     * Retrieves the iterator for this tree. The iterator purposly
     * returns the tokens out of order to avoid unbalanced trees
     */
    public Iterator iterator() {
	return getList().iterator();
    }

    /**
     * Gets the set of all tokens
     *
     * @return the set of tokens
     */
    public Token[] getTokens() {
	List list = getList();
	return (Token[]) list.toArray(new Token[list.size()]);
    }

    /**
     * Gets the list of tokens
     *
     *
     * @return the active list
     */
    public List getList() {
	if (listForIterator == null) {
	    listForIterator = new ArrayList(size());
	    if (head != null) {
		llAdd(head, listForIterator);
	    }
	    if (strictPruning) {
		int size = absoluteBeamWidth > listForIterator.size()
		    ? listForIterator.size() : absoluteBeamWidth;
		listForIterator = listForIterator.subList(0, size);
	    }
	    Collections.shuffle(listForIterator, shuffleSeed);
	}
	return listForIterator;
    }

    /**
      * Adds a token branch to the given list
      *
      * @param token the token to add
      * @param list the list to add the branch of tokens to
      */
    private void llAdd(Token token, List list) {

    	if (token.getRight() != null) {
	    llAdd(token.getRight(), list);
        }

	list.add(token);

    	if (token.getLeft() != null) {
	    llAdd(token.getLeft(), list);
        }
    }

    /**
     * Returns the number of tokens on this active list
     *
     * @return the size of the active list
     */
    public final int size() {
	if (size == -1) {
	    size = countNodes(head);
	}
	return size;
    }
}

