
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

import edu.cmu.sphinx.decoder.Decoder;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.SphinxProperties;

import java.io.File;
import java.io.IOException;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;


/**
 * A recording device.
 */
public class CardMatch implements Recorder {

    private static final String PROP_PREFIX = "demo.jsapi.CardMatch.";

    private static final String PROP_NUM_CARDS = PROP_PREFIX + "numberOfCards";
    private static final int PROP_NUM_CARDS_DEFAULT = 6;

    private static final String PROP_IMAGE_FILES = PROP_PREFIX + "imageFiles";
    
    private static final String PROP_DO_ENDPOINTING = 
        PROP_PREFIX + "doEndpointing";
    private static final boolean PROP_DO_ENDPOINTING_DEFAULT = false;

    private static final String PROP_USE_VOICE = PROP_PREFIX + "useVoice";
    private static final boolean PROP_USE_VOICE_DEFAULT = true;


    private int goodGuessIndex = 0;
    private String[] goodGuessText =
        {"You can see through the cards.",
         "Lucky you!",
         "That is not bad.",
         "No cheating please.",
         "Good for you!",
         "Way to go!"};

    private int badGuessIndex = 0;
    private String[] badGuessText =
        {"I'm sorry!", 
         "You are wrong again!",
         "What a shame.",
         "What is wrong with you?",
         "Can you remember anything?"};
         

    private Decoder decoder;
    private Microphone microphone;
    private CardMatchFrame cardMatchFrame;
    private CardMatchVoice voice;

    private Game game;
    private RuleGrammar ruleGrammar;
    private boolean doEndpointing;
    private boolean useVoice;


