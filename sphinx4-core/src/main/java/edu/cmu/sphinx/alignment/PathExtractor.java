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
 * Manages a feature or item path. Allows navigation to the corresponding
 * feature or item.
 */
public interface PathExtractor {

    /**
     * Finds the item associated with this object.
     * 
     * @param item the starting point for the path navigation
     * 
     * @return the item associated with the path or null
     */
    Item findItem(Item item);

    /**
     * Finds the feature associated with this object.
     * 
     * @param item the starting point for the path navigation
     * 
     * @return the feature associated or "0" if the feature was not found
     */
    Object findFeature(Item item);

    // TODO: add these to the interface should we support binary
    // files
    /*
     * public void writeBinary(); public void readBinary();
     */
}
