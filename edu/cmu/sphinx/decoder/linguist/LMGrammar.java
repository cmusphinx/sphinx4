
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.sphinx.knowledge.dictionary.Word;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.knowledge.language.WordSequence;
import edu.cmu.sphinx.util.Timer;


/**
 * Defines a grammar based upon a lanaguage model
 *
 * This is a simple version that generates one grammar node per word.
 * This grammar can deal with unigram and bigram grammars of up to
 * 1000 or so words. 
 *
 * Note that all probabilities are in the log math domain
 */
public class LMGrammar extends Grammar {

    /**
     * Creates the grammar from the language model.  This Grammar
     * contains one word per grammar node. Each word (and grammar
     * node) is connected to all other words with the given
     * probability
     *
     * @return the initial grammar node
     */
    protected GrammarNode createGrammar()
	throws IOException, NoSuchMethodException {
            
        Timer.start("LMGrammar.create");
        LanguageModel languageModel = getLanguageModel();
        GrammarNode firstNode = null;


        if (languageModel == null) {
            throw new Error("No language model for LMGramar");
        }

        if (languageModel.getMaxDepth() > 2) {
            System.out.println("Warning: LMGrammar  limited to bigrams");
        }

        // if the language model log base doesn't match the log base of
        // this grammar, something lis probably wrong so we should just
        // abort.   TODO: perhaps we should just adapt the probs.

        if (languageModel.getLogMath() != getLogMath()) {
            throw new Error("Mismatch log error in grammar");
        }

	int identity = 0;
        List nodes = new ArrayList();

        Set words = getLanguageModel().getVocabulary();

        // create all of the word nodes

        for (Iterator i = words.iterator(); i.hasNext(); ) {
            String word = (String) i.next();
            GrammarNode node = createGrammarNode(identity++, word);
            if (node != null && !node.isEmpty()) {
                if (node.getWord().equals
                    (getDictionary().getSentenceStartWord())) {
                    firstNode = node;
                } else if (node.getWord().equals
                           (getDictionary().getSentenceEndWord())) {
                    node.setFinalNode(true);
                }
                nodes.add(node);
            }
        }

        if (firstNode == null) {
            throw new Error("No sentence start found in language model");
        }


        for (Iterator i = nodes.iterator(); i.hasNext(); ) {
            GrammarNode prevNode = (GrammarNode) i.next();

            // don't add any branches out of  the final node
            if (prevNode.isFinalNode()) {
                continue;
            }

            for (Iterator j = nodes.iterator(); j.hasNext(); ) {
                GrammarNode nextNode = (GrammarNode) j.next();
                String prevWord = prevNode.getWord().getSpelling();
                String nextWord = nextNode.getWord().getSpelling();
                Word[] wordArray = 
                    {getDictionary().getWord(prevWord),
                     getDictionary().getWord(nextWord)};

                float logProbability = languageModel.getProbability
                    (WordSequence.getWordSequence(wordArray));

                prevNode.add(nextNode,  logProbability);

            }
        }

        Timer.stop("LMGrammar.create");
	return firstNode;
    }
}
