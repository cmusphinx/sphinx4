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
 * Implements a ByteBuffer that contains NGrams. It assumes that the
 * first two bytes of each n-gram entry is the ID of the n-gram. 
 */
public class NGramBuffer {
    
    private ByteBuffer buffer;
    private int numberNGrams;


    /**
     * Constructs a NGramBuffer object with the given ByteBuffer.
     *
     * @param buffer the ByteBuffer with trigrams
     * @param numberNGrams the number of N-gram
     */
    public NGramBuffer(ByteBuffer buffer, int numberNGrams) {
        this.buffer = buffer;
        this.numberNGrams = numberNGrams;
    }


    /**
     * Returns the ByteBuffer of n-grams.
     *
     * @return the ByteBuffer of n-grams
     */
    public ByteBuffer getBuffer() {
	return buffer;
    }


    /**
     * Returns the number of n-grams in this buffer.
     *
     * @return the number of n-grams in this buffer
     */
    public int getNumberNGrams() {
	return numberNGrams;
    }


    /**
     * Returns the word ID of the nth follower, assuming that the ID
     * is the first two bytes of the NGram entry.
     *
     * @param nthFollower starts from 0 to (numberFollowers - 1).
     *
     * @return the word ID
     */
    public final int getWordID(int nthFollower) {
        int nthPosition = nthFollower * (buffer.capacity()/numberNGrams);
        buffer.position(nthPosition);
        return readTwoBytesAsInt();
    }


    /**
     * Returns true if the trigramBuffer is big-endian.
     *
     * @return true if the trigramBuffer is big-endian, false if little-endian
     */
    public final boolean isBigEndian() {
        return (buffer.order() == ByteOrder.BIG_ENDIAN);
    }


    /**
     * Reads the next two bytes from the buffer's current position as an
     * integer.
     *
     * @return the next two bytes as an integer
     */
    public final int readTwoBytesAsInt() {
        int value = (0x000000ff & buffer.get());
        if (isBigEndian()) {
            value <<= 8;
            value |= (0x000000ff & buffer.get());
        } else {
            int second = (0x000000ff & buffer.get());
            second <<= 8;
            value |= second;
        }
        return value;
    }

}
