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
import edu.cmu.sphinx.util.SphinxProperties;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;


/***
 * This class can be used to keep track of a word history.  This class
 * is an immutable class. It can never be modified once it is created
 * (except, perhaps for transient, cached things such as a
 * precalculated hashcode).
 */
public class WordHistory {
    private String[] words;
    private transient int hashCode = -1;

    /**
     * Constructs a word history with the given depth
     *
     * @param depth the maxium depth of the word history
     */
    public WordHistory(int maxSize) {
        words = new String[maxSize];
    }


    /**
     * Returns a new word history with the given word added to the
     * history
     *
     * @param word the word to add to the history
     */
    public WordHistory addWord(String word) {
        WordHistory next = new WordHistory(words.length);
	for (int i = 1; i < words.length; i++) {
	    next.words[i - 1] = this.words[i];
	}
	next.words[next.words.length - 1] = word;
	return next;
    }


    /**
     * Returns a string represntation of this word history
     *
     * @return the string
     */
    public String toString() {
	StringBuffer sb = new StringBuffer();
        for (int i = 0; i < words.length; i++) {
	    if (words[i] != null) {
		sb.append(words[i]);
		sb.append(" ");
	    }
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
		if (words[i] != null) {
		    code += words[i].hashCode() * (i + 1);
		} else {
		    code += 14 * (i + 1);
		}
	    }
	    hashCode = code;
	}
	return hashCode;
    }

    /**
     * compares the given object to see if it is identical to
     * this WordHistory
     *
     * @param o the object to compare this to
     *
     * @return true if the given object is equal to this object
     */
    public boolean equals(Object o) {
        if (o instanceof WordHistory) {
	    WordHistory other = (WordHistory) o;
	    if (this == other) {
	        return true;
	    } else {
	        if (this.words.length == other.words.length) {
		    for (int i = 0; i < this.words.length; i++) {
		        if (this.words[i] ==  other.words[i]) {
			    continue;
			}
			if (this.words[i] == null || other.words[i] == null) {
			   return false;
		        }
		        if (!this.words[i].equals(other.words[i])) {
			    return false;
			}
		    }
		    return true;
		} else {
		    return false;
		}
	    }
	} else {
	    return false;
	}
    }
}

