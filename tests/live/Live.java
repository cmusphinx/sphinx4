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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.DefaultComboBoxModel;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.NISTAlign;
import edu.cmu.sphinx.util.SphinxProperties;


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
            } catch (Exception e) {
                e.printStackTrace();
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
    public Live(String decoderListFile) throws 
        InstantiationException, IOException, LineUnavailableException {
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
        liveFrame.setVisible(true);

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
     *
     * @return the LiveFrame
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
     *
     * @return true if recording started successfully, false if it did not
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
    }

    /**
     * Plays the last recorded utterance.
     */
    public void playUtterance() {
        Microphone microphone = getDecoder().getMicrophone();
        if (microphone.getUtterance() != null) {
            liveFrame.setMessage("Playing back...");
            byte[] audio = microphone.getUtterance().getAudio();
            if (audio != null) {
                audioPlayer.play
                    (audio, microphone.getAudioFormat());
            }
            liveFrame.setMessage("Playing back...finished");
        } else {
            liveFrame.setMessage
                ("Cannot play utterance: it wasn't saved.");
        }
    }

    /**
     * Returns true if the current decoder keeps the audio.
     *
     * @return true if the current decoder kept the last utterance
     */
    public boolean canPlayUtterance() {
        return (getDecoder().getMicrophone().getUtterance() != null);
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
     * Make a new named decoder
     *
     * @param name    the name of the decoder to make
     */
    public LiveDecoder makeDecoder(String name)
        throws InstantiationException, IOException, LineUnavailableException {
        return new LiveDecoder(name, this);
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
                Microphone microphone = new Microphone();
                microphone.initialize
                    ("Microphone", null, decoder.getSphinxProperties(), null);
                decoder.initialize(microphone);
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
        InstantiationException, IOException, LineUnavailableException {

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
    private LiveDecoder getDecoder(String decoderName) throws 
        InstantiationException, IOException, LineUnavailableException {
        
        // obtain the decoder
        LiveDecoder decoder = null;
        if ((decoder = (LiveDecoder) decoders.get(decoderName)) == null) {
            decoder = makeDecoder(decoderName);
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

        String speedLabel = "N/A";
        String cumulativeSpeedLabel = "N/A";

	speedLabel = (timeFormat.format(decoder.getSpeed()) + " X RT");
	cumulativeSpeedLabel = 
	    (timeFormat.format(decoder.getCumulativeSpeed()) + " X RT");

        liveFrame.setSpeedLabel(speedLabel);
        liveFrame.setCumulativeSpeedLabel(cumulativeSpeedLabel);
    }


    /**
     * Does decoding in a separate thread so that it does not
     * block the calling thread. It will automatically update
     * the GUI components once the decoding is completed. This
     * is analogous to the "Control" components in the MVC model.
     */
    class DecodingThread extends Thread {

        /**
         * Constructs a DecodingThread.
         */
        public DecodingThread() {
            super("Decoding");
        }

        /**
         * Implements the run() method of this thread.
         */
        public void run() {
            if (handsFree) {
                while (decoder.getMicrophone().hasMoreData()) {
                    try {
                        System.out.println("Live: decoding ...");
                        lastResult = decoder.decode();
                        decoder.setReferenceText(liveFrame.getReference());
                        if (lastResult != null) {
                            decoder.showFinalResult(lastResult);
                        }
                        System.out.println("Live: ... decoded");
                        updateLiveFrame(decoder.getNISTAlign());
                    } catch (NullPointerException npe) {
                        npe.printStackTrace();
                    }
                }
                liveFrame.setDecoderComboBoxEnabled(true);
            } else {
                lastResult = decoder.decode(liveFrame.getReference());
                if (Boolean.getBoolean("epMode")) {
                    getDecoder().getMicrophone().stopRecording();
                    System.out.println("Speaker turned off.");
                    liveFrame.setMessage("Speaker turned off.");
                }
                updateLiveFrame(decoder.getNISTAlign());
                liveFrame.setDecoderComboBoxEnabled(true);
            }
            liveFrame.setSpeakButtonEnabled(true);
            liveFrame.setStopButtonEnabled(false);
            liveFrame.setNextButtonEnabled(true);
            liveFrame.setPlayButtonEnabled(true);
            System.out.println("DecodingThread completed.");
        }
    }

}
