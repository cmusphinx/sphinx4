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
package edu.cmu.sphinx.linguist.util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.util.Timer;
/**
 * Gets successors from a linguist and times them
 */
public class LinguistTimer {
    private Linguist linguist;
    private boolean details;
    int totalStates;
    int totalEmittingStates;
    int totalNonEmittingStates;
    int totalFinalStates;
    int maxSuccessors;
    /**
     * Creats a LinguistTimer
     * 
     * @param linguist
     *            the linguist to time
     * @param details
     *            if true print out details
     */
    public LinguistTimer(Linguist linguist, boolean details) {
        this.linguist = linguist;
        this.details = details;
    }
    /**
     * tests the linguist
     */
    /**
     * Times the lingust
     * 
     * @param numRuns
     *            the number of simulated runs
     * @param numFrames
     *            the number of simulated frames
     * @param maxBeam
     *            the size of the beam
     */
    public void timeLinguist(int numRuns, int numFrames, int maxBeam) {
        // this test invokes the linguist using access patterns that
        // are similar to a real search. It allows for timing and
        // profiling of the linguist, independent of the search
        // or scoring
        Random random = new Random(1000);
        Timer frameTimer = Timer.getTimer("frameTimer");
        Timer totalTimer = Timer.getTimer("totalTimer");
        // Note: this comparator imposes orderings that are
        // inconsistent with equals.
        System.out.println("TestLinguist: runs " + numRuns + " frames "
                + numFrames + " beam " + maxBeam);
        totalTimer.start();
        for (int runs = 0; runs < numRuns; runs++) {
            int level = 0;
            List activeList = new ArrayList();
            activeList.add(linguist.getSearchGraph().getInitialState());
            linguist.startRecognition();
            for (int i = 0; i < numFrames; i++) {
                List oldList = activeList;
                activeList = new ArrayList(maxBeam * 10);
                frameTimer.start();
                for (int j = 0; j < oldList.size(); j++) {
                    SearchState nextStates = (SearchState) oldList.get(j);
                    expandState(level, activeList, nextStates);
                }
                frameTimer.stop();
                Collections.shuffle(activeList, random);
                if (activeList.size() > maxBeam) {
                    activeList = activeList.subList(0, maxBeam);
                }
            }
            linguist.stopRecognition();
            frameTimer.dump();
        }
        totalTimer.stop();
        System.out.println(" MaxSuccessors : " + maxSuccessors);
        System.out.println(" TotalStates   : " + totalStates);
        System.out.println(" TotalEmitting : " + totalEmittingStates);
        System.out.println("   NonEmitting : " + totalNonEmittingStates);
        System.out.println("  Final States : " + totalFinalStates);
        Timer.dumpAll();
    }
    /**
     * expand the give search state
     * 
     * @param level
     *            the nesting level
     * @param activeList
     *            where next states are placed
     * @param state
     *            the search state to expand
     */
    private void expandState(int level, List activeList, SearchState state) {
        SearchStateArc[] newStates = state.getSuccessors();
        totalStates++;
        // System.out.println(Utilities.pad(level * 2) + state);
        if (newStates.length > maxSuccessors) {
            maxSuccessors = newStates.length;
        }
        for (int i = 0; i < newStates.length; i++) {
            SearchState ns = newStates[i].getState();
            if (ns.isEmitting()) {
                totalEmittingStates++;
                activeList.add(ns);
            } else if (!ns.isFinal()) {
                totalNonEmittingStates++;
                activeList.add(ns);
                if (false && details && ns.isFinal()) {
                    System.out.println("result " + ns.toPrettyString());
                }
                expandState(level + 1, activeList, ns);
            } else {
                totalFinalStates++;
            }
            totalStates++;
        }
    }
}
