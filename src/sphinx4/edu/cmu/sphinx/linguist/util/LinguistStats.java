/*
 * Copyright 1999-2003 Carnegie Mellon University.  
 * Portions Copyright 2003 Sun Microsystems, Inc.  
 * Portions Copyright 2003 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.linguist.util;

import edu.cmu.sphinx.linguist.*;

import java.util.*;

/** A linguist processor that dumps out stats about the search space */
public class LinguistStats extends LinguistProcessor {

    private Map stateCountByType = new HashMap();
    private String name;


    /** Dumps the stats of the linguist */
    public void run() {
        Linguist linguist = getLinguist();
        List queue = new LinkedList();
        Set visitedStates = new HashSet();
        int stateCount = 0;
        queue.add(linguist.getSearchGraph().getInitialState());
        while (queue.size() > 0) {
            SearchState state = (SearchState) queue.remove(0);
            if (!visitedStates.contains(state)) {
                stateCount++;
                incrementStateTypeCount(state);
                visitedStates.add(state);
                SearchStateArc[] arcs = state.getSuccessors();
                for (int i = arcs.length - 1; i >= 0; i--) {
                    SearchState nextState = arcs[i].getState();
                    queue.add(nextState);
                }
                // DEBUG: dump out the unique word states
                if (false && state instanceof WordSearchState) {
                    System.out.println("WS: " + state);
                }
                if (false && state instanceof HMMSearchState) {
                    System.out.println("HS: " + state);
                }
            }
        }
        System.out.println("# ----------- linguist stats ------------ ");
        System.out.println("# Total states: " + stateCount);
        dumpStateTypeCounts();
    }


    /**
     * Keeps track of state counts by class
     *
     * @param state the state to track
     */
    private void incrementStateTypeCount(SearchState state) {
        Integer count = (Integer) stateCountByType.get(state.getClass());
        if (count == null) {
            count = 0;
        }
        count = count.intValue() + 1;
        stateCountByType.put(state.getClass(), count);
    }


    /** Dumps all of the class counts */
    private void dumpStateTypeCounts() {
        for (Iterator i = stateCountByType.keySet().iterator(); i.hasNext();) {
            Class clazz = (Class) i.next();
            System.out.println("# " + clazz + ": "
                    + stateCountByType.get(clazz));
        }
    }
}
