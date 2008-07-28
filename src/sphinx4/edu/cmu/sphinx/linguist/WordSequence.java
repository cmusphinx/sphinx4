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

package edu.cmu.sphinx.linguist;

import edu.cmu.sphinx.linguist.dictionary.Word;

import java.util.ArrayList;
import java.util.List;


/**
 * This class can be used to keep track of a word sequence.  This class is an immutable class. It can never be modified
 * once it is created (except, perhaps for transient, cached things such as a precalculated hashcode).
 */
public final class WordSequence {

    private Word[] words;
    private transient int hashCode = -1;

    /** an empty word sequence, that is, it has no words. */
    public final static WordSequence EMPTY = new WordSequence(0);


    /**
     * Constructs a word sequence with the given depth.
     *
     * @param size the maxium depth of the word history
     */
    private WordSequence(int size) {
        words = new Word[size];
    }


    /**
     * Constructs a word sequence with the given word IDs
     *
     * @param words the word IDs of the word sequence
     */
    public WordSequence(Word[] words) {
        this.words = new Word[words.length];
        System.arraycopy(words, 0, this.words, 0, words.length);
        check();
    }


    /**
     * Constructs a word sequence from the list of words
     *
     * @param list the list of words
     */
    public WordSequence(List<Word> list) {
        this.words = new Word[list.size()];
        for (int i = 0; i < list.size(); i++) {
            words[i] = list.get(i);

        }

        check();
    }


    private void check() {
        for (int i = 0; i < words.length; i++) {
            if (words[i] == null) {
                throw new Error("WordSequence should not have null Words.");
            }
        }
    }


    /**
     * Returns a new word sequence with the given word added to the sequence
     *
     * @param word    the word to add to the sequence
     * @param maxSize the maximum size of the generated sequence
     * @return a new word sequence with the word added (but trimmed to maxSize).
     */
    public WordSequence addWord(Word word, int maxSize) {
        if (maxSize <= 0) {
            return EMPTY;
        }
        int nextSize = ((size() + 1) > maxSize) ? maxSize : (size() + 1);
        WordSequence next = new WordSequence(nextSize);
        int nextIndex = nextSize - 1;
        int thisIndex = size() - 1;
        next.words[nextIndex--] = word;

        while (nextIndex >= 0 && thisIndex >= 0) {
            next.words[nextIndex--] = this.words[thisIndex--];
        }
        next.check();

        return next;
    }


    /**
     * Returns the oldest words in the sequence (the newest word is omitted)
     *
     * @return the oldest words in the sequence, with the newest word omitted
     */
    public WordSequence getOldest() {
        WordSequence next = EMPTY;

        if (size() >= 1) {
            next = new WordSequence(words.length - 1);
            System.arraycopy(this.words, 0, next.words, 0, next.words.length);
        }
        return next;
    }


    /**
     * Returns the newest words in the sequence (the old word is omitted)
     *
     * @return the newest words in the sequence with the oldest word omitted
     */
    public WordSequence getNewest() {
        WordSequence next = EMPTY;

        if (size() >= 1) {
            next = new WordSequence(words.length - 1);
            System.arraycopy(this.words, 1, next.words, 0, next.words.length);
        }
        return next;
    }


    /**
     * Returns a word sequence that is no longer than the given size, that is filled in with the newest words from this
     * sequence
     *
     * @param maxSize the maximum size of the sequence
     * @return a new word sequence, trimmed to maxSize.
     */
    public WordSequence trim(int maxSize) {
        if (maxSize <= 0 || size() == 0) {
            return EMPTY;
        } else if (maxSize == size()) {
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
     * Returns the nth word in this sequence
     *
     * @param n which word to return
     * @return the nth word in this sequence
     */
    public Word getWord(int n) {
        if (n >= words.length) {
            throw new ArrayIndexOutOfBoundsException(n);
        }
        return words[n];
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
     * Returns a string represntation of this word sequence. The format is: [ID_0][ID_1][ID_2].
     *
     * @return the string
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Word word : words) {
            sb.append("[");
            sb.append(word.toString());
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
     * compares the given object to see if it is identical to this WordSequence
     *
     * @param o the object to compare this to
     * @return true if the given object is equal to this object
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof WordSequence) {
            WordSequence other = (WordSequence) o;
            if (words.length == other.words.length) {
                for (int i = 0; i < words.length; i++) {
                    if (!words[i].equals(other.words[i])) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }


    /** Returns a subsequence with both <code>startIndex</code> and <code>stopIndex</code> exclusive. */
    public WordSequence getSubSequence(int startIndex, int stopIndex) {
        List<Word> subseqWords = new ArrayList<Word>();

        for (int i = startIndex; i < stopIndex; i++) {
            subseqWords.add(getWord(i));
        }

        return new WordSequence(subseqWords);
    }


    /** Returns the words of the <code>WordSequence</code>. */
    public Word[] getWords() {
        return getSubSequence(0, size()).words; //create a copy yo keep the class imutable
    }
}
