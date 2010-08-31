/*
 * Copyright 1999-2010 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2010 PC-NG Inc.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.linguist.language.grammar;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.language.grammar.Grammar;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.ExtendedStreamTokenizer;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;

/**
 * Creates a grammar from a text. This grammar could be used to create a precise
 * recognizer that will force align the text with the audio.
 */
public class TextAlignerGrammar extends Grammar implements ResultListener {

    /** The property that defines the logMath component. */
    @S4Component(type = LogMath.class)
    public final static String PROP_LOG_MATH = "logMath";
    
    protected GrammarNode finalNode;
    private LogMath logMath;

    private final List<String> tokens = new ArrayList<String>();

    private int start;
    private final int step = 40;

    public TextAlignerGrammar(final String text, final LogMath logMath, final boolean showGrammar, final boolean optimizeGrammar,
            final boolean addSilenceWords, final boolean addFillerWords, final Dictionary dictionary) {
        super(showGrammar, optimizeGrammar, addSilenceWords, addFillerWords, dictionary);
        this.logMath = logMath;
        setText(text);
    }

    public TextAlignerGrammar() {
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
     @Override
     public void newProperties(PropertySheet ps) throws PropertyException {
         super.newProperties(ps);
         logMath = (LogMath) ps.getComponent(PROP_LOG_MATH);
     }
    
    public void setText(String text) {
        String word;
        try {
            final ExtendedStreamTokenizer tok = new ExtendedStreamTokenizer(new StringReader(text), true);

            tokens.clear();
            while (!tok.isEOF()) {
                while ((word = tok.getString()) != null) {
                    word = word.toLowerCase();
                    tokens.add(word);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a branch of the grammar that corresponds to a transcript. For each
     * word create a node, and link the nodes with arcs. The branch is connected
     * to the initial node and to the final node.
     * 
     * @return the first node of this branch
     */
    protected GrammarNode createGrammar() {

        initialNode = createGrammarNode(Dictionary.SILENCE_SPELLING);
        finalNode = createGrammarNode(Dictionary.SILENCE_SPELLING);
        finalNode.setFinalNode(true);
        final GrammarNode branchNode = createGrammarNode(false);

        final List<GrammarNode> wordGrammarNodes = new ArrayList<GrammarNode>();
        final int end = start + step > tokens.size() ? tokens.size() : start + step;
        for (final String word : tokens.subList(start, end)) {
            // System.out.println ("Creating grammar from " + word);
            final GrammarNode wordNode = createGrammarNode(word);
            wordGrammarNodes.add(wordNode);
        }

        // now connect all the GrammarNodes together
        initialNode.add(branchNode, LogMath.getLogOne());
        final float branchScore = logMath.linearToLog(1.0 / wordGrammarNodes.size());

        for (int i = 0; i < wordGrammarNodes.size(); i++) {
            final GrammarNode wordNode = wordGrammarNodes.get(i);

            branchNode.add(wordNode, branchScore);
            wordNode.add(finalNode, LogMath.getLogOne());

            // add connections to close words
            for (int j = i + 1; j < i + 2; j++) {
                if (0 <= j && j < wordGrammarNodes.size()) {
                    final GrammarNode neighbour = wordGrammarNodes.get(j);
                    wordNode.add(neighbour, LogMath.getLogOne());
                }
            }
        }
        return initialNode;
    }

    @Override
    public void newResult(final Result result) {
        if (result == null)
            return;

        int max = -1;
        for (final String word : result.toString().split(" ")) {
            for (int i = start; i < start + step && i < tokens.size(); i++) {
                if (tokens.get(i).equals(word)) {
                    max = i;
                    break;
                }
            }
        }
        if (max > 0) {
            start = max;
            newGrammar();
            createGrammar();
            postProcessGrammar();
        }
    }
}
