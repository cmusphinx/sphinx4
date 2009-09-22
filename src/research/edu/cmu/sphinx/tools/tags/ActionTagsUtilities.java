/* Copyright 1999,2004 Sun Microsystems, Inc.  
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.tools.tags;

import javax.speech.recognition.*;
import java.util.StringTokenizer;

/**
 * Utilities for handling ECMAScript embedded in JSGF RuleGrammar tags. These utilities are not dependent upon any
 * particular ECMAScript implementation; they merely provide convenience methods for generating ECMAScript scripts to
 * pass to an ECMAScript implementation.
 *
 * @see ActionTagsParser
 */
public class ActionTagsUtilities {

    /** The string used to refer to the "$tokens" field of a value. */
    final static protected String TOKENS = "$tokens";

    /** The string used to refer to the "$phrase" field of a value. */
    final static protected String PHRASE = "$phrase";

    /** The string used to refer to the "$value" field of a value. */
    final static protected String VALUE = "$value";

    /** The last rule determined when parsing the tags in a RuleParse ("$"). */
    final static protected String LAST_RULE = "$";

    /** The prefix used to refer to the previous evaluation of a non-terminal ("$"). */
    final static protected String RULE_PREFIX = "$";

    /** The ECMAScript class name used to create objects for the evaluation of non-terminals ("Rule"). */
    final static protected String CLASS_NAME = "Rule";

    /** The ECMAScript method used to evaluate the script for a non-terminal. */
    final static protected String METHOD_NAME = "__constructor";

    /** Indentation for making the generated ECMAScript a little more readable. */
    final static protected String INDENT = "    ";

    /** If true, generate debug statements in the ECMAScript while parsing the tags. */
    static private boolean debug;


    /**
     * Turn debugging on or off.  If debugging is turned on, the debug method of the action tags parser will be called
     * each time an action tag is to be evaluated.
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
     * Generate an ECMAScript script that defines the classes and functions referenced by the String returned from the
     * getScript method. This script should be evaluated by the ActionTags parser before evaluating scripts returned
     * from the getScript method.  It is a separate method to allow more flexibility in ActionTags parser
     * implementations.  For example, some parsers may wish to maintain an ECMAScript context that is persistent between
     * parses of RuleParse instances.  In these cases, the parser needs to evaluate the script returned by this method
     * only once.  Other parsers may want to generate a new context for each parse of a RuleParse instance. In those
     * cases, the parser will need to evaluate ths script returned by this method each time it handles a new RuleParse
     * instance.
     * <p/>
     * <p>Note that this does not evaluate the script.  It is up to the ActionTags parser to do that.
     *
     * @return a String containing ECMAScript global class and function definitions
     * @see #getScript
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
     * Given a RuleParse, generate an executable ECMAScript script based upon the RuleTags in the RuleParse.  The
     * ECMAScript script returned uses the definitions from the getClassDefinitions method. As a result, it is important
     * that the script returned from the getClassDefinitions method is evaluated by the action tags parser prior to
     * evaluating the script returned from this method.
     * <p/>
     * <p>Note that this does not evaluate the script.  It is up to the action tags parser to do that.
     *
     * @param ruleParse the RuleParse containing RuleTags with embedded ECMAScript
     * @return a String containing ECMAScript script based upon the RuleTags in ruleParse
     * @see #getClassDefinitions
     */
    static public String getScript(RuleParse ruleParse) {
        StringBuilder script = new StringBuilder();
        parseTags(ruleParse, script, null, "", true);
        return script.toString();
    }


    /**
     * A debug utility to determine the flattened parse tree of a RuleParse instance.  This can be useful for debugging
     * ECMAScript tags in a grammar.
     *
     * @param ruleParse the RuleParse containing RuleTags with embedded ECMAScript
     * @return a printable String containing the flattened parse tree of a RuleParse instance.
     */
    static public String getParseTree(RuleParse ruleParse) {
        StringBuilder parseTree = new StringBuilder();
        parseTags(ruleParse, null, parseTree, "", true);
        return parseTree.toString();
    }


