/*
 * Copyright 2014 Alpha Cephei Inc.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.alignment;

import static com.google.common.base.Functions.forMap;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.limit;
import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.*;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static edu.cmu.sphinx.util.PriorityQueue.newPriorityQueue;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.fill;
import static java.util.Collections.emptyList;

import java.util.*;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;

import edu.cmu.sphinx.util.PriorityQueue;


/**
 *
 * @author Alexander Solovets
 */
public class LongTextAligner {

    private final class Alignment {

        public final class Node {

            private final int databaseIndex;
            private final int queryIndex;

            private Node(int row, int column) {
                this.databaseIndex = column;
                this.queryIndex = row;
            }

            public int getDatabaseIndex() {
                return shifts.get(databaseIndex - 1);
            }

            public int getQueryIndex() {
                return indices.get(queryIndex - 1);
            }

            public String getQueryWord() {
                if (queryIndex > 0)
                    return query.get(getQueryIndex());
                return null;
            }

            public String getDatabaseWord() {
                if (databaseIndex > 0)
                    return reftup.get(getDatabaseIndex());
                return null;
            }

            public int getValue() {
                if (isBoundary())
                    return max(queryIndex, databaseIndex);
                return hasMatch() ? 0 : 1;
            }

            public boolean hasMatch() {
                return getQueryWord().equals(getDatabaseWord());
            }

            public boolean isBoundary() {
                return queryIndex == 0 || databaseIndex == 0;
            }

            public boolean isTarget() {
                return queryIndex == indices.size() &&
                       databaseIndex == shifts.size();
            }

            public List<Node> adjacent() {
                List<Node> result = newArrayListWithCapacity(3);
                if (databaseIndex < shifts.size())
                    result.add(new Node(queryIndex, databaseIndex + 1));
                if (queryIndex < indices.size())
                    result.add(new Node(queryIndex + 1, databaseIndex));
                if (queryIndex < indices.size() &&
                    databaseIndex < shifts.size())
                    result.add(new Node(queryIndex + 1, databaseIndex + 1));
                return result;
            }

            @Override
            public boolean equals(Object object) {
                if (!(object instanceof Node))
                    return false;

                Node other = (Node) object;
                return queryIndex == other.queryIndex &&
                       databaseIndex == other.databaseIndex;
            }

            @Override
            public int hashCode() {
                return 31 * (31 * queryIndex + databaseIndex);
            }

            @Override
            public String toString() {
                return String.format("[%d %d]", queryIndex, databaseIndex);
            }
        }

        private final List<Integer> shifts;
        private final List<String> query;
        private final List<Integer> indices;
        private final List<Node> alignment;

        public Alignment(List<String> query, Range<Integer> range) {
            this.query = query;
            indices = newArrayList();
            Set<Integer> shiftSet = newTreeSet();
            for (int i = 0; i < query.size(); ++i) {
                if (tupleIndex.containsKey(query.get(i)))
                    indices.add(i);
                for (Integer shift : tupleIndex.get(query.get(i))) {
                    if (range.contains(shift))
                        shiftSet.add(shift);
                }
            }

            shifts = newArrayList(shiftSet);

            Map<Node, Integer> cost = newHashMap();
            Function<Node, Integer> priority = forMap(cost, Integer.MAX_VALUE);
            PriorityQueue<Node, Integer> openSet = newPriorityQueue(priority);
            Collection<Node> closedSet = newHashSet();
            Map<Node, Node> parents = newHashMap();

            Node startNode = new Node(0, 0);
            cost.put(startNode, 0);
            openSet.insert(startNode);

            while (!openSet.isEmpty()) {
                Node q = openSet.extractMin();
                if (closedSet.contains(q))
                    continue;

                if (q.isTarget()) {
                    List<Node> backtrace = newArrayList();
                    while (parents.containsKey(q)) {
                        if (!q.isBoundary() && q.hasMatch())
                            backtrace.add(q);
                        q = parents.get(q);
                    }
                    alignment = reverse(backtrace);
                    return;
                }

                closedSet.add(q);
                for (Node nb : filter(q.adjacent(), not(in(closedSet)))) {
                    // FIXME: move to appropriate location
                    int l = abs(indices.size() - shifts.size() - q.queryIndex +
                                q.databaseIndex) -
                            abs(indices.size() - shifts.size() -
                                nb.queryIndex +
                                nb.databaseIndex);

                    int oldScore = priority.apply(nb);
                    int newScore = priority.apply(q) + nb.getValue() - l;
                    if (newScore < oldScore) {
                        cost.put(nb, newScore);
                        openSet.insert(nb);
                        parents.put(nb, q);
                    }
                }
            }

            alignment = emptyList();
        }

