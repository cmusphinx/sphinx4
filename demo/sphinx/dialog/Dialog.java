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

package demo.sphinx.dialog;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;


/**
 * A simple Dialog demo showing a simple speech application 
 * built using Sphinx-4 that uses the DialogManager. This 
 * application uses the Sphinx-4 endpointer,
 * which automatically segments incoming audio into utterances and silences.
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


            System.out.println("Initializing ....");

            dialogManager.addNode("dialog_menu", new MyBehavior("menu"));
            dialogManager.addNode("dialog_email", new MyBehavior("email"));
            dialogManager.addNode("dialog_games", new MyBehavior("games"));
            dialogManager.addNode("dialog_news", new MyBehavior("news"));
            dialogManager.addNode("dialog_news", new MyBehavior("music"));
            dialogManager.addNode("dialog_news", new MyBehavior("movies"));

            dialogManager.setInitialNode("dialog_menu");

            System.out.println("Loading ....");

            dialogManager.allocate();

            System.out.println("Running  ....");

            dialogManager.go();

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



class MyBehavior extends NewGrammarDialogNodeBehavior {

    private Set sampleSet;

    public MyBehavior(String name) {
        super(name);
    }

    public void onEntry() throws IOException {
        super.onEntry();
        System.out.println(" ======== " + getGrammarName() + " =====");
        dumpSampleUtterances();
    }

    public String onRecognize(Result result) throws GrammarException {
        String tag = super.onRecognize(result);
        System.out.println(" result " + result);
        if (tag.startsWith("dialog_")) {
            return tag;
        } else {
            return null;
        }
    }

    private Set collectSampleUtterances() {
        Set set = new HashSet();
        for (int i = 0; i < 100; i++) {
            String s = getGrammar().getRandomSentence();
            if (!set.contains(s)) {
                set.add(s);
            }
        }
        return set;
    }
    

    private void dumpSampleUtterances() {
        if (sampleSet == null) {
            sampleSet = collectSampleUtterances();
        }

        for (Iterator i = sampleSet.iterator(); i.hasNext(); ) {
            System.out.println("  " + i.next());
        }
    }
}



