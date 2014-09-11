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

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.fill;
import static java.util.Collections.emptyList;

import java.util.*;

import edu.cmu.sphinx.util.Range;
import edu.cmu.sphinx.util.Utilities;

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
                List<Node> result = new ArrayList<Node>(3);
                if (queryIndex < indices.size() &&
                    databaseIndex < shifts.size()) {
                    result.add(new Node(queryIndex + 1, databaseIndex + 1));
                }
                if (databaseIndex < shifts.size()) {
                    result.add(new Node(queryIndex, databaseIndex + 1));
                }
                if (queryIndex < indices.size()) {
                    result.add(new Node(queryIndex + 1, databaseIndex));
                }

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

        public Alignment(List<String> query, Range range) {
            this.query = query;
            indices = new ArrayList<Integer>();
            Set<Integer> shiftSet = new TreeSet<Integer>();
            for (int i = 0; i < query.size(); i++) {
                if (tupleIndex.containsKey(query.get(i))) {
                    indices.add(i);
                    for (Integer shift : tupleIndex.get(query.get(i))) {
                        if (range.contains(shift))
                            shiftSet.add(shift);
                    }                    
                }
            }

            shifts = new ArrayList<Integer>(shiftSet);

            final Map<Node, Integer> cost = new HashMap<Node, Integer>();
            PriorityQueue<Node> openSet = new PriorityQueue<Node>(1, new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    return cost.get(o1).compareTo(cost.get(o2));
                }
            });
            Collection<Node> closedSet = new HashSet<Node>();
            Map<Node, Node> parents = new HashMap<Node, Node>();

            Node startNode = new Node(0, 0);
            cost.put(startNode, 0);
            openSet.add(startNode);

            while (!openSet.isEmpty()) {
                Node q = openSet.poll();
                if (closedSet.contains(q))
                    continue;

                if (q.isTarget()) {
                    List<Node> backtrace = new ArrayList<Node>();
                    while (parents.containsKey(q)) {
                        if (!q.isBoundary() && q.hasMatch())
                            backtrace.add(q);
                        q = parents.get(q);
                    }
                    alignment = new ArrayList<Node>(backtrace);
                    Collections.reverse(alignment);
                    return;
                }

                closedSet.add(q);
                for (Node nb : q.adjacent()) {
                    
                    if (closedSet.contains(nb))
                        continue;
                    
                    // FIXME: move to appropriate location
                    int l = abs(indices.size() - shifts.size() - q.queryIndex +
                                q.databaseIndex) -
                            abs(indices.size() - shifts.size() -
                                nb.queryIndex +
                                nb.databaseIndex);

                    Integer oldScore = cost.get(nb);
                    Integer qScore = cost.get(q);
                    if (oldScore == null)
                        oldScore = Integer.MAX_VALUE;
                    if (qScore == null)
                        qScore = Integer.MAX_VALUE;
                    
                    int newScore = qScore + nb.getValue() - l;
                    if (newScore < oldScore) {
                        cost.put(nb, newScore);
                        openSet.add(nb);
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

    private final int tupleSize;
    private final List<String> reftup;
    private final HashMap<String, ArrayList<Integer>> tupleIndex;
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
        assert words != null;
        assert tupleSize > 0;

        this.tupleSize = tupleSize;
        this.refWords = words;

        int offset = 0;
        reftup = getTuples(words);

        tupleIndex = new HashMap<String, ArrayList<Integer>>();
        for (String tuple : reftup) {
            ArrayList<Integer> indexes = tupleIndex.get(tuple);
            if (indexes == null) {
                indexes = new ArrayList<Integer>();
                tupleIndex.put(tuple, indexes);
            }
            indexes.add(offset++);
        }
    }

    /**
     * Aligns query sequence with the previously built database.
     *
     * @return indices of alignment
     */
    public int[] align(List<String> query) {
        return align(query, new Range(0, refWords.size()));
    }

    /**
     * Aligns query sequence with the previously built database.
     *
     * @return indices of alignment
     */
    public int[] align(List<String> words, Range range) {
        
        if (range.upperEndpoint() - range.lowerEndpoint() < tupleSize || words.size() < tupleSize) {
            return alignTextSimple(refWords.subList(range.lowerEndpoint(), range.upperEndpoint()), words, range.lowerEndpoint());
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
    private List<String> getTuples(List<String> words) {
        List<String> result = new ArrayList<String>();
        LinkedList<String> tuple = new LinkedList<String>();
        
        Iterator<String> it = words.iterator();
        for (int i = 0; i < tupleSize - 1; i++) {
            tuple.add(it.next());
        }
        while (it.hasNext()) {
            tuple.addLast(it.next());
            result.add(Utilities.join(tuple));
            tuple.removeFirst();
        }
        return result;
    }
    
    static int[] alignTextSimple(List<String> database, List<String> query,
            int offset) {
        int n = database.size() + 1;
        int m = query.size() + 1;
        int[][] f = new int[n][m];

        f[0][0] = 0;
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
