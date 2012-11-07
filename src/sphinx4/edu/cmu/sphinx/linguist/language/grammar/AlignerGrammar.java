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

import java.util.List;
import java.util.ListIterator;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import edu.cmu.sphinx.linguist.dictionary.Dictionary;

import edu.cmu.sphinx.util.ExtendedStreamTokenizer;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;

public class AlignerGrammar extends Grammar {
	@S4Component(type = LogMath.class)
	public final static String PROP_LOG_MATH = "logMath";
	private LogMath logMath;

	private boolean modelRepetitions = false;

	private boolean modelDeletions = false;
	private boolean modelBackwardJumps = false;

	private double selfLoopProbability = 0.0;
	private double backwardTransitionProbability = 0.0;
	private double forwardJumpProbability = 0.0;
	private int numAllowedWordJumps;

	protected GrammarNode finalNode;
	private final List<String> tokens = new ArrayList<String>();

	public AlignerGrammar(final String text, final LogMath logMath,
			final boolean showGrammar, final boolean optimizeGrammar,
			final boolean addSilenceWords, final boolean addFillerWords,
			final Dictionary dictionary) {
		super(showGrammar, optimizeGrammar, addSilenceWords, addFillerWords,
				dictionary);
		this.logMath = logMath;
		setText(text);
	}

	public AlignerGrammar() {

	}

	/*
	 * Reads Text and converts it into a list of tokens
	 */
	public void setText(String text) {
		String word;
		try {
			final ExtendedStreamTokenizer tok = new ExtendedStreamTokenizer(
					new StringReader(text), true);

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

	public void allowDeletions(boolean modelDeletions) {
		this.modelDeletions = modelDeletions;
	}

	public void allowRepetions(boolean modelRepetitions) {
		this.modelRepetitions = modelRepetitions;
	}

	public void allowBackwardJumps(boolean modelBackwardJumps) {
		this.modelBackwardJumps = modelBackwardJumps;
	}

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		super.newProperties(ps);
		logMath = (LogMath) ps.getComponent(PROP_LOG_MATH);
	}

	public void setSelfLoopProbability(double prob) {
		selfLoopProbability = prob;
	}

	public void setBackWardTransitionProbability(double prob) {
		backwardTransitionProbability = prob;
	}

	public void setForwardJumpProbability(double prob) {

		forwardJumpProbability = prob;
	}

	public void setNumAllowedGrammarJumps(int n) {
		if (n >= 0) {
			numAllowedWordJumps = n;
		}
	}

	@Override
	protected GrammarNode createGrammar() throws IOException {

		logger.info("Creating Grammar");
		initialNode = createGrammarNode(Dictionary.SILENCE_SPELLING);
		finalNode = createGrammarNode(Dictionary.SILENCE_SPELLING);
		finalNode.setFinalNode(true);
		final GrammarNode branchNode = createGrammarNode(false);

		final List<GrammarNode> wordGrammarNodes = new ArrayList<GrammarNode>();
		final int end = tokens.size();

		logger.info("Creating Grammar nodes");
		for (final String word : tokens.subList(0, end)) {
			final GrammarNode wordNode = createGrammarNode(word.toLowerCase());
			wordGrammarNodes.add(wordNode);
		}
		logger.info("Done creating grammar node");

		// now connect all the GrammarNodes together
		initialNode.add(branchNode, LogMath.getLogOne());

		createBaseGrammar(wordGrammarNodes, branchNode, finalNode);

		if (modelDeletions) {
			addForwardJumps(wordGrammarNodes, branchNode, finalNode);
		}
		if (modelBackwardJumps) {
			addBackwardJumps(wordGrammarNodes, branchNode, finalNode);
		}
		if (modelRepetitions) {
			addSelfLoops(wordGrammarNodes);
		}
		logger.info("Done making Grammar");
		// initialNode.dumpDot("./graph.dot");
		return initialNode;
	}

	private void addSelfLoops(List<GrammarNode> wordGrammarNodes) {
		ListIterator<GrammarNode> iter = wordGrammarNodes.listIterator();
		while (iter.hasNext()) {
			GrammarNode currNode = iter.next();
			currNode.add(currNode, logMath.linearToLog(selfLoopProbability));
		}

	}

	private void addBackwardJumps(List<GrammarNode> wordGrammarNodes,
			GrammarNode branchNode, GrammarNode finalNode2) {

		GrammarNode currNode;
		for (int i = 0; i < wordGrammarNodes.size(); i++) {
			currNode = wordGrammarNodes.get(i);
			for (int j = Math.max(i - numAllowedWordJumps - 1, 0); j < i - 1; j++) {
				GrammarNode jumpToNode = wordGrammarNodes.get(j);
				currNode.add(jumpToNode,
						logMath.linearToLog(backwardTransitionProbability));
			}
		}
	}

	private void addForwardJumps(List<GrammarNode> wordGrammarNodes,
			GrammarNode branchNode, GrammarNode finalNode) {
		GrammarNode currNode = branchNode;
		for (int i = -1; i < wordGrammarNodes.size(); i++) {
			if (i > -1) {
				currNode = wordGrammarNodes.get(i);
			}
			for (int j = i + 2; j < Math.min(wordGrammarNodes.size(), i
					+ numAllowedWordJumps + 1); j++) {
				GrammarNode jumpNode = wordGrammarNodes.get(j);
				currNode.add(jumpNode,
						logMath.linearToLog(forwardJumpProbability));
			}
		}
		for (int i = wordGrammarNodes.size() - numAllowedWordJumps - 1; i < wordGrammarNodes
				.size() - 1; i++) {
			int j = wordGrammarNodes.size();
			currNode = wordGrammarNodes.get(i);
			currNode.add(
					finalNode,
					logMath.linearToLog((float) forwardJumpProbability
							* Math.pow(Math.E, j - i)));
		}

	}

	private void createBaseGrammar(List<GrammarNode> wordGrammarNodes,
			GrammarNode branchNode, GrammarNode finalNode) {
		GrammarNode currNode = branchNode;
		ListIterator<GrammarNode> iter = wordGrammarNodes.listIterator();
		while (iter.hasNext()) {
			GrammarNode nextNode = iter.next();
			currNode.add(nextNode, LogMath.getLogOne());
			currNode = nextNode;
		}
		currNode.add(finalNode, LogMath.getLogOne());
	}

}
