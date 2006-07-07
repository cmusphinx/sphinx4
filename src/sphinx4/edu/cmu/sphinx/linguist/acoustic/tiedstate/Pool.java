
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

package edu.cmu.sphinx.linguist.acoustic.tiedstate;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Used to pool shared objects in the acoustic model
 */
public class Pool {
    private String name;
    private List pool;
    private Map features = new HashMap();

    /**
     * Creates a new pool
     *
     * @param name the name of the pool
     */
    public Pool(String name) {
	this.name = name;
	pool = new ArrayList();
    }

    /**
     * returns the pool's name.
     *
     * @return the pool name
     */
    public String getName() {
	return name;
    }

    /**
     * returns the object with the given ID from the pool
     *
     * @param id the id of the object
     *
     * @return the object
     *
     * @throws IndexOutOfBoundsException if the ID is out of range
     */
    public Object get(int id) {
	return pool.get(id);
    }

    /**
     * returns the ID of a given object from the pool
     *
     * @param object the object
     *
     * @return the index
     */
    public int indexOf(Object object) {
	return pool.indexOf(object);
    }

    /**
     * Places the given object in the pool
     *
     * @param id a unique ID for this object
     * @param o  the object to add to the pool
     */
    public void put(int id, Object o) {
	if (id == pool.size()) {
	    pool.add(o);
	} else {
	    pool.set(id, o);
	}
    }

    /**
     * Retrieves the size of the pool
     * 
     * @return the size of the pool
     */
    public int size() {
	return pool.size();
    }

    /**
     * Dump information on this pool to the given
     * logger
     * 
     * @param logger the logger to send the info to
     */
    public void logInfo(Logger logger) {
	logger.info("Pool " + name + " Entries: " + size());
    }

    /**
     * Sets a feature for this pool
     *
     * @param name the name of the feature
     * @param value the value for the feature
     *
     */
    public void setFeature(String name, int value) {
	features.put(name, new Integer(value));
    }

    /**
     * Retrieves a feature from this pool
     *
     * @param name the name of the feature
     * @param defaultValue the defaultValue for the pool
     *
     * @return the value for the feature
     */
    public int getFeature(String name, int defaultValue) {
	Integer val = (Integer) features.get(name);
	if (val == null) {
	    return defaultValue;
	} else {
	    return val.intValue();
	}
    }
}

