/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine.recognition;

import javax.speech.recognition.*;
import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/** Utilities methods. */
public class RecognizerUtilities {

    static public PrintStream errOutput = System.err;


    /** Copy all RuleGrammars from recFrom to recTo. */
    public static void copyGrammars(Recognizer recFrom, Recognizer recTo)
            throws GrammarException {
        RuleGrammar gramFrom[] = recFrom.listRuleGrammars();

        for (RuleGrammar grammar : gramFrom) {
            String name = grammar.getName();
            RuleGrammar gramTo = recTo.getRuleGrammar(name);

            // Create a new grammar - if necessary
            if (gramTo == null) {
                try {
                    gramTo = recTo.newRuleGrammar(name);
                } catch (IllegalArgumentException gse) {
                    throw new GrammarException("copyGrammars: " + gse, null);
                }
            }
            copyGrammar(grammar, gramTo);
        }
    }


    /** Copy a grammar from gramFrom to gramTo. */
    public static void copyGrammar(RuleGrammar gramFrom, RuleGrammar gramTo)
            throws GrammarException {
        // Copy imports
        for (RuleName ruleName : gramFrom.listImports()) {
            gramTo.addImport(ruleName);
        }

        // Copy each rule
        for (String ruleName : gramFrom.listRuleNames()) {
            boolean isPublic = false;
            try {
                isPublic = gramFrom.isRulePublic(ruleName);
            } catch (IllegalArgumentException nse) {
                throw new GrammarException(nse.toString(), null);
            }

            Rule rule = gramFrom.getRule(ruleName);
            try {
                gramTo.setRule(ruleName, rule, isPublic);
                if (gramFrom.isEnabled(ruleName)) {
                    gramTo.setEnabled(ruleName, true);
                }
            } catch (IllegalArgumentException E) {
                throw new GrammarException(E.toString(), null);
            }

            // copy sample sentences if appropriate
            if ((gramTo instanceof BaseRuleGrammar) && (gramFrom instanceof BaseRuleGrammar)) {
                List<String> a = ((BaseRuleGrammar)gramFrom).getSampleSentences(ruleName);
                ((BaseRuleGrammar)gramTo).setSampleSentences(ruleName, a);
            }
        }
    }


    static Map<String, Set<String>> xrefs;


    /**
     * For a specified recognizer, build a table that for each rule R lists all rules in all grammars that reference R.
     *
     * @see #getRuleNameRefs
     * @see #getRefsToRuleName
     */
    static public void buildXrefTable(Recognizer rec) {
        xrefs = new HashMap<String, Set<String>>();
        RuleGrammar[] grams = rec.listRuleGrammars();

        if (grams == null) {
            return;
        }

        for (RuleGrammar gram : grams) {
            for (String name : gram.listRuleNames()) {
                // Get the definition of rule name[j] in gram[i]
                Rule r = gram.getRule(name);

                // Build a fully-qualified RuleName for rule name[j] in gram[i]
                RuleName rn = new RuleName(gram.getName() + '.' + name);

                // Identify all rules referenced in r
                List<RuleName> refs = new ArrayList<RuleName>();
                getRuleNameRefs(r, refs);

                for (RuleName ref : refs) {
                    // Get a fully-qualified reference
                    RuleName fullref;
                    try {
                        fullref = gram.resolve(ref);
                    } catch (GrammarException e) {
                        fullref = null;
                    }

                    if (fullref != null) {
                        String key = fullref.toString().intern();
                        Set<String> set = xrefs.get(key);
                        if (set == null) {
                            set = new HashSet<String>();
                            xrefs.put(key, set);
                        }
                        set.add(rn.toString().intern());
                    } else {
                        debugMessageOut("Warning: unresolved rule " + ref + " in grammar " + gram.getName());
                    }
                }
            }
        }
    }


    /** Return an array of references to rule r in grammar g. */
    static public RuleName[] getRefsToRuleName(RuleGrammar g, RuleName r) {
        // Ensure we have a fully qualified rulename
        // (that's how the xref table works)
        r = new RuleName(g.getName() + '.' + r.getSimpleRuleName());

        if (xrefs == null) {
            return null;
        }

        Set<String> set = xrefs.get(r.toString().intern());

        if (set == null) {
            return null;
        }

        RuleName[] rulenames = new RuleName[set.size()];
        int i = 0;
        for (String name : set)
            rulenames[i++] = new RuleName(name);
        return rulenames;
    }


