
/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package demo.jsapi.cardmatch;

import com.sun.speech.engine.recognition.BaseRecognizer;

import edu.cmu.sphinx.decoder.linguist.Grammar;
import edu.cmu.sphinx.decoder.linguist.GrammarNode;

import java.io.FileReader;
import java.io.IOException;

import javax.speech.EngineException;
import javax.speech.recognition.GrammarException;
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
 * Defines a grammar based JSGF grammar rules in a file. The path to
 * the file is defined by the PROP_PATH property.
 *
 * This implementation does not support RuleCount and does not support
 * right-hand recursion.
 *
 * All probabilities are maintained in LogMath log base
 */
public class JSGFGrammar extends Grammar {

    private final static String PROP_PREFIX 
        = "demo.jsapi.cardmatch.JSGFGrammar.";

    /**
     * Sphinx property that defines the location of the JSGF
     * grammar file.
     */
    public final static String PROP_PATH = PROP_PREFIX + "path";

    /**
     * Default value for the location of the JSGF grammar file.
     */
    public final static String PROP_PATH_DEFAULT = "CardMatch.gram";


    private RuleGrammar ruleGrammar;
    private int identity;
    

    private void debugPrintln(String message) {
        if (false) {
            System.out.println(message);
        }
    }


    /**
     * Returns the RuleGrammar of this JSGFGrammar.
     *
     * @return the RuleGrammar
     */
    public RuleGrammar getRuleGrammar() {
        return ruleGrammar;
    }


    /**
     * Creates the grammar.
     *
     * @return the initial node of the Grammar
     */
    protected GrammarNode createGrammar()
        throws IOException, NoSuchMethodException {
        identity = 0;
        String path = props.getString(PROP_PATH, PROP_PATH_DEFAULT);
                
        Recognizer recognizer = new BaseRecognizer();

        try {
            recognizer.allocate();
            ruleGrammar = recognizer.loadJSGF(new FileReader(path));
            ruleGrammar.setEnabled(true);
            
            GrammarNode firstNode = createGrammarNode(identity++, "<sil>");
            GrammarNode finalNode = createGrammarNode(identity++, "<sil>");
            finalNode.setFinalNode(true);
            
            // go through each rule and create a network of GrammarNodes
            // for each of them
            String[] ruleNames = ruleGrammar.listRuleNames();
            for (int i = 0; i < ruleNames.length; i++) {
                String ruleName = ruleNames[i];
                if (ruleGrammar.isRulePublic(ruleName)) {
                    debugPrintln("New Rule: " + ruleName);
                    Rule rule = ruleGrammar.getRule(ruleName);
                    GrammarNode[] newNodes = parseRule(rule);
                    firstNode.add(newNodes[0], 0.0f);
                    newNodes[1].add(finalNode, 0.0f);
                }
            }
            
            return firstNode;
        } catch (EngineException ee) {
            ee.printStackTrace();
            throw new IOException(ee.toString());
        } catch (GrammarException ge) {
            ge.printStackTrace();
            throw new IOException("GrammarException: " + path);
        }
    }


    /**
     * Parses the given Rule into a network of GrammarNodes.
     *
     * @param rule the Rule to parse
     *
     * @return the first and last GrammarNodes created from the parsing
     */
    private GrammarNode[] parseRule(Rule rule) {

        debugPrintln("parseRule: " + rule.toString());

        if (rule instanceof RuleAlternatives) {
            return parseRuleAlternatives((RuleAlternatives)rule);
        } else if (rule instanceof RuleCount) {
            throw new IllegalArgumentException
                ("Unsupported Rule type: RuleCount: " + rule.toString());
        } else if (rule instanceof RuleName) {
            return parseRuleName((RuleName)rule);
        } else if (rule instanceof RuleSequence) {
            return parseRuleSequence((RuleSequence)rule);
        } else if (rule instanceof RuleTag) {
            return parseRuleTag((RuleTag)rule);
        } else if (rule instanceof RuleToken) {
            return parseRuleToken((RuleToken)rule);
        } else if (rule instanceof RuleParse) {
            throw new IllegalArgumentException
                ("Unsupported Rule type: RuleParse: " + rule.toString());
        } else {
            throw new IllegalArgumentException
                ("Unsupported Rule type: " + rule.toString());
        }
    }


