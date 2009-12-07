/**
 * Copyright 1998-2009 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.jsgf.rule;

import java.util.List;

public class RuleSequence extends Rule {
	protected List<Rule> rules;

	public RuleSequence() {
		setRules(null);
	}

	public RuleSequence(List<Rule> rules) {
		setRules(rules);
	}

	public void append(Rule rule) {
		if (rules == null) {
			throw new NullPointerException("null rule to append");
		}
		rules.add(rule);
	}

	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	public String toString() {
		if (rules.size() == 0) {
			return "<NULL>";
		}
		StringBuffer localStringBuffer = new StringBuffer();

		for (int i = 0; i < rules.size(); ++i) {
			if (i > 0)
				localStringBuffer.append(' ');

			Rule r = rules.get(i);
			if ((r instanceof RuleAlternatives) || (r instanceof RuleSequence))
				localStringBuffer.append("( " + r.toString() + " )");
			else {
				localStringBuffer.append(r.toString());
			}
		}
		return localStringBuffer.toString();
	}
}
