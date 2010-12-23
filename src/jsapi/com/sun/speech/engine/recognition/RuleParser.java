/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package com.sun.speech.engine.recognition;

import javax.speech.recognition.*;
import java.util.*;

/**
 * Implementation of the parse method(s) on
 * javax.speech.recognition.RuleGrammar.
 */
public class RuleParser {

	private Recognizer recognizer;

	/*
	 * parse a text string against a particular rule from a particular grammar
	 * returning a RuleParse data structure is successful and null otherwise
	 */
	public static RuleParse parse(String text, Recognizer recognizer,
			RuleGrammar grammar, String ruleName) {
		String inputTokens[] = tokenize(text);
		return parse(inputTokens, recognizer, grammar, ruleName);
	}

	public static RuleParse parse(String inputTokens[], Recognizer recognognizer,
			RuleGrammar grammar, String ruleName) {
		List<RuleParse> list = mparse(inputTokens, recognognizer, grammar,
				ruleName);
		if (list != null)
			for (RuleParse rp : list)
				if (grammar.isRulePublic(rp.getRuleName().getRuleName()))
					return rp;
		return null;
	}

	public static List<RuleParse> mparse(String text, Recognizer R,
			RuleGrammar G, String ruleName) {
		String inputTokens[] = tokenize(text);
		return mparse(inputTokens, R, G, ruleName);
	}

	public static List<RuleParse> mparse(String inputTokens[],
			Recognizer recognizer, RuleGrammar grammar, String ruleName) {
		RuleParser rp = new RuleParser();
		rp.recognizer = recognizer;
		List<RuleParse> res = new ArrayList<RuleParse>();
		String[] rNames = ruleName == null ? grammar.listRuleNames()
				: new String[] { ruleName };
		for (String rName : rNames) {
			if (ruleName == null && !grammar.isEnabled(rName)) {
				continue;
			}
			Rule startRule = grammar.getRule(rName);
			if (startRule == null) {
				System.out.println("BAD RULENAME " + rName);
				continue;
			}
			List<TokenPos> p = rp.parse(grammar, startRule, inputTokens, 0);
			if (p != null && !p.isEmpty()) {
				for (TokenPos tp : p) {
					if (tp.getPos() == inputTokens.length) {
						res.add(new RuleParse(new RuleName(rName), (Rule) tp));
					}
				}
			}
		}
		return res.isEmpty() ? null : res;
	}

	/*
	 * Parse routine called recursively while traversing the Rule structure in a
	 * depth first manner. Returns a list of valid parses.
	 */
	private List<TokenPos> parse(RuleGrammar grammar, Rule r, String[] input,
			int pos) {

		// System.out.println("PARSE " + r.getClass().getName() + ' ' + pos +
		// ' ' + r);

		if (r instanceof RuleName)
			return parse(grammar, (RuleName) r, input, pos);
		if (r instanceof RuleToken)
			return parse(grammar, (RuleToken) r, input, pos);
		if (r instanceof RuleCount)
			return parse(grammar, (RuleCount) r, input, pos);
		if (r instanceof RuleTag)
			return parse(grammar, (RuleTag) r, input, pos);
		if (r instanceof RuleSequence)
			return parse(grammar, (RuleSequence) r, input, pos);
		if (r instanceof RuleAlternatives)
			return parse(grammar, (RuleAlternatives) r, input, pos);

		System.out.println("ERROR UNKNOWN OBJECT " + r);
		return null;
	}

	/*
	 * RULE REFERENCES
	 */
	private List<TokenPos> parse(RuleGrammar grammar, RuleName rn,
			String[] input, int pos) {
		if (rn.getFullGrammarName() == null) {
			rn.setRuleName(grammar.getName() + '.' + rn.getSimpleRuleName());
		}
		String simpleName = rn.getSimpleRuleName();
		if (simpleName.equals("VOID"))
			return null;
		if (simpleName.equals("NULL")) {
			List<TokenPos> res = new ArrayList<TokenPos>();
			res.add(new ParsedRuleParse(rn, RuleName.NULL, pos));
			return res;
		}
		Rule ruleref = grammar.getRule(simpleName);
		if (rn.getFullGrammarName() != grammar.getName()) {
			ruleref = null;
		}
		if (ruleref == null) {
			String gname = rn.getFullGrammarName();
			// System.out.println("gname=" + gname);
			if (gname != null && !gname.isEmpty()) {
				RuleGrammar RG1 = recognizer.getRuleGrammar(gname);
				if (RG1 != null) {
					// System.out.println("simpleName=" + simpleName);
					ruleref = RG1.getRule(simpleName);
					// System.out.println("ruleRef=" + ruleref);
					grammar = RG1;
				} else {
					System.out.println("ERROR: UNKNOWN GRAMMAR " + gname);
					// Thread.dumpStack();
				}
			}
			if (ruleref == null) {
				System.out.println("ERROR: UNKNOWN RULE NAME "
						+ rn.getRuleName() + ' ' + rn);
				// Thread.dumpStack();
				return null;
			}
		}
		List<TokenPos> p = parse(grammar, ruleref, input, pos);
		if (p == null) {
			return null;
		}
		List<TokenPos> res = new ArrayList<TokenPos>();
		for (TokenPos tp : p) {
			if (tp instanceof ParsedEmptyToken) {
				res.add(tp);
				continue;
			}
			try {
				res.add(new ParsedRuleParse(rn, (Rule) tp, tp.getPos()));
			} catch (IllegalArgumentException e) {
				System.out.println("ERROR " + e);
			}
		}
		return res;
	}

