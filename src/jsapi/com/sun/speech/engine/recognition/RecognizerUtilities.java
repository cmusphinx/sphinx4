/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine.recognition;

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.speech.recognition.GrammarException;
import javax.speech.recognition.Recognizer;
import javax.speech.recognition.Rule;
import javax.speech.recognition.RuleAlternatives;
import javax.speech.recognition.RuleCount;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleName;
import javax.speech.recognition.RuleSequence;
import javax.speech.recognition.RuleTag;

/** Utilities methods. */
public class RecognizerUtilities {

    static public PrintStream errOutput = System.err;

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
        }
    }


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
                if (ev && s1.isVoid) return RuleName.VOID;
                if ((en && s1.isNull) || (r1 != rarry[i])) {
                    if (rs == r) {
                        rs = (RuleSequence) rs.copy();
                        rarry = rs.getRules();
                    }
                    if (en && s1.isNull) {
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
                if ((en && s1.isNull) || (ev && s1.isVoid) || (r1 != rarry[i])) {
                    if (ra == r) {
                        ra = (RuleAlternatives) ra.copy();
                        rarry = ra.getRules();
                    }
                    if (en && s1.isNull) {
                        cnull++;
                        r1 = null;
                    }
                    if (ev && s1.isVoid) {
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
            if (ev && s1.isVoid) {
                if (rcount == RuleCount.ONCE_OR_MORE) return RuleName.VOID;
                return RuleName.NULL;
            }
            if (en && s1.isNull) return RuleName.NULL;
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
            if (en && s1.isNull) return RuleName.NULL;
            if (ev && s1.isVoid) return RuleName.VOID;
            if (r1 == rtr) return rtag;
            return new RuleTag(r1, rtag.getTag());
        }

        return r;
    }


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
            int k = 0;
            for (String rname : grammar.listRuleNames()) {
                Rule r1 = grammar.getRuleInternal(rname);
                RuleState s = new RuleState(r1, NRG);
                //System.out.println(rname + " " + r1.getClass() + " " + s.isvoid + " " + s.isnull);
                if ((eliminateVOID && s.isVoid) || (eliminateNULL && s.isNull)) {
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
 }

class RuleState {

    boolean isNull;

    boolean isVoid;

    RuleState(Rule r, RuleGrammar RGL[]) {
        if (r instanceof RuleName) {
            RuleName rn = (RuleName) r;

            /* simple check for void/null */
            String simpleName = rn.getSimpleRuleName();
            if (simpleName.equals("VOID")) {
                isVoid = true;
                return;
            }
            if (simpleName.equals("NULL")) {
                isNull = true;
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
                    isVoid = true;
                    return;
                }
                if (simpleName1.equals("NULL")) {
                    isNull = true;
                    return;
                }
            }
            return;
        }
    }
    Rule getRule(RuleName rn, RuleGrammar RGL[]) {
        String gname = rn.getSimpleGrammarName();
        for (RuleGrammar grammar : RGL) {
            if (grammar.getName().equals(gname)) {
                return grammar.getRuleInternal(rn.getSimpleRuleName());
            }
        }
        return null;
    }
}