    /**
     * Parses the given RuleName into a network of GrammarNodes.
     */
    private GrammarNode[] parseRuleName(RuleName ruleName) {
        debugPrintln("parseRuleName: " + ruleName.toString());
        Rule rule = ruleGrammar.getRule(ruleName.getSimpleRuleName());
        return parseRule(rule);
    }


    /**
     * Parses the given RuleAlternatives into a network of GrammarNodes.
     *
     * @param ruleAlternatives the RuleAlternatives to parse
     *
     * @return the first and last GrammarNodes of the network
     */
    private GrammarNode[] parseRuleAlternatives(RuleAlternatives
                                                ruleAlternatives) {
        debugPrintln("parseRuleAlternatives: " + ruleAlternatives.toString());
        GrammarNode firstNode = createGrammarNode(identity++, false);
        GrammarNode lastNode = createGrammarNode(identity++, false);

        Rule[] rules = ruleAlternatives.getRules();
        float[] weights = ruleAlternatives.getWeights();

        // expand each alternative, and connect them in parallel
        for (int i = 0; i < rules.length; i++) {
            Rule rule = rules[i];
            float weight = 0.0f;
            if (weights != null) {
                weight = weights[i];
            }
            debugPrintln("Alternative: " + rule.toString());
            GrammarNode[] newNodes = parseRule(rule);
            firstNode.add(newNodes[0], weight);
            newNodes[1].add(lastNode, 0.0f);
        }
        
        GrammarNode[] nodes = new GrammarNode[2];
        nodes[0] = firstNode;
        nodes[1] = lastNode;
        return nodes;
    }

    
    /**
     * Parses the given RuleSequence into a network of GrammarNodes.
     *
     * @param ruleSequence the RuleSequence to parse
     *
     * @return the first and last GrammarNodes of the network
     */
    private GrammarNode[] parseRuleSequence(RuleSequence ruleSequence) {

        debugPrintln("parseRuleSequence: " + ruleSequence);

        GrammarNode[] nodes = new GrammarNode[2];
        Rule[] rules = ruleSequence.getRules();

        GrammarNode lastGrammarNode = null;

        // expand and connect each rule in the sequence serially
        for (int i = 0; i < rules.length; i++) {
            Rule rule = rules[i];
            GrammarNode[] newNodes = parseRule(rule);
            
            // first node
            if (i == 0) {
                nodes[0] = newNodes[0];
            }

            // last node
            if (i == (rules.length - 1)) {
                nodes[1] = newNodes[1];
            }

            if (i > 0) {
                lastGrammarNode.add(newNodes[0], 0.0f);
            }
            lastGrammarNode = newNodes[0];
        }

        return nodes;
    }


    /**
     * Parses the given RuleTag into a network GrammarNodes.
     *
     * @param ruleTag the RuleTag to parse
     *
     * @return the first and last GrammarNodes of the network
     */
    private GrammarNode[] parseRuleTag(RuleTag ruleTag) {
        debugPrintln("parseRuleTag: " + ruleTag);
        Rule rule = ruleTag.getRule();
        return parseRule(rule);
    }


    /**
     * Creates a GrammarNode with the word in the given RuleToken.
     *
     * @param ruleToken the RuleToken that contains the word
     *
     * @return a GrammarNode with the word in the given RuleToken
     */
    private GrammarNode[] parseRuleToken(RuleToken ruleToken) {

        debugPrintln("parseRuleToken: " + ruleToken.toString());
        
        GrammarNode node = createGrammarNode(identity++, ruleToken.getText());
        GrammarNode[] nodes = new GrammarNode[2];
        nodes[0] = node;
        nodes[1] = node;
        return nodes;
    }
}
