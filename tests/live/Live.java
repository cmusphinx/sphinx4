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

package tests.live;

import edu.cmu.sphinx.decoder.Decoder;

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.frontend.util.Util;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.NISTAlign;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.text.DecimalFormat;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sound.sampled.LineUnavailableException;

import javax.swing.DefaultComboBoxModel;

import tests.frontend.AudioPlayer;


/**
 * The live decoder main program. This class contains the control logic.
 */
public class Live {

    private static DecimalFormat timeFormat = new DecimalFormat("0.00");
    private DefaultComboBoxModel decoderNameList;
    private Map decoders;

    private AudioPlayer audioPlayer = null; // for play the recording
    private LiveDecoder decoder = null;     // for decoding, obviously
    private LiveFrame liveFrame = null;
    private Result lastResult = null;
    private File currentDirectory = null;

    private boolean showPartialResults = false;
    private boolean hasEndpointer;
    private boolean handsFree;              // uses endpointer

    /**
     * Main program.
     *
     * @param argv first argument should be the decoder list file
     */
    public static void main(String[] argv) {
        if (argv.length < 1) {
            System.out.println("No decoder list file.");
        } else {

            // initialize the LiveDecoder list
            Live live = null;
            String decoderListFile = argv[0];
            try {
                live = new Live(decoderListFile);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.exit(1);
            } catch (LineUnavailableException lue) {
                lue.printStackTrace();
                System.exit(1);
            }
        }
    }


    /**
     * Constructs a Live program with the given file that lists
     * all the available decoders. The first decoder listed in
     * the file will be created.
     *
     * @param decodersFile a file listing all the available decoders
     */
    public Live(String decoderListFile) throws IOException, 
                                               LineUnavailableException {
        String pwd = System.getProperty("user.dir");
        currentDirectory = new File(pwd);

        showPartialResults = Boolean.getBoolean("showPartialResults");
        handsFree = Boolean.getBoolean("handsFree");
        
        decoderNameList = new DefaultComboBoxModel();
        decoders = new HashMap();

        // parse the decoder's list
        parseDecoderListFile(decoderListFile);

        // initialize the Swing GUI JFrame
        liveFrame = new LiveFrame("Live Decoder!", this);
        liveFrame.show();

        initializeFirstDecoder();
        audioPlayer = new AudioPlayer();
    }

    /**
     * Load the first decoder in the list of decoders.
     */
    private void initializeFirstDecoder() {
        // initialize the first decoder
        if (decoderNameList.getSize() > 0) {
            String firstDecoder = System.getProperty("firstDecoder");
            if (firstDecoder == null) {
                firstDecoder = (String) decoderNameList.getElementAt(0);
            }
            info("Initializing first decoder: "  + firstDecoder + " ...\n");
            liveFrame.setDecoderComboBox(firstDecoder);
            info("... done initializing\n");
        }
    }


    /**
     * Returns the LiveFrame.
     */
    public LiveFrame getLiveFrame() {
        return liveFrame;
    }


    /**
     * Terminates this Live object, terminates all Microphones owned
     * by the LiveDecoders.
     */
    public void terminate() {
        for (Iterator i = decoders.values().iterator(); i.hasNext();) {
            LiveDecoder decoder = (LiveDecoder) i.next();
            if (decoder.getMicrophone() != null) {
                decoder.getMicrophone().terminate();
            }
        }
    }


    /**
     * Start decoding.
     */
    public void decode() {
        (new DecodingThread()).start();
    }

    /**
     * Start recording.
     */
    public boolean startRecording() {
        Microphone microphone = getDecoder().getMicrophone();
        microphone.clear();
        return microphone.startRecording();
    }

    /**
     * Stop recording.
     */
    public void stopRecording() {
        getDecoder().getMicrophone().stopRecording();
        if (!handsFree) {
            liveFrame.setButtonsEnabled(false);
        }
    }

    /**
     * Plays the last recorded utterance.
     */
    public void playUtterance() {
        Microphone microphone = getDecoder().getMicrophone();
        byte[] audio = microphone.getUtterance().getAudio();
        if (audio != null) {
            audioPlayer.play
                (audio, microphone.getAudioFormat());
        }
        System.out.println("Finished playing utterance.");
    }


    /**
     * Resets the statistics in the NISTAlign of the current Decoder.
     */
    public void resetStatistics() {
        NISTAlign aligner = getDecoder().getNISTAlign();
        aligner.resetTotals();
        getDecoder().resetSpeed();
        updateLiveFrame(aligner);
    }


    /**
     * Returns the Decoder that is currently in use.
     *
     * @return the decoder that is currently in use
     */
    public LiveDecoder getDecoder() {
        return decoder;
    }


