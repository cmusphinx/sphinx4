/**
 * Copyright 1998-2009 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.jsgf.rule;

public class RuleCount extends Rule {
	protected Rule rule;
	protected int count;

	public static int OPTIONAL = 2;
	public static int ONCE_OR_MORE = 3;
	public static int ZERO_OR_MORE = 4;

	public RuleCount() {
		setRule(null);
		setCount(OPTIONAL);
	}

	public RuleCount(Rule rule, int count) {
		setRule(rule);
		setCount(count);
	}

	public int getCount() {
		return count;
	}

	public Rule getRule() {
		return rule;
	}

	public void setCount(int count) {
		if ((count != OPTIONAL) && (count != ZERO_OR_MORE)
				&& (count != ONCE_OR_MORE)) {
			return;
		}
		this.count = count;
	}

	public void setRule(Rule rule) {
		this.rule = rule;
	}

	public String toString() {
		if (count == OPTIONAL) {
			return '[' + rule.toString() + ']';
		}
		String str = null;

		if ((rule instanceof RuleToken) || (rule instanceof RuleName))
			str = rule.toString();
		else {
			str = '(' + rule.toString() + ')';
		}

		if (count == ZERO_OR_MORE)
			return str + " *";
		if (count == ONCE_OR_MORE) {
			return str + " +";
		}
		return str + "???";
	}
}
