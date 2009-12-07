/**
 * Copyright 1998-2009 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.jsgf;

public class ManagedRuleGrammarFactory implements RuleGrammarFactory {
	
	RuleGrammarManager manager;
	public ManagedRuleGrammarFactory (RuleGrammarManager manager) {
		this.manager = manager;
	}
	
	@Override
	public RuleGrammar newGrammar(String name) {
		assert manager != null;
		
		RuleGrammar grammar = new RuleGrammar(name, manager);
		manager.storeGrammar(grammar);
		return grammar;
	}
}
