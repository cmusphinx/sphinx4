/* Copyright 1999,2004 Sun Microsystems, Inc.  
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.tools.tags;

import javax.speech.recognition.Rule;
import javax.speech.recognition.RuleName;
import javax.speech.recognition.RuleTag;
import javax.speech.recognition.RuleToken;
import javax.speech.recognition.RuleParse;
import javax.speech.recognition.RuleSequence;
import javax.speech.recognition.RuleAlternatives;

import java.util.StringTokenizer;

/**  
 * Utilities for handling ECMAScript embedded in JSGF RuleGrammar tags.
 * These utilities are not dependent upon any particular ECMAScript
 * implementation; they merely provide convenience methods for generating
 * ECMAScript scripts to pass to an ECMAScript implementation.
 *
 * @see ActionTagsParser
 */
public class ActionTagsUtilities {
    /**
     * The string used to refer to the "$tokens" field of a value.
     */
    final static protected String TOKENS = "$tokens";
    
    /**
     * The string used to refer to the "$phrase" field of a value.
     */
    final static protected String PHRASE = "$phrase";

    /**
     * The string used to refer to the "$value" field of a value.
     */
    final static protected String VALUE  = "$value";

    /**
     * The last rule determined when parsing the tags in a RuleParse
     * ("$").
     */
    final static protected String LAST_RULE   = "$";

    /**
     * The prefix used to refer to the previous evaluation of a
     * non-terminal ("$").
     */
    final static protected String RULE_PREFIX = "$";

    /**
     * The ECMAScript class name used to create objects for the
     * evaluation of non-terminals ("Rule").
     */
    final static protected String CLASS_NAME  = "Rule";

    /**
     * The ECMAScript method used to evaluate the script for a
     * non-terminal.
     */
    final static protected String METHOD_NAME = "__constructor";

    /**
     * Indentation for making the generated ECMAScript a little more
     * readable.
     */
    final static protected String INDENT = "    ";

    /**
     * If true, generate debug statements in the ECMAScript while
     * parsing the tags.
     */
    static private boolean debug = false;

    /**
     * Turn debugging on or off.  If debugging is turned on, the debug
     * method of the action tags parser will be called each time an
     * action tag is to be evaluated.
     *
     * @see #isDebugging
     */
    static public void setDebugging(boolean newDebug) {
        debug = newDebug;
    }
    
    /**
     * Is debug turned on?
     *
     * @see #setDebugging
     */
    static public boolean isDebugging() {
        return debug;
    }
    
    /**
     * Generate an ECMAScript script that defines the classes and
     * functions referenced by the String returned from the getScript method.
     * This script should be evaluated by the ActionTags parser before
     * evaluating scripts returned from the getScript method.  It is
     * a separate method to allow more flexibility in ActionTags parser
     * implementations.  For example, some parsers may wish to maintain an
     * ECMAScript context that is persistent between parses of RuleParse
     * instances.  In these cases, the parser needs to evaluate the
     * script returned by this method only once.  Other parsers may want
     * to generate a new context for each parse of a RuleParse instance.
     * In those cases, the parser will need to evaluate ths script returned
     * by this method each time it handles a new RuleParse instance.
     *
     * <p>Note that this does not evaluate the script.  It is up to the
     * ActionTags parser to do that.
     *
     * @see #getScript
     *
     * @return a String containing ECMAScript global class and function
     * definitions
     */
    static public String getClassDefinitions() {
	return
            "// Constructor for the " + CLASS_NAME + " class\n"
            + "function " + CLASS_NAME + "() {\n"
            + "    this." + TOKENS + " = new Array();\n"
            + "    this." + PHRASE + " = '';\n"
            + "}\n\n"
            
            + CLASS_NAME + ".prototype.valueOf = function() {\n"
            + "    var tmp = this.getValue();\n"
            + "    if (tmp == undefined)\n"
            + "        return this;\n"
            + "    else\n"
            + "        return tmp;\n"
            + "}\n\n"
            
            + CLASS_NAME + ".prototype.getValue = function() {\n"
            + "    return this." + VALUE + ";\n"
            + "}\n\n"
            
            + CLASS_NAME + ".prototype.setValue = function(v) {\n"
            + "    this." + VALUE + " = v;\n"
            + "}\n\n"
            
            + CLASS_NAME + ".prototype.toString = function() {\n"
            + "    return this.getPhrase();\n"
            + "}\n\n"
            
            + CLASS_NAME + ".prototype.addTokens = function(token, phrase) {\n"
            + "    this." + TOKENS + " = this." + TOKENS + ".concat(token);\n"
            + "    if (this." + PHRASE + " == '')\n"
            + "        this." + PHRASE + " = phrase;\n"
            + "    else\n"
            + "        this." + PHRASE + " = this." + PHRASE + ".concat(' ' + phrase);\n"
            + "}\n\n"
            
            + CLASS_NAME + ".prototype.getTokens = function() {\n"
            + "    return this." + TOKENS + ";\n"
            + "}\n\n"
            
            + CLASS_NAME + ".prototype.setTokens = function(v) {\n"
            + "    this." + TOKENS + " = v;\n"
            + "}\n\n"
            
            + CLASS_NAME + ".prototype.getPhrase = function() {\n"
            + "    return this." + PHRASE + ";\n"
            + "}\n\n"
            
            + CLASS_NAME + ".prototype.setPhrase = function(v) {\n"
            + "    this." + PHRASE + " = v;\n"
            + "}\n\n";
    }

