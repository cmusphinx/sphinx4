
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
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.util.ExtendedStreamTokenizer;
import edu.cmu.sphinx.util.LogMath;


/**
 * Defines a grammar based upon a set of words in a file. The path to
 * the file is defined by the PROP_PATH property. If the PROP_LOOP
 * property is true, the grammar created will be a looping grammar.
 *
 * All grammar probabilities are maintained in LogMath log base
 */
public class WordListGrammar extends Grammar {

    private final static String PROP_PREFIX =
        "edu.cmu.sphinx.linguist.language.grammar.WordListGrammar.";


    /**
     * Sphinx property that defines the location of the word list
     * grammar
     */
    public final static String PROP_PATH = PROP_PREFIX + "path";


    /**
     * Sphinx property that if true, indicates that this is a looping
     * grammar
     */
    public final static String PROP_LOOP = PROP_PREFIX + "isLooping";

    /**
     * Sphinx property that indicates that a silence word should
     * automatically be added to the grammar
     */
    public final static String PROP_ADD_SILENCE = 
        PROP_PREFIX + "addSilenceWord";


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
	String path = props.getString(PROP_PATH, "spelling.gram");
	boolean isLooping = props.getBoolean(PROP_LOOP, true);
	boolean addSilence = props.getBoolean(PROP_ADD_SILENCE, false);
	String[][] silenceNode = {{Dictionary.SILENCE_SPELLING}};
	String[] silenceAlternative = { Dictionary.SILENCE_SPELLING };
	int identity = 0;
	ExtendedStreamTokenizer tok = new ExtendedStreamTokenizer(path, true);

	List alternatives = new ArrayList();


	if (addSilence) {
	    alternatives.add(silenceAlternative);
	}

	while (!tok.isEOF()) {
	    String word;
	    List words = new ArrayList();
	    while ((word = tok.getString()) != null) {
		words.add(word.toLowerCase());
		// System.out.println("Added " + word);
	    }
	    if (words.size() > 0) {
		String[] wordArray = new String[words.size()];
		for (int i = 0; i < wordArray.length; i++) {
		    wordArray[i] = (String) words.get(i);
		}
		alternatives.add(wordArray);
	    }
	}

	String[][] alternativeArray = new String[alternatives.size()][];

	for (int i = 0; i < alternatives.size(); i++) {
	    alternativeArray[i] = (String[]) alternatives.get(i);
	}

	GrammarNode firstNode = createGrammarNode(identity++, false);
        GrammarNode firstSilenceNode = createGrammarNode(identity++,
		silenceNode);
	GrammarNode wordNode = createGrammarNode(identity++, alternativeArray);
	GrammarNode finalNode = createGrammarNode(identity++, true);

	firstNode.add(wordNode, LogMath.getLogOne());
	firstNode.add(firstSilenceNode, LogMath.getLogOne());
	firstSilenceNode.add(wordNode, LogMath.getLogOne());
	if (isLooping) {
	    wordNode.add(wordNode, LogMath.getLogOne());
	}
	wordNode.add(finalNode, LogMath.getLogOne());

	return firstNode;
    }
}
