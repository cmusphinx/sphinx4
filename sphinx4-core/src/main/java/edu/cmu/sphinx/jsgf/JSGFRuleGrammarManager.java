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

public class JSGFRuleGrammarManager {
	protected Map<String, JSGFRuleGrammar> grammars;

	public JSGFRuleGrammarManager () {
		grammars = new HashMap<String, JSGFRuleGrammar>();
	}
	
	public Collection<JSGFRuleGrammar> grammars() {
		return grammars.values();
	}

	public void remove(JSGFRuleGrammar grammar) {
		String name = grammar.getName();
		grammars.remove(name);
	}

	public void remove(String name) {
		grammars.remove(name);
	}
	
	/** Add a grammar to the grammar list. 
	 * @param grammar to store
	 **/
	protected void storeGrammar(JSGFRuleGrammar grammar) {
		grammars.put(grammar.getName(), grammar);
	}

	/** Retrieve a grammar from the grammar list.
	 * @param name grammar name to load
	 * @return grammar object
	 */
	public JSGFRuleGrammar retrieveGrammar(String name) {
		// System.out.println ("Looking for grammar " + name);
		// for (String key : grammars.keySet()) {
		//  	System.out.println ("    " + key);
		// }
		return grammars.get(name);
	}

	public void linkGrammars() throws JSGFGrammarException {
            for (JSGFRuleGrammar grammar : grammars.values()) {
                    grammar.resolveAllRules();
            }
	}
}