    /**
     * Constructs a default CardMatch object.
     */
    public CardMatch(String context) throws IOException {

        System.out.println("Loading decoder...");
	
	decoder = new Decoder(context);
        SphinxProperties props = decoder.getSphinxProperties();

        doEndpointing = props.getBoolean(PROP_DO_ENDPOINTING,
                                         PROP_DO_ENDPOINTING_DEFAULT);

        useVoice = props.getBoolean(PROP_USE_VOICE,
                                    PROP_USE_VOICE_DEFAULT);

	microphone = new Microphone("mic", context, props);
	decoder.initialize(microphone);
        System.out.println("decoder initialized");

        int numberOfCards = props.getInt
            (PROP_NUM_CARDS, PROP_NUM_CARDS_DEFAULT);
        String[] imageFiles = getImageFiles(props);
        System.out.println("parsed image files");

        game = new Game(numberOfCards, imageFiles);
        System.out.println("created and shuffled cards");

        if (useVoice) {
            try {
                voice = new CardMatchVoice();
                System.out.println("CardMatchVoice loaded");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

	cardMatchFrame = new CardMatchFrame
            ("Card Match", this, game.getCards());
	cardMatchFrame.show();

        JSGFGrammar grammar = 
            (JSGFGrammar)decoder.getRecognizer().getGrammar();
        ruleGrammar = grammar.getRuleGrammar();
    }


    /**
     * Returns an array of all the image file names.
     *
     * @param the SphinxProperties to get the file names from
     *
     * @return an array of all the image file names
     */
    private String[] getImageFiles(SphinxProperties props) {
        String files = props.getString(PROP_IMAGE_FILES, null);
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
     */
    public boolean startRecording() {
	System.out.println("Draining ...");
        decoder.getRecognizer().getFrontEnd().drain();
	System.out.println("Clearing ...");
        microphone.clear();
	System.out.println("Starting ...");
	boolean started = microphone.startRecording();
        if (started) {
	    System.out.println("Started!");
            (new DecodingThread()).start();
        } else {
	    System.out.println("Not started!");
	}
        return started;
    }


    /**
     * Stops recording.
     */
    public boolean stopRecording() {
	microphone.stopRecording();
	System.out.println("Stopped!");
	return true;
    }


    /**
     * Returns true if this Recorder is recording.
     *
     * @return true if this Recorder is recording
     */
    public boolean isRecording() {
	return microphone.getRecording();
    }


    /**
     * Speaks the given string of text.
     */
    private void speak(String text) {
        if (useVoice && voice != null) {
            voice.speak(text);
        }
    }


    /**
     * Returns a line of text saying its a good guess.
     *
     * @return a line of text saying its a good guess
     */
    private String getGoodGuessText() {
        String text = goodGuessText[goodGuessIndex];
        goodGuessIndex++;
        goodGuessIndex %= goodGuessText.length;
        return text;
    }


    /**
     * Returns a line of text saying its a bad guess.
     *
     * @return a line of text saying its a bad guess
     */
    private String getBadGuessText() {
        String text = badGuessText[badGuessIndex];
        badGuessIndex++;
        badGuessIndex %= badGuessText.length;
        return text;
    }


    /**
     * Does decoding in a separate thread so that it does not
     * block the calling thread. It will automatically update
     * the GUI components once the decoding is completed. This
     * is analogous to the "Control" components in the MVC model.
     */
    class DecodingThread extends Thread {

        /**
         * Runs this DecodingThread.
         */
        public void run() {
            System.out.println("Start decoding...");
            Result result = decoder.decode();
            
            if (doEndpointing) {
                microphone.stopRecording();
            }

            System.out.println("...stop decoding");
            String resultText = result.getBestResultNoSilences();

            try {
                String tag = getResultTag(resultText);
                System.out.println("ACTION tag: " + tag);

                if (tag != null) {
                    if (tag.equals("new_game")) {
                        game.startOver();
                        goodGuessIndex = 0;
                        badGuessIndex = 0;
                        cardMatchFrame.updateCardPanel();
                        speak("New game. You can guess now.");
                    } else {
                        // or it tag is "1", "2" ... "6"
                        game.turnCard(tag);
                        cardMatchFrame.invalidate();
                        cardMatchFrame.repaint();
                        (new MatchingThread()).start();
                    }
                }
            } catch (GrammarException ge) {
                ge.printStackTrace();
            } finally {
                cardMatchFrame.setResultTextField(resultText);
                cardMatchFrame.setSpeakButtonSelected(false);
                (new DrainingThread()).start();
            }
        }


        /**
         * Returns the tag for the given result text.
         */
        private String getResultTag(String resultText) 
            throws GrammarException {
            JSGFGrammar grammar = 
                (JSGFGrammar)decoder.getRecognizer().getGrammar();
            RuleGrammar ruleGrammar = grammar.getRuleGrammar();
            RuleParse ruleParse = ruleGrammar.parse(resultText, null);
            if (ruleParse != null) {
                String[] tags = ruleParse.getTags();
                assert tags.length == 1;
                return tags[0];
            } else {
                return null;
            }
        }
    }


    class MatchingThread extends Thread {
        public void run() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            if (game.hasTwoGuesses()) {
                if (game.checkForMatches()) {
                    if (game.hasWon()) {
                        speak
                            ("Ha ha! You finally got all the cards. " +
                             "Congratulations!");
                    } else {
                        speak(getGoodGuessText());
                    }
                } else {
                    speak(getBadGuessText());
                    game.turnGuessedCards();
                }
            }
        }
    }


    class DrainingThread extends Thread {
        public void run() {
            decoder.getRecognizer().getFrontEnd().drain();
        }
    }


    /**
     * Main method for running the CardMatch application.
     */
    public static void main(String[] argv) {
	try {
	    String propertiesFile = argv[0];
	    String pwd = System.getProperty("user.dir");
	    String context = "CardMatch";

	    SphinxProperties.initContext
		(context, new URL
		 ("file://" + pwd + File.separatorChar + propertiesFile));
	    
	    CardMatch cardMatch = new CardMatch(context);

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
