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

package edu.cmu.sphinx.decoder;

import edu.cmu.sphinx.frontend.Signal;

/**
 *  The listener interface for being informed when a non-content
 *  signal is generated
 */
public interface SignalListener {
    /**
     * Method called when a non-content signal is detected
     *
     * @param signal the non-content signal
     *
     */
     public void newSignal(Signal signal);
}

