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
package edu.cmu.sphinx.linguist.language.grammar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * Defines a simple grammar based upon a language model. It generates one
 * {@link GrammarNode grammar node}per word. This grammar can deal with
 * unigram and bigram grammars of up to 1000 or so words. Note that all
 * probabilities are in the log math domain.
 */
public class LMGrammar extends Grammar {
    /**
     * A sphinx property for the language model to be used by this grammar
     */
    public final static String PROP_LANGUAGE_MODEL = "languageModel";
    // ------------------------
    // Configuration data
    // ------------------------
    private LanguageModel languageModel;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_LANGUAGE_MODEL, PropertyType.COMPONENT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        languageModel = (LanguageModel) ps.getComponent(PROP_LANGUAGE_MODEL,
                LanguageModel.class);
    }

    /**
     * Creates the grammar from the language model. This Grammar contains one
     * word per grammar node. Each word (and grammar node) is connected to all
     * other words with the given probability
     * 
     * @return the initial grammar node
     */
    protected GrammarNode createGrammar() throws IOException {
        languageModel.allocate();
        Timer.start("LMGrammar.create");
        GrammarNode firstNode = null;
        if (languageModel.getMaxDepth() > 2) {
            System.out.println("Warning: LMGrammar  limited to bigrams");
        }
        int identity = 0;
        List nodes = new ArrayList();
        Set words = languageModel.getVocabulary();
        // create all of the word nodes
        for (Iterator i = words.iterator(); i.hasNext();) {
            String word = (String) i.next();
            GrammarNode node = createGrammarNode(identity++, word);
            if (node != null && !node.isEmpty()) {
                if (node.getWord().equals(
                        getDictionary().getSentenceStartWord())) {
                    firstNode = node;
                } else if (node.getWord().equals(
                        getDictionary().getSentenceEndWord())) {
                    node.setFinalNode(true);
                }
                nodes.add(node);
            }
        }
        if (firstNode == null) {
            throw new Error("No sentence start found in language model");
        }
        for (Iterator i = nodes.iterator(); i.hasNext();) {
            GrammarNode prevNode = (GrammarNode) i.next();
            // don't add any branches out of the final node
            if (prevNode.isFinalNode()) {
                continue;
            }
            for (Iterator j = nodes.iterator(); j.hasNext();) {
                GrammarNode nextNode = (GrammarNode) j.next();
                String prevWord = prevNode.getWord().getSpelling();
                String nextWord = nextNode.getWord().getSpelling();
                Word[] wordArray = {getDictionary().getWord(prevWord),
                        getDictionary().getWord(nextWord)};
                float logProbability = languageModel
                        .getProbability(WordSequence.getWordSequence(wordArray));
                prevNode.add(nextNode, logProbability);
            }
        }
        Timer.stop("LMGrammar.create");
        languageModel.deallocate();
        return firstNode;
    }
}
