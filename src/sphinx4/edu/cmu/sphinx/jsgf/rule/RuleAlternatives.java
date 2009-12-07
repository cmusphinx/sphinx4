/**
 * Copyright 1998-2009 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.jsgf.rule;

import java.util.List;

public class RuleAlternatives extends Rule {
	protected List<Rule> rules;
	protected List<Float> weights;

	public RuleAlternatives() {
	}
	
	public RuleAlternatives(List<Rule> rules) {
		setRules(rules);
		weights = null;
	}

	public RuleAlternatives(List<Rule> rules, List<Float> weights)
			throws IllegalArgumentException {
		assert (rules.size() == weights.size());
		setRules(rules);
		setWeights(weights);
	}

	public void append(Rule rule) {
		assert rule != null;
		rules.add(rule);
		weights.add(1.0f);
	}

	public List<Rule> getRules() {
		return rules;
	}

	public List<Float> getWeights() {
		return weights;
	}

	public void setRules(List<Rule> rules) {
		if ((weights != null) && (rules.size() != weights.size())) {
			weights = null;
		}
		this.rules = rules;
	}

	public void setWeights(List<Float> newWeights)
			throws IllegalArgumentException {
		if ((newWeights == null) || (newWeights.size() == 0)) {
			weights = null;
			return;
		}

		if (newWeights.size() != rules.size()) {
			throw new IllegalArgumentException(
					"weights/rules array length mismatch");
		}
		float f = 0.0F;

		for (Float w : newWeights) {
			if (Float.isNaN(w))
				throw new IllegalArgumentException("illegal weight value: NaN");
			if (Float.isInfinite(w))
				throw new IllegalArgumentException(
						"illegal weight value: infinite");
			if (w < 0.0D) {
				throw new IllegalArgumentException(
						"illegal weight value: negative");
			}
			f += w;
		}

		if (f <= 0.0D) {
			throw new IllegalArgumentException(
					"illegal weight values: all zero");
		}
		weights = newWeights;
	}

	public String toString() {
		if (rules == null || rules.size() == 0) {
			return "<VOID>";
		}
		StringBuffer stringBuffer = new StringBuffer();

		for (int i = 0; i < rules.size(); ++i) {
			if (i > 0)
				stringBuffer.append(" | ");

			if (weights != null)
				stringBuffer.append("/" + weights.get(i) + "/ ");

			Rule r = rules.get(i);
			if (rules.get(i) instanceof RuleAlternatives)
				stringBuffer.append("( " + r.toString() + " )");
			else {
				stringBuffer.append(r.toString());
			}
		}
		return stringBuffer.toString();
	}
}
