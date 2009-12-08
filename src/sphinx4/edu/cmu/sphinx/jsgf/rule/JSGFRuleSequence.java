/**
 * Copyright 1998-2009 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.jsgf.rule;

import java.util.List;

public class JSGFRuleSequence extends JSGFRule {
	protected List<JSGFRule> rules;

	public JSGFRuleSequence() {
		setRules(null);
	}

	public JSGFRuleSequence(List<JSGFRule> rules) {
		setRules(rules);
	}

	public void append(JSGFRule rule) {
		if (rules == null) {
			throw new NullPointerException("null rule to append");
		}
		rules.add(rule);
	}

	public List<JSGFRule> getRules() {
		return rules;
	}

	public void setRules(List<JSGFRule> rules) {
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

			JSGFRule r = rules.get(i);
			if ((r instanceof JSGFRuleAlternatives) || (r instanceof JSGFRuleSequence))
				localStringBuffer.append("( " + r.toString() + " )");
			else {
				localStringBuffer.append(r.toString());
			}
		}
		return localStringBuffer.toString();
	}
}
