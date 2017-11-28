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

package edu.cmu.sphinx.jsgf;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cmu.sphinx.jsgf.parser.JSGFParser;
import edu.cmu.sphinx.jsgf.rule.*;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.language.grammar.Grammar;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.ConfigurationManagerUtils;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;

/**
 * <h3>Defines a BNF-style grammar based on JSGF grammar rules in a file.</h3>
 * 
 * 
 * The Java Speech Grammar Format (JSGF) is a BNF-style, platform-independent,
 * and vendor-independent textual representation of grammars for use in speech
 * recognition. It is used by the <a
 * href="http://java.sun.com/products/java-media/speech/">Java Speech API
 * (JSAPI) </a>.
 * 
 * Here we only intend to give a couple of examples of grammars written in JSGF,
 * so that you can quickly learn to write your own grammars. For more examples
 * and a complete specification of JSGF, go to
 * 
 * <a href="http://java.sun.com/products/java-media/speech/forDevelopers/JSGF/">
 * http://java.sun.com/products/java-media/speech/forDevelopers/JSGF/ </a>.
 * 
 * 
 * <h3>Example 1: "Hello World" in JSGF</h3>
 * 
 * The example below shows how a JSGF grammar that generates the sentences
 * "Hello World":
 * 
 * <pre>
 *  #JSGF V1.0
 *  public &lt;helloWorld&gt; = Hello World;
 * </pre>
 * 
 * <i>Figure 1: Hello grammar that generates the sentences "Hello World". </i>
 * <p>
 * 
 * The above grammar is saved in a file called "hello.gram". It defines a public
 * grammar rule called "helloWorld". In order for this grammar rule to be
 * publicly accessible, we must be declared it "public". Non-public grammar
 * rules are not visible outside of the grammar file.
 * 
 * The location of the grammar file(s) is(are) defined by the
 * {@link #PROP_BASE_GRAMMAR_URL baseGrammarURL}property. Since all JSGF grammar
 * files end with ".gram", it will automatically search all such files at the
 * given URL for the grammar. The name of the grammar to search for is specified
 * by {@link #PROP_GRAMMAR_NAME grammarName}. In this example, the grammar name
 * is "helloWorld".
 * 
 * <h3>Example 2: Command Grammar in JSGF</h3>
 * 
 * This examples shows a grammar that generates basic control commands like
 * "move a menu thanks please", "close file",
 * "oh mighty computer please kindly delete menu thanks". It is the same as one
 * of the command and control examples in the <a
 * href="http://java.sun.com/products/java-media/speech/forDevelopers/JSGF/"
 * >JSGF specification </a>. It is considerably more complex than the previous
 * example. It defines the public grammar called "basicCmd".
 * 
 * <pre>
 *  #JSGF V1.0
 *  public &lt;basicCmd&gt; = &lt;startPolite&gt; &lt;command&gt; &lt;endPolite&gt;;
 *  &lt;command&gt; = &lt;action&gt; &lt;object&gt;;
 *  &lt;action&gt; = /10/ open |/2/ close |/1/ delete |/1/ move;
 *  &lt;object&gt; = [the | a] (window | file | menu);
 *  &lt;startPolite&gt; = (please | kindly | could you | oh mighty computer) *;
 *  &lt;endPolite&gt; = [ please | thanks | thank you ];
 * </pre>
 * 
 * <i>Figure 2: Command grammar that generates simple control commands. </i>
 * <p>
 * 
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
 * <h3>From JSGF to Grammar Graph</h3>
 * 
 * After the JSGF grammar is read in, it is converted to a graph of words
 * representing the grammar. Lets call this the grammar graph. It is from this
 * grammar graph that the eventual search structure used for speech recognition
 * is built. Below, we show the grammar graphs created from the above JSGF
 * grammars. The nodes <code>"&lt;sil&gt;"</code> means "silence".
 * 
 * <p>
 * <img alt="Hello world" src="doc-files/helloWorld.jpg"> <br>
 * 
 * <i>Figure 3: Grammar graph created from the Hello World grammar. </i>
 * <p>
 * <img alt="Command grammar" src="doc-files/commandGrammar.jpg"> <br>
 * 
 * <i>Figure 4: Grammar graph created from the Command grammar. </i>
 * 
 * <h3>Limitations</h3>
 * 
 * There is a known limitation with the current JSGF support. Grammars that
 * contain non-speech loops currently cause the recognizer to hang.
 * <p>
 * For example, in the following grammar
 * 
 * <pre>
 *  #JSGF V1.0
 *  grammar jsgf.nastygram;
 *  public &lt;nasty&gt; = I saw a ((cat* | dog* | mouse*)+)+;
 * </pre>
 * 
 * the production: ((cat* | dog* | mouse*)+)+ can result in a continuous loop,
 * since (cat* | dog* | mouse*) can represent no speech (i.e. zero cats, dogs
 * and mice), this is equivalent to ()+. To avoid this problem, the grammar
 * writer should ensure that there are no rules that could possibly match no
 * speech within a plus operator or kleene star operator.
 * 
 * <h3>Dynamic grammar behavior</h3> It is possible to modify the grammar of a
 * running application. Some rules and notes:
 * <ul>
 * <li>Unlike a JSAPI recognizer, the JSGF Grammar only maintains one Rule
 * Grammar. This restriction may be relaxed in the future.
 * <li>The grammar should not be modified while a recognition is in process
 * <li>The call to JSGFGrammar.loadJSGF will load in a completely new grammar,
 * tossing any old grammars or changes. No call to commitChanges is necessary
 * (although such a call would be harmless in this situation).
 * <li>RuleGrammars can be modified via calls to RuleGrammar.setEnabled and
 * RuleGrammar.setRule). In order for these changes to take place,
 * JSGFGrammar.commitChanges must be called after all grammar changes have been
 * made.
 * </ul>
 * 
 * <h3>Implementation Notes</h3>
 * <ol>
 * <li>All internal probabilities are maintained in LogMath log base.
 * </ol>
 */
