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

package edu.cmu.sphinx.knowledge.language;
import java.util.List;


/***
 * This class can be used to keep track of a word sequence.  This class
 * is an immutable class. It can never be modified once it is created
 * (except, perhaps for transient, cached things such as a
 * precalculated hashcode).
 */
public class WordSequence {

    private int[] wordIDs;
    private transient int hashCode = -1;
    public final static WordSequence EMPTY = new WordSequence();

    /**
     * Constructs a word sequence with the zero depth.
     */
    private WordSequence() {
        this(0);
    }

    /**
     * Constructs a word sequence with the given depth.
     *
     * @param size the maxium depth of the word history
     */
    private WordSequence(int size) {
        wordIDs = new int[size];
    }

    /**
     * Constructs a word sequence with the given word IDs
     *
     * @param wordIDs the word IDs of the word sequence
     */
    private WordSequence(int[] wordIDs) {
        this.wordIDs = new int[wordIDs.length];
        for (int i = 0; i < wordIDs.length; i++) {
            this.wordIDs[i] = wordIDs[i];
        }
        check();
    }
    
    /**
     * Returns a WordSequence with the given word IDs.
     *
     * @return a WordSequence with the given word IDs
     */
    public static WordSequence getWordSequence(int[] wordIDs) {
        return (new WordSequence(wordIDs));
    }

    /**
     * Constructs a word sequence from the list of words
     *
     * @param list the list of words
     */
    public static WordSequence getWordSequence(List list) {
        WordSequence sequence = new WordSequence(list.size());
        for (int i = 0; i < list.size(); i++) {
            sequence.wordIDs[i] = ((Integer)list.get(i)).intValue();
        }
        sequence.check();
        return sequence;
    }

    private void check() {
        for (int i = 0; i < wordIDs.length; i++) {
            if (wordIDs[i] == 0) {
                throw new Error
                    ("Word IDs should not be 0. Assign another value.");
            }
        }
    }

    /**
     * Returns a new word sequence with the given word added to the
     * sequence
     *
     * @param word the word to add to the sequence
     * @param maxSize the maximum size of the generated sequence
     */
    public WordSequence addWord(int wordID, int maxSize) {
        if (maxSize <= 0) {
            return EMPTY;
        }
        int nextSize = ((size() + 1) > maxSize) ? maxSize : (size() + 1);
        WordSequence next = new WordSequence(nextSize);
        int nextIndex = nextSize - 1;
        int thisIndex = size() - 1;
        next.wordIDs[nextIndex--] = wordID;

        while (nextIndex >= 0 && thisIndex >= 0) {
            next.wordIDs[nextIndex--] = this.wordIDs[thisIndex--];
        }
        next.check();

	return next;
    }

    /**
     * Returns the oldest words in the sequence (the newest word is
     * omitted)
     *
     * @return the oldest words in the sequence, with the newest word omitted
     */
    public WordSequence getOldest() {
        WordSequence next = EMPTY;

        if (size() >= 1) {
            next = new WordSequence(wordIDs.length -1);
            for (int i = 0; i < next.wordIDs.length; i++) {
                next.wordIDs[i] = this.wordIDs[i];
            }
        }
	return next;
    }

    /**
     * Returns the newest words in the sequence (the old word is
     * omitted)
     *
     * @return the newest words in the sequence with the oldest word omitted
     */
    public WordSequence getNewest() {
        WordSequence next = EMPTY;

        if (size() >= 1) {
            next = new WordSequence(wordIDs.length -1);
            for (int i = 0; i < next.wordIDs.length; i++) {
                next.wordIDs[i] = this.wordIDs[i + 1];
            }
        }
	return next;
    }

    /**
     * Returns a word sequence that is no longer than the given size,
     * that is filled in with the newest words from this sequence
     *
     * @param maxSize the maximum size of the sequence
     *
     */
    public WordSequence trim(int maxSize) {
        if (maxSize <= 0 || size() == 0) {
            return EMPTY;
        } else  if (maxSize == size()) {
            return this;
        } else {
            if (maxSize > size()) {
                maxSize = size();
            }
            WordSequence next = new WordSequence(maxSize);
            int thisIndex = wordIDs.length - 1;
            int nextIndex = next.wordIDs.length - 1;

            for (int i = 0; i < maxSize; i++) {
                next.wordIDs[nextIndex--] = this.wordIDs[thisIndex--];
            }
            return next;
        }
    }

    /**
     * Returns the nth word in this sequence
     *
     * @param n which word to return
     *
     * @return the nth word in this sequence
     */
    public int getWordID(int n) {
        if (n >= wordIDs.length) {
            throw new ArrayIndexOutOfBoundsException(n);
        }
        return wordIDs[n];
    }

    /**
     * Returns the number of words in this sequence
     *
     * @return the number of words
     */
    public int size() {
        return wordIDs.length;
    }

    /**
     * Returns a string represntation of this word sequence. The format is:
     * [ID_0][ID_1][ID_2].
     *
     * @return the string
     */
    public String toString() {
	StringBuffer sb = new StringBuffer();
        for (int i = 0; i < wordIDs.length; i++) {
            sb.append("[");
            sb.append(wordIDs[i]);
            sb.append("]");
	}
	return sb.toString();
    }
    
    /**
     * Returns an English text form of this word sequence,
     * e.g., "this is a".
     *
     * @return the English text form
     */
    public String toText() {
	StringBuffer sb = new StringBuffer(20);
	for (int i = 0; i < wordIDs.length; i++) {
	    if (i != 0) {
		sb.append(" ");
	    }
	    sb.append(wordIDs[i]);
	}
	return sb.toString();
    }

    /**
     * Calculates the hashcode for this object
     *
     * @return a hascode for this object
     */
    public int hashCode() {
	if (hashCode == -1) {
	    int code = 123;
	    for (int i = 0; i < wordIDs.length; i++) {
                code += wordIDs[i] * (i + 1);
	    }
	    hashCode = code;
	}
	return hashCode;
    }

    /**
     * compares the given object to see if it is identical to
     * this WordSequence
     *
     * @param o the object to compare this to
     *
     * @return true if the given object is equal to this object
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof WordSequence) {
	    WordSequence other = (WordSequence) o;
            if (this.wordIDs.length == other.wordIDs.length) {
                for (int i = 0; i < this.wordIDs.length; i++) {
                    if (this.wordIDs[i] != other.wordIDs[i]) {
                        return false;
                    }
                }
                return true;
            } 
	} 
        return false;
    }
}
