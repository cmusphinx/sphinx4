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

package edu.cmu.sphinx.linguist.language.ngram.large;


/**
 * Implements a buffer that contains NGrams. It assumes that the
 * first two bytes of each n-gram entry is the ID of the n-gram. 
 */
class NGramBuffer {
    
    private byte[] buffer;
    private int numberNGrams;
    private int position;
    private boolean bigEndian;
    private boolean used;


    /**
     * Constructs a NGramBuffer object with the given byte[].
     *
     * @param buffer the byte[] with trigrams
     * @param numberNGrams the number of N-gram
     */
    public NGramBuffer(byte[] buffer, int numberNGrams, boolean bigEndian) {
        this.buffer = buffer;
        this.numberNGrams = numberNGrams;
	this.bigEndian = bigEndian;
	this.position = 0;
    }


    /**
     * Returns the byte[] of n-grams.
     *
     * @return the byte[] of n-grams
     */
    public byte[] getBuffer() {
	return buffer;
    }


    /**
     * Returns the size of the buffer in bytes.
     *
     * @return the size of the buffer in bytes
     */
    public int getSize() {
        return buffer.length;
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
     * Returns the position of the buffer.
     *
     * @return the position of the buffer
     */
    protected int getPosition() {
	return position;
    }


    /**
     * Sets the position of the buffer.
     *
     * @param position new buffer position
     */
    protected void setPosition(int position) {
	this.position = position;
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
        int nthPosition = nthFollower * (buffer.length/numberNGrams);
        setPosition(nthPosition);
        return readTwoBytesAsInt();
    }


    /**
     * Returns true if the trigramBuffer is big-endian.
     *
     * @return true if the trigramBuffer is big-endian, false if little-endian
     */
    public final boolean isBigEndian() {
        return bigEndian;
    }


    /**
     * Reads the next two bytes from the buffer's current position as an
     * integer.
     *
     * @return the next two bytes as an integer
     */
    public final int readTwoBytesAsInt() {
        if (bigEndian) {
	    int value = (0x000000ff & buffer[position++]);
	    value <<= 8;
            value |= (0x000000ff & buffer[position++]);
	    return value;
        } else {
            int value = (0x000000ff & buffer[position+1]);
	    value <<= 8;
            value |= (0x000000ff & buffer[position]);
	    position += 2;
	    return value;
        }
    }


    /**
     * Returns true if this buffer was used in the last utterance.
     *
     * @return true if this buffer was used in the last utterance
     */
    public boolean getUsed() {
        return used;
    }


    /**
     * Sets whether this buffer was used in the last utterance
     *
     * @param used true if this buffer was used in the last utterance,
     *             false otherwise
     */
    public void setUsed(boolean used) {
        this.used = used;
    }
}
