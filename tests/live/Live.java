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

package tests.live;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.DefaultComboBoxModel;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.instrumentation.AccuracyTracker;
import edu.cmu.sphinx.instrumentation.SpeedTracker;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.ResultListener;
import edu.cmu.sphinx.util.NISTAlign;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

/**
 * The live decoder main program. This class contains the control logic.
 */
public class Live {

    private static DecimalFormat timeFormat = new DecimalFormat("0.00");
    private DefaultComboBoxModel recognizerNameList;
    private Map recognizers;

    private AudioPlayer audioPlayer = null; // for play the recording
    private LiveRecognizer currentRecognizer = null;
    private LiveFrame liveFrame = null;
    private Result lastResult = null;
    private File currentDirectory = null;

    private boolean showPartialResults = false;
    private boolean handsFree; // uses endpointer
    private boolean epMode;

    /**
     * Main program.
     * 
     * @param argv
     *                first argument should be the decoder list file
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
     * Constructs a Live program with the given file that lists all the
     * available decoders. The first decoder listed in the file will be
     * created.
     * 
     * @param decodersFile
     *                a file listing all the available decoders
     */
    public Live(String decoderListFile) throws InstantiationException,
            IOException, LineUnavailableException {
        String pwd = System.getProperty("user.dir");
        currentDirectory = new File(pwd);

        showPartialResults = Boolean.getBoolean("showPartialResults");
        handsFree = Boolean.getBoolean("handsFree");
        epMode = Boolean.getBoolean("epMode");

        recognizerNameList = new DefaultComboBoxModel();
        recognizers = new HashMap();

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
        if (recognizerNameList.getSize() > 0) {
            String firstDecoder = System.getProperty("firstDecoder");
            if (firstDecoder == null) {
                firstDecoder = (String) recognizerNameList.getElementAt(0);
            }
            info("Initializing first decoder: " + firstDecoder + " ...\n");
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
     * Terminates this Live object, terminates all Microphones owned by the
     * LiveDecoders.
     */
    public void terminate() {
        for (Iterator i = recognizers.values().iterator(); i.hasNext();) {
            LiveRecognizer lr = (LiveRecognizer) i.next();
            lr.deallocate();
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
        currentRecognizer.getMicrophone().clear();
        return currentRecognizer.getMicrophone().startRecording();
    }

    /**
     * Stop recording.
     */
    public void stopRecording() {
        currentRecognizer.getMicrophone().stopRecording();
    }

    public String getNextReference() {
        return currentRecognizer.getNextReference();
    }
    /**
     * Plays the last recorded utterance.
     */
    public void playUtterance() {
        Microphone microphone = currentRecognizer.getMicrophone();
        if (microphone.getUtterance() != null) {
            liveFrame.setMessage("Playing back...");
            byte[] audio = microphone.getUtterance().getAudio();
            if (audio != null) {
                audioPlayer.play
                    (audio, microphone.getUtterance().getAudioFormat());
            }
            liveFrame.setMessage("Playing back...finished");
        } else {
            liveFrame.setMessage("Cannot play utterance: it wasn't saved.");
        }
    }

    /**
     * Returns true if the current decoder keeps the audio.
     * 
     * @return true if the current decoder kept the last utterance
     */
    public boolean canPlayUtterance() {
        return currentRecognizer.getMicrophone().getUtterance() != null;
    }

    /**
     * Resets the statistics in the NISTAlign of the current Decoder.
     */
    public void resetStatistics() {
        currentRecognizer.getAligner().resetTotals();
        currentRecognizer.resetSpeed();
        updateLiveFrame(currentRecognizer.getAligner());
    }
    
    public void setTestFile(String testFile) {
        currentRecognizer.setTestFile(testFile);
    }

    /**
     * Sets the current recognizer to the one with the given name.
     * 
     * @param recognizerName
     *                name of the recognizer to use
     */
    public void setDecoder(String recognizerName) throws IOException {
        String changeMessage = "Changing to " + recognizerName + 
            " recognizer ...\n";
        info(changeMessage);
        liveFrame.setMessage(changeMessage);
        liveFrame.setSpeakButtonEnabled(false);
        liveFrame.setNextButtonEnabled(false);
        liveFrame.setPlayButtonEnabled(false);
        liveFrame.setClearButtonEnabled(false);

        if (currentRecognizer != null) {
            currentRecognizer.deallocate();
            currentRecognizer = null;
        }

        LiveRecognizer nextRecognizer = 
            (LiveRecognizer) recognizers.get(recognizerName);
        if (nextRecognizer.allocate()) {
            // if the decoder switch is successful
            currentRecognizer = nextRecognizer;
            liveFrame.setTestFile(currentRecognizer.getTestFile());
            if (!handsFree) {
                liveFrame.setReferenceLabel
                    (currentRecognizer.getNextReference());
            }  else {
                liveFrame.setReferenceLabel("");
            }
            liveFrame.setRecognitionLabel("");
            liveFrame.setSpeakButtonEnabled(true);
            liveFrame.setNextButtonEnabled(true);
            liveFrame.setPlayButtonEnabled(true);
            liveFrame.setClearButtonEnabled(true);
            liveFrame.setMessage(recognizerName + ", press \"Speak\" to start");

        } else {
            liveFrame.setMessage("Error trying to use " + recognizerName);
        }
        info("... done changing\n");
    }

    /**
     * Returns the DefaultComboBoxList that stores all the decoder names.
     * 
     * @return the DefaultComboBoxList that stores all the decoder names
     */
    public DefaultComboBoxModel getDecoderList() {
        return recognizerNameList;
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
     * @param currentDirectory
     *                sets the current directory
     */
    public void setCurrentDirectory(File currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    /**
     * Prints an info message to System.out.
     * 
     * @param message
     *                the message to print
     */
    private void info(String message) {
        System.out.print(message);
    }

    /**
     * Parses the given list of decoders, and create a SynthesizerProperties
     * for each of them. Also creates all the LiveDecoders.
     * 
     * @param decoderListFile
     *                a file listing all the available decoders
     */
    private void parseDecoderListFile(String decoderListFile)
            throws  IOException,  LineUnavailableException {

        info("Parsing file " + decoderListFile + " ");

        Properties properties = new Properties();
        properties.load(new FileInputStream(decoderListFile));

        String decoderLine = properties.getProperty("decoders");
        String[] recognizerNames = decoderLine.split("\\s+");

        for (int i = 0; i < recognizerNames.length; i++) {

            // name of the recognizer
            String recognizerName = properties.getProperty(recognizerNames[i]
                    + ".name");
            if (recognizerName == null) {
                throw new NullPointerException("No name for recognizer "
                        + recognizerNames[i]);
            }

            // cofnig file
            String configFile = properties.getProperty(recognizerNames[i]
                    + ".configFile");
            if (configFile == null) {
                throw new NullPointerException("No config file for recognizer "
                        + recognizerNames[i]);
            }

            // transcript file
            String testFile = properties.getProperty(recognizerNames[i]
                    + ".testFile");

            // add the name of the decoder to the recognizerNameList
            recognizerNameList.addElement(recognizerName);

            recognizers.put(recognizerName, new LiveRecognizer(recognizerName,
                    configFile, testFile));
            info(".");
        }

        info("done parsing " + decoderListFile + "\n");
    }


    /**
     * Updates the LiveFrame with the statistics in the given NISTAlign.
     * 
     * @param aligner
     *                the NISTAlign to get statistics from
     */
    private void updateLiveFrame(NISTAlign aligner) {
        liveFrame.setRecognitionLabel(aligner.getHypothesis());

        float wordAccuracy = (aligner.getTotalWordAccuracy() * 100);
        liveFrame.setWordAccuracyLabel(wordAccuracy + "%");

        float sentenceAccuracy = (aligner.getTotalSentenceAccuracy() * 100);
        liveFrame.setSentenceAccuracyLabel(sentenceAccuracy + "%");

        String speedLabel = "N/A";
        String cumulativeSpeedLabel = "N/A";

        speedLabel = (timeFormat.format(currentRecognizer.getSpeed()) + " X RT");
        cumulativeSpeedLabel = (timeFormat.format(currentRecognizer.getCumulativeSpeed()) + " X RT");

        
        liveFrame.setSpeedLabel(speedLabel);
        liveFrame.setCumulativeSpeedLabel(cumulativeSpeedLabel);
    }
    

    /**
     * Shows the given partial Result.
     *
     * @param result the partial Result to show
     */
    protected void showPartialResult(Result result) {
        getLiveFrame().setRecognitionLabel(result.toString());
    }


    /**
     * Does decoding in a separate thread so that it does not block the calling
     * thread. It will automatically update the GUI components once the
     * decoding is completed. This is analogous to the "Control" components in
     * the MVC model.
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
            Microphone microphone = currentRecognizer.getMicrophone();
            Recognizer recognizer = currentRecognizer.getRecognizer();
            
            if (handsFree) {
                while (microphone.hasMoreData()) {
                    // just sleep for 500ms so that it won't appear to
                    // be flipping through so quickly
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    String nextReference = getNextReference();
                    liveFrame.setReferenceLabel(nextReference);
                    liveFrame.setRecognitionLabel("");
                    recognizer.recognize(nextReference);
                }
                liveFrame.setDecoderComboBoxEnabled(true);
            } else {
                lastResult = recognizer.recognize(liveFrame.getReference());

                if (epMode) {
                    microphone.stopRecording();
                    liveFrame.setMessage("Speaker turned off.");
                }

            }
            liveFrame.setSpeakButtonEnabled(true);
            liveFrame.setStopButtonEnabled(false);
            liveFrame.setNextButtonEnabled(true);
            liveFrame.setPlayButtonEnabled(true);
            liveFrame.setDecoderComboBoxEnabled(true);
        }
    }

    /**
     * Issues a warning
     * @param msg the warning
     */
    private void warn(String msg) {
        System.err.println("Warning: " + msg);
    }

    /**
     * A recognizer container, associates a recognizer name, config file
     * test file utterance, along with the statistics and microphone control
     */
    class LiveRecognizer {
        private String name;
        private String testFile;
        private String configName;
        private ConfigurationManager cm;
        private Recognizer recognizer;
        private Microphone microphone;
        private SpeedTracker speedTracker;
        private NISTAlign aligner;
        private boolean allocated;
        private List referenceList;
        private Iterator iterator;

        /**
         * Creates a new live recognizer
         * @param name the name of the recognizer
         * @param configName the config file for the recognizer
         * @param testFile the test utterance file
         */
        LiveRecognizer(String name, String configName, String testFile)  {
            this.name = name;
            this.testFile = testFile;
            this.configName = configName;
            allocated = false;
        }

        /**
         * Gets the name of this recognizer
         * @return the name of this recognizer
         */
        String getName() {
            return name;
        }

        /**
         * Gets the test file in use by this recognizer
         * @return the test file
         */
        String getTestFile() {
            return testFile;
        }

        /**
         * Allocates this recognizer
         * @return
         */
        boolean allocate() {
            try {
                if (!allocated) {
                    URL url = new File(configName).toURI().toURL();
                    cm = new ConfigurationManager(url);
                    recognizer = (Recognizer) cm.lookup("recognizer");
                    microphone = (Microphone) cm.lookup("microphone");
                    speedTracker = (SpeedTracker) cm.lookup("speedTracker");
                    aligner = ((AccuracyTracker) cm.lookup("accuracyTracker")).getAligner();
                    recognizer.allocate();
                    setTestFile(testFile);

                    recognizer.addResultListener(new ResultListener() {
                            public void newResult(Result result) {
                                if (!result.isFinal() && 
                                    showPartialResults) {
                                    showPartialResult(result);
                                }
                                if (result.isFinal()) {
                                    updateLiveFrame
                                        (currentRecognizer.getAligner());
                                }
                            }
                        });
                    allocated = true;
                }

            } catch (InstantiationException e) {
                warn("Can't create recognizer from " + configName + " " + e);
            } catch (PropertyException pe) {
                warn("Can't configure recognizer " + pe);
            } catch (IOException ioe) {
                warn("Can't allocate recognizer " + ioe);
            }
            return allocated;
        }
        
        
        /**
         * Deallocates this recognizer
         *
         */
        void deallocate() {
            if (allocated) {
                recognizer.deallocate();
                allocated = false;
            }
        }


        /**
         * Retrieves the microphone object in use by this recognizer
         * @return the microphone
         */
        Microphone getMicrophone() {
            return microphone;
        }
        

        /**
         * Returns the actual recognizer 
         * @return the recognizer
         */
        Recognizer getRecognizer() {
            return recognizer;
        }

        /**
         * Determines if this recognzier has been allocated
         * @return true if the recognizer has been allocated
         */
        boolean isAllocated() {
            return allocated;
        }

        
        /**
         * Gets the aligner (which tracks accuracy statistics)
         * @return the aligner used by this recognizer
         */
        NISTAlign getAligner() {
            return aligner;
        }

        /**
         * Returns the cumulative speed of this recognizer as a fraction of real time.
         * 
         * @return the cumulative speed of this recognizer
         */
        public float getCumulativeSpeed() {
            return speedTracker.getCumulativeSpeed();
        }
        
        /**
         * Returns the current speed of this recognizer as a fraction of real time.
         * 
         * @return the current speed of this recognizer
         */
        public float getSpeed() {
            return speedTracker.getSpeed();
        }
        
        /**
         * Resets the speed statistics
         *
         */
        public void resetSpeed() {
            speedTracker.reset();
        }
        
        /**
         * Returns the next utterance in the test file. If its at the last
         * utterance already, it will cycle back to the first utterance.
         * If there is no utterance in the file at all, it will return 
         * an empty string.
         *
         * @return the next utterance in the test file; if no utterance,
         *    it will return an empty string
         */
        public String getNextReference() {
            if (iterator == null || !iterator.hasNext()) {
                iterator = referenceList.listIterator();
            }
            String next = "";
            if (iterator.hasNext()) {
                next = (String) iterator.next();
                if (next == null) {
                    next = "";
                }
            }
            return next;
        }
        
        /**
         * Sets the file of test utterances to be the given file
         * @param testFile the name of the test file
         */
        void setTestFile(String testFile)  {
            try {
                this.testFile = testFile;
                referenceList = new ArrayList();
                BufferedReader reader = new BufferedReader(new FileReader(testFile));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    referenceList.add(line);
                }
                iterator = referenceList.listIterator();
                reader.close();
            } catch (FileNotFoundException e) {
                warn("Can't find  file " + e);
            } catch (IOException e) {
                warn("Can't read  file " + e);
            }
        }
    }
}
