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
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.knowledge.dictionary.Dictionary;
import edu.cmu.sphinx.knowledge.dictionary.FastDictionary;
import edu.cmu.sphinx.knowledge.dictionary.FullDictionary;
import edu.cmu.sphinx.knowledge.dictionary.Pronunciation;

import java.io.IOException;
import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;


/**
 * Classes that implement this interface create grammars
 *
 * Note that all grammar probabilities are maintained in LogMath log
 * domain.
 */
public abstract class  Grammar {
    private GrammarNode initialNode;
    private Set grammarNodes;
    private Dictionary dictionary;
    private LanguageModel languageModel;
    private LogMath logMath;
    protected SphinxProperties props;
    private boolean dumpGrammar = false; 	// should be a property
    private final static GrammarWord[][] EMPTY_ALTERNATIVE =
	new GrammarWord[0][0];


    /**
     * Initializes a grammar using the given dictionary
     *
     * @param context the name of the sphinx context
     *
     * @param languageModel the language model
     * @param dictionary the dictionary used to get word
     * pronunciations
     *
     * @throws java.io.IOException if the grammar could not be loaded
     */

    public void initialize(String context,
                           LanguageModel languageModel, Dictionary dictionary)
			throws IOException, NoSuchMethodException {
	if (dictionary == null) {
            throw new Error("Dictionary is null!");
	}

        this.languageModel = languageModel;
	this.props = SphinxProperties.getSphinxProperties(context);
	this.dictionary = dictionary;
        this.logMath = LogMath.getLogMath(context);

	grammarNodes = new HashSet();
	initialNode = createGrammar();
	if (dumpGrammar) {
	    dump();
	}
	this.dictionary = null;
    }

    /**
     * Initializes a grammar using the default dictionary and no
     * language model.
     *
     * @param context the name of the sphinx context
     *
     * @throws java.io.IOException if the grammar could not be loaded
     */
    public void initialize(String context)
	throws IOException, NoSuchMethodException {
	initialize(context, (LanguageModel) null, (Dictionary) null);
    }

    /**
     * Initializes a grammar using the given reference sentence
     *
     * @param context the name of the sphinx context
     *
     * @param dictionary the dictionary used to get word
     * pronunciations
     *
     * @param referenceText the reference sentence used to define the
     * grammar
     *
     * @throws java.io.IOException if the grammar could not be loaded
     */

    public void initialize(String context, Dictionary dictionary,
			   String referenceText)
			throws IOException, NoSuchMethodException {
	if (dictionary == null) {
            throw new Error("Dictionary is null!");
	}
	this.props = SphinxProperties.getSphinxProperties(context);
	this.dictionary = dictionary;
	grammarNodes = new HashSet();
	initialNode = createGrammar(referenceText);
	if (dumpGrammar) {
	    dump();
	}
	this.dictionary = null;
    }

    /**
     * Retrievs log base used to represent probabilities in this grammar
     *
     * @return the log math representing the log base for the gramamr
     */
    public LogMath getLogMath() {
        return logMath;
    }

    /**
     * Returns the initial node for the grammar
     *
     * @return the initial grammar node
     */
    public GrammarNode getInitialNode() {
	return initialNode;
    }

    /**
     * Dumps statistics for this grammar
     *
     */
    public void dumpStatistics() {
        int successorCount = 0;
        System.out.println("Num nodes : " +  getNumNodes());
        for (Iterator i = grammarNodes.iterator(); i.hasNext(); ) {
            GrammarNode node = (GrammarNode) i.next();
            successorCount += node.getSuccessors().length;
        }
        System.out.println("Num arcs  : " + successorCount);
        System.out.println("Avg arcs  : " +
                ((float) successorCount / getNumNodes()));
    }

    /**
     * Returns the dictionary used by this grammar
     *
     * @return the dictionary
     *
     */
    protected Dictionary getDictionary() {
	return dictionary;
    }


    /**
     * Returns the language model for the grammar
     *
     * @return the language model (null if there is no language model)
     */
    protected LanguageModel getLanguageModel() {
        return languageModel;
    }

    /**
     * Dumps the grammar
     */
    public void dump() {
	initialNode.dump();
    }

    /**
     * returns the number of nodes in this grammar
     *
     * @return the number of nodes
     */
    public int getNumNodes() {
	return grammarNodes.size();
    }


    /**
     * returns the set of of nodes in this grammar
     *
     * @return the set of nodes
     */
    public Set getGrammarNodes() {
	return grammarNodes;
    }




    /**
     * Creates a grammar. Subclasses of
     * grammar should implement this method.
     *
     * @return the initial node for the grammar
     *
     * @throws java.io.IOException if the grammar could not be loaded
     * @throws java.lang.NoSuchMethodException if called with inappropriate subclass.
     */
    protected abstract GrammarNode createGrammar()
	throws IOException, NoSuchMethodException;

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
     * Returns a new GrammarNode with the given set of alternatives.
     *
     * @param identity the id for this node
     * @param alts the set of alternative word lists for this GrammarNode
     */
    protected GrammarNode createGrammarNode(int identity, String[][] alts) {
	GrammarNode node;
        GrammarWord[][] alternatives = new GrammarWord[alts.length][];

	for (int i = 0; i < alternatives.length; i++) {
	    alternatives[i] = new GrammarWord[alts[i].length];
	    for (int j = 0; j < alts[i].length; j++) {

		Pronunciation[] pronunciation =
		    getDictionary().getPronunciations(alts[i][j], null);

		if (pronunciation == null) {
		    alternatives = EMPTY_ALTERNATIVE;
		    break;
		} else {
		    alternatives[i][j] = new GrammarWord (alts[i][j],
			 getDictionary().getPronunciations(alts[i][j], null));
		}
	    }
	}
            
        node = new GrammarNode(identity, alternatives);
	add(node);
	return node;
    }

    /**
     * Returns a new GrammarNode with the given single word. If the
     * word is not in the dictionary, an empty node is created
     *
     * @param identity the id for this node
     * @param word the word for this grammar node
     */
    protected GrammarNode createGrammarNode(int identity, String word) {
	GrammarNode node = null;
        GrammarWord[][] alternatives = EMPTY_ALTERNATIVE;
	Pronunciation[] pronunciation =
	    getDictionary().getPronunciations(word, null);

	if (pronunciation != null) {
	    alternatives = new GrammarWord[1][];
	    alternatives[0] = new GrammarWord[1];
	    alternatives[0][0] = new GrammarWord (word, pronunciation);
            node = new GrammarNode(identity, alternatives);
            add(node);
	} else {
            node = createGrammarNode(identity, false);
            System.out.println("Can't find pronunciation for " + word);
        }
	return node;
    }

    /**
     * Creates a grammar node in this grammar with the given identity
     *
     * @param identity the identity of the node
     * @param isFinal if true, this is a final node
     *
     * @return the grammar node
     */
    protected GrammarNode createGrammarNode(int identity, boolean isFinal) {
	GrammarNode node;
        node = new GrammarNode(identity, isFinal);
	add(node);
	return node;
    }


    /**
     * Adds the given grammar node to the set of nodes for this
     * grammar
     *
     * @param node the grammar node
     */
    private void add(GrammarNode node) {
	grammarNodes.add(node);
    }
}
