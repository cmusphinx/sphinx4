
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

package edu.cmu.sphinx.linguist.language.grammar;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.sphinx.util.ExtendedStreamTokenizer;
import edu.cmu.sphinx.util.LogMath;


/**
 * Defines a grammar based upon a list of words in a file.
 * The format of the file is just one word per line. For example,
 * for an isolated digits grammar the file will simply look like:
 * <pre>
 * zero
 * one
 * two
 * three
 * four
 * five
 * six
 * seven
 * eight
 * nine
 * </pre>
 * The path to the file is defined by the {@link #PROP_PATH PROP_PATH}
 * property. If the {@link #PROP_LOOP PROP_LOOP} property is true,
 * the grammar created will be a looping grammar.
 * Using the above digits grammar example,
 * setting PROP_LOOP to true will make it a connected-digits grammar.
 * <p>
 * All probabilities are maintained in LogMath log base.
 */
public class SimpleWordListGrammar extends Grammar {


    private final static String PROP_PREFIX
	= "edu.cmu.sphinx.linguist.language.grammar.SimpleWordListGrammar.";


    /**
     * Sphinx property that defines the location of the word list
     * grammar
     */
    public final static String PROP_PATH = PROP_PREFIX + "path";


    /**
     * The default value for PROP_PATH.
     */
    public final static String PROP_PATH_DEFAULT = "spelling.gram";


    /**
     * Sphinx property that if true, indicates that this is a looping
     * grammar
     */
    public final static String PROP_LOOP = PROP_PREFIX + "isLooping";


    /**
     * The default value for PROP_LOOP.
     */
    public final static boolean PROP_LOOP_DEFAULT = true;


    /**
     * Create class from reference text (not implemented).
     *
     * @param bogusText dummy variable
     *
     * @throws NoSuchMethogException if called with reference sentence
     */
    protected GrammarNode createGrammar(String bogusText)
	throws NoSuchMethodException {
	throw new NoSuchMethodException("Does not create "
				       + "grammar with reference text");
    }

    /**
     * Creates the grammar.
     *
     */
    protected GrammarNode createGrammar()
	throws IOException, NoSuchMethodException {
	String path = props.getString(PROP_PATH, PROP_PATH_DEFAULT);
	boolean isLooping = props.getBoolean(PROP_LOOP, PROP_LOOP_DEFAULT);

	int identity = 0;
	ExtendedStreamTokenizer tok = new ExtendedStreamTokenizer(path,true);


        GrammarNode initialNode = createGrammarNode(identity++, "<sil>");
	GrammarNode branchNode = createGrammarNode(identity++, false);
	GrammarNode finalNode = createGrammarNode(identity++, "<sil>");
        finalNode.setFinalNode(true);

	
	List wordGrammarNodes = new LinkedList();

	while (!tok.isEOF()) {
	    String word;
	    while ((word = tok.getString()) != null) {
		word = word.toLowerCase();
		GrammarNode wordNode = createGrammarNode(identity++, word);
		wordGrammarNodes.add(wordNode);
	    }
	}

	// now connect all the GrammarNodes together


	initialNode.add(branchNode, LogMath.getLogOne());
	float branchScore = getLogMath().linearToLog
	    (1.0/wordGrammarNodes.size());

	for (Iterator i = wordGrammarNodes.iterator(); i.hasNext();) {
	    GrammarNode wordNode = (GrammarNode) i.next();
	    branchNode.add(wordNode, branchScore);
            wordNode.add(finalNode, LogMath.getLogOne());
            if (isLooping) {
                wordNode.add(branchNode, LogMath.getLogOne());
            } 
	}

	return initialNode;
    }
}
