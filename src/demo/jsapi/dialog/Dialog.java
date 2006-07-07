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

package demo.jsapi.dialog;

import edu.cmu.sphinx.result.Result;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.recognizer.Recognizer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.Rule;


/**
 * A simple Dialog demo showing a simple speech application 
 * built using Sphinx-4 that uses the DialogManager. 
 *
 * This demo uses a DialogManager to manage a set of dialog states.
 * Each dialog state potentially has its own grammar. 
 */
public class Dialog {

    /**
     * Main method for running the Dialog demo.
     */
    public static void main(String[] args) {
        try {
            URL url;
            if (args.length > 0) {
                url = new File(args[0]).toURI().toURL();
            } else {
                url = Dialog.class.getResource("dialog.config.xml");
            }
            ConfigurationManager cm = new ConfigurationManager(url);

            DialogManager dialogManager = (DialogManager)
                cm.lookup("dialogManager");

            Recognizer weatherRecognizer = (Recognizer)
                cm.lookup("weatherRecognizer");


            System.out.println("\nWelcome to the Sphinx-4 Dialog Demo "
                    + " - Version 1.0\n");

            dialogManager.addNode("menu",   new MyBehavior());
            dialogManager.addNode("email",  new MyBehavior());
            dialogManager.addNode("games",  new MyBehavior());
            dialogManager.addNode("news",   new MyBehavior());
            dialogManager.addNode("music",  new MyMusicBehavior());
            dialogManager.addNode("movies", new MyBehavior());
            dialogManager.addNode("phone",  new MyBehavior());
            dialogManager.addNode("books",  new MyBehavior());
            dialogManager.addNode("weather",new WeatherNode(weatherRecognizer));

            dialogManager.setInitialNode("menu");

            System.out.println("Loading dialogs ...");

            dialogManager.allocate();
            System.out.println("Loading weather recognizer ...");
            weatherRecognizer.allocate();


            System.out.println("Running  ...");

            dialogManager.go();

            System.out.println("Cleaning up  ...");

            dialogManager.deallocate();

        } catch (IOException e) {
            System.err.println("Problem when loading Dialog: " + e);
        } catch (PropertyException e) {
            System.err.println("Problem configuring Dialog: " + e);
        } catch (InstantiationException e) {
            System.err.println("Problem creating Dialog: " + e);
        }
        System.exit(0);
    }
}


/**
 *  Defines the standard behavior for a node. The standard behavior
 *  is:
 *  <ul>
 *  <li> On entry the set of sentences that can be spoken is
 *  displayed.
 *  <li> On recognition if a tag returned contains the prefix 'dialog_' it
 *  indicates that control should transfer to another dialog node.
 *  </ul>
 */
class MyBehavior extends NewGrammarDialogNodeBehavior {
    private Collection sampleUtterances;


    /**
     *  Executed when we are ready to recognize
     */
    public void onReady() {
        super.onReady();
        help();
    }

    /**
     * Displays the help message for this node. Currently we display
     * the name of the node and the list of sentences that can be
     * spoken.
     */
    protected void help() {
        System.out.println(" ======== " + getGrammarName() + " =======");
        dumpSampleUtterances();
        System.out.println(" =================================");
    }

    /**
     * Executed when the recognizer generates a result. Returns the
     * name of the next dialog node to become active, or null if we
     * should stay in this node
     *
     * @param result the recongition result
     * @return the name of the next dialog node or null if control
     * should remain in the current node.
     */
    public String onRecognize(Result result) throws GrammarException {
        String tag = super.onRecognize(result);

        if (tag != null) {
            System.out.println("\n " 
                    + result.getBestFinalResultNoFiller() + "\n");
            if (tag.equals("exit")) {
                System.out.println("Goodbye! Thanks for visiting!\n");
                System.exit(0);
            } if (tag.equals("help")) {
                help();
            } else if (tag.equals("stats")) {
                Timer.dumpAll();
            } else if (tag.startsWith("goto_")) {
                return tag.replaceFirst("goto_", "");
            }  else if (tag.startsWith("browse")) {
                execute(tag);
            }
        } else {
            System.out.println("\n Oops! didn't hear you.\n");
        }
        return null;
    }


    /**
     * execute the given command
     *
     * @param cmd the command to execute
     */
    private void execute(String cmd) {
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            // if we can't run the command, just fall back to
            // a non-working demo.
        }
    }

    /**
     *  Collects the set of possible utterances. 
     *  
     *  TODO: Note the current
     *  implementation just generates a large set of random utterances
     *  and tosses away any duplicates. There's no guarantee that this
     *  will generate all of the possible utterances. (yep, this is a hack)
     *
     *  @return the set of sample utterances
     */
    private Collection collectSampleUtterances() {
        Set set = new HashSet();
        for (int i = 0; i < 100; i++) {
            String s = getGrammar().getRandomSentence();
            if (!set.contains(s)) {
                set.add(s);
            }
        }

        List sampleList = new ArrayList(set);
        Collections.sort(sampleList);
        return sampleList;
    }
    

    /**
     * Dumps out the set of sample utterances for this node
     */
    private void dumpSampleUtterances() {
      if (sampleUtterances == null) {
            sampleUtterances = collectSampleUtterances();
      }

      for (Iterator i = sampleUtterances.iterator(); i.hasNext(); ) {
        System.out.println("  " + i.next());
      }
   }

    /**
     * Indicated that the grammar has changed and the collection
     * of sample utterances should be regenerated.
     */
    protected void grammarChanged() {
        sampleUtterances = null;
    }
}

