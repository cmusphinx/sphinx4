/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.linguist.language.grammar;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.util.LogMath;

public class AlignerGrammar extends Grammar {

    protected GrammarNode finalNode;
    private final List<String> tokens = new ArrayList<String>();

    public AlignerGrammar(final boolean showGrammar, final boolean optimizeGrammar, final boolean addSilenceWords,
            final boolean addFillerWords, final Dictionary dictionary) {
        super(showGrammar, optimizeGrammar, addSilenceWords, addFillerWords, dictionary);
    }

    public AlignerGrammar() {
    }

    /*
     * Reads Text and converts it into a list of tokens
     */
    public void setText(String text) {
        setWords(asList(text.split(" ")));
    }

    public void setWords(Iterable<String> words) {
        tokens.clear();
        for (String word : words) {
            if (!word.isEmpty()) {
                tokens.add(word);
            }
        }
        createGrammar();
        postProcessGrammar();
    }

    @Override
    protected GrammarNode createGrammar() {

        logger.info("Making Grammar");

        initialNode = createGrammarNode(Dictionary.SILENCE_SPELLING);
        finalNode = createGrammarNode(true);

        GrammarNode prevNode = initialNode;
        for (final String word : tokens) {
            final GrammarNode wordNode = createGrammarNode(word);
            final GrammarNode alternativeNode = createGrammarNode(false);
            final GrammarNode exitNode = createGrammarNode(false);
            prevNode.add(wordNode, LogMath.LOG_ONE);
            prevNode.add(alternativeNode, LogMath.LOG_ONE);
            wordNode.add(exitNode, LogMath.LOG_ONE);
            alternativeNode.add(exitNode, LogMath.LOG_ONE);
            prevNode = exitNode;
        }
        prevNode.add(finalNode, LogMath.LOG_ONE);

        logger.info("Done making Grammar");
        return initialNode;
    }

}