    static protected void getRuleNameRefs(Rule r, List<RuleName> refs) {
        if (r instanceof RuleAlternatives || r instanceof RuleSequence) {
            Rule[] array;
            if (r instanceof RuleAlternatives) {
                array = ((RuleAlternatives) r).getRules();
            } else {
                array = ((RuleSequence) r).getRules();
            }

            if (array != null) {
                for (Rule rule : array) {
                    getRuleNameRefs(rule, refs);
                }
            }
        } else if (r instanceof RuleTag) {
            getRuleNameRefs(((RuleTag) r).getRule(), refs);
        } else if (r instanceof RuleCount) {
            getRuleNameRefs(((RuleCount) r).getRule(), refs);
        } else if (r instanceof RuleName) {
            // Put a copy in the Xref list (avoid side-effects of linking)
            RuleName tmp = (RuleName) r;
            refs.add(new RuleName(tmp.getRuleName()));
        }
    }


    static private Set<String> oldMessages = new HashSet<String>();


    /** Print out a message - don't ever repeat it. */
    static protected void debugMessageOut(String message) {
        message = message.intern();
        if (oldMessages.contains(message)) {
            return;
        }
        errOutput.println(message);
        oldMessages.add(message);
    }

    /*
      * transform grammar to eliminate <NULL> and <VOID>
      *
      * NULL and VOID can be removed by transforming the grammar
      * as follows:
      *
      * RuleSequence:
      *   (x  y  <NULL>) --> (x y)
      *   (x  y  <VOID>) --> <VOID>
      *   (<NULL>) --> <NULL>
      *   (<VOID>) --> <VOID>
      *
      * RuleAlternatives:
      *   (x | y | <NULL>) --> [(x | y)]
      *   (x | y | <VOID>) --> (x | y)
      *   (<NULL>) --> <NULL>
      *   (<VOID>) --> <VOID>
      *   (<VOID> | <NULL>) --> <NULL>
    *
    * RuleCount:
    *   [<VOID>]  --> <NULL>
    *    <VOID>*  --> <NULL>
    *    <VOID>+  --> <VOID>
    *   [<NULL>]  --> <NULL>
    *    <NULL>*  --> <NULL>
    *    <NULL>+  --> <NULL>
    *
    * RuleTag:
    *   <NULL>{tag} --> <NULL>
    *   <VOID>{tag} --> <VOID>
    *
    * RuleName:
    *    If a rule has been marked as being void or null then apply the
    *    following:
    *    <RuleName> --> <VOID>
    *
    * The steps of transformation are:
    *
    *     just return the original grammar
    *
    *  2. Make a copy of the grammar and Mark all rules as non-void & non-null
    *
    *  3. Go through each rule in series and apply the transformations.
    *
    *  4. Repeat step 3 until one pass has been made through all the
    *     rules in which no transformations were performed
    *
    *  5. Remove NULL and VOID rules from copies grammars
    *
    *  6. Remove any empty grammars
    *
    *  7. Return the transformed array of grammars
    *
    * Because all transforms are some form of a reduction, the process is
    * guaranteed to complete.
    *
    */


    static RuleGrammar[] transform(RuleGrammar RG[], boolean eliminateNULL, boolean eliminateVOID)
            throws GrammarException {
        //if (!eliminateNULL && !eliminateVOID) return RG;
        RuleGrammar NRG[] = RG.clone();
        boolean somethingchanged = true;
        boolean nochange = true;
        while (somethingchanged) {
            somethingchanged = false;
            for (int i = 0; i < NRG.length; i++) {
                for (String rname : NRG[i].listRuleNames()) {
                    Rule r = NRG[i].getRuleInternal(rname);
                    Rule nr = transform(r, NRG, eliminateNULL, eliminateVOID);
                    if (nr != r) {
                        nochange = false;
                        somethingchanged = true;
                        if (NRG[i] == RG[i]) {
                            NRG[i] = new BaseRuleGrammar(null, RG[i].getName());
                            RecognizerUtilities.copyGrammar(RG[i], NRG[i]);
                        }
                        boolean isPublic;
                        try {
                            isPublic = NRG[i].isRulePublic(rname);
                        } catch (IllegalArgumentException nse) {
                            throw new GrammarException(nse.toString(), null);
                        }
                        NRG[i].setRule(rname, nr, isPublic);
                    }
                }
            }
        }

        if (nochange) return NRG;

        /*
        * Now remove unused rules
        */
        List<RuleGrammar> nr = new ArrayList<RuleGrammar>();
        for (RuleGrammar grammar : NRG) {
            RuleGrammar NG = null;
            int k = 0;
            for (String rname : grammar.listRuleNames()) {
                Rule r1 = grammar.getRuleInternal(rname);
                RuleState s = new RuleState(r1, NRG);
                //System.out.println(rname + " " + r1.getClass() + " " + s.isvoid + " " + s.isnull);
                if ((eliminateVOID && s.isvoid) || (eliminateNULL && s.isnull)) {
                    grammar.deleteRule(rname);
                } else k++;
            }
            if (k > 0) nr.add(grammar);
        }

        /*
        * Now return all non-null grammars
        */
        return nr.toArray(new RuleGrammar[nr.size()]);
    }