    /**
     * Method that appends the debug script for a tag to a StringBuilder.
     *
     * @see #isDebugging
     * @see #setDebugging
     */
    static protected void appendDebugScript(StringBuilder script, String indent, String tag) {
        /* Convert all quotes in the tag to escaped quotes.  Otherwise,
        * ECMAScript will treat them as quotes.
        */
        StringTokenizer st = new StringTokenizer(tag.trim(), "\"");
        StringBuilder sb = new StringBuilder();
        if (st.hasMoreTokens()) {
            sb.append(st.nextToken());
            while (st.hasMoreTokens()) {
                sb.append("\\\"");
                sb.append(st.nextToken());
            }
        }
        script.append(indent).append("//vvvvvvvvvvvvvv DEBUG vvvvvvvvvvvvvv//\n");
        //[[[WDW - cannot break lines apart easily because block statements
        //do not get handled properly.]]]
        //
        st = new StringTokenizer(sb.toString(), "\n");
        sb = new StringBuilder();
        sb.append(st.nextToken());
        while (st.hasMoreTokens()) {
            sb.append("\\n");
            sb.append(st.nextToken());
        }
        //sb = new StringBuilder(sb.toString().replace('\n',' '));
        script.append(indent).append("var dbgStr = debug(\"<\" + this.$name + \"> ").append(sb).append("\");\n");
        script.append(indent).append("while (dbgStr != null) {\n");
        script.append(indent).append(INDENT + "print(\"dbgStr = \" + dbgStr);\n");
        script.append(indent).append(INDENT + "if (dbgStr == \"skip\") {\n");
        script.append(indent).append(INDENT + INDENT + "dbgStr = null;\n");
        script.append(indent).append(INDENT + "} else if (dbgStr == \"step\") {\n");
        script.append(indent).append(INDENT + INDENT + "//vvvvvvvvvv FROM GRAMMAR vvvvvvvvvv\n");
        script.append(indent).append(INDENT + INDENT).append(tag.trim()).append('\n');
        script.append(indent).append(INDENT + INDENT + "//^^^^^^^^^^ FROM GRAMMAR ^^^^^^^^^^\n");
        script.append(indent).append(INDENT + INDENT + "dbgStr = null;\n");
        script.append(indent).append(INDENT + "} else {\n");
        script.append(indent).append(INDENT + INDENT + "dbgStr = debug(eval(dbgStr));\n");
        script.append(indent).append(INDENT + "}\n");
        script.append(indent).append("}\n");
        script.append(indent).append("//^^^^^^^^^^^^^^ DEBUG ^^^^^^^^^^^^^^//\n");
    }


    /**
     * The method that actually does all the work.  This is a recursive method that continually appends ECMAScript to
     * the script passed in.
     *
     * @param rule      the Rule containing RuleTags with embedded ECMAScript
     * @param script    the ECMAScript as it is appears so far
     * @param parseTree the flattened parse tree as it appears so far
     * @param indent    the indentation to make the generated ECMAScript look a little prettier
     * @param root      is this the top level object?
     */
    static protected void parseTags(Rule rule,
                                    StringBuilder script,
                                    StringBuilder parseTree,
                                    String indent,
                                    boolean root) {
        if (rule instanceof RuleParse) {
            RuleParse p = (RuleParse) rule;
            String simpleRuleName = p.getRuleName().getSimpleRuleName();
            String ruleName = p.getRuleName().getRuleName();
            String objName = RULE_PREFIX + simpleRuleName;

            if (parseTree != null) {
                parseTree.append(indent).append('<').append(ruleName).append(">\n");
            }

            if (script != null) {
                script.append(indent).append("var " + LAST_RULE + ";\n");
                script.append(indent).append("var ").append(objName).append(" = new " + CLASS_NAME + "();\n");
                script.append(indent).append(objName).append('.' + METHOD_NAME + " = function() {\n");
                script.append(indent).append(INDENT + "this.$name = \"").append(objName).append("\";\n");
            }

            parseTags(p.getRule(), script, parseTree, indent + INDENT, false);

            if (script != null) {
                script.append(indent).append(INDENT + "if (this.$value == undefined) {\n");
                script.append(indent).append(INDENT + INDENT + "this.$value = this.getPhrase();\n");
                script.append(indent).append(INDENT + "}\n");

                script.append(indent).append("}\n");
                script.append(indent).append(objName).append('.' + METHOD_NAME + "();\n");
                script.append(indent).append(LAST_RULE + " = ").append(objName).append(";\n");

                if (!root) {
                    script.append(indent).append("this.addTokens(" + LAST_RULE + ".getTokens(), " + LAST_RULE + ".getPhrase()" + ");\n");
                } else {
                    //script.append(indent + objName + " = undefined;\n");
                    script.append(indent).append("delete ").append(objName).append(";\n");
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
                for (Rule rule1 : rules) {
                    parseTags(rule1, script, parseTree, indent, false);
                }
            }
        } else if (rule instanceof RuleToken) {
            String t = '\'' + ((RuleToken) rule).getText() + '\'';
            if (parseTree != null) {
                parseTree.append(indent).append(t).append('\n');
            }
            if (script != null) {
                script.append(indent).append("this.addTokens(").append(t).append(',').append(t).append(");\n");
            }
        } else if (rule instanceof RuleTag) {
            parseTags(((RuleTag) rule).getRule(),
                    script, parseTree, indent, false);
            String tag = ((RuleTag) rule).getTag();
            if (parseTree != null) {
                parseTree.append(indent).append('{').append(tag).append("}\n");
            }
            if (script != null) {
                if (isDebugging()) {
                    appendDebugScript(script, indent, tag);
                } else {
                    script.append(indent).append(INDENT + "//vvvvvvvvvv FROM GRAMMAR vvvvvvvvvv\n");
                    script.append(indent).append('{').append(tag).append("}\n");
                    script.append(indent).append(INDENT + "//^^^^^^^^^^ FROM GRAMMAR ^^^^^^^^^^\n");
                }
            }
        } else if (rule instanceof RuleName) {
            if (parseTree != null) {
                String name = ((RuleName) rule).getRuleName();
                if (!name.equals("NULL") && !name.equals("VOID")) {
                    parseTree.append(indent).append(name).append('\n');
                }
            }
        } else {
            throw new RuntimeException("Unexected rule type "
                    + rule.getClass().getName());
        }
    }
}
