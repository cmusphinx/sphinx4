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

import edu.cmu.sphinx.knowledge.dictionary.*;
import java.io.IOException;

/**
 * Dummy trainer dictionary.
 */
public class TrainerDictionary {

    static private Dictionary dictionary;
    static String context = "nada";

    static final String UTTERANCE_BEGIN_SYMBOL = "<s>";
    static final String UTTERANCE_END_SYMBOL = "</s>";
    static final String SILENCE_SYMBOL = "SIL";

    static public Dictionary getDictionary() {
	try {
	    dictionary = new FullDictionary(context);
	} catch (IllegalArgumentException iae) {
	    System.out.println("IAE " + iae);
	} catch (IOException ie) {
	    System.out.println("IE " + ie);
	}
	return dictionary;
    }

}
