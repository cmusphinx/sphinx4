/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package demo.sphinx.jsgf;

import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import java.io.IOException;
import java.net.URL;
import javax.speech.recognition.GrammarException;
import javax.speech.recognition.Rule;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

/**
 * A demonstration of how to use JSGF grammars in sphinx-4. This
 * program demonstrates how an application can cycle through a number of JSGF
 * grammars as well as how an application can build up grammar rules
 * directly
 *
 */
public class JSGFDemo {
    private Recognizer recognizer;
    private JSGFGrammar jsgfGrammarManager;
    private Microphone microphone;


    /**
     * Creates a new JSGFDemo. The demo will retrieve the
     * configuration from the jsgf.config.xml file found in the
     * classpath for the JSGFDemo. The jsgf.config.xml should at the
     * minimum define a recognizer with the name 'recognizer', a
     * JSGFGrammar named 'jsgfGrammar' and a Microphone named
     * 'microphone'.
     *
     * @throws IOException if an I/O error occurs
     * @throws PropertyException if a property configuration occurs
     * @throws InstantiationException if a problem occurs while
     * creating any of the recognizer components.
     */
    public JSGFDemo() throws 
            IOException, PropertyException, InstantiationException {

        URL url = JSGFDemo.class.getResource("jsgf.config.xml");
        ConfigurationManager cm = new ConfigurationManager(url);

        // retrive the recognizer, jsgfGrammar and the microphone from
        // the configuration file.

        recognizer = (Recognizer) cm.lookup("recognizer");
        jsgfGrammarManager = (JSGFGrammar) cm.lookup("jsgfGrammar");
        microphone = (Microphone) cm.lookup("microphone");
    }


    /**
     * Executes the demo. The demo will cycle through four separate
     * JSGF grammars,a 'movies' grammar, a 'news' grammar a 'books'
     * grammar and a 'music' grammar.  The news, books and movies
     * grammars are loaded from the corresponding JSGF grammar file,
     * while the music grammar is loaded from a file, but then
     * augmented via RuleGrammar.setRule.
     */
    public void execute() throws IOException, GrammarException  {
        System.out.println("JSGF Demo Version 1.0\n");

        System.out.print(" Loading recognizer ...");
        recognizer.allocate();
        System.out.println(" Ready");

        if (microphone.startRecording()) {
            loadAndRecognize("movies");
            loadAndRecognize("news");
            loadAndRecognize("books");
            loadAndRecognizeMusic();
        } else {
            System.out.println("Can't start the microphone");
        }

        System.out.print("\nDone. Cleaning up ...");
        recognizer.deallocate();

        System.out.println(" Goodbye.\n");
        System.exit(0);
    }

    /**
     * Load the grammar with the given grammar name and start
     * recognizing speech using the grammar.  Spoken utterances will
     * be echoed to the terminal.  This method will return when the
     * speaker utters the exit phrase for the grammar. The exit phrase
     * is a phrase in the grammar with the word 'exit' as a tag.
     *
     * @throws IOException if an I/O error occurs
     * @throws GrammarException if a grammar format error is detected
     */
    private void loadAndRecognize(String grammarName) throws
            IOException, GrammarException  {
        jsgfGrammarManager.loadJSGF(grammarName);
        dumpSampleSentences(grammarName);
        recognizeAndReport();
    }

    /**
     * Performs recognition with the currently loaded grammar.
     * Recognition for potentially multiple utterances until an 'exit'
     * tag is returned.
     *
     * @htrows GrammarException if an error in the JSGF grammar is
     * encountered
     */
    private void recognizeAndReport() throws GrammarException {
        boolean done = false;


        while (!done)  {
            Result result = recognizer.recognize();
            String bestResult = result.getBestFinalResultNoFiller();
            RuleGrammar ruleGrammar = jsgfGrammarManager.getRuleGrammar();
            RuleParse ruleParse = ruleGrammar.parse(bestResult, null);
            if (ruleParse != null) {
                System.out.println("\n  " + bestResult + "\n");
                done = isExit(ruleParse);
            } 
        }
    }

    /**
     * Searches through the tags of the rule parse for an 'exit' tag.
     *
     * @return true if an 'exit' tag is found
     */
    private boolean isExit(RuleParse ruleParse) {
        String[] tags = ruleParse.getTags();

        for (int i = 0; tags != null && i < tags.length; i++) {
            if (tags[i].trim().equals("exit")) {
                return true;
            }
        }
        return  false;
    }

    /**
     * Loads the music grammar and augments it with a set of rules for
     * playing the top dozen or so movie songs of all time.
     *
     * @throws IOException if an I/O error occurs
     * @throws GrammarException if a grammar format error is detected
     */
    private void loadAndRecognizeMusic() throws
            IOException, GrammarException  {
        jsgfGrammarManager.loadJSGF("music");
        RuleGrammar ruleGrammar = jsgfGrammarManager.getRuleGrammar();
        addRule(ruleGrammar, "song1", "listen to over the rainbow");
        addRule(ruleGrammar, "song2", "listen to as time goes by");
        addRule(ruleGrammar, "song3", "listen to singing in the rain");
        addRule(ruleGrammar, "song4", "listen to moon river");
        addRule(ruleGrammar, "song5", "listen to white christmas");
        addRule(ruleGrammar, "song6", "listen to mrs robinson");
        addRule(ruleGrammar, "song7", "listen to when you wish upon a star");
        addRule(ruleGrammar, "song8", "listen to the way we were");
        addRule(ruleGrammar, "song9", "listen to staying alive");
        addRule(ruleGrammar, "song10", "listen to the sound of music");
        addRule(ruleGrammar, "song11", "listen to the man that got away");
        addRule(ruleGrammar, "song12", 
                    "listen to diamonds are a girl's best friend");
        jsgfGrammarManager.commitChanges();
        dumpSampleSentences("music");
        recognizeAndReport();
    }

    /**
     * Adds the given jsgf as a rule to a grammar
     *
     * @param ruleGrammar the grammar to receive the new rule
     * @param ruleName the name of the rule
     * @param jsgf the definition for the new rule
     */
    private void addRule(RuleGrammar ruleGrammar, 
                String ruleName, String jsgf) throws GrammarException {
        Rule newRule = ruleGrammar.ruleForJSGF(jsgf);
        ruleGrammar.setRule(ruleName, newRule, true);
        ruleGrammar.setEnabled(ruleName, true);
    }



    /**
     * Dumps out a set of sample sentences for this grammar.  
     *  TODO: Note the current
     *  implementation just generates a large set of random utterances
     *  and tosses away any duplicates. There's no guarantee that this
     *  will generate all of the possible utterances. (yep, this is a hack)
     *
     */
    private void dumpSampleSentences(String title) {
        System.out.println(" ====== " + title + " ======");
        System.out.println("Speak one of: \n");
        jsgfGrammarManager.dumpRandomSentences(200);
        System.out.println(" ============================");
    }
    
    /**
     * Main method for running the jsgf demo.
     * @param args program arguments (none)
     */
    public static void main(String[] args) {
        try {
            JSGFDemo jsgfDemo = new JSGFDemo();
            jsgfDemo.execute();
        } catch (IOException ioe) {
            System.out.println("I/O Error " + ioe);
        } catch (PropertyException e) {
            System.out.println("Problem configuring recognizer" + e);
        } catch (InstantiationException  e) {
            System.out.println("Problem creating components " + e);
        } catch (GrammarException  e) {
            System.out.println("Problem with Grammar " + e);
        }
    }
}