public class JSGFGrammar extends JSGFBaseGrammar {

    public JSGFGrammar(String location, String grammarName,
                           boolean showGrammar, boolean optimizeGrammar,
                           boolean addSilenceWords, boolean addFillerWords,
                           Dictionary dictionary) throws MalformedURLException,
            ClassNotFoundException {
        this(ConfigurationManagerUtils.resourceToURL(location),
                grammarName, showGrammar, optimizeGrammar, addSilenceWords,
                addFillerWords, dictionary);
    }

    public JSGFGrammar(URL baseURL, String grammarName,
                           boolean showGrammar, boolean optimizeGrammar,
                           boolean addSilenceWords, boolean addFillerWords,
                           Dictionary dictionary) {
        super(baseURL, grammarName, showGrammar, optimizeGrammar, addSilenceWords, addFillerWords, dictionary);
    }

    public JSGFGrammar() {

    }

    // ///////////////////////////////////////////////////////////////////
    // Loading part
    // //////////////////////////////////////////////////////////////////

    private static URL grammarNameToURL(URL baseURL, String grammarName)
            throws MalformedURLException {

        // Convert each period in the grammar name to a slash "/"
        // Append a slash and the converted grammar name to the base URL
        // Append the ".gram" suffix
        grammarName = grammarName.replace('.', '/');
        StringBuilder sb = new StringBuilder();
        if (baseURL != null) {
            sb.append(baseURL);
            if (sb.charAt(sb.length() - 1) != '/')
                sb.append('/');
        }
        sb.append(grammarName).append(".gram");
        String urlstr = sb.toString();

        URL grammarURL = null;
        try {
            grammarURL = new URL(urlstr);
        } catch (MalformedURLException me) {
            grammarURL = ClassLoader.getSystemResource(urlstr);
            if (grammarURL == null)
                throw new MalformedURLException(urlstr);
        }

        return grammarURL;
    }