    /**
     * Sets the current Decoder to the one with the given name.
     *
     * @param decoderName name of the Decoder to use 
     */
    public void setDecoder(String decoderName) throws IOException {
        String changeMessage = "Changing to " + decoderName + " decoder\n";
        info(changeMessage);
        liveFrame.setMessage(changeMessage);

        try {
            decoder = getDecoder(decoderName);
            if (!decoder.isInitialized()) {
                decoder.initialize
		    (new Microphone("mic", decoderName,
				    decoder.getSphinxProperties()));
            }            
        } catch (LineUnavailableException lue) {
            // if the audio line is unavailable for some reason
            String errorMessage = "Cannot change to " + decoderName;
            liveFrame.setMessage(errorMessage);
            lue.printStackTrace();
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // if the decoder switch is successful
        liveFrame.setTestFile(getDecoder().getTestFile());
        liveFrame.setReferenceLabel(getDecoder().getNextReference());
        liveFrame.setMessage("Using " + decoderName + " decoder");
        
        info("... done changing\n");
    }


    /**
     * Returns the DefaultComboBoxList that stores all the decoder names.
     *
     * @return the DefaultComboBoxList that stores all the decoder names
     */
    public DefaultComboBoxModel getDecoderList() {
        return decoderNameList;
    }


    /**
     * Returns the current directory.
     *
     * @return the current directory
     */
    public File getCurrentDirectory() {
        return currentDirectory;
    }


    /**
     * Sets the current directory.
     *
     * @param currentDirectory sets the current directory
     */
    public void setCurrentDirectory(File currentDirectory) {
        this.currentDirectory = currentDirectory;
    }


    /**
     * Prints an info message to System.out.
     *
     * @param message the message to print
     */
    private void info(String message) {
        System.out.print(message);
    }


    /**
     * Parses the given list of decoders, and create a
     * SynthesizerProperties for each of them. Also creates
     * all the LiveDecoders.
     *
     * @param decoderListFile a file listing all the available decoders
     */
    private void parseDecoderListFile(String decoderListFile) throws 
    IOException, LineUnavailableException {

        info("Parsing file " + decoderListFile + " ");

        Properties properties = new Properties();
        properties.load(new FileInputStream(decoderListFile));

        String decoderLine = properties.getProperty("decoders");
        String[] decoders = decoderLine.split(" ");

        for (int i = 0; i < decoders.length; i++) {

            // name of the decoder
            String decoderName = properties.getProperty(decoders[i] + ".name");
            if (decoderName == null) {
                throw new NullPointerException
                    ("No name for decoder " + decoders[i]);
            }

            // properties file
            String propertiesFile = properties.getProperty
                (decoders[i] + ".propertiesFile");
            if (propertiesFile == null) {
                throw new NullPointerException
                    ("No properties file for decoder " + decoders[i]);
            }

            // initialize the SphinxProperty object
            SphinxProperties.initContext(decoderName, 
                                         (new File(propertiesFile)).toURL());
            
            // transcript file
            String testFile = properties.getProperty
                (decoders[i] + ".testFile");
            
            // add the name of the decoder to the decoderNameList
            decoderNameList.addElement(decoderName);
            
            // create the LiveDecoders and a Microphone for each of them 
            LiveDecoder decoder = getDecoder(decoderName);
            decoder.setTestFile(testFile);
            decoder.setShowPartialResults(showPartialResults);
            
            if (decoder.hasEndpointer()) {
                hasEndpointer = true;
                System.out.println(decoderName + " has endpointer");
            }
            
            info(".");
        }

        info("done parsing " + decoderListFile + "\n");
    }


    /**
     * Return a Decoder with the given name and decoder properties.
     *
     * @param decoderName the name of the Decoder
     *
     * @return the requested Decoder
     */
    private LiveDecoder getDecoder(String decoderName) throws IOException,
    LineUnavailableException {
        
        // obtain the decoder
        LiveDecoder decoder = null;
        if ((decoder = (LiveDecoder) decoders.get(decoderName)) == null) {
            decoder = new LiveDecoder(decoderName, this);
            decoders.put(decoderName, decoder);
        }

        return decoder;
    }


    /**
     * Updates the LiveFrame with the statistics in the given NISTAlign.
     *
     * @param aligner the NISTAlign to get statistics from
     */
    private void updateLiveFrame(NISTAlign aligner) {
        liveFrame.setRecognitionLabel(aligner.getHypothesis());
        
        float wordAccuracy = (aligner.getTotalWordAccuracy() * 100);
        liveFrame.setWordAccuracyLabel(wordAccuracy + "%");
                
        float sentenceAccuracy = (aligner.getTotalSentenceAccuracy() * 100);
        liveFrame.setSentenceAccuracyLabel(sentenceAccuracy + "%");

        liveFrame.setSpeedLabel
            (timeFormat.format(decoder.getSpeed()) + " X RT");
        
        liveFrame.setCumulativeSpeedLabel
            (timeFormat.format(decoder.getCumulativeSpeed()) +" X RT");
    }


    /**
     * Does decoding in a separate thread so that it does not
     * block the calling thread. It will automatically update
     * the GUI components once the decoding is completed. This
     * is analogous to the "Control" components in the MVC model.
     */
    class DecodingThread extends Thread {

        public DecodingThread() {
            super("Decoding");
        }

        public void run() {
            if (!handsFree) {
                lastResult = decoder.decode(liveFrame.getReference());
                updateLiveFrame(decoder.getNISTAlign());
                liveFrame.setButtonsEnabled(true);
                liveFrame.setDecoderComboBoxEnabled(true);
            } else {
                liveFrame.setNextButtonEnabled(true);
                while (decoder.getMicrophone().getRecording()) {
                    try {
                        System.out.println("Live: decoding");
                        lastResult = decoder.decode(liveFrame.getReference());
                        System.out.println("Live: decoded");
                        updateLiveFrame(decoder.getNISTAlign());
                        System.out.println("Live: updatedFrame");
                    } catch (NullPointerException npe) {
                        npe.printStackTrace();
                    }
                }
            }
            System.out.println("DecodingThread completed.");
        }
    }

}
