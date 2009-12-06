package com.sun.speech.engine.recognition;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.speech.recognition.Grammar;
import javax.speech.recognition.RuleGrammar;

public class BaseRecognizerGrammarManager {
	protected Map<String, RuleGrammar> grammars;
	protected boolean caseSensitiveNames = true;

	BaseRecognizerGrammarManager (boolean caseSensitiveNames) {
		this.caseSensitiveNames = caseSensitiveNames; 
		grammars = new HashMap<String, RuleGrammar>();
	}
	
	Collection<RuleGrammar> grammars() {
		return grammars.values();
	}

	public void remove(RuleGrammar grammar) {
		String name = grammar.getName();
		grammars.remove(name);
	}

	/**
	 * Determine if the Recognizer has any modal grammars. 
	 */
	protected boolean checkForModalGrammars() {
		for (RuleGrammar rg : grammars.values()) {
			if (rg.getActivationMode() == Grammar.RECOGNIZER_MODAL) {
				return true;
			}
		}
		return false;
	}


	/** Add a grammar to the grammar list. */
	protected void storeGrammar(RuleGrammar grammar) {
		if (caseSensitiveNames) {
			grammars.put(grammar.getName(), grammar);
		} else {
			grammars.put(grammar.getName().toLowerCase(), grammar);
		}
	}

	/** Retrieve a grammar from the grammar list. */
	protected RuleGrammar retrieveGrammar(String name) {
		return grammars.get(caseSensitiveNames ? name : name
				.toLowerCase());
	}
}