    /**
     * Commit changes to all loaded grammars and all changes of grammar since
     * the last commitChange
     * 
     * @throws JSGFGrammarParseException parse exception occurred
     * @throws JSGFGrammarException other exception occurred
     * @throws IOException exception during IO
     */
    @Override
    public void commitChanges() throws IOException, JSGFGrammarParseException,
            JSGFGrammarException {
        try {
            if (loadGrammar) {
                if (manager == null)
                    getGrammarManager();
                ruleGrammar = loadNamedGrammar(grammarName);
                loadImports(ruleGrammar);
                loadGrammar = false;
            }

            manager.linkGrammars();
            ruleStack = new RuleStack();
            newGrammar();

            firstNode = createGrammarNode("<sil>");
            GrammarNode finalNode = createGrammarNode("<sil>");
            finalNode.setFinalNode(true);

            // go through each rule and create a network of GrammarNodes
            // for each of them

            for (String ruleName : ruleGrammar.getRuleNames()) {
                if (ruleGrammar.isRulePublic(ruleName)) {
                    String fullName = getFullRuleName(ruleName);
                    GrammarGraph publicRuleGraph = new GrammarGraph();
                    ruleStack.push(fullName, publicRuleGraph);
                    JSGFRule rule = ruleGrammar.getRule(ruleName);
                    GrammarGraph graph = processRule(rule);
                    ruleStack.pop();

                    firstNode.add(publicRuleGraph.getStartNode(), 0.0f);
                    publicRuleGraph.getEndNode().add(finalNode, 0.0f);
                    publicRuleGraph.getStartNode().add(graph.getStartNode(),
                            0.0f);
                    graph.getEndNode().add(publicRuleGraph.getEndNode(), 0.0f);
                }
            }
            postProcessGrammar();
            if (logger.isLoggable(Level.FINEST)) {
                dumpGrammar();
            }
        } catch (MalformedURLException mue) {
            throw new IOException("bad base grammar URL " + baseURL + ' ' + mue);
        }
    }

    /**
     * Load grammars imported by the specified RuleGrammar if they are not
     * already loaded.
     *
     * @throws JSGFGrammarParseException
     */
    private void loadImports(JSGFRuleGrammar grammar) throws IOException,
            JSGFGrammarParseException {

        for (JSGFRuleName ruleName : grammar.imports) {
            // System.out.println ("Checking import " + ruleName);
            String grammarName = ruleName.getFullGrammarName();
            JSGFRuleGrammar importedGrammar = getNamedRuleGrammar(grammarName);

            if (importedGrammar == null) {
                // System.out.println ("Grammar " + grammarName +
                // " not found. Loading.");
                importedGrammar = loadNamedGrammar(ruleName
                        .getFullGrammarName());
            }
            if (importedGrammar != null) {
                loadImports(importedGrammar);
            }
        }
        loadFullQualifiedRules(grammar);
    }

    private JSGFRuleGrammar getNamedRuleGrammar(String grammarName) {
        return manager.retrieveGrammar(grammarName);
    }

    /**
     * Load named grammar from import rule
     *
     * @param grammarName
     * @return already loaded grammar
     * @throws JSGFGrammarParseException
     * @throws IOException
     */
    private JSGFRuleGrammar loadNamedGrammar(String grammarName)
            throws JSGFGrammarParseException, IOException {

        URL url = grammarNameToURL(baseURL, grammarName);
        JSGFRuleGrammar ruleGrammar = JSGFParser.newGrammarFromJSGF(url,
                new JSGFRuleGrammarFactory(manager));
        ruleGrammar.setEnabled(true);

        return ruleGrammar;
    }

    /**
     * Load grammars imported by a fully qualified Rule Token if they are not
     * already loaded.
     *
     * @param grammar
     * @throws IOException
     * @throws JSGFGrammarParseException
     */
    private void loadFullQualifiedRules(JSGFRuleGrammar grammar)
            throws IOException, JSGFGrammarParseException {

        // Go through every rule
        for (String ruleName : grammar.getRuleNames()) {
            String rule = grammar.getRule(ruleName).toString();
            // check for rule-Tokens
            int index = 0;
            while (index < rule.length()) {
                index = rule.indexOf('<', index);
                if (index < 0) {
                    break;
                }
                // Extract rule name
                JSGFRuleName extractedRuleName = new JSGFRuleName(rule
                        .substring(index + 1, rule.indexOf('>', index + 1))
                        .trim());
                index = rule.indexOf('>', index) + 1;

                // Check for full qualified rule name
                if (extractedRuleName.getFullGrammarName() != null) {
                    String grammarName = extractedRuleName.getFullGrammarName();
                    JSGFRuleGrammar importedGrammar = getNamedRuleGrammar(grammarName);
                    if (importedGrammar == null) {
                        importedGrammar = loadNamedGrammar(grammarName);
                    }
                    if (importedGrammar != null) {
                        loadImports(importedGrammar);
                    }
                }
            }
        }
    }


}
