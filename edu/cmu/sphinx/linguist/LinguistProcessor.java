/*
 * Copyright 1999-2003 Carnegie Mellon University.  
 * Portions Copyright 2002-2003 Sun Microsystems, Inc.  
 * Portions Copyright 2002-2003 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.linguist;

import edu.cmu.sphinx.util.SphinxProperties;

/**
 * A standard interface for a linguist processor
 *
 */
public interface LinguistProcessor {

  /**
   * Allows for pluggable behaviors for the linguist.  Linguist
   * processors are typically invoked after the linguist has been
   * 'initialized', but before recognition begins. LinguistProcessors
   * are often used to do things such as optimize the sentence hmm or
   * dumping out the sentence hmm in a fashion suitable for plotting.
   *
   * @param props the set of sphinx properties
   * @param linguist the initial state of the sentence hmm
   *
   */
    public void  process(SphinxProperties props, Linguist linguist);

}


