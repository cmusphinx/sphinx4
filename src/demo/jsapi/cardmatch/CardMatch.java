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

package demo.jsapi.cardmatch;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

import demo.jsapi.cardmatch.CardMatchFrame;
import demo.jsapi.cardmatch.Game;
import demo.jsapi.cardmatch.Recorder;
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

/**
 * A recording device.
 */
public class CardMatch implements Recorder, Configurable {

    /**
     * The sphinx property for the recognizer to use
     */

    public static final String PROP_RECOGNIZER = "recognizer";

    /**
     * The sphinx property for the microphone to use
     */

    public static final String PROP_MICROPHONE = "microphone";

    /**
     * The sphinx property for the grammar to use
     */

    public static final String PROP_GRAMMAR = "grammar";

    /**
     * The SphinxProperty for the number of cards in this game.
     */
    public static final String PROP_NUM_CARDS = "numberOfCards";

    /**
     * The default value of PROP_NUM_CARDS.
     */
    public static final int PROP_NUM_CARDS_DEFAULT = 6;

    /**
     * The SphinxProperty for the image files.
     */
    public static final String PROP_IMAGE_FILES = "imageFiles";

    /**
     * The default value of PROP_IMAGE_FILES.
     */
    public static final String PROP_IMAGE_FILES_DEFAULT = null;

    /**
     * The SphinxProperty specifying whether to do endpointing.
     */
    public static final String PROP_DO_ENDPOINTING = "doEndpointing";

    /**
     * The default value of PROP_DO_ENDPOINTING.
     */
    public static final boolean PROP_DO_ENDPOINTING_DEFAULT = false;

    /**
     * The SphinxProperty specifying whether to use a voice.
     */
    public static final String PROP_USE_VOICE = "useVoice";

    /**
     * The default value of PROP_USE_VOICE.
     */
    public static final boolean PROP_USE_VOICE_DEFAULT = false;

    // ------------------------------
    // Configuration data
    // ------------------------------
    private String name;
    private Recognizer recognizer;
    private Microphone microphone;
    private JSGFGrammar grammar;
    private List imageFiles;
    private boolean doEndpointing;
    private boolean useVoice;
    private int numberOfCards;

    private CardMatchFrame cardMatchFrame;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        this.name = name;
        registry.register(PROP_RECOGNIZER, PropertyType.COMPONENT);
        registry.register(PROP_GRAMMAR, PropertyType.COMPONENT);
        registry.register(PROP_MICROPHONE, PropertyType.COMPONENT);

        registry.register(PROP_DO_ENDPOINTING, PropertyType.BOOLEAN);
        registry.register(PROP_USE_VOICE, PropertyType.BOOLEAN);

        registry.register(PROP_NUM_CARDS, PropertyType.INT);
        registry.register(PROP_IMAGE_FILES, PropertyType.STRING_LIST);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        recognizer = (Recognizer) ps.getComponent(PROP_RECOGNIZER,
                Recognizer.class);
        grammar = (JSGFGrammar) ps
                .getComponent(PROP_GRAMMAR, JSGFGrammar.class);
        microphone = (Microphone) ps.getComponent(PROP_MICROPHONE,
                Microphone.class);

        doEndpointing = ps.getBoolean(PROP_DO_ENDPOINTING,
                PROP_DO_ENDPOINTING_DEFAULT);
        useVoice = ps.getBoolean(PROP_USE_VOICE, PROP_USE_VOICE_DEFAULT);

        numberOfCards = ps.getInt(PROP_NUM_CARDS, PROP_NUM_CARDS_DEFAULT);
        imageFiles = ps.getStrings(PROP_IMAGE_FILES);

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Starts the card game
     *  
     */
    public void go() {
        try {
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
        } catch (IOException e) {
            System.err.println("Can't load recognizer " + e);
        }

    }

    /**
     * Returns an array of all the image file names.
     * 
     * @param props
     *                the SphinxProperties to get the file names from
     * 
     * @return an array of all the image file names
     */
    private String[] getImageFiles(String files) {

        if (files == null) {
            throw new IllegalStateException("No image files");
        } else {
            List fileList = new ArrayList();
            StringTokenizer tokenizer = new StringTokenizer(files);
            while (tokenizer.hasMoreTokens()) {
                String name = tokenizer.nextToken();
                if (name.length() > 0) {
                    fileList.add(name);
                }
            }
            return (String[]) fileList.toArray(new String[fileList.size()]);
        }
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

    /**
     * Drains the frontend of any remaining audio
     */
    private void drain() {
        // TODO: implement FrontEnd.drain();
        // decoder.getRecognizer().getFrontEnd().drain();
    }

    /**
     * Does decoding in a separate thread so that it does not block the calling
     * thread. It will automatically update the GUI components once the
     * decoding is completed. This is analogous to the "Control" components in
     * the MVC model.
     */
    class DecodingThread extends Thread {

        /**
         * Runs this DecodingThread.
         */
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
     * @param resultText
     *                the result text
     * 
     * @return the tag associated with the result
     */
    private String getResultTag(String resultText) {
        try {
            RuleGrammar ruleGrammar = grammar.getRuleGrammar();
            RuleParse ruleParse = ruleGrammar.parse(resultText, null);
            if (ruleParse != null) {
                String[] tags = ruleParse.getTags();
                assert tags.length == 1;
                return tags[0];
            } else {
                return null;
            }
        } catch (GrammarException ge) {
            ge.printStackTrace();
            return null;
        }
    }
    /**
     * Main method for running the CardMatch application.
     */
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
        } catch (InstantiationException e) {
            System.err.println("Problem creating cardmatch: " + e);
        }
    }

}
