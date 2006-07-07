
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

package edu.cmu.sphinx.decoder.pruner;

import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.Registry;


/**
 * A Null pruner. Does no actual pruning
 */
public class NullPruner implements Pruner {
    private String name;

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String, edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry) throws PropertyException {
        this.name = name;
        
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }
    
    /**
     * Creates a simple pruner
     *
     */
    public NullPruner() {
    }



    /**
     * starts the pruner
     */
    public void startRecognition() {
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
    public void stopRecognition() {
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.decoder.pruner.Pruner#allocate()
     */
    public void allocate() {
        
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.decoder.pruner.Pruner#deallocate()
     */
    public void deallocate() {
        
    }

}
