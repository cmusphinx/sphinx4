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

package edu.cmu.sphinx.decoder.linguist;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.decoder.linguist.Grammar;
import edu.cmu.sphinx.decoder.linguist.GrammarNode;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;


/**
 * Defines an empty grammar
 *
 */
public class NullGrammar extends Grammar {

    /**
     * Creates the grammar from the language model.  
     *
     * @return the initial grammar node
     */
    protected GrammarNode createGrammar()
	throws IOException, NoSuchMethodException {
            
	return null;
    }
}
