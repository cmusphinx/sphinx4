/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.result;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

/**
 * A confusion set is a set of words with their associated posteriors. In Java terms it's a SortedMap from posteriors to
 * sets of WordResults, but the class is called a set because that's what this beast is known as in the literature.
 *
 * @author P. Gorniak
 */
public class ConfusionSet extends TreeMap<Double, Set<WordResult>> {

	private static final long serialVersionUID = 1L;


	/**
     * Add a word hypothesis to this confusion set.
     *
     * @param word the hypothesis to add
     */
    public void addWordHypothesis(WordResult word) {
        Set<WordResult> wordSet = getWordSet(word.getConfidence());
        if (wordSet == null) {
            wordSet = new HashSet<WordResult>();
            put(word.getConfidence(), wordSet);
        }
        wordSet.add(word);
    }


    /**
     * Get the word set with this confidence.
     *
     * @param posterior the confidence (posterior)
     * @return a set of hypotheses with this confidence, null if no such hypotheses
     */
    public Set<WordResult> getWordSet(double posterior) {
        return get(new Double(posterior));
    }


    /**
     * Return the set of best hypothesis. This will usually contain one hypothesis, but might contain more case there
     * are some that have exactly equal confidences.
     *
     * @return a set of best hypotheses
     */
    public Set<WordResult> getBestHypothesisSet() {
        return get(lastKey());
    }


    /**
     * Return the single best hypothesis. Breaks ties arbitrarily.
     *
     * @return the best hypothesis stored in this confusion set (by confidence)
     */
    public WordResult getBestHypothesis() {
        Set<WordResult> s = getBestHypothesisSet();
        return (WordResult) s.iterator().next();
    }


    /**
     * Get the highest posterior (confidence) stored in this set.
     *
     * @return the highest posterior
     */
    public double getBestPosterior() {
        return ((Double) lastKey()).doubleValue();
    }


    /**
     * Check whether this confusion set contains the given word
     *
     * @param word the word to look for
     * @return true if word is in this set
     */
    public boolean containsWord(String word) {
        return getWordResult(word) != null;
    }


    /**
     * Check whether this confusion set contains any fillers.
     *
     * @return whether fillers are found
     */
    public boolean containsFiller() {
        for (Set<WordResult> wordSet : values()) {
            for (WordResult wordResult : wordSet) {
                if (wordResult.isFiller()) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Returns the WordResult in this ConfusionSet for the given word.
     *
     * @param word the word to look for
     * @return the WordResult for the given word, or null if no WordResult for the given word is found
     */
    public WordResult getWordResult(String word) {
        for (Set<WordResult> wordSet : values()) {
            for (WordResult wordResult : wordSet) {                
                String resultSpelling = wordResult.toString();
                //= wordResult.getPronunciation().getWord().getSpelling();
                if (resultSpelling.equals(word)) {
                    return wordResult;
                }
            }
        }
        return null;
    }


    /**
     * Dumps out the contents of this ConfusionSet.
     *
     * @param name the name of the confusion set
     */
    public void dump(String name) {
        System.out.print(name + " :");
        for (Iterator<Set<WordResult>> i = values().iterator(); i.hasNext();) {
            Set<WordResult> wordSet = i.next();
            for (Iterator<WordResult> r = wordSet.iterator(); r.hasNext();) {
                WordResult wordResult = (WordResult) r.next();
                System.out.print
                        (" " +
                                wordResult.getPronunciation().getWord().getSpelling());
            }
        }
        System.out.println();
    }


    public String toString() {
        StringBuffer b = new StringBuffer();
        Iterator<Double> i = keySet().iterator();
        while (i.hasNext()) {
            Double p = i.next();
            Set<WordResult> words = get(p);
            b.append(p.toString()).append(":");
            Iterator<WordResult> j = words.iterator();
            while (j.hasNext()) {
                b.append(j.next());
                if (j.hasNext()) {
                    b.append(",");
                }
            }
        }
        return b.toString();
    }
}
