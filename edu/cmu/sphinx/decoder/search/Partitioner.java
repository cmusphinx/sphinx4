
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

package edu.cmu.sphinx.decoder.search;


/**
 * Partitions a list of tokens according to the token score.
 */
public class Partitioner {

    /**
     * Partitions sub-array of tokens around the rth token.
     *
     * @param tokens the token array to partition
     * @param p the starting index of the subarray
     * @param r the pivot and the ending index of the subarray, inclusive
     *
     * @return the index (after partitioning) of the element 
     *     around which the array is partitioned
     */
    private int partitions(Token[] tokens, int p, int r) {

        Token pivot = tokens[r];
        int i = p - 1;
        for (int j = p; j < r; j++) {
            Token current = tokens[j];
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
     * Partitions sub-array of tokens around the x-th token by
     * selecting the midpoint of the token array as the pivot.
     *
     * @param tokens the token array to partition
     * @param p the starting index of the subarray
     * @param r the ending index of the subarray, inclusive
     *
     * @return the index of the element around which the array is partitioned
     */
    private int midPointPartition(Token[] tokens, int p, int r) {
        int i = (p + r)/2;
        Token temp = tokens[r];
        tokens[r] = tokens[i];
        tokens[i] = temp;
        return partitions(tokens, p, r);
    }


    /**
     * Partitions the given array of tokens in place, so that the highest
     * scoring n token will be at the beginning of the array, not in any order.
     *
     * @param tokens the array of tokens to partition
     * @param size the number of tokens to partition
     * @param n the number of tokens in the final partition
     *
     * @return the index of the last element in the partition
     */
    public int partition(Token[] tokens, int size, int n) {
        if (tokens.length > n) {
            return midPointSelect(tokens, 0, size - 1, n);
        } else {
            int r = -1;
            float lowestScore = Float.MAX_VALUE;
            for (int i = 0; i < tokens.length; i++) {
                Token current = tokens[i];
                float currentScore = current.getScore();
                if (currentScore <= lowestScore) {
                    lowestScore = currentScore;
                    r = i; // "r" is the returned index
                }
            }

            // exchange tokens[r] <=> last token,
            // where tokens[r] has the lowest score
            int last = size - 1;
            if (last >= 0) {
                Token lastToken = tokens[last];
                tokens[last] = tokens[r];
                tokens[r] = lastToken;
            }

            // return the last index
            return last;
        }
    }


    /**
     * Selects the token with the ith largest token score.
     *
     * @param tokens the token array to partition
     * @param p the starting index of the subarray
     * @param r the ending index of the subarray, inclusive
     * @param i the token with the i-th largest score
     * 
     * @return the index of the token with the ith largest score
     */
    private int midPointSelect(Token[] tokens, int p, int r, int i) {
        if (p == r) {
            return p;
        }
        int q = midPointPartition(tokens, p, r);
        int k = q - p + 1;
        if (i == k) {
            return q;
        } else if (i < k) {
            return midPointSelect(tokens, p, q - 1, i);
        } else {
            return midPointSelect(tokens, q + 1, r, i - k);
        }
    }
}
