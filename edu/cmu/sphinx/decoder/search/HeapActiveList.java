
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.decoder.search.ActiveList;

/**
 * An active list is maintained as a heap, or so called 'priority queue'.
 * A heap is a complete binary tree. This implementation is a min-heap,
 * meaning that the value of the parent is always
 * smaller than that of its children. This heap, like most other
 * heaps, is maintained in an array. A heap looks like:
 * <code>
 *              1
 *            /   \ 
 *          4       3
 *         / \     / \ 
 *        5   10  8   7
 *       / \ 
 *     11   12
 * </code>
 * and the array implementation will look like:
 * <code>
 * Values:   1  4  3  5 10  8  7 11 12
 * Indices:  0  1  2  3  4  5  6  7  8
 * </code>
 *
 * Since a heap is a complete binary tree, it is always
 * balanced, so the run times of the add(), and replace() methods 
 * are guaranteed to be log(N), where N is the total number of 
 * elements already in the heap. This is a fixed capacity heap, 
 * and the capacity is specified by:
 * <code>edu.cmu.sphinx.AciveList.absoluteBeamWidth</code>
 * Since it is fixed capacity, the add() method takes at most log(capacity)
 * run time.
 *
 * For more detail information, refer to:
 * <br>
 * <a href="http://cmusphinx.sourceforge.net/cgi-bin/twiki/view/Sphinx4/HeapActiveList">http://cmusphinx.sourceforge.net/cgi-bin/twiki/view/Sphinx4/HeapActiveList</a>
 *
 * Note that all scores are maintained in the LogMath log domain
 */
public class HeapActiveList implements ActiveList {

    private SphinxProperties props = null;
    private int size = 0;
    private Token[] heap;   // the heap
    private float relativeBeamWidth;
    private float logPruneScore;
    private StatisticsVariable tokens;
    private StatisticsVariable tokensReplaced;

    
    /**
     * Constructs a default HeapActiveList with no backing array.
     */
    public HeapActiveList() {
    }

    /**
     * Constructs a HeapActiveList with a backing array of the
     * given capacity.
     *
     * @param capacity capacity of this HeapActiveList
     */
    public HeapActiveList(int capacity) {
        this();
        this.heap = new Token[capacity];
    }

    /**
     * Returns the highest score, or zero if no tokens in this list.
     * 
     * @return the highest score, or 0.0f if no tokens in this list.
     * The score is in the LogMath log domain.
     */
    public float getHighestScore() {
        if (size == 0) {
            return 0.0f;
        } else {
            // we cannot just search all the leaf nodes, because
            // the scores might be changed by the scoring stage and
            // the Heap might be out of order now
            float logHighestScore = heap[0].getScore();
            for (int i = 1; i < size; i++) {
                float logScore = heap[i].getScore();
                if (logScore > logHighestScore) {
                    logHighestScore = logScore;
                }
            }
            return logHighestScore;
        }
    }

    /**
     * Determines if a token with the given score
     * is insertable into the list
     *
     * @param logScore the entry score (in the log math log domain)
     * of the token to insert
     * 
     * @return true if its insertable
     */
    public boolean isInsertable(float logScore) {
	if (isNotFull()) {
	    return true;
	} else {
	    return (logScore > heap[0].getScore());
	}
    }

    /**
     * Adds the given token to the list.
     *
     * If the list is not full, it simply puts the Token in the last
     * slot, and trickle the Token up the heap based on it score.
     * If the list is full, it replaces the root Token,
     * which is the lowest scoring Token, and then trickles the Token
     * down the heap based on it score.
     *
     * @param token the token to add
     */
    public void add(Token token) {
        tokens.value++;
	// if the heap is not full
	if (isNotFull()) {

            // place the new token at the bottom/end of the heap (size),
            // and start trickling it up by comparing its score
            // with its ancestors
            setToken(token, trickleUp(size, token.getScore()));
            size++;

	} else { // if heap is full
            heap[0].setLocation(-1);
            setToken(token, 0);
	    heapify(0);
	}
    }

    /**
     * Replaces an old token with a new token. This method is
     * guaranteed to run in O(log(capacity)) time.
     *
     * @param oldToken the token to replace (or null in which case,
     * replace works like add).
     *
     * @param newToken the new token to be placed in the list.
     *
     */
    public void replace(Token oldToken, Token newToken) {
        if (oldToken.getLocation() == -1) {
            add(newToken);
        } else {
            int location = oldToken.getLocation();
            oldToken.setLocation(-1);

            if (location == 0) {
                setToken(newToken, 0);
                heapify(0);

            } else { // if the oldToken is in the middle
                float logScore = newToken.getScore();
                int parent = parent(location);
                Token parentToken = heap[parent];

                // now decide whether to trickle up or down (heapify)
                // by compare the score and its parent's score

                if (logScore < parentToken.getScore()) {
                    setToken(newToken, trickleUp(location, logScore));
                } else {
                    setToken(newToken, location);
                    heapify(location);
                }
            }
        }
    }

    /**
     * This method does the opposite of heapify. It trickles up the token
     * at position i.
     *
     * @param i the index of the node to start trickling up
     *
     * @return the appropriate location for the given score
     */
    private int trickleUp(int i, float logScore) {
        int parent = parent(i);
        Token parentToken = heap[parent];
        
        while (i > 0 && logScore < parentToken.getScore()) {
            setToken(parentToken, i);
            i = parent;
            parent = parent(i);
            parentToken = heap[parent];
        }
        return i;
    }

