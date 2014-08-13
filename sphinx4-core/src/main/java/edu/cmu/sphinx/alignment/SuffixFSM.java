/**
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.alignment;

import java.io.IOException;
import java.net.URL;

/**
 * Implements a finite state machine that checks if a given string is a suffix.
 */
public class SuffixFSM extends PronounceableFSM {

    /**
     * Constructs a SuffixFSM.
     */
    public SuffixFSM(URL url) throws IOException {
        super(url, false);
    }
}
