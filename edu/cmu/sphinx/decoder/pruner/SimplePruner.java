
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

package edu.cmu.sphinx.decoder.pruner;

import edu.cmu.sphinx.decoder.search.ActiveList;


/**
 * Performs the default pruning behavior which is to invoke
 * the purge on the active list
 */
public class SimplePruner implements Pruner {

    /**
     * Starts the pruner
     */
    public void start() {}


    /**
     * Initializes this Pruner with the given context.
     *
     * @param context the context to use
     */
    public void initialize(String context) {
    }


    /**
     * prunes the given set of states
     *
     * @param activeList a activeList of tokens
     */
    public ActiveList prune(ActiveList activeList) {
	return activeList.purge(); 
    }


    /**
     * Performs post-recognition cleanup. 
     */
    public void stop() {}
}


