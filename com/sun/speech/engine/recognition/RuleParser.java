/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package com.sun.speech.engine.recognition;

import java.util.StringTokenizer;
import java.util.Vector;

import javax.speech.recognition.Recognizer;
import javax.speech.recognition.Rule;
import javax.speech.recognition.RuleAlternatives;
import javax.speech.recognition.RuleCount;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleName;
import javax.speech.recognition.RuleParse;
import javax.speech.recognition.RuleSequence;
import javax.speech.recognition.RuleTag;
import javax.speech.recognition.RuleToken;

/**
 * Implementation of the parse method(s) on 
 * javax.speech.recognition.RuleGrammar.
 *
 * @version 1.5 10/27/99 16:33:49
 */
public class RuleParser {

    private Recognizer theRec;
    
    /*
     * parse a text string against a particular rule from a particluar grammar
     * returning a RuleParse data structure is successful and null otherwise
     */
    public static RuleParse parse(String text,Recognizer R, RuleGrammar G,String ruleName) {
        String inputTokens[] = tokenize(text);
        return parse(inputTokens,R,G,ruleName);
    }
    
    public static RuleParse parse(String inputTokens[],Recognizer R, RuleGrammar G,String ruleName) {
        RuleParse rpa[] = mparse(inputTokens,R,G,ruleName);
        if (rpa == null) {
            return null;
        } else {
            return rpa[0];
        }
    }
    
    public static RuleParse[] mparse(String text,Recognizer R, RuleGrammar G,String ruleName) {
        String inputTokens[] = tokenize(text);
        return mparse(inputTokens,R,G,ruleName);
    }
    
    public static RuleParse[] mparse(String inputTokens[],Recognizer R, RuleGrammar G,String ruleName) {
        RuleParser rp = new RuleParser();
        rp.theRec = R;
        String rNames[];
        Rule startRule = null;
        if (ruleName != null) {
            rNames = new String[1];
            rNames[0] = ruleName;
        } else {
            rNames = G.listRuleNames();
        }
        Vector p = null;
        int j = 0; 
        Vector t = new Vector();
        for (j=0; j<rNames.length; j++) {
            if ((ruleName == null) && !(G.isEnabled(rNames[j]))) {
                continue;
            }
            startRule = G.getRule(rNames[j]);
            if (startRule == null) { 
                System.out.println("BAD RULENAME " + rNames[j]);
                continue;
            }
            p = rp.parse(G,startRule,inputTokens,0);
            if ((p != null) && (p.size() != 0)) {
                for (int i=0; i<p.size(); i++) {
                    tokenPos tp = (tokenPos) p.elementAt(i);
                    if (tp.getPos() == inputTokens.length) {
                        RuleName rn = new RuleName(rNames[j]);
                        t.addElement(new RuleParse(rn,(Rule)tp));
                    }
                }
            }
        }
        if (t.size() == 0) {
            return null;
        }
        RuleParse rpa[] = new RuleParse[t.size()];
        t.copyInto(rpa);
        return rpa;
    }
  
