package edu.cmu.sphinx.alignment;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.min;

import java.util.Arrays;
import java.util.List;

/**
 * Performs global alignment of two sequences using Needleman-Wunsch algorithm.
 *
 * @author Alexander Solovets
 */
public class GlobalSequenceAligner<T> implements SequenceAligner<T> {
    private final List<T> reference;
    private final int offset;

    /**
     * Creates new aligner for the specified reference sequence.
     *
     * @param reference reference sequence
     */
    public GlobalSequenceAligner(List<T> reference) {
        this(reference, 0);
    }

    /**
     * Creates new aligner for the specified references sequence.
     *
     * @param reference reference sequence
     * @param offset value to add to each positive item of the resulting array
     */
    public GlobalSequenceAligner(List<T> reference, int offset) {
        this.reference = newArrayList(reference);
        this.offset = offset;
    }

    /**
     * Aligns provided hypothesis sequence with the reference sequence.
     *
     * @param hypothesis hypothesis sequence
     * @return array of indices
     */
    public int[] align(List<T> hypothesis) {
        int n = reference.size() + 1;
        int m = hypothesis.size() + 1;
        int[][] f = new int[n][m];

        for (int i = 1; i < n; ++i) {
            f[i][0] = i;
        }

        for (int j = 1; j < m; ++j) {
            f[0][j] = j;
        }

        for (int i = 1; i < n; ++i) {
            for (int j = 1; j < m; ++j) {
                int match = f[i - 1][j - 1];
                T refWord = reference.get(i - 1);
                T hypWord = hypothesis.get(j - 1);
                if (!refWord.equals(hypWord)) {
                    ++match;
                }
                int insert = f[i][j - 1] + 1;
                int delete = f[i - 1][j] + 1;
                f[i][j] = min(match, min(insert, delete));
            }
        }

        --n;
        --m;
        int[] alignment = new int[m];
        Arrays.fill(alignment, -1);
        while (m > 0) {
            if (n == 0) {
                --m;
            } else {
                T refWord = reference.get(n - 1);
                T queryWord = hypothesis.get(m - 1);
                if (f[n - 1][m - 1] <= f[n - 1][m - 1]
                        && f[n - 1][m - 1] <= f[n][m - 1]
                        && refWord.equals(queryWord)) {
                    alignment[--m] = --n + offset;
                } else {
                    if (f[n - 1][m] < f[n][m - 1]) {
                        --n;
                    } else {
                        --m;
                    }
                }
            }
        }

        return alignment;
    }
}
