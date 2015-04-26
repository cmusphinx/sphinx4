/*
 * Copyright 2014 Alpha Cephei Inc.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.alignment;

import java.util.List;

public interface TextTokenizer {
    
    
    /**
     * Cleans the text and returns the list of lines
     * 
     * @param text Input text 
     * @return a list of lines in the text.
     */
    List<String> expand(String text);
}
