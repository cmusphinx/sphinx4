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


package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.Audio;
import edu.cmu.sphinx.frontend.AudioSource;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.Utterance;
import edu.cmu.sphinx.frontend.util.Util;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;


/**
 * A Microphone captures audio data from the system's underlying
 * audio input systems. Converts these audio data into Audio
 * objects. The Microphone should be run in a separate thread.
 * When the method <code>startRecording()</code> is called, it will
 * start capturing audio, and stops when <code>stopRecording()</code>
 * is called. An Utterance is created for all the audio captured
 * in between calls to <code>startRecording()</code> and
 * <code>stopRecording()</code>.
 * Calling <code>getAudio()</code> returns the captured audio
 * data as Audio objects.
 */
public class Microphone extends DataProcessor implements AudioSource {

    /**
     * Sphinx property that specifies whether or not the microphone
     * will release the audio between utterances.  On certain systems
     * (linux for one), closing and reopening the audio does not work
     * too well.
     */
    public final static String PROP_CLOSE_AUDIO_BETWEEN_UTTERANCES =
	"edu.cmu.sphinx.frontend.util.Microphone.closeAudioBetweenUtterances";

    /**
     * The default value for the PROP_CLOSE_AUDIO_BETWEEN_UTTERANCES
     * property
     */
    public final static boolean
	PROP_CLOSE_AUDIO_BETWEEN_UTTERANCES_DEFAULT = true;
    /**
     * Parameters for audioFormat
     */
    private AudioFormat audioFormat;
    private float sampleRate = 8000f;
    private int sampleSizeInBits = 16;
    private int channels = 1;
    private boolean signed = true;
    private boolean bigEndian = true;

    /**
     * The audio capturing device.
     */
    private TargetDataLine audioLine = null;
    private LineListener lineListener = new MicrophoneLineListener();
    private List audioList;
    private Utterance currentUtterance;

    private int frameSizeInBytes;
    private volatile boolean started = false;
    private volatile boolean recording = false;
    private volatile boolean closed = false;
    private boolean tracing = false;
    private boolean closeAudioBetweenUtterances = true;

    private static Logger logger = Logger.getLogger
        ("edu.cmu.sphinx.frontend.Microphone");
    

