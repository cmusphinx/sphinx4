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

package edu.cmu.sphinx.trainer;

import edu.cmu.sphinx.linguist.dictionary.Dictionary;

/** Provides mechanisms for accessing an utterance's transcription. */
public interface Transcript {

    /** @return the transcript's text */
    public String getTranscriptText();


    /** @return the transcript's dictionary. */
    public Dictionary getDictionary();


    /** @return whether the transcript is exact. */
    public boolean isExact();


    /** @return the number of words in the transcription. */
    public int numberOfWords();


    /** Start the iterator for the words in the transcription. */
    public void startWordIterator();


    /** @return whether there are more words. */
    public boolean hasMoreWords();


    /** @return the next word. */
    public String nextWord();
}
