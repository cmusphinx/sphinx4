/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.jsapi;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.speech.EngineException;
import javax.speech.recognition.GrammarException;
import javax.speech.recognition.GrammarSyntaxDetail;
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

import com.sun.speech.engine.recognition.BaseRecognizer;

import edu.cmu.sphinx.linguist.language.grammar.Grammar;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * Defines a BNF-style grammar based on JSGF grammar rules in a file.
 * 
 * <p>
 * The Java Speech Grammar Format (JSGF) is a BNF-style, platform-independent,
 * and vendor-independent textual representation of grammars for use in speech
 * recognition. It is used by the <a
 * href="http://java.sun.com/products/java-media/speech/">Java Speech API
 * (JSAPI) </a>.
 * 
 * <p>
 * Here we only intend to give a couple of examples of grammars written in
 * JSGF, so that you can quickly learn to write your own grammars. For more
 * examples and a complete specification of JSGF, go to
 * <p><a
 * href="http://java.sun.com/products/java-media/speech/forDevelopers/JSGF/">http://java.sun.com/products/java-media/speech/forDevelopers/JSGF/
 * </a>.
 * 
 * <p>
 * <b>Example 1: "Hello World" in JSGF </b>
 * 
 * <p>
 * The example below shows how a JSGF grammar that generates the sentences
 * "Hello World":
 * 
 * <p>
 * <table width="100%" cellpadding="10">
 * <tr>
 * <td bgcolor="#DDDDDD">
 * 
 * <pre>
 *  #JSGF V1.0
 *  
 *  public &lt;helloWorld&gt; = Hello World;
 * </pre>
 * 
 * </td>
 * </tr>
 * </table>
 * 
 * <i>Figure 1: Hello grammar that generates the sentences "Hello World". </i>
 * 
 * <p>
 * The above grammar is saved in a file called "hello.gram". It defines a
 * public grammar rule called "helloWorld". In order for this grammar rule to
 * be publicly accessible, we must be declared it "public". Non-public grammar
 * rules are not visible outside of the grammar file.
 * 
 * <p>
 * The location of the grammar file(s) is(are) defined by the
 * {@link #PROP_BASE_GRAMMAR_URL baseGrammarURL}property. Since all JSGF
 * grammar files end with ".gram", it will automatically search all such files
 * at the given URL for the grammar. The name of the grammar to search for is
 * specified by {@link #PROP_GRAMMAR_NAME grammarName}. In this example, the
 * grammar name is "helloWorld".
 * 
 * <p>
 * <b>Example 2: Command Grammar in JSGF </b>
 * 
 * <p>
 * This examples shows a grammar that generates basic control commands like
 * "move a menu thanks please", "close file", "oh mighty computer please kindly
 * delete menu thanks". It is the same as one of the command & control examples
 * in the <a
 * href="http://java.sun.com/products/java-media/speech/forDevelopers/JSGF/">JSGF
 * specification </a>. It is considerably more complex than the previous
 * example. It defines the public grammar called "basicCmd".
 * 
 * <p>
 * <table width="100%" cellpadding="10">
 * <tr>
 * <td bgcolor="#DDDDDD">
 * 
 * <pre>
 *  #JSGF V1.0
 *  
 *  public &lt;basicCmd&gt; = &lt;startPolite&gt; &lt;command&gt; &lt;endPolite&gt;;
 *  
 *  &lt;command&gt; = &lt;action&gt; &lt;object&gt;;
 *  &lt;action&gt; = /10/ open |/2/ close |/1/ delete |/1/ move;
 *  &lt;object&gt; = [the | a] (window | file | menu);
 *  
 *  &lt;startPolite&gt; = (please | kindly | could you | oh mighty computer) *;
 *  &lt;endPolite&gt; = [ please | thanks | thank you ];
 * </pre>
 * 
 * </td>
 * </tr>
 * </table>
 * 
 * <i>Figure 2: Command grammar that generates simple control commands. </i>
 * 
 * <p>
 * The features of JSGF that are shown in this example includes:
 * <ul>
 * <li>using other grammar rules within a grammar rule.
 * <li>the OR "|" operator.
 * <li>the grouping "(...)" operator.
 * <li>the optional grouping "[...]" operator.
 * <li>the zero-or-many "*" (called Kleene star) operator.
 * <li>a probability (e.g., "open" is more likely than the others).
 * </ul>
 * 
 * <p>
 * <h3>From JSGF to Grammar Graph</h3>
 * 
 * After the JSGF grammar is read in, it is converted to a graph of words
 * representing the grammar. Lets call this the grammar graph. It is from this
 * grammar graph that the eventual search structure used for speech recognition
 * is built. Below, we show the grammar graphs created from the above JSGF
 * grammars. The nodes <code>"&lt;sil&gt;"</code> means "silence".
 * 
 * <p>
 * <img src="doc-files/helloWorld.jpg"> <br>
 * <i>Figure 3: Grammar graph created from the Hello World grammar. </i>
 * 
 * <p>
 * <img src="doc-files/commandGrammar.jpg"> <br>
 * <i>Figure 4: Grammar graph created from the Command grammar. </i>
 * 
 * <p>
 * <h3>Limitations</h3>
 *
 * There is a known limitation with the current JSGF support.
 * Grammars that contain non-speech loops
 * currently cause the recognizer to hang.  
 * <p>
 * For example, in the following grammar
 *
 * <pre>
 *  #JSGF V1.0
 *  grammar jsgf.nastygram;
 *  public <nasty> = I saw a ((cat* | dog* | mouse*)+)+;
 * </pre>
 *
 * the production: ((cat* | dog* | mouse*)+)+ can result in a
 * continuous loop, since (cat* | dog* | mouse*) can represent no
 * speech (i.e. zero cats, dogs and mice), this is equivalent to ()+.
 * To avoid this problem, the grammar writer should ensure that there
 * are no rules that could possibly match no speech within a plus
 * operator or kleene star operator.
 * 
 * <p>
 * <h3>Implementation Notes</h3>
 * <ol>
 * <li>All internal probabilities are maintained in LogMath log base.
 * </ol>
 */
public class JSGFGrammar extends Grammar {

    /**
     * Sphinx property that defines the location of the JSGF grammar file.
     */
    public final static String PROP_BASE_GRAMMAR_URL = "grammarLocation";


    /**
     * Sphinx property that defines the location of the JSGF grammar file.
     */
    public final static String PROP_GRAMMAR_NAME = "grammarName";

    /**
     * Default value for PROP_GRAMMAR_NAME
     */
    public final static String PROP_GRAMMAR_NAME_DEFAULT = "default.gram";
    
    /**
     * Sphinx property that defines the logMath component. 
     */
    
    public final static String PROP_LOG_MATH = "logMath";
    

    // ---------------------
    // Configurable data
    // ---------------------
    private RuleGrammar ruleGrammar;
    private int identity;
    private Map ruleNameStack = new HashMap();
    private Recognizer recognizer;
    private String grammarName;
    private URL baseURL = null;
    private LogMath logMath;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_BASE_GRAMMAR_URL, PropertyType.RESOURCE);
        registry.register(PROP_GRAMMAR_NAME, PropertyType.STRING);
        registry.register(PROP_LOG_MATH, PropertyType.COMPONENT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        baseURL = ps.getResource(PROP_BASE_GRAMMAR_URL);
        grammarName = ps
                .getString(PROP_GRAMMAR_NAME, PROP_GRAMMAR_NAME_DEFAULT);
        logMath = (LogMath) ps.getComponent(PROP_LOG_MATH, LogMath.class);

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
     * Sets the URL context of the JSGF grammars.
     *
     * @param url the URL context of the grammars
     */
    public void setBaseURL(URL url) {
        baseURL = url;
    }

    /**
     * Creates the grammar.
     * 
     * @return the initial node of the Grammar
     */
    protected GrammarNode createGrammar() throws IOException {
        identity = 0;

        recognizer = new BaseRecognizer();

        try {
            recognizer.allocate();
            ruleGrammar = recognizer.loadJSGF(baseURL, grammarName);
            recognizer.commitChanges();
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
                    GrammarGraph graph = parseRule(rule);
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
            dumpGrammarException(ge);
            throw new IOException("GrammarException: " + ge);
        } catch (MalformedURLException mue) {
            throw new IOException("bad base grammar url " + baseURL + " "
                    + mue);

        }
    }

    /**
     * Parses the given Rule into a network of GrammarNodes.
     * 
     * @param rule
     *                the Rule to parse
     * 
     * @return a grammar graph
     */
    private GrammarGraph parseRule(Rule rule) throws GrammarException {
        GrammarGraph result;

        if (rule != null) {
            debugPrintln("parseRule: " + rule.toString());
        }

        if (rule instanceof RuleAlternatives) {
            result = parseRuleAlternatives((RuleAlternatives) rule);
        } else if (rule instanceof RuleCount) {
            result = parseRuleCount((RuleCount) rule);
        } else if (rule instanceof RuleName) {
            result = parseRuleName((RuleName) rule);
        } else if (rule instanceof RuleSequence) {
            result = parseRuleSequence((RuleSequence) rule);
        } else if (rule instanceof RuleTag) {
            result = parseRuleTag((RuleTag) rule);
        } else if (rule instanceof RuleToken) {
            result = parseRuleToken((RuleToken) rule);
        } else if (rule instanceof RuleParse) {
            throw new IllegalArgumentException(
                    "Unsupported Rule type: RuleParse: " + rule.toString());
        } else {
            throw new IllegalArgumentException("Unsupported Rule type: "
                    + rule.toString());
        }
        return result;
    }

    /**
     * Parses the given RuleName into a network of GrammarNodes.
     * 
     * @param initialRuleName
     *                the RuleName rule to parse
     * 
     * @return a grammar graph
     */
    private GrammarGraph parseRuleName(RuleName initialRuleName)
            throws GrammarException {
        debugPrintln("parseRuleName: " + initialRuleName.toString());
        GrammarGraph result = (GrammarGraph) ruleNameStack.get(initialRuleName
                .getRuleName());

        if (result != null) { // its a recursive call
            return result;
        } else {
            result = new GrammarGraph();
            ruleNameStack.put(initialRuleName.getRuleName(), result);
        }
        RuleName ruleName = ruleGrammar.resolve(initialRuleName);

        if (ruleName == RuleName.NULL) {
            result.getStartNode().add(result.getEndNode(), 0.0f);
        } else if (ruleName == RuleName.VOID) {
            // no connection for void
        } else {
            if (ruleName == null) {
                throw new GrammarException("Can't resolve " + initialRuleName
                        + " g " + initialRuleName.getFullGrammarName());
            }
            RuleGrammar rg = recognizer.getRuleGrammar(ruleName
                    .getFullGrammarName());
            if (rg == null) {
                throw new GrammarException("Can't resolve grammar name "
                        + ruleName.getFullGrammarName());
            }

            Rule rule = rg.getRule(ruleName.getSimpleRuleName());
            if (rule == null) {
                throw new GrammarException("Can't resolve rule: "
                        + ruleName.getRuleName());
            }
            GrammarGraph ruleResult = parseRule(rule);
            result.getStartNode().add(ruleResult.getStartNode(), 0.0f);
            ruleResult.getEndNode().add(result.getEndNode(), 0.0f);
            ruleNameStack.remove(ruleName.getRuleName());
        }
        return result;
    }

    /**
     * Parses the given RuleCount into a network of GrammarNodes.
     * 
     * @param ruleCount
     *                the RuleCount object to parse
     * 
     * @return a grammar graph
     */
    private GrammarGraph parseRuleCount(RuleCount ruleCount)
            throws GrammarException {
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

        if (count == RuleCount.ONCE_OR_MORE || count == RuleCount.ZERO_OR_MORE) {
            newNodes.getEndNode().add(newNodes.getStartNode(), 0.0f);
        }
        return result;
    }

    /**
     * Parses the given RuleAlternatives into a network of GrammarNodes.
     * 
     * @param ruleAlternatives
     *                the RuleAlternatives to parse
     * 
     * @return a grammar graph
     */
    private GrammarGraph parseRuleAlternatives(RuleAlternatives ruleAlternatives)
            throws GrammarException {
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
     * Normalize the weights. The weights should always be zero or greater. We
     * need to convert the weights to a log probability.
     * 
     * @param weights
     *                the weights to normalize
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
                    weights[i] = LogMath.getLogZero();
                } else {
                    weights[i] = logMath.linearToLog(weights[i] / sum);
                }
            }
        }
    }

    /**
     * Parses the given RuleSequence into a network of GrammarNodes.
     * 
     * @param ruleSequence
     *                the RuleSequence to parse
     * 
     * @return the first and last GrammarNodes of the network
     */
    private GrammarGraph parseRuleSequence(RuleSequence ruleSequence)
            throws GrammarException {

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
     * @param ruleTag
     *                the RuleTag to parse
     * 
     * @return the first and last GrammarNodes of the network
     */
    private GrammarGraph parseRuleTag(RuleTag ruleTag) throws GrammarException {
        debugPrintln("parseRuleTag: " + ruleTag);
        Rule rule = ruleTag.getRule();
        return parseRule(rule);
    }

    /**
     * Creates a GrammarNode with the word in the given RuleToken.
     * 
     * @param ruleToken
     *                the RuleToken that contains the word
     * 
     * @return a GrammarNode with the word in the given RuleToken
     */
    private GrammarGraph parseRuleToken(RuleToken ruleToken) {
        debugPrintln("parseRuleToken: " + ruleToken.toString());

        GrammarNode node = createGrammarNode(identity++, ruleToken.getText());
        return new GrammarGraph(node, node);
    }

    /**
     * Dumps out a grammar exception
     * 
     * @param ge
     *                the grammar exception
     *  
     */
    private void dumpGrammarException(GrammarException ge) {
        System.out.println("Grammar exception " + ge);
        GrammarSyntaxDetail[] gsd = ge.getDetails();
        if (gsd != null) {
            for (int i = 0; i < gsd.length; i++) {
                System.out.println("Grammar Name: " + gsd[i].grammarName);
                System.out.println("Grammar Loc : " + gsd[i].grammarLocation);
                System.out.println("Import Name : " + gsd[i].importName);
                System.out.println("Line number : " + gsd[i].lineNumber);
                System.out.println("char number : " + gsd[i].charNumber);
                System.out.println("Rule name   : " + gsd[i].ruleName);
                System.out.println("Message     : " + gsd[i].message);
            }
        }
    }

    /**
     * Debugging println
     * 
     * @param message
     *                the message to optionally print
     */
    private void debugPrintln(String message) {
        if (false) {
            System.out.println(message);
        }
    }

    /**
     * Dumps interesting things about this grammar
     */
    private void dumpGrammar() {
        System.out.println("Imported rules { ");
        RuleName[] imports = ruleGrammar.listImports();

        for (int i = 0; i < imports.length; i++) {
            System.out
                    .println("  Import " + i + " " + imports[i].getRuleName());
        }
        System.out.println("}");

        System.out.println("Rulenames { ");
        String[] names = ruleGrammar.listRuleNames();

        for (int i = 0; i < names.length; i++) {
            System.out.println("  Name " + i + " " + names[i]);
        }
        System.out.println("}");
    }

    /**
     * Represents a graph of grammar nodes. A grammar graph has a single
     * starting node and a single ending node
     */
    class GrammarGraph {
        private GrammarNode startNode;
        private GrammarNode endNode;

        /**
         * Creates a grammar graph with the given nodes
         * 
         * @param startNode
         *                the staring node of the graph
         * @param endNode
         *                the ending node of the graph
         */
        GrammarGraph(GrammarNode startNode, GrammarNode endNode) {
            this.startNode = startNode;
            this.endNode = endNode;
        }

        /**
         * Creates a graph with non-word nodes for the start and ending nodes
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
         * Gets the ending node
         * 
         * @return the ending node for the graph
         */
        GrammarNode getEndNode() {
            return endNode;
        }
    }
}
