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

package edu.cmu.sphinx.knowledge.language.large;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * The bigram followers of the word "the" are "apple", "orange", 
 * and "pear" in the following bigrams:
 * <code>
 * <b>the apple
 * <b>the orange
 * <b>the pear
 * </code>
 */
public class BigramFollowers {
    
    private ByteBuffer bigramBuffer;
    private int numberFollowers;


    /**
     * Constructs a BigramFollowers object with the given ByteBuffer.
     *
     * @param bigramsOnDisk the ByteBuffer with bigrams
     * @param numberFollowers the number of bigram follows in the ByteBuffer
     */
    public BigramFollowers(ByteBuffer bigramsOnDisk,
                           int numberFollowers) {
        this.bigramBuffer = bigramsOnDisk;
        this.numberFollowers = numberFollowers;
    }


    /**
     * Finds the bigram probabilities for the given second word in a bigram.
     *
     * @param secondWordID the ID of the second word
     *
     * @return the BigramProbability of the given second word
     */
    public BigramProbability findBigram(int secondWordID) {

        // System.out.println("Looking for: " + secondWordID);

        int mid, start = 0, end = numberFollowers;
        BigramProbability bigram = null;

        while ((end - start) > 0) {
            mid = (start + end)/2;
            int midWordID = getWordID(mid);
            // System.out.println("mid is: " + mid + ", ID: " + midWordID);
            if (midWordID == secondWordID) {
                bigram = getBigramProbability(mid);
                break;
            } else if (midWordID < secondWordID) {
                start = mid + 1;
            } else if (midWordID > secondWordID) {
                end = mid;
            }
        }
        return bigram;
    }


    /**
     * Returns the word ID of the nth follower.
     *
     * @param n starts from 0 to (numberFollowers - 1).
     *
     * @return the word ID
     */
    private final int getWordID(int nthFollower) {
        int nthPosition = nthFollower * 8; // 8 is number of bytes per follower
        bigramBuffer.position(nthPosition);
        return readTwoBytesAsInt();
    }


    /**
     * Returns the BigramProbability of the nth follower.
     *
     * @param nthFollower which follower
     *
     * @return the BigramProbability of the nth follower
     */
    private final BigramProbability getBigramProbability(int nthFollower) {
        int nthPosition = nthFollower * 8;
        bigramBuffer.position(nthPosition);

        int wordID = readTwoBytesAsInt();
        int probID = readTwoBytesAsInt();
        int backoffID = readTwoBytesAsInt();
        int firstTrigram = readTwoBytesAsInt();

        // move the pointer to the firstTrigramEntry of the next bigram
        bigramBuffer.position(bigramBuffer.position() + 6);
        int nextFirstTrigram = readTwoBytesAsInt();

        return (new BigramProbability
                (wordID, probID, backoffID, firstTrigram, 
                 nextFirstTrigram - firstTrigram));
    }


    /**
     * Returns true if the bigramBuffer is big-endian.
     *
     * @return true if the bigramBuffer is big-endian, false if little-endian
     */
    private final boolean isBigEndian() {
        return (bigramBuffer.order() == ByteOrder.BIG_ENDIAN);
    }


    /**
     * Reads the next two bytes from the buffer's current position as an
     * integer.
     *
     * @return the next two bytes as an integer
     */
    private final int readTwoBytesAsInt() {
        int value = (0x000000ff & bigramBuffer.get());
        if (isBigEndian()) {
            value <<= 8;
            value |= (0x000000ff & bigramBuffer.get());
        } else {
            int second = (0x000000ff & bigramBuffer.get());
            second <<= 8;
            value |= second;
        }
        return value;
    }

}
