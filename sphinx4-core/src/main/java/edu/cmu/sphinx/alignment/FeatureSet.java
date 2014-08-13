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
package edu.cmu.sphinx.alignment;


/**
 * Represents the abstract interface to an entity that has a set of features.
 * Provides interfaces to set and get the name/value pairs as well as providing
 * a set of convenience methods for setting and retrieving values of a
 * particular type.
 */

public interface FeatureSet {

    /**
     * Determines if the given feature is present.
     *
     * @param name the name of the feature of interest
     *
     * @return true if the named feature is present
     */
    boolean isPresent(String name);

    /**
     * Removes the named feature from this set of features.
     *
     * @param name the name of the feature of interest
     */
    void remove(String name);

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
    String getString(String name);

    /**
     * Convenience method that returns the named feature as an int.
     *
     * @param name the name of the feature
     *
     * @return the value associated with the name or null if the value is not
     *         found
     *
     * @throws ClassCastException if the associated value is not an int
     */
    int getInt(String name);

    /**
     * Convenience method that returns the named feature as a float.
     *
     * @param name the name of the feature
     *
     * @return the value associated with the name or null if the value is not
     *         found
     *
     * @throws ClassCastException if the associated value is not a float.
     */
    float getFloat(String name);

    /**
     * Returns the named feature as an object.
     *
     * @param name the name of the feature
     *
     * @return the value associated with the name or null if the value is not
     *         found
     */
    Object getObject(String name);

    /**
     * Convenience method that sets the named feature as an int.
     *
     * @param name the name of the feature
     * @param value the value of the feature
     */
    void setInt(String name, int value);

    /**
     * Convenience method that sets the named feature as a float
     *
     * @param name the name of the feature
     * @param value the value of the feature
     */
    void setFloat(String name, float value);

    /**
     * Convenience method that sets the named feature as a String.
     *
     * @param name the name of the feature
     * @param value the value of the feature
     */
    void setString(String name, String value);

    /**
     * Sets the named feature .
     *
     * @param name the name of the feature
     * @param value the value of the feature
     */
    void setObject(String name, Object value);
}
