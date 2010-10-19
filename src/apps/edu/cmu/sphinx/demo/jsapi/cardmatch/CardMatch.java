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

package edu.cmu.sphinx.demo.jsapi.cardmatch;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.*;

import javax.speech.EngineException;
import javax.speech.EngineStateError;
import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

import com.sun.speech.engine.recognition.BaseRecognizer;
import com.sun.speech.engine.recognition.BaseRuleGrammar;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/** A recording device. */
public class CardMatch implements Recorder, Configurable {

    /** The property for the recognizer to use */
    @S4Component(type = Recognizer.class)
    public static final String PROP_RECOGNIZER = "recognizer";

    /** The property for the microphone to use */
    @S4Component(type = Microphone.class)
    public static final String PROP_MICROPHONE = "microphone";

    /** The property for the grammar to use */
    @S4Component(type = JSGFGrammar.class)
    public static final String PROP_GRAMMAR = "grammar";

    /** The property for the number of cards in this game. */
    @S4Integer(defaultValue = 6)
    public static final String PROP_NUM_CARDS = "numberOfCards";

    /** The property for the image files. */
    @S4String(defaultValue = "")
    public static final String PROP_IMAGE_FILES = "imageFiles";

    /** The property specifying whether to do endpointing. */
    @S4Boolean(defaultValue = false)
    public static final String PROP_DO_ENDPOINTING = "doEndpointing";

    /** The property specifying whether to use a voice. */
    @S4Boolean(defaultValue = false)
    public static final String PROP_USE_VOICE = "useVoice";

    // ------------------------------
    // Configuration data
    // ------------------------------
    private String name;
    private Recognizer recognizer;
    private Microphone microphone;
    private JSGFGrammar grammar;
    private List<String> imageFiles;
    private boolean doEndpointing;
    private boolean useVoice;
    private int numberOfCards;

    private CardMatchFrame cardMatchFrame;


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    public void newProperties(PropertySheet ps) throws PropertyException {
        recognizer = (Recognizer) ps.getComponent(PROP_RECOGNIZER);
        grammar = (JSGFGrammar) ps.getComponent(PROP_GRAMMAR);
        microphone = (Microphone) ps.getComponent(PROP_MICROPHONE);

        doEndpointing = ps.getBoolean(PROP_DO_ENDPOINTING);
        useVoice = ps.getBoolean(PROP_USE_VOICE);

        numberOfCards = ps.getInt(PROP_NUM_CARDS);
        imageFiles = Arrays.asList(ps.getString(PROP_IMAGE_FILES).split(";"));
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#getName()
    */
    public String getName() {
        return name;
    }


    /** Starts the card game */
    public void go() {
        System.out.println("Loading recognizer, please wait ...");
        recognizer.allocate();
        System.out.println("Here we go ...");
        Game game = new Game(numberOfCards, imageFiles);
        cardMatchFrame = new CardMatchFrame("Card Match", this, game, useVoice);

        // add a listener for closing this JFrame and quitting the program
        cardMatchFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                recognizer.deallocate();
                System.exit(0);
            }
        });

        cardMatchFrame.setVisible(true);

    }


    /**
     * Starts recording.
     *
     * @return <code>true</code> if recording started successfully.
     */
    public boolean startRecording() {
        // drain();
        System.out.println("Wait, turning on mic... ");
        microphone.clear();
        boolean started = microphone.startRecording();
        if (started) {
            (new DecodingThread()).start();
            System.out.println("You can speak now.");
        } else {
            System.out.println("Error turning on mic.");
        }
        return started;
    }


    /**
     * Stops recording.
     *
     * @return <code>true</code> if the recording was stopped properly
     */
    public boolean stopRecording() {
        microphone.stopRecording();
        System.out.println("Microphone stopped.");
        return true;
    }


    /**
     * Returns true if this Recorder is recording.
     *
     * @return <code>true</code> if this Recorder is recording
     */
    public boolean isRecording() {
        return microphone.isRecording();
    }


    /** Drains the frontend of any remaining audio */
    private void drain() {
        // TODO: implement FrontEnd.drain();
        // decoder.getRecognizer().getFrontEnd().drain();
    }


    /**
     * Does decoding in a separate thread so that it does not block the calling thread. It will automatically update the
     * GUI components once the decoding is completed. This is analogous to the "Control" components in the MVC model.
     */
    class DecodingThread extends Thread {

        /** Runs this DecodingThread. */
        public void run() {
            Result result = recognizer.recognize();

            if (doEndpointing) {
                stopRecording();
            }

            if (result != null) {
                String resultText = result.getBestResultNoFiller();
                String tag = getResultTag(resultText);
                cardMatchFrame.processResults(resultText, tag);
            }
            drain();
        }
    }


    /**
     * Returns the tag for the given result text.
     *
     * @param resultText the result text
     * @return the tag associated with the result
     */
    private String getResultTag(String resultText) {
        try {
            BaseRecognizer recognizer = new BaseRecognizer(grammar.getGrammarManager());
            recognizer.allocate();
            RuleGrammar ruleGrammar = new BaseRuleGrammar(recognizer, grammar.getRuleGrammar());
            RuleParse ruleParse = ruleGrammar.parse(resultText, null);
            if (ruleParse != null) {
                String[] tags = ruleParse.getTags();
                assert tags.length == 1;
                return tags[0];
            } else {
                return null;
            }
        } catch (GrammarException e) {
            e.printStackTrace();
        } catch (EngineException e) {
            e.printStackTrace();
        } catch (EngineStateError e) {
            e.printStackTrace();
        }
        return null;
    }


    /** Main method for running the CardMatch application. */
    public static void main(String[] args) {
        try {
            URL url;
            if (args.length > 0) {
                url = new File(args[0]).toURI().toURL();
            } else {
                url = CardMatch.class.getResource("cardmatch.config.xml");
            }
            ConfigurationManager cm = new ConfigurationManager(url);
            CardMatch cardMatch = (CardMatch) cm.lookup("cardMatch");
            cardMatch.go();
        } catch (IOException e) {
            System.err.println("Problem when loading cardmatch: " + e);
        } catch (PropertyException e) {
            System.err.println("Problem configuring cardmatch: " + e);
        }
    }

}
