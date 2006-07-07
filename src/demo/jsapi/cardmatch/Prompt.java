
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

package demo.jsapi.cardmatch;


/**
 * An interface that represents a spoken prompt
 */
public interface Prompt {

    /**
     * Gets the next text to be spoken
     *
     * @return the text to be spoken
     */
    public String getText();

    /**
     * Resets the prompt to its initial state
     */
    public void reset();
}
