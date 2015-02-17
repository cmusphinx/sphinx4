/**
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute,
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */
package edu.cmu.sphinx.alignment.tokenizer;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of the FeatureSet interface.
 */
public class FeatureSet {

    private final Map<String, Object> featureMap;
    static DecimalFormat formatter;

    /**
     * Creates a new empty feature set
     */
    public FeatureSet() {
        featureMap = new LinkedHashMap<String, Object>();
    }

    /**
     * Determines if the given feature is present.
     *
     * @param name the name of the feature of interest
     *
     * @return true if the named feature is present
     */
    public boolean isPresent(String name) {
        return featureMap.containsKey(name);
    }

    /**
     * Removes the named feature from this set of features.
     *
     * @param name the name of the feature of interest
     */
    public void remove(String name) {
        featureMap.remove(name);
    }

    /**
     * Convenience method that returns the named feature as a string.
     *
     * @param name the name of the feature
     *
     * @return the value associated with the name or null if the value is not
     *         found
     *
     * @throws ClassCastException if the associated value is not a String
     */
    public String getString(String name) {
        return (String) getObject(name);
    }

    /**
     * Convenience method that returns the named feature as a int.
     *
     * @param name the name of the feature
     *
     * @return the value associated with the name or null if the value is not
     *         found
     *
     * @throws ClassCastException if the associated value is not an int.
     */
    public int getInt(String name) {
        return ((Integer) getObject(name)).intValue();
    }

    /**
     * Convenience method that returns the named feature as a float.
     *
     * @param name the name of the feature
     *
     * @return the value associated with the name or null if the value is not
     *         found.
     *
     * @throws ClassCastException if the associated value is not a float
     */
    public float getFloat(String name) {
        return ((Float) getObject(name)).floatValue();
    }

    /**
     * Returns the named feature as an object.
     *
     * @param name the name of the feature
     *
     * @return the value associated with the name or null if the value is not
     *         found
     */
    public Object getObject(String name) {
        return featureMap.get(name);
    }

    /**
     * Convenience method that sets the named feature as a int.
     *
     * @param name the name of the feature
     * @param value the value of the feature
     */
    public void setInt(String name, int value) {
        setObject(name, new Integer(value));
    }

    /**
     * Convenience method that sets the named feature as a float.
     *
     * @param name the name of the feature
     * @param value the value of the feature
     */
    public void setFloat(String name, float value) {
        setObject(name, new Float(value));
    }

    /**
     * Convenience method that sets the named feature as a String.
     *
     * @param name the name of the feature
     * @param value the value of the feature
     */
    public void setString(String name, String value) {
        setObject(name, value);
    }

    /**
     * Sets the named feature.
     *
     * @param name the name of the feature
     * @param value the value of the feature
     */
    public void setObject(String name, Object value) {
        featureMap.put(name, value);
    }
}