/**
 * An extension of the standard node behavior for music. This node will
 * add rules to the grammar based upon the contents of the music.txt
 * file. This provides an example of how to extend a grammar directly from
 * code as opposed to writing out a JSGF file.
 */
class MyMusicBehavior extends MyBehavior {
    private List songList = new ArrayList();

    /**
     * Creates a music behavior
     *
     */
    MyMusicBehavior() {
        try {
            InputStream is = Dialog.class.getResourceAsStream("playlist.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String song;

            while ((song = br.readLine()) != null) {
                if (song.length() > 0) {
                    songList.add(song);
                }
            }
            br.close();
        } catch (IOException ioe) {
            System.err.println("Can't get playlist");
        }
    }

    /**
     *  Executed when we enter this node. Displays the active grammar
     */
    public void onEntry() throws IOException {
        super.onEntry();

        // now lets add our custom songs from the play list
        // First, get the JSAPI RuleGrammar

        RuleGrammar ruleGrammar = getGrammar().getRuleGrammar();

        // now lets add a rule for each song in the play list

        String ruleName = "song";
        int count = 1;

        try {
            for (Iterator i = songList.iterator(); i.hasNext(); ) {
                String song = (String) i.next();
                String newRuleName = ruleName + count;
                Rule newRule = ruleGrammar.ruleForJSGF("listen to " + song 
                        + " { " + newRuleName + " }" );
                ruleGrammar.setRule(newRuleName, newRule, true);
                ruleGrammar.setEnabled(newRuleName, true);
                count++;
            }
        } catch (GrammarException ge) {
            System.out.println("Trouble with the grammar " + ge);
            throw new IOException("Can't add rules for playlist " + ge);
        }
        // now lets commit the changes
        getGrammar().commitChanges();
        grammarChanged();
    }

    /**
     * Dumps out the rules of the given rule grammar, used for
     * debugging.
     *
     * @param rg the rule grammar to dump
     */
    private void dumpGrammar(RuleGrammar rg) {
        String[] ruleNames = rg.listRuleNames();

        for (int i = 0; i < ruleNames.length; i++) {
            String enabled = rg.isEnabled(ruleNames[i]) ? "ON" : "OFF";
            System.out.println("Rule: " + ruleNames[i] + " " +
                    rg.getRule(ruleNames[i]) + " " + enabled);
        }
    }
}

/**
 *  Defines the behavior for a weather node. The weather node allows
 *  the user to dictate a weather forecast.  To do this we can't use a
 *  JSGF grammar since JSGF grammars are not appropriate for
 *  dictation, so instead we have a special weather node that will
 *  install a 'weather recognizer' as the current recognizer when this
 *  node is active.  The weather recognizer is configured to use a
 *  language model suitable for weather forecasts.
 */

class WeatherNode extends DialogNodeBehavior {
    private Recognizer weatherRecognizer;
    private Recognizer savedRecognizer;
    private DialogManager dialogManager;

    /**
     * Creates the WeatherNode
     *
     * @param recognizer the weather recognizer
     */
    public WeatherNode(Recognizer recognizer) {
            this.weatherRecognizer = recognizer;
    }

    /**
     * Called during the initialization phase
     *
     * @param node the dialog node that the behavior is attached to
     */
    public void onInit(DialogManager.DialogNode node) {
        super.onInit(node);
        dialogManager = node.getDialogManager();
    }

    /**
     * Called when this node becomes the active node
     */
    public void onEntry() throws IOException {
        savedRecognizer = dialogManager.getRecognizer();
        dialogManager.setRecognizer(weatherRecognizer);
        System.out.println("  Give your best imitation of ");
        System.out.println("  a british weather forecaster");
        System.out.println();
        System.out.println(" Say 'forecast over' when you are done.");
        System.out.println();
    }

    /**
     * Called when this node is ready to perform recognition
     */
    public void onReady() {
        trace("Ready " + getName());
    }

    /*
     * Called with the recognition results. Should return a string
     * representing the name of the next node.
     *
     * @return a tag indicating where to go next. An empty string
     * indicates that control should stay in this node.
     */
    public String onRecognize(Result result) throws GrammarException {
        trace("Recognize result: " + result.getBestFinalResultNoFiller());
        String forecast = result.getBestFinalResultNoFiller();

        System.out.println("Forecast: " + forecast);

        if ("forecast over".equals(forecast)) {
            return "menu";
        }
        return "";
    }

    /**
     * Called when this node is no lnoger the active node
     */
    public void onExit() {
        System.out.println();
        dialogManager.setRecognizer(savedRecognizer);
    }
}
