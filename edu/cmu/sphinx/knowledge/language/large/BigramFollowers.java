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

import java.nio.MappedByteBuffer;


public class BigramFollowers {
    
    private MappedByteBuffer bigramBuffer;
    private int numberFollowers;


    /**
     * Constructs a BigramFollowers object with the given
     * MappedByteBuffer.
     *
     * @param bigramsOnDisk the MappedByteBuffer with bigrams on disk
     */
    public BigramFollowers(MappedByteBuffer bigramsOnDisk,
                           int numberFollowers) {
        this.bigramBuffer = bigramsOnDisk;
        this.numberFollowers = numberFollowers;
    }


    /**
     * Loads all bigram followers from disk to physical memory.
     */
    public final void load() {
        bigramBuffer.load();
    }


    /**
     * Tells whether or not the bigrams are loaded into memory
     *
     * @return true if its likely that the bigrams are loaded
     */
    public final boolean isLoaded() {
        return bigramBuffer.isLoaded();
    }


    /**
     * Finds the bigram probabilities for the given second word in a bigram.
     *
     * @param secondWordID the ID of the second word
     *
     * @return the BigramProbability of the given second word
     */
    public BigramProbability findBigram(int secondWordID) {
        int mid, start = 0, end = numberFollowers;
        BigramProbability bigram = null;

        while ((end - start) > 0) {
            mid = (start + end)/2;
            int midWordID = getWordID(mid);            
            if (midWordID == secondWordID) {
                bigram = getBigramProbability(mid);
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

        return (new BigramProbability
                (wordID, probID, backoffID, firstTrigram));
    }


    /**
     * Reads the next two bytes from the buffer's current position as an
     * integer.
     *
     * @return the next two bytes as an integer
     */
    private final int readTwoBytesAsInt() {
        int value = 0x00000000;
        value |= bigramBuffer.get();
        value <<= 8;
        value |= bigramBuffer.get();
        return value;
    }

}