        public List<Node> getIndices() {
            return alignment;
        }
    }

    private final Joiner joiner;
    private final int tupleSize;
    private final List<String> reftup;
    private final Multimap<String, Integer> tupleIndex;
    private List<String> refWords;

    /**
     * Constructs new text aligner that servers requests for alignment of
     * sequence of words with the provided database sequence. Sequences are
     * aligned by tuples comprising one or more subsequent words.
     *
     * @param words list of words forming the database
     * @param tupleSize size of a tuple, must be greater or equal to 1
     */
    public LongTextAligner(List<String> words, int tupleSize) {
        checkNotNull(words, "word list must not be null");
        checkArgument(tupleSize > 0, "tuple size must be greater than zero");

        joiner = Joiner.on(' ');
        this.tupleSize = tupleSize;
        this.refWords = words;

        int offset = 0;
        reftup = getTuples(words);
        tupleIndex = ArrayListMultimap.create();
        for (String tuple : reftup)
            tupleIndex.put(tuple, offset++);
    }

    /**
     * Aligns query sequence with the previously built database.
     *
     * @return indices of alignment
     */
    public int[] align(List<String> query) {
        return align(query, Range.closed(0, refWords.size() - 1));
    }

    /**
     * Aligns query sequence with the previously built database.
     *
     * @return indices of alignment
     */
    public int[] align(List<String> words, Range<Integer> range) {
        
        if (range.upperEndpoint() - range.lowerEndpoint() < tupleSize || words.size() < tupleSize) {
            return alignTextSimple(refWords.subList(range.lowerEndpoint(), range.upperEndpoint() + 1), words, range.lowerEndpoint());
        }
        
        
        int[] result = new int[words.size()];
        fill(result, -1);
        int lastIndex = 0;
        for (Alignment.Node node : new Alignment(getTuples(words), range)
                .getIndices()) {
            // for (int j = 0; j < tupleSize; ++j)
            lastIndex = max(lastIndex, node.getQueryIndex());
            for (; lastIndex < node.getQueryIndex() + tupleSize; ++lastIndex)
                result[lastIndex] = node.getDatabaseIndex() + lastIndex -
                                    node.getQueryIndex();
        }
        return result;
    }

    /**
     * Makes list of tuples of the given size out of list of words.
     *
     * @param words words
     * @return list of tuples of size {@link #tupleSize}
     */
    private List<String> getTuples(Iterable<String> words) {
        List<String> result = newArrayList();
        LinkedList<String> tuple = newLinkedList(limit(words, tupleSize - 1));
        for (String word : skip(words, tupleSize - 1)) {
            tuple.addLast(word);
            result.add(joiner.join(tuple));
            tuple.removeFirst();
        }
        return result;
    }
    
    static int[] alignTextSimple(List<String> database, List<String> query,
            int offset) {
        int n = database.size() + 1;
        int m = query.size() + 1;
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
                String refWord = database.get(i - 1);
                String queryWord = query.get(j - 1);
                if (!refWord.equals(queryWord)) {
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
                String refWord = database.get(n - 1);
                String queryWord = query.get(m - 1);
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