    static Rule transform(Rule r, RuleGrammar RGL[], boolean en, boolean ev) {
        //System.out.println("xf " + r.getClass() + r);

        /*
        * RULESEQUENCE
        */
        if (r instanceof RuleSequence) {
            RuleSequence rs = (RuleSequence) r;
            Rule rarry[] = rs.getRules();
            if ((rarry == null) || (rarry.length == 0)) {
                return null;
            }
            int cnull = 0;
            for (int i = 0; i < rarry.length; i++) {
                Rule r1 = transform(rarry[i], RGL, en, ev);
                RuleState s1 = new RuleState(r1, RGL);
                if (ev && s1.isvoid) return RuleName.VOID;
                if ((en && s1.isnull) || (r1 != rarry[i])) {
                    if (rs == r) {
                        rs = (RuleSequence) rs.copy();
                        rarry = rs.getRules();
                    }
                    if (en && s1.isnull) {
                        cnull++;
                        r1 = null;
                    }
                    rarry[i] = r1;
                    rs.setRules(rarry);
                }
            }
            if (cnull == 0) return rs;
            if (cnull == rarry.length) return RuleName.NULL;
            Rule barry[] = new Rule[rarry.length - cnull];
            int j = 0;
            for (Rule rule : rarry) {
                if (rule != null) {
                    barry[j++] = rule;
                }
            }
            rs.setRules(barry);
            return rs;
        }

        /*
        * RULEALTERNATIVES
        */
        if (r instanceof RuleAlternatives) {
            RuleAlternatives ra = (RuleAlternatives) r;
            Rule rarry[] = ra.getRules();
            if ((rarry == null) || (rarry.length == 0)) {
                return null;
            }
            int cnull = 0;
            int cvoid = 0;
            for (int i = 0; i < rarry.length; i++) {
                Rule r1 = transform(rarry[i], RGL, en, ev);
                //System.out.println("rs: " + r1);
                RuleState s1 = new RuleState(r1, RGL);
                if ((en && s1.isnull) || (ev && s1.isvoid) || (r1 != rarry[i])) {
                    if (ra == r) {
                        ra = (RuleAlternatives) ra.copy();
                        rarry = ra.getRules();
                    }
                    if (en && s1.isnull) {
                        cnull++;
                        r1 = null;
                    }
                    if (ev && s1.isvoid) {
                        cvoid++;
                        r1 = null;
                    }
                    rarry[i] = r1;
                    ra.setRules(rarry);
                }
            }
            if ((cnull == 0) && (cvoid == 0)) return ra;
            if (cvoid == rarry.length) return RuleName.VOID;
            if ((cvoid + cnull) == rarry.length) return RuleName.NULL;
            Rule barry[] = new Rule[rarry.length - (cnull + cvoid)];
            int j = 0;
            for (Rule rule : rarry) {
                if (rule != null) {
                    barry[j++] = rule;
                }
            }
            ra.setRules(barry);
            if (cnull > 0) return new RuleCount(ra, RuleCount.OPTIONAL);
            else return ra;
        }

        /*
        * RULECOUNT (e.g. [], *, or + )
        */
        if (r instanceof RuleCount) {
            RuleCount rc = (RuleCount) r;
            int rcount = rc.getCount();
            Rule r1 = rc.getRule();
            Rule r2 = transform(r1, RGL, en, ev);
            RuleState s1 = new RuleState(r2, RGL);
            if (ev && s1.isvoid) {
                if (rcount == RuleCount.ONCE_OR_MORE) return RuleName.VOID;
                return RuleName.NULL;
            }
            if (en && s1.isnull) return RuleName.NULL;
            if (r1 == r2) return rc;
            return new RuleCount(r2, rcount);
        }

        /*
        * TAGS
        */
        if (r instanceof RuleTag) {
            RuleTag rtag = (RuleTag) r;
            Rule rtr = rtag.getRule();
            Rule r1 = transform(rtr, RGL, en, ev);
            RuleState s1 = new RuleState(r1, RGL);
            if (en && s1.isnull) return RuleName.NULL;
            if (ev && s1.isvoid) return RuleName.VOID;
            if (r1 == rtr) return rtag;
            return new RuleTag(r1, rtag.getTag());
        }

        return r;
    }


