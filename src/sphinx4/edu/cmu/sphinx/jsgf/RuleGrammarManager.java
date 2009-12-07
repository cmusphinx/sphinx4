/**
 * Copyright 1998-2009 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.jsgf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RuleGrammarManager {
	protected Map<String, RuleGrammar> grammars;
	protected boolean caseSensitiveNames = true;

	RuleGrammarManager (boolean caseSensitiveNames) {
		this.caseSensitiveNames = caseSensitiveNames; 
		grammars = new HashMap<String, RuleGrammar>();
	}
	
	public RuleGrammarManager () {
		grammars = new HashMap<String, RuleGrammar>();
	}
	
	Collection<RuleGrammar> grammars() {
		return grammars.values();
	}

	public void remove(RuleGrammar grammar) {
		String name = grammar.getName();
		grammars.remove(name);
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

	public void linkGrammars() throws GrammarException {
        for (RuleGrammar grammar : grammars.values()) {
                grammar.resolveAllRules();
        }
	}
}