	/*
	 * LITERAL TOKENS
	 */
	private List<TokenPos> parse(RuleGrammar grammar, RuleToken rt,
			String[] input, int pos) {
		if (pos >= input.length) {
			return null;
		}
		// System.out.println(rt.getText() + " ?= " + input[pos]);
		// TODO: what about case sensitivity ??????
		String tText = rt.getText().toLowerCase();
		if (tText.equals(input[pos]) || (input[pos].equals("%"))
				|| (input[pos].equals("*"))) {
			List<TokenPos> res = new ArrayList<TokenPos>();
			res.add(new ParsedRuleToken(rt.getText(), pos + 1));
			if (input[pos].equals("*")) {
				res.add(new ParsedRuleToken(rt.getText(), pos));
			}
			return res;
		} else {
			if (tText.indexOf(' ') < 0) {
				return null;
			}
			if (!tText.startsWith(input[pos])) {
				return null;
			}
			String ta[] = tokenize(tText);
			int j = 0;
			while (true) {
				if (j >= ta.length) {
					break;
				}
				if (pos >= input.length) {
					return null;
				}
				if (!ta[j].equals(input[pos])) {
					return null;
				}
				pos++;
				j++;
			}
			List<TokenPos> res = new ArrayList<TokenPos>();
			res.add(new ParsedRuleToken(rt.getText(), pos));
			return res;
		}
	}

	/*
	 * ALTERNATIVES
	 */
	private List<TokenPos> parse(RuleGrammar grammar, RuleAlternatives ra,
			String[] input, int pos) {
		List<TokenPos> res = new ArrayList<TokenPos>();
		for (Rule rule : ra.getRules()) {
			List<TokenPos> p = parse(grammar, rule, input, pos);
			if (p != null)
				res.addAll(p);
		}
		return res;
	}

	/*
	 * RULESEQUENCE
	 */
	private List<TokenPos> parse(RuleGrammar grammar, RuleSequence rs,
			String[] input, int pos) {
		Rule[] rarry = rs.getRules();
		if (rarry == null || rarry.length == 0) {
			return null;
		}
		List<TokenPos> p = parse(grammar, rarry[0], input, pos);
		if (p == null) {
			return null;
		}
		List<TokenPos> res = new ArrayList<TokenPos>();
		// System.out.println("seq sz" + p.size());
		for (TokenPos tp : p) {
			// System.out.println("seq  " + p.get(j));
			int nPos = tp.getPos();
			if (rarry.length == 1) {
				if (tp instanceof ParsedEmptyToken) {
					res.add(tp);
					continue;
				}
				try {
					res.add(new ParsedRuleSequence(new Rule[] { (Rule) tp }, tp
							.getPos()));
				} catch (IllegalArgumentException e) {
					System.out.println(e);
				}
				continue;
			}
			Rule[] nra = Arrays.copyOfRange(rarry, 1, rarry.length);
			RuleSequence nrs = new RuleSequence(nra);
			// System.out.println("2parse " + nPos + nrs);
			List<TokenPos> q = parse(grammar, nrs, input, nPos);
			if (q == null) {
				continue;
			}
			// System.out.println("2 seq sz " + p.size());
			for (TokenPos tp1 : q) {
				// System.out.println("2 seq  " + q.get(k));
				// System.out.println("tp " + tp);
				// System.out.println("tp1 " + tp1);
				if (tp1 instanceof ParsedEmptyToken) {
					res.add(tp);
					continue;
				}
				if (tp instanceof ParsedEmptyToken) {
					res.add(tp1);
					continue;
				}
				Rule[] ra;
				if (tp1 instanceof RuleSequence) {
					RuleSequence r2 = (RuleSequence) tp1;
					Rule[] r2r = r2.getRules();
					ra = new Rule[r2r.length + 1];
					ra[0] = (Rule) tp;
					System.arraycopy(r2r, 0, ra, 1, r2r.length);
				} else {
					ra = new Rule[] { (Rule) tp, (Rule) tp1 };
				}
				try {
					res.add(new ParsedRuleSequence(ra, tp1.getPos()));
				} catch (IllegalArgumentException e) {
					System.out.println(e);
				}
			}
		}
		return res;
	}

