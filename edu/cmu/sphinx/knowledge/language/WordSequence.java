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
    private String[] words;
    private transient int hashCode = -1;
    private final static WordSequence EMPTY = new WordSequence();

    /**
     * Constructs a word sequence with the given depth
     *
     * @param size the maxium depth of the word history
     */
    private WordSequence(int size) {
        words = new String[size];
    }

    /**
     * Constructs a word sequence with the given depth
     *
     * @param size the maxium depth of the word history
     */
    public WordSequence() {
        this(0);
    }

    /**
     * Constructs a word sequence with the given depth
     *
     * @param word1 the oldest word in the sequence
     */
    public WordSequence(String word1) {
        this(1);
        words[0] = word1;
        check();
    }

    /**
     * Constructs a word sequence with the given depth
     *
     * @param word1 the oldest word in the sequence
     * @param word2 the second oldest  word in the sequence
     */
    public WordSequence(String word1, String word2) {
        this(2);
        words[0] = word1;
        words[1] = word2;
        check();
    }

    /**
     * Constructs a word sequence with the given depth
     *
     * @param word1 the oldest word in the sequence
     * @param word2 the second oldest  word in the sequence
     * @param word3 the newest word in the sequence
     */
    public WordSequence(String word1, String word2, String word3) {
        this(3);
        words[0] = word1;
        words[1] = word2;
        words[2] = word3;
        check();
    }

    /**
     * Constructs a word sequence from the list of words
     *
     * @param list the list of words
     */
    public WordSequence(List list) {
        this(list.size());
        for (int i = 0; i < list.size(); i++) {
            words[i] = (String) list.get(i);
        }
        check();
    }


    /**
     * checks to make sure that the WordSequence is properly
     * constructed
     */
    private void check() {
        for (int i = 0; i < words.length; i++) {
            assert words[i] != null;
        }
    }


    /**
     * Returns a new word sequence with the given word added to the
     * sequence
     *
     * @param word the word to add to the sequence
     * @param maxSize the maximum size of the generated sequence
     */
    public WordSequence addWord(String word, int maxSize) {
        int nextSize = size() + 1 > maxSize ? maxSize : size() + 1;
        WordSequence next = new WordSequence(nextSize);
        int nextIndex = nextSize - 1;
        int thisIndex = size() - 1;
        next.words[nextIndex--] = word;

        while (nextIndex >= 0 && thisIndex >= 0) {
            next.words[nextIndex--] = this.words[thisIndex--];
        }

	return next;
    }

    /**
     * Returns the oldest words in the sequence (the newest word is
     * omitted)
     *
     */
    public WordSequence getOldest() {
        WordSequence next = EMPTY;

        if (size() >= 1) {
            next = new WordSequence(words.length -1);
            for (int i = 0; i < next.words.length; i++) {
                next.words[i] = this.words[i];
            }
        }
	return next;
    }

    /**
     * Returns the new words in the sequence (the old word is
     * omitted)
     *
     */
    public WordSequence getNewest() {
        WordSequence next = EMPTY;

        if (size() >= 1) {
            next = new WordSequence(words.length -1);
            for (int i = 0; i < next.words.length; i++) {
                next.words[i] = this.words[i + 1];
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
        if (maxSize == 0 || size() == 0) {
            return EMPTY;
        } else  if (maxSize == size()) {
            return this;
        } else {
            if (maxSize > size()) {
                maxSize = size();
            }
            WordSequence next = new WordSequence(maxSize);
            int thisIndex = words.length - 1;
            int nextIndex = next.words.length - 1;

            for (int i = 0; i < maxSize; i++) {
                next.words[nextIndex--] = this.words[thisIndex--];
            }
            return next;
        }
    }


    /**
     * Returns the number of words in this sequence
     *
     * @return the number of words
     */
    public int size() {
        return words.length;
    }



    /**
     * Returns a string represntation of this word sequence
     *
     * @return the string
     */
    public String toString() {
	StringBuffer sb = new StringBuffer();
        for (int i = 0; i < words.length; i++) {
            sb.append("[");
            sb.append(words[i]);
            sb.append("]");
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
	    for (int i = 0; i < words.length; i++) {
                code += words[i].hashCode() * (i + 1);
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
            if (this.words.length == other.words.length) {
                for (int i = 0; i < this.words.length; i++) {
                    if (this.words[i] ==  other.words[i]) {
                        continue;
                    }
                    if (!this.words[i].equals(other.words[i])) {
                        return false;
                    }
                }
                return true;
            } 
	} 
        return false;
    }
}

