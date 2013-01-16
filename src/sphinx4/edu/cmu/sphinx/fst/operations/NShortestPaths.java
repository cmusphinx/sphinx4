/**
 * 
 * Copyright 1999-2012 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.fst.operations;

import java.util.ArrayList;
import java.util.HashMap;

import edu.cmu.sphinx.fst.Arc;
import edu.cmu.sphinx.fst.Fst;
import edu.cmu.sphinx.fst.State;
import edu.cmu.sphinx.fst.utils.Pair;
import edu.cmu.sphinx.fst.semiring.Semiring;

/**
 * N-shortest paths operation.
 * 
 * See: M. Mohri, M. Riley,
 * "An Efficient Algorithm for the n-best-strings problem", Proceedings of
 * the International Conference on Spoken Language Processing 2002 (ICSLP
 * '02).
 * 
 * @author John Salatas <jsalatas@users.sourceforge.net>
 * 
 */
public class NShortestPaths {
    private NShortestPaths() {
    }

    /**
     * Calculates the shortest distances from each state to the final.
     * 
     * See: M. Mohri,
     * "Semiring Framework and Algorithms for Shortest-Distance Problems",
     * Journal of Automata, Languages and Combinatorics, 7(3), pp. 321-350,
     * 2002.
     * 
     * @param fst the fst to calculate the shortest distances
     * @return the array containing the shortest distances
     */
    public static float[] shortestDistance(Fst fst) {

        Fst reversed = Reverse.get(fst);

        float[] d = new float[reversed.getNumStates()];
        float[] r = new float[reversed.getNumStates()];

        Semiring semiring = reversed.getSemiring();

        for (int i = 0; i < d.length; i++) {
            d[i] = semiring.zero();
            r[i] = semiring.zero();
        }

        State[] stateMap = new State[reversed.getNumStates()];
        for (int i = 0; i < reversed.getNumStates(); i++) {
            stateMap[i] = null;
        }
        ArrayList<Integer> queue = new ArrayList<Integer>();

        queue.add(reversed.getStart().getId());
        stateMap[reversed.getStart().getId()] = reversed.getStart();

        d[reversed.getStart().getId()] = semiring.one();
        r[reversed.getStart().getId()] = semiring.one();

        while (queue.size() > 0) {
            State q = stateMap[queue.remove(0)];
            float rnew = r[q.getId()];
            r[q.getId()] = semiring.zero();
            int numArcs = q.getNumArcs();
            for (int i = 0; i < numArcs; i++) {
                Arc a = q.getArc(i);
                State nextState = a.getNextState();
                float dnext = d[a.getNextState().getId()];
                float dnextnew = semiring.plus(dnext,
                        semiring.times(rnew, a.getWeight()));
                if (dnext != dnextnew) {
                    d[a.getNextState().getId()] = dnextnew;
                    r[a.getNextState().getId()] = semiring.plus(r[a
                            .getNextState().getId()], semiring.times(rnew,
                            a.getWeight()));
                    if (!queue.contains(nextState.getId())) {
                        queue.add(nextState.getId());
                        stateMap[nextState.getId()] = nextState;
                    }
                }
            }
        }
        return d;
    }

    /**
     * Calculates the n-best shortest path from the initial to the final state.
     * 
     * @param fst the fst to calculate the nbest shortest paths
     * @param n number of best paths to return
     * @param determinize if true the input fst will bwe determinized prior the
     *            operation
     * @return an fst containing the n-best shortest paths
     */
    public static Fst get(Fst fst, int n, boolean determinize) {
        if (fst == null) {
            return null;
        }

        if (fst.getSemiring() == null) {
            return null;
        }
        Fst fstdet = fst;
        if (determinize) {
            fstdet = Determinize.get(fst);
        }
        Semiring semiring = fstdet.getSemiring();
        Fst res = new Fst(semiring);
        res.setIsyms(fstdet.getIsyms());
        res.setOsyms(fstdet.getOsyms());

        float[] d = shortestDistance(fstdet);

        ExtendFinal.apply(fstdet);

        int[] r = new int[fstdet.getNumStates()];
        for (int i = 0; i < r.length; i++) {
            r[i] = 0;
        }

        ArrayList<Pair<State, Float>> queue = new ArrayList<Pair<State, Float>>();
        HashMap<Pair<State, Float>, Pair<State, Float>> previous = new HashMap<Pair<State, Float>, Pair<State, Float>>(
                fst.getNumStates(), 1.f);
        HashMap<Pair<State, Float>, State> stateMap = new HashMap<Pair<State, Float>, State>(
                fst.getNumStates(), 1.f);

        State start = fstdet.getStart();
        queue.add(new Pair<State, Float>(start, semiring.one()));
        previous.put(queue.get(0), null);

        while (queue.size() > 0) {
            Pair<State, Float> pair = getLess(queue, d, semiring);
            State p = pair.getLeft();
            Float c = pair.getRight();

            State s = new State(p.getFinalWeight());
            res.addState(s);
            stateMap.put(pair, s);
            if (previous.get(pair) == null) {
                // this is the start state
                res.setStart(s);
            } else {
                // add the incoming arc from previous to current
                State previouState = stateMap.get(previous.get(pair));
                State previousOldState = previous.get(pair).getLeft();
                int numArcs = previousOldState.getNumArcs();
                for (int j = 0; j < numArcs; j++) {
                    Arc a = previousOldState.getArc(j);
                    if (a.getNextState().getId() == p.getId()) {
                        previouState.addArc(new Arc(a.getIlabel(), a
                                .getOlabel(), a.getWeight(), s));
                    }
                }
            }

            Integer stateIndex = p.getId();
            r[stateIndex]++;

            if ((r[stateIndex] == n)
                    && (p.getFinalWeight() != res.getSemiring().zero())) {
                break;
            }

            if (r[stateIndex] <= n) {
                int numArcs = p.getNumArcs();
                for (int j = 0; j < numArcs; j++) {
                    Arc a = p.getArc(j);
                    float cnew = semiring.times(c, a.getWeight());
                    Pair<State, Float> next = new Pair<State, Float>(
                            a.getNextState(), cnew);
                    previous.put(next, pair);
                    queue.add(next);
                }
            }
        }

        return res;
    }

    /**
     * Removes from the queue the pair with the lower path cost
     */
    private static Pair<State, Float> getLess(
            ArrayList<Pair<State, Float>> queue, float[] d, Semiring semiring) {
        Pair<State, Float> res = queue.get(0);

        for (Pair<State, Float> p : queue) {
            State previousState = res.getLeft();
            State nextState = p.getLeft();
            float previous = res.getRight();
            float next = p.getRight();
            if (semiring.naturalLess(
                    semiring.times(next, d[nextState.getId()]),
                    semiring.times(previous, d[previousState.getId()]))) {
                res = p;
            }
        }
        queue.remove(res);
        return res;
    }
}