    /*
    * Test code
    */
    static public void main(String args[]) {

        Recognizer R = null;

        if (args.length < 1) {
            System.out.println("usage: java GXFormer grammar-file");
            return;
        }

        boolean elimVOID = true;
        boolean elimNULL = true;
        if (args.length > 1) elimNULL = Boolean.parseBoolean(args[1]);
        if (args.length > 2) elimVOID = Boolean.parseBoolean(args[2]);

        try {
            R = new BaseRecognizer();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (R == null) {
            System.out.println("failed to create a recognizer");
            return;
        }
        try {
            R.allocate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        RuleGrammar G = null;

        URL u = null;
        File f = new File(".");
        try {
            u = new URL("file:" + f.getAbsolutePath());
        } catch (MalformedURLException e) {
            System.out.println("Could not get URL for current directory " + e);
            return;
        }

        try {
            G = R.loadJSGF(u, args[0]);
        } catch (Exception e1) {
            System.out.println("GRAMMAR ERROR: In grammar " + args[0]
                    + ", or its imports, at URL base " + u
                    + ' ' + e1);
            e1.printStackTrace();
            return;
        }
        if (G == null) {
            System.out.println("ERROR LOADING GRAMMAR");
            return;
        }

        try {
            R.commitChanges();
        } catch (Exception ge) {
            System.out.println("Grammar " + G.getName()
                    + " caused a GrammarException when initially loaded");
            ge.printStackTrace();
            return;
        }

        System.out.println("Grammar loaded -- transforming");

        RuleGrammar RG[] = R.listRuleGrammars();


        RuleGrammar NRG[] = null;
        try {
            NRG = transform(RG, elimNULL, elimVOID);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        for (RuleGrammar grammar : NRG) {
            System.out.println("Grammar: " + grammar.getName());
            for (String rname : grammar.listRuleNames()) {
                System.out.print('<' + rname + "> = ");
                Rule r = grammar.getRuleInternal(rname);
                System.out.println(r);
            }
            System.out.println("");
            System.out.println("");
        }
    }

    /**
     * Replaces unprintable characters by their escaped (or unicode escaped) equivalents in the given string;
     * used when those characters cannot be used as part of an ASCII string literal.
     */
    public static final String addEscapes(String str) {
        StringBuilder retval = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            switch (ch) {
                case 0:
                    continue;
                case '\b':
                    retval.append("\\b");
                    continue;
                case '\t':
                    retval.append("\\t");
                    continue;
                case '\n':
                    retval.append("\\n");
                    continue;
                case '\f':
                    retval.append("\\f");
                    continue;
                case '\r':
                    retval.append("\\r");
                    continue;
                case '\"':
                    retval.append("\\\"");
                    continue;
                case '\'':
                    retval.append("\\\'");
                    continue;
                case '\\':
                    retval.append("\\\\");
                    continue;
                default:
                    if (ch < 0x20 || ch > 0x7e) {
                        String s = "0000" + Integer.toString(ch, 16);
                        retval.append("\\u").append(s, s.length() - 4, s.length());
                    } else {
                        retval.append(ch);
                    }
                    continue;
            }
        }
        return retval.toString();
    }
}

class RuleState {

    Rule getRule(RuleName rn, RuleGrammar RGL[]) {
        String gname = rn.getSimpleGrammarName();
        for (RuleGrammar grammar : RGL) {
            if (grammar.getName().equals(gname)) {
                return grammar.getRuleInternal(rn.getSimpleRuleName());
            }
        }
        return null;
    }


    RuleState(Rule r, RuleGrammar RGL[]) {
        if (r instanceof RuleName) {
            RuleName rn = (RuleName) r;

            /* simple check for void/null */
            String simpleName = rn.getSimpleRuleName();
            if (simpleName.equals("VOID")) {
                isvoid = true;
                return;
            }
            if (simpleName.equals("NULL")) {
                isnull = true;
                return;
            }

            /* now lookup the rule and see if its RHS is null/void */
            Rule r1 = getRule(rn, RGL);
            if (r1 == null) System.out.println("ERROR: Could not find rule " + rn);
            if (r1 instanceof RuleName) {
                RuleName rn1 = (RuleName) r1;

                /* simple check for void/null */
                String simpleName1 = rn1.getSimpleRuleName();
                if (simpleName1.equals("VOID")) {
                    isvoid = true;
                    return;
                }
                if (simpleName1.equals("NULL")) {
                    isnull = true;
                    return;
                }
            }
            return;
        }
    }


    boolean isvoid;
    boolean isnull;
}



