/**
 * Copyright 1998-2009 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.jsgf.rule;

public class RuleTag extends Rule {
	protected Rule rule;
	protected String tag;

	public RuleTag() {
		setRule(null);
		setTag(null);
	}

	public RuleTag(Rule rule, String tag) {
		setRule(rule);
		setTag(tag);
	}

	private String escapeTag(String tag) {
		StringBuffer stringBuilder = new StringBuffer(tag);

		if ((tag.indexOf('}') >= 0) || (tag.indexOf('\\') >= 0)
				|| (tag.indexOf('{') >= 0)) {
			for (int i = stringBuilder.length() - 1; i >= 0; --i) {
				int j = stringBuilder.charAt(i);
				if ((j == '\\') || (j == '}') || (j == '{')) {
					stringBuilder.insert(i, '\\');
				}
			}
		}
		return stringBuilder.toString();
	}

	public Rule getRule() {
		return rule;
	}

	public String getTag() {
		return tag;
	}

	public void setRule(Rule rule) {
		this.rule = rule;
	}

	public void setTag(String tag) {
		if (tag == null)
			this.tag = "";
		else
			this.tag = tag;
	}

	public String toString() {
		String str = " {" + escapeTag(tag) + "}";

		if ((rule instanceof RuleToken) || (rule instanceof RuleName)) {
			return rule.toString() + str;
		}
		return "(" + rule.toString() + ")" + str;
	}
}
