
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

package edu.cmu.sphinx.decoder.linguist;

import com.sun.speech.engine.recognition.BaseRecognizer;

import edu.cmu.sphinx.decoder.linguist.Grammar;
import edu.cmu.sphinx.decoder.linguist.GrammarNode;

import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

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
        = "edu.cmu.sphinx.decoder.linguist.JSGFGrammar.";

    /**
     * Sphinx property that defines the location of the JSGF
     * grammar file.
     */
    public final static String PROP_PATH = PROP_PREFIX + "path";

    /**
     * Default value for the location of the JSGF grammar file.
     */
    public final static String PROP_PATH_DEFAULT = "grammar.jsgf";

    private final static String NULL = "NULL";
    private final static String VOID = "VOID";

    private RuleGrammar ruleGrammar;
    private int identity;
    private Map ruleNameStack = new HashMap();
    

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
                    GrammarGraph graph  = parseRule(rule);
                    firstNode.add(graph.getStartNode(), 0.0f);
                    graph.getEndNode().add(finalNode, 0.0f);
                }
            }
            return firstNode;
        } catch (EngineException ee) {
            // ee.printStackTrace();
            throw new IOException(ee.toString());
        } catch (GrammarException ge) {
            // ge.printStackTrace();
            throw new IOException("GrammarException: " + ge );
        }
    }


    /**
     * Parses the given Rule into a network of GrammarNodes.
     *
     * @param rule the Rule to parse
     *
     * @return a grammar graph
     */
    private GrammarGraph parseRule(Rule rule) {
        GrammarGraph result;

        if (rule != null) {
            debugPrintln("parseRule: " + rule.toString());
        }

        if (rule instanceof RuleAlternatives) {
            result = parseRuleAlternatives((RuleAlternatives)rule);
        } else if (rule instanceof RuleCount) {
            result = parseRuleCount((RuleCount) rule);
        } else if (rule instanceof RuleName) {
            result = parseRuleName((RuleName)rule);
        } else if (rule instanceof RuleSequence) {
            result = parseRuleSequence((RuleSequence)rule);
        } else if (rule instanceof RuleTag) {
            result = parseRuleTag((RuleTag)rule);
        } else if (rule instanceof RuleToken) {
            result = parseRuleToken((RuleToken)rule);
        } else if (rule instanceof RuleParse) {
            throw new IllegalArgumentException
                ("Unsupported Rule type: RuleParse: " + rule.toString());
        } else {
            throw new IllegalArgumentException
                ("Unsupported Rule type: " + rule.toString());
        }
        return result;
    }


    /**
     * Parses the given RuleName into a network of GrammarNodes.
     *
     * @param ruleName the RuleName rule to parse
     *
     * @return a grammar graph
     */
    private GrammarGraph parseRuleName(RuleName ruleName) {
        debugPrintln("parseRuleName: " + ruleName.toString());
        GrammarGraph  result = (GrammarGraph) 
            ruleNameStack.get(ruleName.getRuleName());

        if (result != null) {  // its a recursive call
            return result;
        } else {
            result = new GrammarGraph();
            ruleNameStack.put(ruleName.getRuleName(), result);
        }
        if (ruleName.getSimpleRuleName().equals(NULL)) {
            result.getStartNode().add(result.getEndNode(), 0.0f);
        } else if (ruleName.getSimpleRuleName().equals(VOID)) {
            // no connection for void
        } else {
            Rule rule = ruleGrammar.getRule(ruleName.getSimpleRuleName());
            if (rule == null) {
                throw new IllegalArgumentException("Missing rule: " +
                        ruleName.getSimpleRuleName());
            }
            GrammarGraph ruleResult =  parseRule(rule);
            result.getStartNode().add(ruleResult.getStartNode(), 0.0f);
            ruleResult.getEndNode().add(result.getEndNode(), 0.0f);
            ruleNameStack.remove(ruleName.getRuleName());
        }
        return result;
    }

    /**
     * Parses the given RuleCount into a network of GrammarNodes.
     *
     * @param ruleCount the RuleCount object to parse
     *
     * @return a grammar graph
     */
    private GrammarGraph parseRuleCount(RuleCount ruleCount) {
        debugPrintln("parseRuleCount: " + ruleCount);
        GrammarGraph result = new GrammarGraph();
        int count = ruleCount.getCount();
        GrammarGraph newNodes = parseRule(ruleCount.getRule());

        result.getStartNode().add(newNodes.getStartNode(), 0.0f);
        newNodes.getEndNode().add(result.getEndNode(), 0.0f);

        // if this is optional, add a bypass arc

        if (count == RuleCount.ZERO_OR_MORE || count == RuleCount.OPTIONAL) {
            result.getStartNode().add(result.getEndNode(), 0.0f);
        }

        // if this can possibly occur more than once, add a loopback

        if (count == RuleCount.ONCE_OR_MORE || count ==RuleCount.ZERO_OR_MORE) {
            newNodes.getEndNode().add(newNodes.getStartNode(), 0.0f);
        }
        return result;
    }


    /**
     * Parses the given RuleAlternatives into a network of GrammarNodes.
     *
     * @param ruleAlternatives the RuleAlternatives to parse
     *
     * @return a grammar graph
     */
    private GrammarGraph parseRuleAlternatives(RuleAlternatives
                                                ruleAlternatives) {
        debugPrintln("parseRuleAlternatives: " + ruleAlternatives.toString());
        GrammarGraph result = new GrammarGraph();

        Rule[] rules = ruleAlternatives.getRules();
        float[] weights = ruleAlternatives.getWeights();
        normalizeWeights(weights);

        // expand each alternative, and connect them in parallel
        for (int i = 0; i < rules.length; i++) {
            Rule rule = rules[i];
            float weight = 0.0f;
            if (weights != null) {
                weight = weights[i];
            }
            debugPrintln("Alternative: " + rule.toString());
            GrammarGraph newNodes = parseRule(rule);
            result.getStartNode().add(newNodes.getStartNode(), weight);
            newNodes.getEndNode().add(result.getEndNode(), 0.0f);
        }
        
        return result;
    }


    /**
     * Normalize the weights. The weights should always be zero or
     * greater. We need to convert the weights to a log probability.
     *
     * @param weights the weights to normalize
     */
    private void normalizeWeights(float[] weights) {
        if (weights != null) {
            double sum = 0.0;
            for (int i = 0; i < weights.length; i++) {
                if (weights[i] < 0) {
                    throw new IllegalArgumentException("negative weight");
                }
                sum += weights[i];
            }
            for (int i = 0; i < weights.length; i++) {
                if (sum == 0.0f) {
                    weights[i] = getLogMath().getLogZero();
                } else {
                    weights[i] = getLogMath().linearToLog(weights[i] / sum);
                }
            }
        } 
    }


    
    /**
     * Parses the given RuleSequence into a network of GrammarNodes.
     *
     * @param ruleSequence the RuleSequence to parse
     *
     * @return the first and last GrammarNodes of the network
     */
    private GrammarGraph parseRuleSequence(RuleSequence ruleSequence) {

        GrammarNode startNode = null;
        GrammarNode endNode = null;
        debugPrintln("parseRuleSequence: " + ruleSequence);

        Rule[] rules = ruleSequence.getRules();

        GrammarNode lastGrammarNode = null;

        // expand and connect each rule in the sequence serially
        for (int i = 0; i < rules.length; i++) {
            Rule rule = rules[i];
            GrammarGraph newNodes = parseRule(rule);
            
            // first node
            if (i == 0) {
                startNode = newNodes.getStartNode();
            }

            // last node
            if (i == (rules.length - 1)) {
                endNode = newNodes.getEndNode();
            }

            if (i > 0) {
                lastGrammarNode.add(newNodes.getStartNode(), 0.0f);
            }
            lastGrammarNode = newNodes.getEndNode();
        }

        return new GrammarGraph(startNode, endNode);
    }


    /**
     * Parses the given RuleTag into a network GrammarNodes.
     *
     * @param ruleTag the RuleTag to parse
     *
     * @return the first and last GrammarNodes of the network
     */
    private GrammarGraph parseRuleTag(RuleTag ruleTag) {
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
    private GrammarGraph parseRuleToken(RuleToken ruleToken) {
        debugPrintln("parseRuleToken: " + ruleToken.toString());
        
        GrammarNode node = createGrammarNode(identity++, ruleToken.getText());
        return new GrammarGraph(node, node);
    }

    /**
     * Debugging println
     *
     * @param message the message to optionally print
     */
    private void debugPrintln(String message) {
        if (false) {
            System.out.println(message);
        }
    }

    /**
     * Represents a graph of grammar nodes. A grammar graph has a
     * single starting node and a single ending node
     */
    class GrammarGraph {
        private GrammarNode startNode;
        private GrammarNode endNode;

        /**
         * Creates a grammar graph with the given nodes
         *
         * @param startNode the staring node of the graph
         * @param endNode the ending node of the graph
         */
        GrammarGraph(GrammarNode startNode, GrammarNode endNode) {
            this.startNode = startNode;
            this.endNode = endNode;
        }

        /**
         * Creates a graph with non-word nodes for the start and
         * ending nodes
         */
        GrammarGraph() {
            startNode = createGrammarNode(identity++, false);
            endNode = createGrammarNode(identity++, false);
        }

        /**
         * Gets the starting node
         *
         * @return the starting node for the graph
         */
        GrammarNode getStartNode() {
            return startNode;
        }

        /**
         * Gets the ending  node
         *
         * @return the ending  node for the graph
         */
        GrammarNode getEndNode() {
            return endNode;
        }
    }
}