    /**
     * Given a RuleParse, generate an executable ECMAScript script based
     * upon the RuleTags in the RuleParse.  The ECMAScript script returned
     * uses the definitions from the getClassDefinitions method.
     * As a result, it is important that the script returned from the
     * getClassDefinitions method is evaluated by the action tags parser
     * prior to evaluating the script returned from this method.
     *    
     * <p>Note that this does not evaluate the script.  It is up to the
     * action tags parser to do that.
     *
     * @see #getClassDefinitions
     *
     * @param ruleParse the RuleParse containing RuleTags with embedded
     * ECMAScript
     *
     * @return a String containing ECMAScript script based upon the RuleTags
     * in ruleParse
     */
    static public String getScript(RuleParse ruleParse) {
        StringBuffer script = new StringBuffer();
        parseTags(ruleParse, script, null, "", true);
        return script.toString();
    }
    
    /**
     * A debug utility to determine the flattened parse tree of a
     * RuleParse instance.  This can be useful for debugging ECMAScript
     * tags in a grammar.
     *
     * @param ruleParse the RuleParse containing RuleTags with embedded
     * ECMAScript
     *
     * @return a printable String containing the flattened parse tree
     * of a RuleParse instance.
     */
    static public String getParseTree(RuleParse ruleParse) {
        StringBuffer parseTree = new StringBuffer();
        parseTags(ruleParse, null, parseTree, "", true);
        return parseTree.toString();
    }

    /**
     * Method that returns the debug script for a tag.
     *
     * @see #isDebugging
     * @see #setDebugging
     */
    static protected String getDebugScript(String indent, String tag) {
        StringBuffer script = new StringBuffer();
        
        /* Convert all quotes in the tag to escaped quotes.  Otherwise,
         * ECMAScript will treat them as quotes.
         */
        StringTokenizer st = new StringTokenizer(tag.trim(),"\"");
        StringBuffer sb = new StringBuffer();
        if (st.hasMoreTokens()) {
            sb.append(st.nextToken());
            while (st.hasMoreTokens()) {
                sb.append("\\\"");
                sb.append(st.nextToken());
            }
        }
        script.append(indent + "//vvvvvvvvvvvvvv DEBUG vvvvvvvvvvvvvv//\n");
        //[[[WDW - cannot break lines apart easily because block statements
        //do not get handled properly.]]]
        //
        st = new StringTokenizer(sb.toString(),"\n");
        sb = new StringBuffer();
        sb.append(st.nextToken());
        while (st.hasMoreTokens()) {
            sb.append("\\n");
            sb.append(st.nextToken());
        }
        //String line = sb.toString().replace('\n',' ');
        String line = sb.toString();
        script.append(
            indent
            + "var dbgStr = debug(\"<\" + this.$name + \"> "
            + line + "\");\n");
        script.append(indent + "while (dbgStr != null) {\n");
        script.append(indent + INDENT + "print(\"dbgStr = \" + dbgStr);\n");
        script.append(indent + INDENT
                      + "if (dbgStr == \"skip\") {\n");
        script.append(indent + INDENT + INDENT
                      + "dbgStr = null;\n");
        script.append(indent + INDENT
                      + "} else if (dbgStr == \"step\") {\n");
        script.append(indent + INDENT + INDENT
                      + "//vvvvvvvvvv FROM GRAMMAR vvvvvvvvvv\n");
        script.append(indent + INDENT + INDENT
                      + tag.trim() + "\n");
        script.append(indent + INDENT + INDENT
                      + "//^^^^^^^^^^ FROM GRAMMAR ^^^^^^^^^^\n");
        script.append(indent + INDENT + INDENT
                      + "dbgStr = null;\n");
        script.append(indent + INDENT
                      + "} else {\n");
        script.append(indent + INDENT + INDENT
                      + "dbgStr = debug(eval(dbgStr));\n");
        script.append(indent + INDENT + "}\n");
        script.append(indent + "}\n");
        script.append(indent + "//^^^^^^^^^^^^^^ DEBUG ^^^^^^^^^^^^^^//\n");
        return script.toString();
    }
    
