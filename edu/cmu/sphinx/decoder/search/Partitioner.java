
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
import java.util.Random;

import edu.cmu.sphinx.decoder.scorer.Scoreable;

/**
 * Partitions a list of tokens according to the token score.
 */
public class Partitioner {

    private Random random = new Random(System.currentTimeMillis());
    private float bestTokenScore;
    private Token bestToken;


    /**
     * Partitions sub-array of tokens around the rth token.
     *
     * @param token the token array to partition
     * @param p the starting index of the subarray
     * @param r the pivot and the ending index of the subarray, inclusive
     *
     * @return the index (after partitioning) of the element 
     *     around which the array is partitioned
     */
    private int partition(Token[] tokens, int p, int r) {

        Token pivot = tokens[r];
        int i = p - 1;
        for (int j = p; j < r; j++) {
            Token current = tokens[j];
            if (current.getScore() >= bestTokenScore) {
                bestToken = current;
                bestTokenScore = current.getScore();
            }

            if (current.getScore() >= pivot.getScore()) {
                i++;
                tokens[j] = tokens[i];
                tokens[i] = current;
            }
        }
        i++;
        tokens[r] = tokens[i];
        tokens[i] = pivot;
        return i;
    }


    /**
     * Partitions sub-array of tokens around the x-th token by randomly
     * selecting the pivot.
     *
     * @param token the token array to partition
     * @param p the starting index of the subarray
     * @param r the ending index of the subarray, inclusive
     *
     * @return the index of the element around which the array is partitioned
     */
    private int randomPartition(Token[] tokens, int p, int r) {
        int i = random.nextInt(r - p) + p;
        Token temp = tokens[r];
        tokens[r] = tokens[i];
        tokens[i] = temp;
        return partition(tokens, p, r);
    }


    /**
     * Partitions the given array of tokens in place, so that the highest
     * score n token will be at the beginning of the array, not in any order.
     *
     * @param tokens the array of tokens to partition
     * @param n the number of tokens to partition
     *
     * @return the actual number of tokens in the resulting partition
     */
    public int partition(Token[] tokens, int n) {
        bestTokenScore = -Float.MAX_VALUE;
        bestToken = null;
        int r = tokens.length;
        if (tokens.length > n) {
            r = randomSelect(tokens, 0, tokens.length - 1, n);
        }
        if (bestToken == null) {
            for (int i = 0; i < tokens.length; i++) {
                Token current = tokens[i];
                if (current.getScore() >= bestTokenScore) {
                    bestToken = current;
                    bestTokenScore = current.getScore();
                }
            }
        }

        return r;
    }


    /**
     * Returns the highest scoring token in the array given to the
     * last call to method partition().
     *
     * @return the highest scoring token
     */
    public Token getBestToken() {
        return bestToken;
    }


    /**
     * Selects the token with the ith largest token score.
     *
     * @param token the token array to partition
     * @param p the starting index of the subarray
     * @param r the ending index of the subarray, inclusive
     * @param i the token with the i-th largest score
     * 
     * @return the index of the token with the ith largest score
     */
    private int randomSelect(Token[] tokens, int p, int r, int i) {
        if (p == r) {
            return p;
        }
        int q = randomPartition(tokens, p, r);
        int k = q - p + 1;
        if (i == k) {
            return q;
        } else if (i < k) {
            return randomSelect(tokens, p, q - 1, i);
        } else {
            return randomSelect(tokens, q + 1, r, i - k);
        }
    }
}
