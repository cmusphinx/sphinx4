
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

package edu.cmu.sphinx.research.parallel;

import edu.cmu.sphinx.decoder.search.ActiveList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implements a FeatureStream used in parallel decoding.
 * Each kind of FeatureStream is a singleton (i.e., there is only 
 * one instance).
 */
public class FeatureStream {

    private static final Map streams = new HashMap();

    /**
     * Name of the FeatureStream.
     */
    private final String name;

    /**
     * Eta value assigned to the FeatureStream.
     */
    private float eta;

    /**
     * Token ActiveList for this FeatureStream.
     */
    private ActiveList activeList;

    private FeatureStream(String name) {
        this.name = name;
    }

    /**
     * Returns a FeatureStream with the given name.
     *
     * @param name the name of the FeatureStream
     */
    public static FeatureStream getFeatureStream(String name) {
        FeatureStream stream = (FeatureStream)streams.get(name);
        if (stream == null) {
            stream = new FeatureStream(name);
            streams.put(name, stream);
        }
        return stream;
    }

    /**
     * Returns an iterator of all FeatureStreams
     *
     * @return an iterator
     */
    public static Iterator iterator() {
        return streams.values().iterator();
    }

    /**
     * Returns the name of this FeatureStream.
     *
     * @return the name of this FeatureStream
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the eta value of this FeatureStream.
     *
     * @return the eta value of this FeatureStream
     */
    public float getEta() {
        return eta;
    }

    /**
     * Returns the active list
     *
     * @return the active list
     */
    public ActiveList getActiveList() {
        return activeList;
    }

    /**
     * Sets the eta value of this FeatureStream
     *
     * @param eta the eta value
     */
    public void setEta(float eta) {
        this.eta = eta;
    }

    /**
     * Sets the active list
     *
     * @param list the active list
     */
    public void setActiveList(ActiveList list) {
        this.activeList = list;
    }
}        
