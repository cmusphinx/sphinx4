
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

package edu.cmu.sphinx.decoder.search;

import edu.cmu.sphinx.decoder.search.Pruner;
import edu.cmu.sphinx.decoder.search.ActiveList;


/**
 * A Null pruner. Does no actual pruning
 */
public class NullPruner implements Pruner {


    /**
     * Creates a simple pruner
     *
     */
    public NullPruner() {
    }

    /**
     * Initializes this Pruner with the given context.
     *
     * @param context the context to use
     */
    public void initialize(String context) {
    }

    /**
     * starts the pruner
     */
    public void start() {
    }

    /**
     * prunes the given set of states
     *
     * @param activeList the active list of tokens
     *
     * @return the pruned (and possibly new) activeList
     */
    public ActiveList prune(ActiveList activeList) {
	return activeList;
    }


    /**
     * Performs post-recognition cleanup. 
     */
    public void stop() {
    }
}