    /**
     * Restore the heap property that the value of the parent is always
     * smaller than that of its children, starting at the given node.
     * It does this by trickling down the token at position i.
     *
     * @param i the index of the node to start heapifying
     */
    private void heapify(int i) {
	int left = left(i);
	int right = right(i);
	int smallest = i;
        Token iToken = heap[i];
        Token smallestToken = iToken;
        Token leftToken, rightToken;

        // the following two 'if' statements simply tries to
        // figure out the token with the smallest score
        // between the given token and its left and right children 

        while (left < size) {
            leftToken = heap[left];
            if (leftToken.getScore() < iToken.getScore()) {
                smallestToken = leftToken;
                smallest = left;
            }
            if (right < size) {
                rightToken = heap[right];
                if (rightToken.getScore() < smallestToken.getScore()) {
                    smallestToken = rightToken;
                    smallest = right;
                }
            }

            // If the given token is not the one with the smallest score
            // among itself and its two children, move the one with the
	    // smallest score up to the position of the given token.
	    // Then reset the current position (i) to that of the smallest
	    // scoring token.
	    // Otherwise, break out of this while loop, since we have
	    // found the best position for the given token.

            if (smallest != i) {
                setToken(smallestToken, i);
                i = smallest;
                left = left(i);
                right = right(i);
            } else {
                break;
            }
        }
        
        setToken(iToken, i);
    }

    /**
     * Returns true if the token is scored high enough to grow.
     *
     * @param token the token to check, false otherwise
     */
    public boolean isWorthGrowing(Token token) {
        return (token.getScore() >= logPruneScore);
    }

    /**
     * This method just calculates the pruning score for this HeapActiveList,
     * just to set things up for the isWorthGrowing() method.
     *
     * @return this HeapActiveList, without actually pruning it
     */
    public ActiveList purge() {
        this.logPruneScore = getHighestScore() + relativeBeamWidth;
	return this;
    }

    /**
     * Creates a new version of the active list with
     * the same general properties of this one
     *
     * @return a new active list
     */
    public ActiveList createNew() {
	HeapActiveList newTree = new HeapActiveList();
        newTree.heap = new Token[heap.length];
        newTree.props = props;
        newTree.relativeBeamWidth = relativeBeamWidth;
        newTree.tokens = tokens;
        newTree.tokensReplaced = tokensReplaced;
	return newTree;
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
	int length = props.getInt(PROP_ABSOLUTE_BEAM_WIDTH, 2000);
	this.heap = new Token[length];
	this.size = 0;
	double linearRelativeBeamWidth  
	    = props.getDouble(PROP_RELATIVE_BEAM_WIDTH, 0);
	LogMath logMath = LogMath.getLogMath(props.getContext());
	this.relativeBeamWidth = 
	    (float) logMath.linearToLog(linearRelativeBeamWidth);
        tokens =
	    StatisticsVariable.getStatisticsVariable(props.getContext(),
                                                     "tokensCreated");
        tokensReplaced =
            StatisticsVariable.getStatisticsVariable(props.getContext(),
                                                     "tokensReplaced");
    }

    /**
     * Returns an iterator over the elements in this active list
     * 
     * @return an iterator
     */
    public Iterator iterator() {
	return getList().iterator();
    }

    /**
     * Returns all the tokens in a List.
     *
     * @return a List of all tokens
     */
    public List getList() {
	List list = Arrays.asList(getTokens());
        Collections.reverse(list);
        return list;
    }

    /**
     * Returns the size of this list
     * 
     * @return the size
     */
    public int size() {
	return size;
    }

    /**
     * Gets the set of all tokens
     *
     * @return the set of tokens
     */
    public Token[] getTokens() {
	if (isFull()) {
	    return heap;
	} else {
	    Token[] newTokens = new Token[size];
	    System.arraycopy(heap, 0, newTokens, 0, size);
	    return newTokens;
	}
    }

    /**
     * Sets the Token at the given location.
     *
     * @param token the Token to set
     * @param the location
     */
    private final void setToken(Token token, int location) {
        heap[location] = token;
        token.setLocation(location);
    }

    /**
     * Returns the parent token index of the given token index.
     *
     * @return the parent token index of the given token index
     */
    private final int parent(int i) {
	return (i-1)/2;
    }

    /**
     * Returns the left child token index of the given token index.
     *
     * @return the left child token index of the given token index
     */
    private final int left(int i) {
	return (i * 2) + 1;
    }

    /**
     * Returns the right child token index of the given token index.
     *
     * @return the right child token index of the given token index
     */
    private final int right(int i) {
	return ((i * 2) + 2);
    }

    /**
     * Returns true if this list has filled its capacity,
     * as specified by the absoluteBeamWidth.
     *
     * @return true if this list is full, false otherwise
     */
    private final boolean isFull() {
	return ( size >= heap.length );
    }

    /**
     * Return true if this list is not full.
     *
     * @return true if this list is not full, false otherwise
     */
    private final boolean isNotFull() {
	return ( size < heap.length );
    }

    /**
     * Returns the string representation of this HeapActiveList.
     *
     * @return the string representation of this HeapActiveList
     */
    public String toString() {
        String out = "";
        int line = 1;
        for (int i = 0; i < size; i++) {
            if (i == (Math.pow(2, line) - 1)) {
                out += "\n";
                line++;
            }
            out += i + "(" + heap[i].getScore() + ") ";
        }
        return out;
    }
}

