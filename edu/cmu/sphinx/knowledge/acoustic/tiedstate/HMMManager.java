
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

package edu.cmu.sphinx.knowledge.acoustic.tiedstate;
import edu.cmu.sphinx.knowledge.acoustic.HMM;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.knowledge.acoustic.HMMPosition;

import java.util.HashMap;
import java.util.logging.Logger;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;



/**
 * Manages the HMMS. HMMs are grouped by word position
 */
public class HMMManager {

    private List allHMMs = new ArrayList();
    private Map[] hmmsPerPosition = new Map[HMMPosition.MAX_POSITIONS];

    /**
     * Put an HMM into this manager
     *
     * @param hmm the hmm to manage
     */
    public void put(HMM hmm) {
	Map hmmMap = getHMMMap(hmm.getPosition());
	hmmMap.put(hmm.getUnit(), hmm);
	allHMMs.add(hmm);
    }


    /**
     * Retrieves an HMM by position and unit
     *
     * @param position the position of the HMM
     * @param unit the unit that this HMM represents
     *
     * @return the HMM for the unit at the given position or null if
     * 	no HMM at the position could be found
     */
    public HMM get(HMMPosition position, Unit unit) {
	Map hmmMap = getHMMMap(position);
	return (HMM) hmmMap.get(unit);
    }


    /**
     * Gets an iterator that iterates through all HMMs
     *
     * @return an iterator that iterates through all HMMs
     */
    public Iterator getIterator() {
	return allHMMs.iterator();
    }

    /**
     * Gets the map associated with the given position. Creates the
     * map as necessary.
     *
     * @param pos the position of interest
     *
     * @return the map of HMMs for the given position.
     */
    private Map getHMMMap(HMMPosition pos) {
	Map hmmMap = (Map) hmmsPerPosition[pos.getIndex()];
	if (hmmMap == null) {
	    hmmMap = new LinkedHashMap();
	    hmmsPerPosition[pos.getIndex()] = hmmMap;
	}
	return hmmMap;
    }

    /**
     * Returns the number of HMMS in this manager
     *
     * @return the number of HMMs
     */
    private int getNumHMMs() {
	int count = 0;

        for (int i = 0; i < hmmsPerPosition.length; i++) {
	    Map map = hmmsPerPosition[i];
	    if (map != null) {
		count += map.size();
	    }
	}
	return count;
    }

    /**
     * Log information about this manager
     *
     * @param logger logger to use for this logInfo
     */
    public void logInfo(Logger logger) {
	logger.info("HMM Manager: " + getNumHMMs() + " hmms");
    }
}