    /*
     * parse routine called recursively while traversing the Rule structure
     * in a depth first manner. Returns a list of valid parses.
     */
    private Vector parse(RuleGrammar G, Rule r, String input[], int iPos) {

        //System.out.println("PARSE " + r.getClass().getName() + " " + iPos + " " + r);

        /*
         * RULE REFERENCES
         */
        if (r instanceof RuleName) {
            RuleName rn = (RuleName)r;
            String simpleName = rn.getSimpleRuleName();
	    if (simpleName.equals("VOID")) return null;
	    if (simpleName.equals("NULL")) {
	      Vector p = new Vector();
	      jsgfRuleParse rp1 = new jsgfRuleParse(rn,RuleName.NULL);
	      rp1.setPos(iPos);
	      p.addElement(rp1);
	      return p;
	    }
            Rule ruleref = G.getRule(simpleName);
            if (ruleref == null) {
                String gname = rn.getFullGrammarName();
                //System.out.println("gname=" + gname);
                if ((gname != null) && (gname.length() > 0)) {
                    RuleGrammar RG1 = theRec.getRuleGrammar(gname);
                    if (RG1 != null) {
                        //System.out.println("simpleName=" + simpleName);
                        ruleref = RG1.getRule(simpleName);
                        //System.out.println("ruleRef=" + ruleref);
                        G = RG1;
                    } else {
                        System.out.println("ERROR: UNKNOWN GRAMMAR " + gname);
                        //Thread.dumpStack();
                    }
                }
                if (ruleref == null) {
                    System.out.println("ERROR: UNKNOWN RULE NAME " + rn.getRuleName() + " " + rn);
                    //Thread.dumpStack();
                    return null;
                }
            }
            Vector p = parse(G,ruleref,input,iPos);
            if (p == null) {
                return null;
            }
            Vector t = new Vector();
            for (int j=0; j<p.size(); j++) {
                tokenPos tp = (tokenPos) p.elementAt(j);
                if (tp instanceof emptyToken) { 
                    t.addElement(tp); 
                    continue; 
                }
                Rule ar[] = new Rule[1];
                ar[0] = (Rule)p.elementAt(j);
                try {
                    jsgfRuleParse rulep = new jsgfRuleParse(rn,ar[0]);
                    rulep.setPos(tp.getPos());
                    t.addElement(rulep);
                } catch (IllegalArgumentException e) {
                    System.out.println("ERROR " + e);
                }
            }
            return t;
        }

        /*
         * LITERAL TOKENS
         */
        if (r instanceof RuleToken) {
            if (iPos >= input.length) {
                return null;
            }
            RuleToken rt = (RuleToken)r;
            //System.out.println(rt.getText() + " ?= " + input[iPos]);
            // TODO: what about case sensitivity ??????
            String tText = rt.getText().toLowerCase();
            if (tText.equals(input[iPos]) || (input[iPos].equals("%"))  || (input[iPos].equals("*"))) {
                Vector v = new Vector();
                jsgfRuleToken tok = new jsgfRuleToken(rt.getText());
                tok.setPos(iPos+1);
                v.addElement(tok);
                if (input[iPos].equals("*")) {
                    jsgfRuleToken tok2 = new jsgfRuleToken(rt.getText());
                    tok2.setPos(iPos);
                    v.addElement(tok2);
                }
                return v;
            } else {
                if (tText.indexOf(' ') < 0) {
                    return null;
                }
                if (!tText.startsWith(input[iPos])) {
                    return null;
                }
                String ta[] = tokenize(tText);
                int j = 0;
                while (true) {
                    if (j >= ta.length) {
                        break;
                    }
                    if (iPos >= input.length) {
                        return null;
                    }
                    if (!ta[j].equals(input[iPos])) {
                        return null;
                    }
                    iPos++;
                    j++;
                }
                Vector v = new Vector();
                jsgfRuleToken tok = new jsgfRuleToken(rt.getText());
                tok.setPos(iPos);
                v.addElement(tok);
                return v;
            }
        }
    
        /*
         * ALTERNATIVES
         */
        if (r instanceof RuleAlternatives) {
            RuleAlternatives ra = (RuleAlternatives)r;
            Rule rar[] = ra.getRules();
            Vector alts = new Vector();
            for (int i=0; i<rar.length; i++) {
                Vector p = parse(G,rar[i],input,iPos);
                if (p == null) {
                    continue;
                }
                for (int j=0; j<p.size(); j++) {
                    alts.addElement(p.elementAt(j));
                }
            }
            return alts;
        }
    
        /*
         * RULESEQUENCE
         */
        if (r instanceof RuleSequence) {
            RuleSequence rs = (RuleSequence) r;
            Rule rarry[] = rs.getRules();
            if ((rarry == null) || (rarry.length == 0)) {
                return null;
            }
            Vector p = parse(G,rarry[0],input,iPos);
            if (p == null) {
                return null;
            }
            Vector res = new Vector();
            //System.out.println("seq sz" + p.size());
            for (int j=0; j<p.size(); j++) {
                //System.out.println("seq  " + p.elementAt(j));
                tokenPos tp = (tokenPos)p.elementAt(j);
                Rule rule0 = (Rule)p.elementAt(j);
                int nPos = tp.getPos();
                if (rarry.length == 1) {
                    if (rule0 instanceof emptyToken) { 
                        res.addElement(rule0); 
                        continue; 
                    }
                    jsgfRuleSequence rp = null;
                    Rule ra[] = new Rule[1];
                    ra[0] = rule0;
                    try {
                        rp = new jsgfRuleSequence(ra);
                        rp.setPos(tp.getPos());
                        res.addElement(rp);
                    }
                    catch(IllegalArgumentException e) {
                        System.out.println(e);
                    }
                    continue;
                }
                Rule nra[] = new Rule[rarry.length-1];
                System.arraycopy(rarry,1,nra,0,nra.length);
                RuleSequence nrs = new RuleSequence(nra);
                //System.out.println("2parse " + nPos + nrs);
                Vector q = parse(G,nrs,input,nPos);
                if (q == null) {
                    continue;
                }
                //System.out.println("2 seq sz " + p.size());
                for (int k=0; k<q.size(); k++) {
                    //System.out.println("2 seq  " + q.elementAt(k));
                    Rule r1 = (Rule) q.elementAt(k);
                    Rule ra[]=null;
                    tokenPos tp1 = (tokenPos) q.elementAt(k);
                    //System.out.println("rule0 " + rule0);
                    //System.out.println("r1 " + r1);
                    if (r1 instanceof emptyToken) { 
                        res.addElement(rule0); 
                        continue; 
                    }  
                    if (rule0 instanceof emptyToken) { 
                        res.addElement(r1); 
                        continue; 
                    }
                    if (r1 instanceof RuleSequence) {
                        RuleSequence r2 = (RuleSequence)r1;
                        Rule r2r[] = r2.getRules();
                        ra = new Rule[r2r.length+1];
                        ra[0]= rule0;
                        System.arraycopy(r2r,0,ra,1,r2r.length);
                    } else {
                        ra = new Rule[2];
                        ra[0]=rule0;
                        ra[1]=r1;
                    }
                    jsgfRuleSequence rp = null;
                    try {
                        rp = new jsgfRuleSequence(ra);
                        rp.setPos(tp1.getPos());
                        res.addElement(rp);
                    } catch(IllegalArgumentException e) {
                        System.out.println(e);
                    }
                }
            }
            return res;
        }
    
        /*
         * TAGS
         */
        if (r instanceof RuleTag) {
            RuleTag rtag = (RuleTag)r;
            String theTag = rtag.getTag();
            //System.out.println("tag="+theTag);
            Vector p = parse(G,rtag.getRule(),input,iPos);
            if (p == null) {
                return null;
            }
            Vector t = new Vector();
            for (int j=0; j<p.size(); j++) {
                tokenPos tp = (tokenPos) p.elementAt(j);
                if (tp instanceof emptyToken) { 
                    t.addElement(tp); 
                    continue; 
                }; 
                jsgfRuleTag tag = new jsgfRuleTag((Rule)tp,theTag);
                tag.setPos(tp.getPos());
                t.addElement(tag);
            }
            return t;
        }
    
        //
        // RULECOUNT (e.g. [], *, or + )
        //
        if (r instanceof RuleCount) {
            RuleCount rc = (RuleCount)r;
            int rcount = rc.getCount();
            emptyToken empty = new emptyToken();
            empty.setPos(iPos);
            Vector p = parse(G,rc.getRule(),input,iPos);
            if (p == null) {
                if (rcount == RuleCount.ONCE_OR_MORE) return null;
                Vector v = new Vector();
                v.addElement(empty);
                return v;
            }
            if (rcount != RuleCount.ONCE_OR_MORE) {
                p.addElement(empty);
            }
            if (rcount == RuleCount.OPTIONAL) {
                return p;
            }
            for (int m=2; m<=input.length-iPos; m++) {
                Rule ar[] = new Rule[m];
                for (int n=0; n<m; n++) {
                    ar[n]=rc.getRule();
                }
                RuleSequence rs1 = new RuleSequence(ar);
                Vector q = parse(G,rs1,input,iPos);
                if (q == null) {
                    return p;
                }
                for (int z=0; z<q.size(); z++) {
                    p.addElement(q.elementAt(z));
                }
            }
            return p;
        }
    
        System.out.println("ERROR UNKNOWN OBJECT " + r);
        return null;
    }

