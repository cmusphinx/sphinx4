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

import edu.cmu.sphinx.result.Result;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import javax.speech.recognition.GrammarException;


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

    private Set sampleSet;

    /**
     *  Creates a MyBehavior with the given grammar
     *  
     *  @param name the name of the grammar to be active with this
     *  node is the active node
     */
    public MyBehavior(String name) {
        super(name);
    }

    /**
     *  Executed when we enter this node. Displays the active grammar
     */
    public void onEntry() throws IOException {
        super.onEntry();
        System.out.println(" ======== " + getGrammarName() + " =====");
        dumpSampleUtterances();
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
        System.out.println(" result " + result);
        if (tag.startsWith("dialog_")) {
            return tag;
        } else {
            return null;
        }
    }

    /**
     *  Collects the set of possible utterances. 
     *  
     *  TODO: Note the current
     *  implementation just generates a large set of random utterances
     *  and tosses away any duplicates. There's no guarantee that this
     *  will generate all of the possible utterances.
     *
     *  @return the set of sample utterances
     */
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
    

    /**
     * Dumps out the set of sample utterances for this node
     */
    private void dumpSampleUtterances() {
        if (sampleSet == null) {
            sampleSet = collectSampleUtterances();
        }

        for (Iterator i = sampleSet.iterator(); i.hasNext(); ) {
            System.out.println("  " + i.next());
        }
    }
}