	/*
	 * TAGS
	 */
	private List<TokenPos> parse(RuleGrammar grammar, RuleTag rtag,
			String[] input, int pos) {
		String theTag = rtag.getTag();
		// System.out.println("tag="+theTag);
		List<TokenPos> p = parse(grammar, rtag.getRule(), input, pos);
		if (p == null) {
			return null;
		}
		List<TokenPos> res = new ArrayList<TokenPos>();
		for (TokenPos tp : p) {
			if (tp instanceof ParsedEmptyToken) {
				res.add(tp);
				continue;
			}
			res.add(new ParsedRuleTag((Rule) tp, theTag, tp.getPos()));
		}
		return res;
	}

	/*
	 * RULECOUNT (e.g. [], *, or + )
	 */
	private List<TokenPos> parse(RuleGrammar grammar, RuleCount rc,
			String[] input, int pos) {
		int rcount = rc.getCount();
		ParsedEmptyToken empty = new ParsedEmptyToken(pos);
		List<TokenPos> p = parse(grammar, rc.getRule(), input, pos);
		if (p == null) {
			if (rcount == RuleCount.ONCE_OR_MORE)
				return null;
			List<TokenPos> res = new ArrayList<TokenPos>();
			res.add(empty);
			return res;
		}
		if (rcount != RuleCount.ONCE_OR_MORE) {
			p.add(empty);
		}
		if (rcount == RuleCount.OPTIONAL) {
			return p;
		}
		for (int m = 2; m <= input.length - pos; m++) {
			Rule[] ar = new Rule[m];
			Arrays.fill(ar, rc.getRule());
			List<TokenPos> q = parse(grammar, new RuleSequence(ar), input, pos);
			if (q == null) {
				return p;
			}
			p.addAll(q);
		}
		return p;
	}

	/*
	 * tokenize a string
	 */
	static String[] tokenize(String text) {
		StringTokenizer st = new StringTokenizer(text);
		int size = st.countTokens();
		String res[] = new String[size];
		int i = 0;
		while (st.hasMoreTokens()) {
			res[i++] = st.nextToken().toLowerCase();
		}
		return res;
	}

	/*
	 * interface for keeping track of where a token occurs in the tokenized
	 * input string
	 */
	interface TokenPos {
		public int getPos();
	}

	/* extension of RuleToken with tokenPos interface */
	@SuppressWarnings("serial")
	class ParsedRuleToken extends RuleToken implements TokenPos {

		int pos;

		public ParsedRuleToken(String x, int pos) {
			super(x);
			this.pos = pos;
		}

		public int getPos() {
			return pos;
		}

	}

	@SuppressWarnings("serial")
	class ParsedEmptyToken extends ParsedRuleToken {

		public ParsedEmptyToken(int pos) {
			super("EMPTY", pos);
		}
	}

	/* extension of RuleTag with tokenPos interface */
	@SuppressWarnings("serial")
	class ParsedRuleTag extends RuleTag implements TokenPos {

		int pos;

		public ParsedRuleTag(Rule r, String x, int pos) {
			super(r, x);
			this.pos = pos;
		}

		public int getPos() {
			return pos;
		}

	}

	/* extension of RuleSequence with tokenPos interface */
	@SuppressWarnings("serial")
	class ParsedRuleSequence extends RuleSequence implements TokenPos {

		int pos;

		public ParsedRuleSequence(Rule rules[], int pos) {
			super(rules);
			this.pos = pos;
		}

		public int getPos() {
			return pos;
		}

	}

	/* extension of RuleParse with tokenPos interface */
	@SuppressWarnings("serial")
	class ParsedRuleParse extends RuleParse implements TokenPos {

		int pos;

		public ParsedRuleParse(RuleName rn, Rule r, int pos)
				throws IllegalArgumentException {
			super(rn, r);
			this.pos = pos;
		}
		
		public int getPos() {
			return pos;
		}
	}
}
