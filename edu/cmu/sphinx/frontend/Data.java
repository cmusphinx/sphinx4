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


package edu.cmu.sphinx.frontend;

/**
 * Implements the interface for all Data objects that passes between
 * DataProcessors. Subclass of Data can contain the actual
 * data, or be a signal (e.g., utterance start, utterance end, speech start,
 * speech end).
 * 
 * @see DoubleData
 * @see Signal
 */
public interface Data {

}
