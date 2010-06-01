/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine.recognition;

import javax.speech.recognition.*;

import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFRuleGrammar;
import edu.cmu.sphinx.jsgf.parser.JSGFParser;
import edu.cmu.sphinx.jsgf.rule.JSGFRule;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleAlternatives;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleCount;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleName;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleSequence;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleTag;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleToken;

import java.io.Serializable;
import java.util.*;

/** Implementation of javax.speech.recognition.RuleGrammar. */
@SuppressWarnings("serial")
public class BaseRuleGrammar extends BaseGrammar implements RuleGrammar,
        Serializable {

    private JSGFRuleGrammar jsgfGrammar;

    /**
     * Create a new BaseRuleGrammar
     * 
     * @param rec
     *            the BaseRecognizer for this grammar
     * @param grammar
     *            the JSGF grammar object behind this grammar
     */
    public BaseRuleGrammar(BaseRecognizer rec, JSGFRuleGrammar grammar) {
        super(rec, grammar.getName());

        assert grammar != null;
        this.jsgfGrammar = grammar;
    }

    /**
     * Creates a new BaseRuleGrammar
     * 
     * @param recognizer
     *            the recognizer for this grammar
     * @param name
     *            the name for this grammar
     */
    public BaseRuleGrammar(BaseRecognizer recognizer, String name) {
        super(recognizer, name);
    }

    /**
     * Set the enabled property of the Grammar. From
     * javax.speech.recognition.Grammar.
     * 
     * @param enabled
     *            the new desired state of the enabled property.
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        jsgfGrammar.setEnabled(enabled);
    }

    /**
     * Parse partial JSGF text to a Rule object. From
     * javax.speech.recognition.RuleGrammar.
     */
    public Rule ruleForJSGF(String text) {
        return convert(JSGFParser.ruleForJSGF(text));
    }

    /*
     * Recursively go through the rules and create the same structures from
     * JSAPI
     */
    public JSGFRule convert(Rule rule) {

        if (rule == null)
            return null;

        if (rule instanceof RuleName) {
            if (rule == RuleName.NULL)
                return JSGFRuleName.NULL;
            if (rule == RuleName.VOID)
                return JSGFRuleName.NULL;
            return new JSGFRuleName(((RuleName) rule).getRuleName());
        }
        if (rule instanceof RuleAlternatives) {
            RuleAlternatives ruleAlternatives = (RuleAlternatives) rule;
            List<JSGFRule> subrules = new ArrayList<JSGFRule>();
            for (Rule r : ruleAlternatives.getRules()) {
                subrules.add(convert(r));
            }
            List<Float> weights = null;
            if (ruleAlternatives.getWeights() != null) {
                weights = new ArrayList<Float>();
                for (float f : ruleAlternatives.getWeights()) {
                    weights.add(f);
                }
            }
            return new JSGFRuleAlternatives(subrules, weights);
        }

        if (rule instanceof RuleSequence) {
            RuleSequence ruleSequence = (RuleSequence) rule;
            List<JSGFRule> subrules = new ArrayList<JSGFRule>();
            for (Rule r : ruleSequence.getRules()) {
                subrules.add(convert(r));
            }
            return new JSGFRuleSequence(subrules);
        }

        if (rule instanceof RuleCount) {
            RuleCount ruleCount = (RuleCount) rule;
            return new JSGFRuleCount(convert(ruleCount.getRule()), ruleCount
                    .getCount());
        }

        if (rule instanceof RuleTag) {
            RuleTag ruleTag = (RuleTag) rule;
            return new JSGFRuleTag(convert(ruleTag.getRule()), ruleTag.getTag());
        }

        if (rule instanceof RuleToken) {
            RuleToken ruleToken = (RuleToken) rule;
            return new JSGFRuleToken(ruleToken.getText());
        }
        System.out.println("Can't convert rule " + rule.getClass());
        return null;
    }

    /*
     * Recursively go through the rules and create the same structures for JSAPI
     */
    private Rule convert(JSGFRule rule) {

        if (rule == null)
            return null;

        if (rule instanceof JSGFRuleName) {
            if (rule == JSGFRuleName.NULL)
                return RuleName.NULL;
            if (rule == JSGFRuleName.VOID)
                return RuleName.NULL;

            JSGFRuleName ruleName = (JSGFRuleName) rule;
            return new RuleName(ruleName.getRuleName());
        }

        if (rule instanceof JSGFRuleAlternatives) {
            JSGFRuleAlternatives ruleAlternatives = (JSGFRuleAlternatives) rule;
            Rule[] subrules = new Rule[ruleAlternatives.getRules().size()];
            int i = 0;
            for (JSGFRule subrule : ruleAlternatives.getRules()) {
                subrules[i] = convert(subrule);
                i++;
            }
            float[] weights = null;
            if (ruleAlternatives.getWeights() != null) {
                weights = new float[ruleAlternatives.getWeights().size()];
                i = 0;
                for (Float f : ruleAlternatives.getWeights()) {
                    weights[i] = f;
                    i++;
                }
            }
            return new RuleAlternatives(subrules, weights);
        }

        if (rule instanceof JSGFRuleSequence) {
            JSGFRuleSequence ruleSequence = (JSGFRuleSequence) rule;
            Rule[] subrules = new Rule[ruleSequence.getRules().size()];
            int i = 0;
            for (JSGFRule subrule : ruleSequence.getRules()) {
                subrules[i] = convert(subrule);
                i++;
            }
            return new RuleSequence(subrules);
        }

        if (rule instanceof JSGFRuleCount) {
            JSGFRuleCount ruleCount = (JSGFRuleCount) rule;
            return new RuleCount(convert(ruleCount.getRule()), ruleCount
                    .getCount());
        }

        if (rule instanceof JSGFRuleTag) {
            JSGFRuleTag ruleTag = (JSGFRuleTag) rule;
            return new RuleTag(convert(ruleTag.getRule()), ruleTag.getTag());
        }

        if (rule instanceof JSGFRuleToken) {
            JSGFRuleToken ruleToken = (JSGFRuleToken) rule;
            return new RuleToken(ruleToken.getText());
        }

        System.out.println("Unknown rule type " + rule.getClass());
        return null;
    }

    /**
     * Set a rule in the grammar either by creating a new rule or updating an
     * existing rule.
     * 
     * @param ruleName
     *            the name of the rule.
     * @param rule
     *            the definition of the rule.
     * @param isPublic
     *            whether this rule is public or not.
     */
    public void setRule(String ruleName, Rule rule, boolean isPublic)
            throws NullPointerException, IllegalArgumentException {
        jsgfGrammar.setRule(ruleName, convert(rule), isPublic);
        grammarChanged = true;
    }

    /**
     * Return a copy of the data structure for the named rule. From
     * javax.speech.recognition.RuleGrammar.
     * 
     * @param ruleName
     *            the name of the rule.
     */
    public Rule getRule(String ruleName) {
        return convert(jsgfGrammar.getRule(ruleName));
    }

    /**
     * Return the data structure for the named rule. From
     * javax.speech.recognition.RuleGrammar.
     * 
     * @param ruleName
     *            the name of the rule.
     */
    public Rule getRuleInternal(String ruleName) {
        return convert(jsgfGrammar.getRule(ruleName));
    }

    /**
     * Test whether the specified rule is public. From
     * javax.speech.recognition.RuleGrammar.
     * 
     * @param ruleName
     *            the name of the rule.
     */
    public boolean isRulePublic(String ruleName)
            throws IllegalArgumentException {
        return jsgfGrammar.isRulePublic(ruleName);
    }

    /**
     * List the names of all rules define in this Grammar. From
     * javax.speech.recognition.RuleGrammar.
     */
    public String[] listRuleNames() {
        Set<String> names = jsgfGrammar.getRuleNames();
        return names.toArray(new String[names.size()]);
    }

    /**
     * Delete a rule from the grammar. From
     * javax.speech.recognition.RuleGrammar.
     * 
     * @param ruleName
     *            the name of the rule.
     */
    public void deleteRule(String ruleName) throws IllegalArgumentException {
        jsgfGrammar.deleteRule(ruleName);
        grammarChanged = true;
    }

    /**
     * Set the enabled state of the listed rule. From
     * javax.speech.recognition.RuleGrammar.
     * 
     * @param ruleName
     *            the name of the rule.
     * @param enabled
     *            the new enabled state.
     */
    public void setEnabled(String ruleName, boolean enabled)
            throws IllegalArgumentException {
        jsgfGrammar.setEnabled(enabled);
    }

    /**
     * Set the enabled state of the listed rules. From
     * javax.speech.recognition.RuleGrammar.
     * 
     * @param ruleNames
     *            the names of the rules.
     * @param enabled
     *            the new enabled state.
     */
    public void setEnabled(String[] ruleNames, boolean enabled)
            throws IllegalArgumentException {
        for (String ruleName : ruleNames)
            setEnabled(ruleName, enabled);
    }

    /**
     * Return enabled state of rule. From javax.speech.recognition.RuleGrammar.
     * 
     * @param ruleName
     *            the name of the rule.
     */
    public boolean isEnabled(String ruleName) throws IllegalArgumentException {
        return jsgfGrammar.isEnabled(ruleName);
    }

    /**
     * Resolve a simple or qualified rulename as a full rulename. From
     * javax.speech.recognition.RuleGrammar.
     * 
     * @param ruleName
     *            the name of the rule.
     */
    public RuleName resolve(RuleName ruleName) throws GrammarException {
        JSGFRuleName jsgfRuleName = (JSGFRuleName) convert(ruleName);
        try {
            return (RuleName) convert(jsgfGrammar.resolve(jsgfRuleName));
        } catch (JSGFGrammarException e) {
            throw new GrammarException(e.getMessage());
        }
    }

    /**
     * Import all rules or a specified rule from another grammar. From
     * javax.speech.recognition.RuleGrammar.
     * 
     * @param importName
     *            the name of the rule(s) to import.
     */
    public void addImport(RuleName importName) {
        jsgfGrammar.addImport((JSGFRuleName) convert(importName));
        grammarChanged = true;
    }

    /**
     * Remove an import. From javax.speech.recognition.RuleGrammar.
     * 
     * @param importName
     *            the name of the rule(s) to remove.
     */
    public void removeImport(RuleName importName)
            throws IllegalArgumentException {
        jsgfGrammar.addImport((JSGFRuleName) convert(importName));
        grammarChanged = true;
    }

    /** List the current imports. From javax.speech.recognition.RuleGrammar. */
    public RuleName[] listImports() {
        assert jsgfGrammar != null;

        List<JSGFRuleName> imports = jsgfGrammar.getImports();
        RuleName[] result = new RuleName[imports.size()];
        for (int i = 0; i < imports.size(); i++) {
            result[i] = (RuleName) convert(imports.get(i));
        }
        return result;
    }

    /**
     * Parse the text string against the specified rule. Uses the RuleParser
     * class. From javax.speech.recognition.RuleGrammar.
     * 
     * @param text
     *            the text to parse.
     * @param ruleName
     *            the name of rule to use for parsing.
     */
    public RuleParse parse(String text, String ruleName)
            throws GrammarException {
        return RuleParser.parse(text, recognizer, this,
                ruleName == null ? ruleName : JSGFRuleName
                        .stripRuleName(ruleName));
    }

    /**
     * Parse the tokens string against the specified rule. Uses the RuleParser
     * class. From javax.speech.recognition.RuleGrammar.
     * 
     * @param tokens
     *            the tokens to parse.
     * @param ruleName
     *            the name of rule to use for parsing.
     */
    public RuleParse parse(String tokens[], String ruleName)
            throws GrammarException {
        return RuleParser.parse(tokens, recognizer, this,
                ruleName == null ? ruleName : JSGFRuleName
                        .stripRuleName(ruleName));
    }

    /**
     * Parse the nth best result of a FinalRuleResult against the specified
     * rule. Uses the RuleParser class. From
     * javax.speech.recognition.RuleGrammar.
     * 
     * @param r
     *            the FinalRuleResult.
     * @param nBest
     *            the nth best result to use.
     * @param ruleName
     *            the name of rule to use for parsing.
     */
    public RuleParse parse(FinalRuleResult r, int nBest, String ruleName)
            throws GrammarException {
        // Some JSAPI implementations we run into are not JSAPI complaint,
        // so try a few alternatives
        ResultToken rt[] = r.getAlternativeTokens(nBest);
        if (rt != null || (rt = r.getBestTokens()) != null) {
            String tokens[] = new String[rt.length];
            for (int i = 0; i < rt.length; i++) {
                tokens[i] = rt[i].getSpokenText();
            }
            return parse(tokens, ruleName);
        } else {
            return parse(r.toString(), ruleName);
        }
    }

    /**
     * Returns a string containing the specification for this grammar.
     * 
     * @return specification for this grammar.
     */
    public String toString() {
        return jsgfGrammar.toString();
    }

    /** Add a new RuleGrammar comment. */
    public void addRuleDocComment(String rname, String comment) {
        jsgfGrammar.addRuleDocComment(rname, comment);
    }

    /** Retrieve a RuleGrammar comment. */
    public String getRuleDocComment(String rname) {
        return jsgfGrammar.getRuleDocComment(rname);
    }

    /** Add a new import comment. */
    public void addImportDocComment(RuleName imp, String comment) {
        jsgfGrammar.addImportDocComment((JSGFRuleName) convert(imp), comment);
    }

    /** Retrieve an import comment. */
    public String getImportDocComment(RuleName imp) {
        return jsgfGrammar.getImportDocComment((JSGFRuleName) convert(imp));
    }

    /** Add the Grammar comment. */
    public void addGrammarDocComment(String comment) {
        jsgfGrammar.addGrammarDocComment(comment);
    }

    /** Retrieve the Grammar comment. */
    public String getGrammarDocComment() {
        return jsgfGrammar.getGrammarDocComment();
    }

    public boolean isRuleChanged(String ruleName) {
        return jsgfGrammar.isRuleChanged(ruleName);
    }

    public void setRuleChanged(String ruleName, boolean changed) {
        jsgfGrammar.setRuleChanged(ruleName, changed);
    }
}
