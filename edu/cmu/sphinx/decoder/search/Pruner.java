
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

import java.util.List;

/**
 * Provides a mechanism for pruning a set of StateTokens
 *
 */
public interface Pruner {

    /**
     * Starts the pruner
     */
    public void start();


    /**
     * Initializes this Pruner with the given context.
     *
     * @param context the context to use
     */
    public void initialize(String context);



    /**
     * prunes the given set of states
     *
     * @param stateTokenList a list containing StateToken objects to
     * be scored
     *
     * @return the pruned list, (may be the sample list as
     * stateTokenList)
     */
    public ActiveList prune(ActiveList stateTokenList);


    /**
     * Performs post-recognition cleanup. 
     */
    public void stop();
}


