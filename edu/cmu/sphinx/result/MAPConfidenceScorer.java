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

import edu.cmu.sphinx.decoder.search.Token;

import java.util.Iterator;

/**
 * Computes confidences for the highest scoring path in a Result.
 * The highest scoring path refers to the path with the maximum
 * a posteriori (MAP) probability, which is why this class is so named.
 */
public class MAPConfidenceScorer implements ConfidenceScorer {

    /**
     * Computes confidences for a Result and returns a ConfidenceResult,
     * a compact representation of all the hypothesis contained in the
     * result together with their per-word and per-path confidences.
     *
     * @param result the result to compute confidences for
     * @return a confidence result
     */
    public ConfidenceResult score(Result result) {
        SausageMaker sausageMaker = new SausageMaker();
        ConfidenceResult sausage = sausageMaker.score(result);
        WordResultPath mapPath = new WordResultPath();
        Token mapToken = result.getBestToken();

        /* start with the last slot */
        int slot = sausage.size() - 1;

        while (mapToken != null) {

            /* find a word token */
            while (!mapToken.isWord()) {
                mapToken = mapToken.getPredecessor();
            }

            /* if a word is found */
            if (mapToken != null) {
                ConfusionSet cs = sausage.getConfusionSet(slot);
                String word = mapToken.getWord().getSpelling();
                WordResult wr = null;

                /*
                 * NOTE: since there is no Word for <noop>,
                 * we check for <unk> instead
                 */
                while (slot >= 0 &&
                       (((wr = cs.getWordResult(word)) == null) &&
                        ((wr = cs.getWordResult("<unk>")) != null))) {
                    slot--;
                    cs = sausage.getConfusionSet(slot);
                }
                if (wr != null) {
                    System.out.println("Confidence for " + word + ": " +
                                       wr.getConfidence());
                    mapPath.add(0, wr);
                } else {
                    throw new Error("Can't find WordResult in ConfidenceResu[Blt slot " +
                                    slot + " for word " + word);
                }
                mapToken = mapToken.getPredecessor();
            } else {
                break;
            }
        }

        return (new MAPConfidenceResult(sausage, mapPath));
    }

    /**
     * The confidence result for the highest scoring path.
     */
    class MAPConfidenceResult implements ConfidenceResult {

        private ConfidenceResult sausage;
        private Path mapPath;

        /**
         * Constructs a MAPConfidenceResult.
         *
         * @param sausage the sausge that this MAPConfidenceResult is based on
         * @param mapPath the maximum posterior probability path
         */
        public MAPConfidenceResult(ConfidenceResult sausage, Path mapPath) {
            this.sausage = sausage;
            this.mapPath = mapPath;
        }

        /**
         * Returns the path with the maximum posterior probability path.
         * This path should be the same as that returned by 
         * Result.getBestToken().
         */
        public Path getBestHypothesis() {
            return mapPath;
        }

        /**
         * Get the number of word slots contained in this result
         *
         * @return length of the result
         */
        public int size() {
            return sausage.size();
        }

        /**
         * Iterator through the confusion sets in this result.
         *
         * @return confusion set iterator
         */
        public Iterator confusionSetIterator() {
            return sausage.confusionSetIterator();
        }

        /**
         * Get the nth confusion set in this result
         *
         * @param i the index of the confusion set to get
         * @return the requested confusion set
         */
        public ConfusionSet getConfusionSet(int i) {
            return sausage.getConfusionSet(i);
        }
    }
}