    /*
     * tokenize a string
     */
    static String []tokenize(String text) {
        StringTokenizer st = new StringTokenizer(text);
        int size = st.countTokens();
        String res[] = new String[size];
        int i=0;
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
        public void setPos(int i);
    }

    /* extension of RuleToken with tokenPos interface */
    class jsgfRuleToken extends RuleToken implements tokenPos {
        int iPos = 0;
        public jsgfRuleToken(String x) { super(x); }
        public int  getPos() { return iPos; }
        public void setPos(int i) { iPos = i; }
    }

    class emptyToken extends jsgfRuleToken {
        public emptyToken() { super("EMPTY"); }
    }

    /* extension of RuleTag with tokenPos interface */
    class jsgfRuleTag extends RuleTag implements tokenPos {
        int iPos = 0;
        public jsgfRuleTag(Rule r, String x) { super(r,x); }
        public int getPos() { return iPos; }
        public void setPos(int i) { iPos = i; }
    }

    /* extension of RuleSequence with tokenPos interface */
    class jsgfRuleSequence extends RuleSequence implements tokenPos {
        int iPos = 0;
        public jsgfRuleSequence(Rule rules[]) { super(rules); }
        public int getPos() { return iPos; }
        public void setPos(int i) { iPos = i; }
    }


    /* extension of RuleParse with tokenPos interface */
    class jsgfRuleParse extends RuleParse implements tokenPos {
        int iPos = 0;
        public jsgfRuleParse() { super(); }
        public jsgfRuleParse(RuleName rn,Rule r) throws IllegalArgumentException { 
            super(rn,r);
        }
        public int getPos() { return iPos; }
        public void setPos(int i) { iPos = i; }
    }
  


}