    /**
     * Constructs a Microphone with the given InputStream.
     *
     * @param name the name of this Microphone
     * @param context the context of this Microphone
     * @param props the SphinxProperties to read properties from
     */
    public Microphone(String name, String context, SphinxProperties props) 
	throws IOException {
        super(name, context);
	setProperties(props);
        audioFormat = new AudioFormat(sampleRate, sampleSizeInBits,
                                      channels, signed, bigEndian);
        audioList = new LinkedList();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    public void setProperties(SphinxProperties props) {

        sampleRate = props.getInt(FrontEnd.PROP_SAMPLE_RATE,
                                  FrontEnd.PROP_SAMPLE_RATE_DEFAULT);
	closeAudioBetweenUtterances =
	    props.getBoolean(PROP_CLOSE_AUDIO_BETWEEN_UTTERANCES,
                             PROP_CLOSE_AUDIO_BETWEEN_UTTERANCES_DEFAULT);

        SphinxProperties properties = getSphinxProperties();

        frameSizeInBytes = properties.getInt
            (FrontEnd.PROP_BYTES_PER_AUDIO_FRAME,
             FrontEnd.PROP_BYTES_PER_AUDIO_FRAME_DEFAULT);

        if (frameSizeInBytes % 2 == 1) {
            frameSizeInBytes++;
        }

        sampleSizeInBits = getSphinxProperties().getInt
            (FrontEnd.PROP_BITS_PER_SAMPLE, 
             FrontEnd.PROP_BITS_PER_SAMPLE_DEFAULT);
    }


    /**
     * Terminates this Microphone. In this version, it currently
     * does nothing.
     */
    public void terminate() {}


    /**
     * Returns the current AudioFormat used by this Microphone.
     *
     * @return the current AudioFormat
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }


    /**
     * Returns the current Utterance.
     *
     * @return the current Utterance
     */
    public Utterance getUtterance() {
        return currentUtterance;
    }


    /**
     * Prints the given message to System.out.
     *
     * @param message the message to print
     */
    private void printMessage(String message) {
	if (tracing) {
	    System.out.println("Microphone: " + message);
	}
    }


    /**
     * This Thread records audio, and caches them in an audio buffer.
     */

    class RecordingThread extends Thread {

        public RecordingThread(String name) {
            super(name);
        }

        /**
         * Implements the run() method of the Thread class.
         * Records audio, and cache them in the audio buffer.
         */
        public void run() {
            
            if (audioLine != null && audioLine.isOpen()) {
                
		if (audioLine.isRunning()) {
		    printMessage("Whoops: line is running");
		}
                audioLine.start();
		if (tracing) {
		    printMessage("started recording");
		}

                currentUtterance = new Utterance("Microphone", getContext());
                                
                while (getRecording() && !getClosed()) {
		    if (tracing) {
			printMessage("reading ...");
		    }
                                        
                    // Read the next chunk of data from the TargetDataLine.
                    byte[] data = new byte[frameSizeInBytes];
                    int numBytesRead = audioLine.read(data, 0, data.length);
                    
                    if (numBytesRead != frameSizeInBytes) {
                        numBytesRead = (numBytesRead % 2 == 0) ?
                            numBytesRead + 2 : numBytesRead + 3;
                        
                        byte[] shrinked = new byte[numBytesRead];
                        System.arraycopy(data, 0, shrinked, 0, numBytesRead);
                        data = shrinked;
                    }

                    currentUtterance.add(data);

                    double[] samples = Util.bytesToSamples
                        (data, 0, data.length, sampleSizeInBits/8, signed);
                    
                    audioList.add(new Audio(samples));

		    if (tracing) {
			printMessage(
			    "recorded 1 frame (" + numBytesRead + ") bytes");
		    }
                }

                audioList.add(new Audio(Signal.UTTERANCE_END));
                
                audioLine.stop();
		if (closeAudioBetweenUtterances) {
                    audioLine.close();
		    audioLine = null;
		}
                
		if (tracing) {
		    printMessage("stopped recording");
		}
                
            } else {
		if (tracing) {
		    printMessage("Unable to open line");
		}
            }
        }
    }


    /**
     * Opens the audio capturing device so that it will be ready
     * for capturing audio.
     *
     * @return true if the audio capturing device is opened successfully;
     *     false otherwise
     */
    private boolean open() {

	if (audioLine != null) {
	    return true;
	}

        DataLine.Info info = new DataLine.Info
            (TargetDataLine.class, audioFormat);
        
        if (!AudioSystem.isLineSupported(info)) {
            logger.severe(audioFormat + " not supported");
            return false;
        }


        // Obtain and open the line.
        try {
            audioLine = (TargetDataLine) AudioSystem.getLine(info);
            audioLine.addLineListener(lineListener);
            audioLine.open(audioFormat);
            return true;
        } catch (LineUnavailableException ex) {
            audioLine = null;
            ex.printStackTrace();
            return false;
        }        
    }


    /**
     * Clears all cached audio data.
     */
    public void clear() {
        audioList = new LinkedList();
    }


    /**
     * Starts recording audio. This method will return only
     * when a START event is received, meaning that this Microphone
     * has started capturing audio.
     *
     * @return true if the recording started successfully; false otherwise
     */
    public synchronized boolean startRecording() {
        if (open()) {
            audioList.add(new Audio(Signal.UTTERANCE_START));
            setRecording(true);
            RecordingThread recorder = new RecordingThread("Microphone");
            recorder.start();
            while (!getStarted()) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
            return true;
        } else {
            return false;
        }
    }


    /**
     * Stops recording audio.
     */
    public synchronized void stopRecording() {
        if (audioLine != null) {
            setRecording(false);
            setStarted(false);
        }
    }

    
    /**
     * Reads and returns the next Audio object from this
     * Microphone, return null if there is no more audio data.
     * All audio data captured in-between <code>startRecording()</code>
     * and <code>stopRecording()</code> is cached in an Utterance
     * object. Calling this method basically returns the next
     * chunk of audio data cached in this Utterance.
     *
     * @return the next Audio or <code>null</code> if none is
     *     available
     *
     * @throws java.io.IOException
     */
    public Audio getAudio() throws IOException {

        getTimer().start();

        Audio output = null;

        do {
            if (audioList.size() > 0) {
                output = (Audio) audioList.remove(0);
            }
        } while (output == null && recording);

        getTimer().stop();

        return output;
    }


    private boolean getStarted() {
        return started;
    }


    private void setStarted(boolean started) {
        this.started = started;
    }


    /**
     * Returns true if this Microphone is currently
     * in a recording state; false otherwise
     *
     * @return true if recording, false if not recording
     */ 
    public boolean getRecording() {
        return recording;
    }

    
    /**
     * Sets whether this Microphone is in a recording state.
     *
     * @param recording true to set this Microphone
     * in a recording state false to a non-recording state
     */
    private void setRecording(boolean recording) {
        this.recording = recording;
    }


    /**
     * Returns true if this Microphone thread finished running.
     * Normally, this Microphone is run in its own thread. If this
     * method returns true, it means the <code>run()</code> method
     * of the thread is finished running.
     *
     * @return true if this Microphone thread has finished running
     */
    private boolean getClosed() {
        return closed;
    }


    /**
     * Sets whether to terminate the Microphone thread.
     *
     * @param closed true to terminate the Micrphone thread
     */
    private void setClosed(boolean closed) {
        this.closed = closed;
    }


    /**
     * Provides a LineListener for this Microphone
     */
    class MicrophoneLineListener implements LineListener {

        /**
         * Implements update() method of LineListener interface.
         * Responds to the START line event by waking up all the
         * threads that are waiting on the Microphone's monitor.
         *
         * @param event the LineEvent to handle
         */
        public void update(LineEvent event) {
            // System.out.println("MicrophoneLineListener: update " + event);
            if (event.getType().equals(LineEvent.Type.START)) {
                setStarted(true);
                synchronized (Microphone.this) {
                    Microphone.this.notifyAll();
                }
            }
        }
    }
}