    /**
     * The method that actually does all the work.  This is a recursive
     * method that continually appends ECMAScript to the script passed in.
     *
     * @param rule the Rule containing RuleTags with embedded ECMAScript
     * @param script the ECMAScript as it is appears so far
     * @param parseTree the flattened parse tree as it appears so far
     * @param indent the indentation to make the generated ECMAScript look
     * a little prettier
     * @param root is this the top level object?
     */
    static protected void parseTags(Rule rule,
                                    StringBuffer script,
                                    StringBuffer parseTree,
                                    String indent,
                                    boolean root) {
	if (rule instanceof RuleParse) {
	    RuleParse p = (RuleParse) rule;
	    String simpleRuleName = p.getRuleName().getSimpleRuleName();
	    String ruleName = p.getRuleName().getRuleName();
            String objName = RULE_PREFIX + simpleRuleName;

            if (parseTree != null) {
                parseTree.append(indent + "<" + ruleName + ">\n");
            }
            
            if (script != null) {
                script.append(
                    indent
                    + "var " + LAST_RULE + ";\n");
                script.append(
                    indent
                    + "var " + objName + " = new " + CLASS_NAME + "();\n");
                script.append(
                    indent
                    + objName + "." + METHOD_NAME + " = function() {\n");
                script.append(
                    indent + INDENT
                    + "this.$name = \"" + objName + "\";\n");
            }
            
	    parseTags(p.getRule(), script, parseTree, indent + INDENT, false);
            
            if (script != null) {
                script.append(
                    indent + "    if (this.$value == undefined) {\n");
                script.append(
                    indent + "        this.$value = this.getPhrase();\n");
                script.append(
                    indent + "    }\n");
                
                script.append(indent + "}\n");
                script.append(indent + objName + "." + METHOD_NAME + "();\n");
                script.append(indent + LAST_RULE + " = " + objName + ";\n");
                
                if (!root) {
                    script.append(indent + "this.addTokens("
                                  + LAST_RULE + ".getTokens(), "
                                  + LAST_RULE + ".getPhrase()"
                                  + ");\n");
                } else {
                    //script.append(indent + objName + " = undefined;\n");
                    script.append(indent + "delete " + objName + ";\n");
                }
            }
	} else if ((rule instanceof RuleSequence) 
                   || (rule instanceof RuleAlternatives)) {
            Rule rules[];
            if (rule instanceof RuleSequence) {
                rules = ((RuleSequence) rule).getRules();
            } else {
                rules = ((RuleAlternatives) rule).getRules();
	    }
            if (rules != null) {
		for (int i = 0; i < rules.length; i++) {
		    parseTags(rules[i], script, parseTree, indent, false);
		}
            }   
	} else if (rule instanceof RuleToken) {
	    String t = "'" + ((RuleToken) rule).getText() + "'";
            if (parseTree != null) {
                parseTree.append(indent + t + "\n");
            }
            if (script != null) {
                script.append(
                    indent + "this.addTokens(" + t + "," + t + ");\n");
            }
	} else if (rule instanceof RuleTag) {
	    parseTags(((RuleTag) rule).getRule(),
                      script, parseTree, indent, false);
            String tag = ((RuleTag) rule).getTag();
            if (parseTree != null) {
                parseTree.append(indent + "{" + tag + "}\n");
            }
            if (script != null) {
                if (isDebugging()) {
                    script.append(getDebugScript(indent,tag));
                } else {
                    script.append(indent + INDENT
                      + "//vvvvvvvvvv FROM GRAMMAR vvvvvvvvvv\n");
                    script.append(indent + "{" + tag + "}\n");
                    script.append(indent + INDENT
                      + "//^^^^^^^^^^ FROM GRAMMAR ^^^^^^^^^^\n");
                }
            }
	} else if (rule instanceof RuleName) {
            if (parseTree != null) {
                String name = ((RuleName) rule).getRuleName();
                if (!name.equals("NULL") && !name.equals("VOID")) {
                    parseTree.append(indent + name + "\n");
                }
            }
        } else {
	    throw new RuntimeException("Unexected rule type "
                                       + rule.getClass().getName());
	}
    }
}
