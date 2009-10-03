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
 * Implementation of the parse method(s) on javax.speech.recognition.RuleGrammar.
 *
 * @version 1.5 10/27/99 16:33:49
 */
public class RuleParser {

    private Recognizer theRec;

    /*
    * parse a text string against a particular rule from a particluar grammar
    * returning a RuleParse data structure is successful and null otherwise
    */
    public static RuleParse parse(String text, Recognizer R, RuleGrammar G, String ruleName) {
        String inputTokens[] = tokenize(text);
        return parse(inputTokens, R, G, ruleName);
    }

    public static RuleParse parse(String inputTokens[], Recognizer R, RuleGrammar G, String ruleName) {
        List<RuleParse> list = mparse(inputTokens, R, G, ruleName);
        if (list != null)
            for (RuleParse rp : list)
                if (G.isRulePublic(rp.getRuleName().getRuleName()))
                    return rp;
        return null;
    }

    public static List<RuleParse> mparse(String text, Recognizer R, RuleGrammar G, String ruleName) {
        String inputTokens[] = tokenize(text);
        return mparse(inputTokens, R, G, ruleName);
    }

    public static List<RuleParse> mparse(String inputTokens[], Recognizer R, RuleGrammar G, String ruleName) {
        RuleParser rp = new RuleParser();
        rp.theRec = R;
        List<RuleParse> res = new ArrayList<RuleParse>();
        String[] rNames = ruleName == null ? G.listRuleNames() : new String[] { ruleName };
        for (String rName : rNames) {
            if (ruleName == null && !G.isEnabled(rName)) {
                continue;
            }
            Rule startRule = G.getRule(rName);
            if (startRule == null) {
                System.out.println("BAD RULENAME " + rName);
                continue;
            }
            List<tokenPos> p = rp.parse(G, startRule, inputTokens, 0);
            if (p != null && !p.isEmpty()) {
                for (tokenPos tp : p) {
                    if (tp.getPos() == inputTokens.length) {
                        res.add(new RuleParse(new RuleName(rName), (Rule)tp));
                    }
                }
            }
        }
        return res.isEmpty() ? null : res;
    }

    /*
    * parse routine called recursively while traversing the Rule structure
    * in a depth first manner. Returns a list of valid parses.
    */
    private List<tokenPos> parse(RuleGrammar grammar, Rule r, String[] input, int pos) {

        //System.out.println("PARSE " + r.getClass().getName() + ' ' + pos + ' ' + r);

        if (r instanceof RuleName)
            return parse(grammar, (RuleName)r, input, pos);
        if (r instanceof RuleToken)
            return parse(grammar, (RuleToken)r, input, pos);
        if (r instanceof RuleCount)
            return parse(grammar, (RuleCount)r, input, pos);
        if (r instanceof RuleTag)
            return parse(grammar, (RuleTag)r, input, pos);
        if (r instanceof RuleSequence)
            return parse(grammar, (RuleSequence)r, input, pos);
        if (r instanceof RuleAlternatives)
            return parse(grammar, (RuleAlternatives)r, input, pos);

        System.out.println("ERROR UNKNOWN OBJECT " + r);
        return null;
    }

    /*
     * RULE REFERENCES
     */
    private List<tokenPos> parse(RuleGrammar grammar, RuleName rn, String[] input, int pos) {
        if (rn.getFullGrammarName() == null) {
            rn.setRuleName(grammar.getName() + '.' + rn.getSimpleRuleName());
        }
        String simpleName = rn.getSimpleRuleName();
        if (simpleName.equals("VOID")) return null;
        if (simpleName.equals("NULL")) {
            List<tokenPos> res = new ArrayList<tokenPos>();
            res.add(new jsgfRuleParse(rn, RuleName.NULL, pos));
            return res;
        }
        Rule ruleref = grammar.getRule(simpleName);
        if (rn.getFullGrammarName() != grammar.getName()) {
            ruleref = null;
        }
        if (ruleref == null) {
            String gname = rn.getFullGrammarName();
            //System.out.println("gname=" + gname);
            if (gname != null && !gname.isEmpty()) {
                RuleGrammar RG1 = theRec.getRuleGrammar(gname);
                if (RG1 != null) {
                    //System.out.println("simpleName=" + simpleName);
                    ruleref = RG1.getRule(simpleName);
                    //System.out.println("ruleRef=" + ruleref);
                    grammar = RG1;
                } else {
                    System.out.println("ERROR: UNKNOWN GRAMMAR " + gname);
                    //Thread.dumpStack();
                }
            }
            if (ruleref == null) {
                System.out.println("ERROR: UNKNOWN RULE NAME " + rn.getRuleName() + ' ' + rn);
                //Thread.dumpStack();
                return null;
            }
        }
        List<tokenPos> p = parse(grammar, ruleref, input, pos);
        if (p == null) {
            return null;
        }
        List<tokenPos> res = new ArrayList<tokenPos>();
        for (tokenPos tp : p) {
            if (tp instanceof emptyToken) {
                res.add(tp);
                continue;
            }
            try {
                res.add(new jsgfRuleParse(rn, (Rule)tp, tp.getPos()));
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR " + e);
            }
        }
        return res;
    }

