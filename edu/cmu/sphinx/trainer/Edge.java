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

package edu.cmu.sphinx.trainer;


/**
 * This is a dummy implementation of Edge just so that we can compile.
 */
public interface Edge {

    /**
     * Sets the destination node for a given edge.
     */
    public void setDestination(Node node);

    /**
     * Sets source node for a given edge.
     */
    public void setSource(Node node);
}
