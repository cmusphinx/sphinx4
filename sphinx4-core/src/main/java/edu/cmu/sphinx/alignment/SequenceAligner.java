package edu.cmu.sphinx.alignment;

import java.util.List;

/**
 * Aligns sequences.
 *
 * @author Alexander Solovets
 */
public interface SequenceAligner<T> {
    /**
     * Aligns provided hypothesis sequence with some reference sequence.
     *
     * @param hypothesis hypothesis sequence
     * @return an array of indices corresponding to the reference sequence or -1
     */
    int[] align(List<T> hypothesis);
}
