/*
 * Copyright 2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.util;

import java.util.Iterator;
import java.io.IOException;


/*
 * Provides a standard interface to a batching mechanism
 *
 */
public interface BatchManager {


    /**
     * Starts processing the batch
     *
     */
    public void start() throws IOException;

    /**
     * Gets the next available batch item or null if no more are
     * available
     *
     * @return the next available batch item
     */
    public BatchItem getNextItem() throws IOException ;

    /**
     * Stops processing the batch
     */
    public void stop() throws IOException;


    /**
     * Returns the name of the file
     *
     * @return the filename
     */
    public String getFilename();
}
