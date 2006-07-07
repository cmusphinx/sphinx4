/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine.recognition;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import javax.speech.recognition.FinalRuleResult;
import javax.speech.recognition.GrammarException;
import javax.speech.recognition.ResultToken;
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
 * Implementation of javax.speech.recognition.RuleGrammar.
 *
 * @version 1.8 11/04/99 14:54:02
 */
public class BaseRuleGrammar extends BaseGrammar
    implements RuleGrammar, Serializable 
{
    protected Hashtable rules;
    protected Vector    imports;    
    protected Vector    importedRules;

    /**
     * Create a new BaseRuleGrammar
     * @param R the BaseRecognizer for this Grammar.
     * @param name the name of this Grammar.
     */
    public BaseRuleGrammar(BaseRecognizer R, String name) {
        super(R,name);
        rules = new Hashtable();
        imports = new Vector();
        importedRules = new Vector();
    }

//////////////////////
// Begin overridden Grammar Methods
//////////////////////
    /**
     * Set the enabled property of the Grammar.
     * From javax.speech.recognition.Grammar.
     * @param enabled the new desired state of the enabled property.
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
	if (rules == null) {
	    return;
	}
        Enumeration e = rules.elements();
        while (e.hasMoreElements()) {
            GRule g = (GRule) e.nextElement();
            if (g.isPublic) {
                g.isEnabled = enabled;
            }
        }
    }
//////////////////////
// End overridden Grammar Methods
//////////////////////

//////////////////////
// Begin RuleGrammar Methods
//////////////////////
    /**
     * Parse partial JSGF text to a Rule object.
     * From javax.speech.recognition.RuleGrammar.
     */
    public Rule ruleForJSGF(String text) {
        return JSGFParser.ruleForJSGF(text);
    }

    /**
     * Set a rule in the grammar either by creating a new rule or
     * updating an existing rule.
     * @param ruleName the name of the rule.
     * @param rule the definition of the rule.
     * @param isPublic whether this rule is public or not.
     */
    public void setRule(String ruleName, Rule rule, boolean isPublic)
        throws NullPointerException, IllegalArgumentException 
    {
        GRule r = new GRule();
        r.ruleName = stripRuleName(ruleName);

        // Copy and convert all RuleName's to BaseRuleName's so we
        // can keep track of the fully resolved name.
        r.rule = BaseRuleName.copyForBaseRuleName(rule);

        r.isPublic = isPublic;
        r.isEnabled = false;
        r.hasChanged = true;
        rules.put(stripRuleName(ruleName),r);
        grammarChanged=true;
    }

    /**
     * Return a copy of the data structure for the named rule.
     * From javax.speech.recognition.RuleGrammar.
     * @param ruleName the name of the rule.
     */
    public Rule getRule(String ruleName) {
        GRule r = (GRule)rules.get(stripRuleName(ruleName));
        if (r == null) {
            return null;
        }
        return r.rule.copy(); 
    }

    /**
     * Return the data structure for the named rule.
     * From javax.speech.recognition.RuleGrammar.
     * @param ruleName the name of the rule.
     */
    public Rule getRuleInternal(String ruleName) {
        GRule r = (GRule)rules.get(stripRuleName(ruleName));
        if (r == null) {
            return null;
        }
        return r.rule; 
    }

    /** 
     * Test whether the specified rule is public.
     * From javax.speech.recognition.RuleGrammar.
     * @param ruleName the name of the rule.
     */
    public boolean isRulePublic(String ruleName)
        throws IllegalArgumentException 
    {
        GRule r = (GRule)rules.get(stripRuleName(ruleName));
        if (r == null) {
            throw new IllegalArgumentException("Unknown Rule: " + ruleName);
        } else {
            return r.isPublic;
        }
    }
    
    /**
     * List the names of all rules define in this Grammar.
     * From javax.speech.recognition.RuleGrammar.
     */
    public String[] listRuleNames() {
	if (rules == null) {
	    return new String[0];
	}
        String rn[] = new String[rules.size()];
        int i=0;
        Enumeration e = rules.elements();
        while (e.hasMoreElements()) {
            GRule g = (GRule) e.nextElement();
            rn[i++] = g.ruleName; 
        }
        return rn;
    }

    /** 
     * Delete a rule from the grammar.
     * From javax.speech.recognition.RuleGrammar.
     * @param ruleName the name of the rule.
     */
    public void deleteRule(String ruleName)
        throws IllegalArgumentException 
    {
        GRule r = (GRule)rules.get(stripRuleName(ruleName));
        if (r == null) {
            throw new IllegalArgumentException("Unknown Rule: " + ruleName);
        } else {
            rules.remove(ruleName);
        }
        grammarChanged=true;
    }
    
    /**
     * Set the enabled state of the listed rule.
     * From javax.speech.recognition.RuleGrammar.
     * @param ruleName the name of the rule.
     * @param enabled the new enabled state.
     */
    public void setEnabled(String ruleName, boolean enabled)
        throws IllegalArgumentException 
    {
        GRule r = (GRule)rules.get(stripRuleName(ruleName));
        if (r == null) {
            throw new IllegalArgumentException("Unknown Rule: " + ruleName);
        } else if (r.isEnabled != enabled) {
            r.isEnabled = enabled;
            //sjagrammarChanged=true;
        }
    }

    /**
     * Set the enabled state of the listed rules.
     * From javax.speech.recognition.RuleGrammar.
     * @param ruleNames the names of the rules.
     * @param enabled the new enabled state.
     */
    public void setEnabled(String[] ruleNames, boolean enabled)
        throws IllegalArgumentException 
    {
        for (int i=0; i < ruleNames.length; i++) {
            GRule r = (GRule)rules.get(stripRuleName(ruleNames[i]));
            if (r == null) {
                throw new IllegalArgumentException("Unknown Rule: " + 
                                                   ruleNames[i]);
            } else if (r.isEnabled != enabled) {
                r.isEnabled = enabled;
                //sjagrammarChanged=true;
            }
        }
    }

    /**
     * Return enabled state of rule.
     * From javax.speech.recognition.RuleGrammar.
     * @param ruleName the name of the rule.
     */
    public boolean isEnabled(String ruleName)
        throws IllegalArgumentException 
    {
        GRule r = (GRule)rules.get(stripRuleName(ruleName));
        if (r == null) {
            throw new IllegalArgumentException("Unknown Rule: " + ruleName);
        } else {
            return r.isEnabled;
        }
    }   
        
    /**
     * Resolve a simple or qualified rulename as a full rulename.
     * From javax.speech.recognition.RuleGrammar.
     * @param ruleName the name of the rule.
     */
    public RuleName resolve(RuleName name) 
        throws GrammarException
    {
        RuleName rn = new RuleName(name.getRuleName());

        String simpleName = rn.getSimpleRuleName();
        String grammarName = rn.getSimpleGrammarName();
        String packageName = rn.getPackageName();
        String fullGrammarName = rn.getFullGrammarName();
        
        // Check for badly formed RuleName
        if (packageName != null && grammarName == null) {
            throw new GrammarException("Error: badly formed rulename " + rn, 
                                       null);
        }
	
	if (name.getSimpleRuleName().equals("NULL")) {
	  return RuleName.NULL;
	}

	if (name.getSimpleRuleName().equals("VOID")) {
	  return RuleName.VOID;
	}

        // Check simple case: a local rule reference
        if (fullGrammarName == null && this.getRule(simpleName) != null) {
            return new RuleName(myName + "." + simpleName);
        }

        // Check for fully-qualified reference
        if (fullGrammarName != null) {
            RuleGrammar g = myRec.getRuleGrammar(fullGrammarName);
            if (g != null) {
                if (g.getRule(simpleName) != null) {
                    // we have a successful resolution
                    return new RuleName(fullGrammarName + "." + simpleName);
                }
            }
        }

        // Collect all matching imports into a vector.  After trying to
        // match rn to each import statement the vec will have
        // size()=0 if rn is unresolvable
        // size()=1 if rn is properly resolvable
        // size()>1 if rn is an ambiguous reference
        Vector matches = new Vector();
        
        
        // Get list of imports
        // Add local grammar to simply the case of checking for
        // a qualified or fully-qualified local reference.
        RuleName imports[] = listImports();
        
        if (imports == null) {
            imports = new RuleName[1];
            imports[0] = new RuleName(getName() + ".*");
        } else {
            RuleName[] tmp = new RuleName[imports.length + 1];
            System.arraycopy(imports, 0, tmp, 0, imports.length);
            tmp[imports.length] = new RuleName(getName() + ".*");
            imports = tmp;
        }
    
        // Check each import statement for a possible match
        //
        for(int i=0; i<imports.length; i++) { 
            // TO-DO: update for JSAPI 1.0
            String iSimpleName = imports[i].getSimpleRuleName();
            String iGrammarName = imports[i].getSimpleGrammarName();
            String iPackageName = imports[i].getPackageName();
            String iFullGrammarName = imports[i].getFullGrammarName();
      
            // Check for badly formed import name
            if (iFullGrammarName == null)
                throw new GrammarException("Error: badly formed import " + 
                                           imports[i], null);
      
            // Get the imported grammar
            RuleGrammar gref = myRec.getRuleGrammar(iFullGrammarName);
            if (gref == null) {
                RecognizerUtilities.debugMessageOut(
                    "Warning: import of unknown grammar " +
                    imports[i] + " in " + getName());
                continue;
            }
      
            // If import includes simpleName, test that it really exists
            if (!iSimpleName.equals("*") 
                && gref.getRule(iSimpleName) == null) {
                RecognizerUtilities.debugMessageOut(
                    "Warning: import of undefined rule " + 
                    imports[i] + " in " + getName());
                continue;
            } 
      
            // Check for fully-qualified or qualified reference
            if (iFullGrammarName.equals(fullGrammarName)
                || iGrammarName.equals(fullGrammarName)) {
                // Know that either
                //    import <ipkg.igram.???> matches <pkg.gram.???>
                // OR
                //    import <ipkg.igram.???> matches <gram.???>
                // (ipkg may be null)
	
                if (iSimpleName.equals("*")) {
                    if (gref.getRule(simpleName) != null) {
                        // import <pkg.gram.*> matches <pkg.gram.rulename>
                        matches.addElement(
                            new RuleName(iFullGrammarName + "." + simpleName));
                    }
                    continue;
                } else {
                    // Now testing
                    //    import <ipkg.igram.iRuleName> against <??.gram.ruleName>
                    //
                    if (iSimpleName.equals(simpleName)) {
                        // import <pkg.gram.rulename> exact match for <???.gram.rulename>
                        matches.addElement(new RuleName(iFullGrammarName + 
                                                        "." + simpleName));
                    }
                    continue;
                }
            }
      
            // If we get here and rn is qualified or fully-qualified 
            // then the match failed - try the next import statement
            if (fullGrammarName != null) {
                continue;
            }
      
            // Now test
            //    import <ipkg.igram.*> against <simpleName>
      
            if (iSimpleName.equals("*")) {
                if (gref.getRule(simpleName) != null) {
                    // import <pkg.gram.*> matches <simpleName>
                    matches.addElement(new RuleName(iFullGrammarName + 
                                                    "." + simpleName));
                }
                continue;
            }
      
            // Finally test
            //    import <ipkg.igram.iSimpleName> against <simpleName>
            
            if (iSimpleName.equals(simpleName)) {
                matches.addElement(new RuleName(iFullGrammarName + 
                                                "." + simpleName));
                continue;
            }
        }

        //
        // The return behavior depends upon number of matches
        //
        if (matches.size() == 0) {
            // Return null if rulename is unresolvable
            return null;
        } else if (matches.size() > 1) {
            // Throw exception if ambiguous reference
            StringBuffer b = new StringBuffer();
            b.append("Warning: ambiguous reference " + rn + " in " + 
                     getName() + " to ");
            for (int i = 0; i<matches.size(); i++) {
                RuleName tmp = (RuleName)(matches.elementAt(i));
                if (i > 0) {
                    b.append(" and ");
                }
                b.append(tmp);
            }
            throw new GrammarException(b.toString(), null);
        } else {
            // Return successfully
            return (RuleName)(matches.elementAt(0));
        }
    }

    /**
     * Import all rules or a specified rule from another grammar.
     * From javax.speech.recognition.RuleGrammar.
     * @param importName the name of the rule(s) to import.
     */
    public void addImport(RuleName importName) {
        if (!imports.contains(importName)) {
            imports.addElement(importName);
            grammarChanged=true;
        }
    }
  
    /**
     * Remove an import.
     * From javax.speech.recognition.RuleGrammar.
     * @param importName the name of the rule(s) to remove.
     */
    public void removeImport(RuleName importName)
        throws IllegalArgumentException 
    {
        if (imports.contains(importName)) {
            imports.removeElement(importName);
            grammarChanged=true;
        }
    }

    /**
     * List the current imports.
     * From javax.speech.recognition.RuleGrammar.
     */
    public RuleName[] listImports() {
	if (imports == null) {
	    return new RuleName[0];
	}
        RuleName rn[] = new RuleName[imports.size()];
        int i=0;
        Enumeration e = imports.elements();
        while (e.hasMoreElements()) {
            RuleName r = (RuleName) e.nextElement();
            rn[i++] = r;
        }
        return rn;
    }

    /**
     * Parse the text string against the specified rule.
     * Uses the RuleParser class.
     * From javax.speech.recognition.RuleGrammar.
     * @param text the text to parse.
     * @param ruleName the name of rule to use for parsing.
     */
    public RuleParse parse(String text, String ruleName) 
        throws GrammarException
    {
        if (ruleName != null) {
            ruleName = stripRuleName(ruleName);
        }
        return RuleParser.parse(text,myRec,this,ruleName);
    }
    
    /**
     * Parse the tokens string against the specified rule.
     * Uses the RuleParser class.
     * From javax.speech.recognition.RuleGrammar.
     * @param tokens the tokens to parse.
     * @param ruleName the name of rule to use for parsing.
     */
    public RuleParse parse(String tokens[], String ruleName)
        throws GrammarException
    {
        if (ruleName != null) {
            ruleName = stripRuleName(ruleName);
        }
        return RuleParser.parse(tokens,myRec,this,ruleName);
    }

    /**
     * Parse the nth best result of a FinalRuleResult against the specified 
     * rule.
     * Uses the RuleParser class.
     * From javax.speech.recognition.RuleGrammar.
     * @param r the FinalRuleResult.
     * @param nBest the nth best result to use.
     * @param ruleName the name of rule to use for parsing.
     */
    public RuleParse parse(FinalRuleResult r, int nBest, String ruleName)
        throws GrammarException
    {
        // Some JSAPI implementations we run into are not JSAPI compliant,
        // so try a few alternatives
        ResultToken rt[] = r.getAlternativeTokens(nBest);
        if (rt != null || (rt = r.getBestTokens()) != null) {
            String tokens[] = new String[rt.length];
            for (int i=0; i<rt.length; i++) {
                tokens[i] = rt[i].getSpokenText();
            }
            return parse(tokens, ruleName);
        } else {
            return parse(r.toString(), ruleName);
        }
    }

    /**
     * NOT IMPLEMENTED YET.
     * Return a String containing the specification for this Grammar.
     */
    public String toString() {
        throw new RuntimeException(
            "toString not yet implemented.");
    }
//////////////////////
// End RuleGrammar Methods
//////////////////////

//////////////////////
// NON-JSAPI METHODS
//////////////////////
    /**
     * Marked a rulename as changed.
     */
    protected void setRuleChanged(String ruleName, boolean x) {
        GRule r = (GRule)rules.get(stripRuleName(ruleName));
        if (r == null) {
            return;
        }
        r.hasChanged=x;
        grammarChanged=true;
    }
  
    /**
     * Resolve and linkup all rule references contained in all rules.
     */
    protected void resolveAllRules()
        throws GrammarException
    {
        StringBuffer b = new StringBuffer();

        // First make sure that all imports are resolvable
        RuleName imports[] = listImports();

        if (imports != null) {
            for(int i=0; i<imports.length; i++) {
                String gname = imports[i].getFullGrammarName();
                RuleGrammar GI = myRec.getRuleGrammar(gname);
                if (GI == null) {   
                    b.append("Undefined grammar " + gname + 
                             " imported in " + getName() + "\n");
                }
            }
        }
        if (b.length() > 0) {
            throw new GrammarException(b.toString(), null);
        }
        
        String rn[] = listRuleNames();
        for(int i=0; i<rn.length; i++) {
            resolveRule(getRuleInternal(rn[i]));
        }
    }

    /**
     * Resolve the given rule.
     */
    protected void resolveRule(Rule r)
        throws GrammarException
    {

      if (r instanceof RuleToken) {
            return;
        }
    
        if (r instanceof RuleAlternatives) {
            RuleAlternatives ra = (RuleAlternatives)r;
            Rule array[] = ra.getRules();
            for(int i=0; i<array.length; i++) {
                resolveRule(array[i]);
            }
            return;
        }

        if (r instanceof RuleSequence) {
            RuleSequence rs = (RuleSequence)r;
            Rule array[] = rs.getRules();
            for(int i=0; i<array.length; i++) {
                resolveRule(array[i]);
            }
            return;
        }
    
        if (r instanceof RuleCount) {
            RuleCount rc = (RuleCount) r;
            resolveRule(rc.getRule());
            return;
        }

        if (r instanceof RuleTag) {
            RuleTag rt = (RuleTag) r;
            resolveRule(rt.getRule());
            return;
        }
    
        if (r instanceof BaseRuleName) {
            BaseRuleName rn = (BaseRuleName) r;
            RuleName resolved = resolve(rn);

            if (resolved == null) {
                throw new GrammarException(
                    "Unresolvable rulename in grammar " + 
                    getName() + ": " + rn.toString(), null);
            } else {
                //[[[WDW - This forces all rule names to be fully resolved.
                //This should be changed.]]]
                rn.resolvedRuleName = resolved.getRuleName();
                rn.setRuleName(resolved.getRuleName());
                return;
            }
        }

        throw new GrammarException("Unknown rule type", null);
    }

    /**
     * Add a rule as imported.
     */
    protected void setRuleIsImported(String simpleName) {
        if (!importedRules.contains(simpleName)) {
            importedRules.addElement(simpleName);
        }
    }
    
    /**
     * See if a rule has changed.
     */
    protected boolean isRuleChanged(String ruleName) {
        GRule r = (GRule)rules.get(stripRuleName(ruleName));
        if (r == null) { 
            return false;
        }
        return r.hasChanged;
    }
    
    /** 
     * Storage for documentation comments for rules for jsgfdoc. 
     */
    Properties ruleDocComments = new Properties();

    /** 
     * Storage for documentation comments for imports for jsgfdoc. 
     */
    Properties importDocComments = new Properties();

    /** 
     * Storage for documentation comments for the grammar for jsgfdoc. 
     */
    String grammarDocComment = null;

    /**
     * Add a new RuleGrammar comment.
     */
    void addRuleDocComment(String rname, String comment) { 
        ruleDocComments.put(rname, comment); 
    }

    /**
     * Retrieve a RuleGrammar comment.
     */
    public String getRuleDocComment(String rname) { 
        return ruleDocComments.getProperty(rname, null); 
    }

    /**
     * Add a new import comment.
     */
    void addImportDocComment(RuleName imp, String comment) {
        importDocComments.put(imp.toString(), comment); 
    }

    /**
     * Retrieve an import comment.
     */
    public String getImportDocComment(RuleName imp) { 
        return importDocComments.getProperty(imp.toString(), null); 
    }

    /**
     * Add the Grammar comment.
     */
    void addGrammarDocComment(String comment) { 
        grammarDocComment = comment; 
    }

    /**
     * Retrieve the Grammar comment.
     */
    public String getGrammarDocComment() { 
        return grammarDocComment; 
    }

    /**
     * annote the specified rule with its line number from the
     * source file. This is used for debugging purposes.
     */
    void setSourceLine(String rname,int i) {
        GRule r = (GRule)rules.get(stripRuleName(rname));
        /* TODO : exception */
        if (r == null) {
            return;
        }
        r.lineno=i;
    }
    
    /**
     * get the line number in the source file for the specified rule name
     */
    public int getSourceLine(String rname) {
        GRule r = (GRule)rules.get(stripRuleName(rname));
        /* TODO : exception */
        if (r == null) {
            return 0;
        }
        return r.lineno;
    }
    
    /**
     * add a sample sentence to the list of sample sentences that
     * go with the specified rule
     */
    void addSampleSentence(String rname,String sample) {
        GRule r = (GRule)rules.get(stripRuleName(rname));
        /* TODO : exception */
        if (r == null) {
            return;
        }
        r.samples.addElement(sample);
    }
    
    /**
     * add a sample sentence to the list of sample sentences that
     * go with the specified rule
     */
    void setSampleSentences(String rname,Vector samps) {
        GRule r = (GRule)rules.get(stripRuleName(rname));
        /* TODO : exception */
        if (r == null) {
            return;
        }
        r.samples = samps;
    }
    
    /**
     * get the list of sample sentences that
     * go with the specified rule
     */
    public Vector getSampleSentences(String rname) {
        GRule r = (GRule)rules.get(stripRuleName(rname));
        if (r == null) {
            return null;
        }
        return r.samples;
    }
    
    /**
     * Non-JSAPI method
     * returns a unique 32-bit id for the specified rule
     */
    public int getRuleID(String rname) {
        GRule r = (GRule)rules.get(stripRuleName(rname));
        if (r == null) {
            return -1; 
        }
        return r.hashCode();
    }
    
    /**
     * Remove a pair of leading/trailing angle brackets <>.
     */
    protected String stripRuleName(String n)
    {
        if (n.startsWith("<") && n.endsWith(">")) {
            return n.substring(1, n.length()-1);
        }
        return n;
    }
    
    /** 
     * Internal class used to hold rules and their attributes
     */
    class GRule implements Serializable {
        String ruleName;
        Rule rule;
        boolean isPublic;
        boolean isEnabled;
        boolean hasChanged;
        Vector samples = new Vector();
        int lineno = 0;
    }
}