    /*
     * LITERAL TOKENS
     */
    private List<tokenPos> parse(RuleGrammar grammar, RuleToken rt, String[] input, int pos) {
        if (pos >= input.length) {
            return null;
        }
        //System.out.println(rt.getText() + " ?= " + input[pos]);
        // TODO: what about case sensitivity ??????
        String tText = rt.getText().toLowerCase();
        if (tText.equals(input[pos]) || (input[pos].equals("%")) || (input[pos].equals("*"))) {
            List<tokenPos> res = new ArrayList<tokenPos>();
            res.add(new jsgfRuleToken(rt.getText(), pos + 1));
            if (input[pos].equals("*")) {
                res.add(new jsgfRuleToken(rt.getText(), pos));
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
            List<tokenPos> res = new ArrayList<tokenPos>();
            res.add(new jsgfRuleToken(rt.getText(), pos));
            return res;
        }
    }

    /*
    * ALTERNATIVES
    */
    private List<tokenPos> parse(RuleGrammar grammar, RuleAlternatives ra, String[] input, int pos) {
        List<tokenPos> res = new ArrayList<tokenPos>();
        for (Rule rule : ra.getRules()) {
            List<tokenPos> p = parse(grammar, rule, input, pos);
            if (p != null)
                res.addAll(p);
        }
        return res;
    }

    /*
    * RULESEQUENCE
    */
    private List<tokenPos> parse(RuleGrammar grammar, RuleSequence rs, String[] input, int pos) {
        Rule[] rarry = rs.getRules();
        if (rarry == null || rarry.length == 0) {
            return null;
        }
        List<tokenPos> p = parse(grammar, rarry[0], input, pos);
        if (p == null) {
            return null;
        }
        List<tokenPos> res = new ArrayList<tokenPos>();
        //System.out.println("seq sz" + p.size());
        for (tokenPos tp : p) {
            //System.out.println("seq  " + p.get(j));
            int nPos = tp.getPos();
            if (rarry.length == 1) {
                if (tp instanceof emptyToken) {
                    res.add(tp);
                    continue;
                }
                try {
                    res.add(new jsgfRuleSequence(new Rule[] { (Rule)tp }, tp.getPos()));
                } catch (IllegalArgumentException e) {
                    System.out.println(e);
                }
                continue;
            }
            Rule[] nra = Arrays.copyOfRange(rarry, 1, rarry.length);
            RuleSequence nrs = new RuleSequence(nra);
            //System.out.println("2parse " + nPos + nrs);
            List<tokenPos> q = parse(grammar, nrs, input, nPos);
            if (q == null) {
                continue;
            }
            //System.out.println("2 seq sz " + p.size());
            for (tokenPos tp1 : q) {
                //System.out.println("2 seq  " + q.get(k));
                //System.out.println("tp " + tp);
                //System.out.println("tp1 " + tp1);
                if (tp1 instanceof emptyToken) {
                    res.add(tp);
                    continue;
                }       
                if (tp instanceof emptyToken) {
                    res.add(tp1);
                    continue;
                }
                Rule[] ra;
                if (tp1 instanceof RuleSequence) {
                    RuleSequence r2 = (RuleSequence)tp1;
                    Rule[] r2r = r2.getRules();
                    ra = new Rule[r2r.length + 1];
                    ra[0] = (Rule)tp;
                    System.arraycopy(r2r, 0, ra, 1, r2r.length);
                } else {
                    ra = new Rule[] { (Rule)tp, (Rule)tp1 };
                }
                try {
                    res.add(new jsgfRuleSequence(ra, tp1.getPos()));
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
    private List<tokenPos> parse(RuleGrammar grammar, RuleTag rtag, String[] input, int pos) {
        String theTag = rtag.getTag();
        //System.out.println("tag="+theTag);
        List<tokenPos> p = parse(grammar, rtag.getRule(), input, pos);
        if (p == null) {
            return null;
        }
        List<tokenPos> res = new ArrayList<tokenPos>();
        for (tokenPos tp : p) {
            if (tp instanceof emptyToken) {
                res.add(tp);
                continue;
            }
            res.add(new jsgfRuleTag((Rule)tp, theTag, tp.getPos()));
        }
        return res;
    }

    //
    // RULECOUNT (e.g. [], *, or + )
    //
    private List<tokenPos> parse(RuleGrammar grammar, RuleCount rc, String[] input, int pos) {
        int rcount = rc.getCount();
        emptyToken empty = new emptyToken(pos);
        List<tokenPos> p = parse(grammar, rc.getRule(), input, pos);
        if (p == null) {
            if (rcount == RuleCount.ONCE_OR_MORE) return null;
            List<tokenPos> res = new ArrayList<tokenPos>();
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
            List<tokenPos> q = parse(grammar, new RuleSequence(ar), input, pos);
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

    /* interface for keeping track of where a token occurs in the
    * tokenized input string
    */
    interface tokenPos {
        public int getPos();
    }

    /* extension of RuleToken with tokenPos interface */
    class jsgfRuleToken extends RuleToken implements tokenPos {

        int pos;

        public jsgfRuleToken(String x, int pos) {
            super(x);
            this.pos = pos;
        }

        public int getPos() {
            return pos;
        }

    }

    class emptyToken extends jsgfRuleToken {

        public emptyToken(int pos) {
            super("EMPTY", pos);
        }
    }

    /* extension of RuleTag with tokenPos interface */
    class jsgfRuleTag extends RuleTag implements tokenPos {

        int pos;

        public jsgfRuleTag(Rule r, String x, int pos) {
            super(r, x);
            this.pos = pos;
        }

        public int getPos() {
            return pos;
        }

    }

    /* extension of RuleSequence with tokenPos interface */
    class jsgfRuleSequence extends RuleSequence implements tokenPos {

        int pos;

        public jsgfRuleSequence(Rule rules[], int pos) {
            super(rules);
            this.pos = pos;
        }

        public int getPos() {
            return pos;
        }

    }

    /* extension of RuleParse with tokenPos interface */
    class jsgfRuleParse extends RuleParse implements tokenPos {

        int pos;

        public jsgfRuleParse(RuleName rn, Rule r, int pos) throws IllegalArgumentException {
            super(rn, r);
            this.pos = pos;
        }

        public int getPos() {
            return pos;
        }

    }

}


