/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;


/**
 * A Microphone captures audio data from the system's underlying
 * audio input systems. Converts these audio data into Audio
 * objects.
 */
public class Microphone extends DataProcessor implements 
AudioSource, Runnable {

    /**
     * Parameters for audioFormat
     */
    private AudioFormat audioFormat;
    private float sampleRate;
    private int sampleSizeInBits = 16;
    private int channels = 1;
    private boolean signed = true;
    private boolean bigEndian = true;

    /**
     * The audio capturing device.
     */
    private TargetDataLine audioLine = null;
    private Utterance currentUtterance = null;

    private int frameSizeInBytes;
    private volatile boolean recording = false;
    private volatile boolean closed = false;
    
    private boolean utteranceEndSent = false;
    private boolean utteranceStarted = false;


    /**
     * Constructs a Microphone with the given InputStream.
     *
     * @param name the name of this Microphone
     * @param context the context of this Microphone
     */
    public Microphone(String name, String context) {
        super(name, context);
	initSphinxProperties();
        audioFormat = new AudioFormat(sampleRate, sampleSizeInBits,
                                      channels, signed, bigEndian);
        currentUtterance = new Utterance(context);
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties() {
	SphinxProperties properties = getSphinxProperties();

        sampleRate = (float) properties.getInt
            (FrontEnd.PROP_SAMPLE_RATE, 8000);
        frameSizeInBytes = properties.getInt
	    (FrontEnd.PROP_BYTES_PER_AUDIO_FRAME, 4096);

        if (frameSizeInBytes % 2 == 1) {
            frameSizeInBytes++;
        }
    }


    /**
     * Terminates this Microphone, effectively terminates this
     * thread of execution.
     */
    public void terminate() {
        setClosed(true);
    }


    /**
     * Implements the <code>run()</code> method of Runnable.
     * It waits for instructions to record audio. The method
     * <code>startRecording()</code> will cause it to start recording
     * from the system audio capturing device.
     * Once it starts recording,
     * it will keep recording until it receives instruction to stop
     * recording. The method <code>stopRecording()</code> will cause
     * it to stop recording.
     */
    public void run() {
        while (!getClosed()) {
            waitToRecord();
            record();
        }
    }


    /**
     * This thread waits until some other thread calls <code>record()</code>
     */
    private synchronized void waitToRecord() {
        synchronized(this) {
            while (!getRecording()) {
                try {
                    System.out.println("waiting to record");
                    wait();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
            System.out.println("finished waiting");
        }
    }


    /**
     * Records audio, and cache them in the audio buffer.
     */
    private void record() {

        if (audioLine != null && audioLine.isOpen()) {

            System.out.println("started recording");

            audioLine.start();

            while (getRecording()) {
                // Read the next chunk of data from the TargetDataLine.
                byte[] data = new byte[frameSizeInBytes];
                int numBytesRead =  audioLine.read(data, 0, data.length);
                
                if (numBytesRead == frameSizeInBytes) {
                    currentUtterance.add(data);
                } else {
                    numBytesRead = (numBytesRead % 2 == 0) ?
                        numBytesRead + 2 : numBytesRead + 3;

                    byte[] shrinked = new byte[numBytesRead];
                    System.arraycopy(data, 0, shrinked, 0, numBytesRead);
                    currentUtterance.add(shrinked);
                }

                System.out.println("recorded 1 frame (" + 
                                   numBytesRead + ") bytes");
            }

            audioLine.stop();
            audioLine.close();

            System.out.println("stopped recording");

        } else {
            System.out.println("Unable to open line");
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
        DataLine.Info info = new DataLine.Info
            (TargetDataLine.class, audioFormat);
        
        if (!AudioSystem.isLineSupported(info)) {
            System.out.println(audioFormat + " not supported");
            return false;
        }

        // Obtain and open the line.
        try {
            audioLine = (TargetDataLine) AudioSystem.getLine(info);
            audioLine.open(audioFormat);
            return true;
        } catch (LineUnavailableException ex) {
            audioLine = null;
            System.out.println("Line unavailable");
            return false;
        }        
    }


    /**
     * Starts recording audio
     *
     * @return true if the recording started successfully; false otherwise
     */
    public synchronized boolean startRecording() {
        reset();
        if (open()) {
            setRecording(true);
            notify();
            return true;
        } else {
            return false;
        }
    }


    /**
     * Stops recording audio.
     */
    public void stopRecording() {
        if (audioLine != null) {
            setRecording(false);
        }
    }


    /**
     * Resets the Microphone, effectively clearing the audio buffer.
     */
    public void reset() {
        currentUtterance = new Utterance(getContext());
        utteranceStarted = false;
        utteranceEndSent = false;
    }

    
    /**
     * Reads and returns the next Audio from the InputStream of
     * Microphone, return null if no data is read and end of file
     * is reached.
     *
     * @return the next Audio or <code>null</code> if none is
     *     available
     *
     * @throws java.io.IOException
     */
    public Audio getAudio() throws IOException {

        getTimer().start();

        Audio output = null;

        if (!utteranceStarted) {                
            utteranceStarted = true;
            output = new Audio(Signal.UTTERANCE_START);
        } else {
            output = readNextFrame();
            if (output == null) {
                if (!utteranceEndSent) {
                    output = new Audio(Signal.UTTERANCE_END);
                    utteranceEndSent = true;
                }
            }
        }

        getTimer().stop();

        return output;
    }


    /**
     * Returns the next Audio from AudioBuffer, or null if
     * there is none available.
     *
     * @return an Audio object or null
     *
     * @throws java.io.IOException
     */
    private Audio readNextFrame() throws IOException {

        // read one frame's worth of bytes
        byte[] audioFrame = null;

        do {
            audioFrame = currentUtterance.getNext();
        } while (audioFrame == null && getRecording());

        if (audioFrame == null) {
            return null;
        }

        // turn it into an Audio object
        Audio audio = new Audio
            (Util.byteToDoubleArray(audioFrame, 0, audioFrame.length));
        
        if (getDump()) {
            System.out.println("FRAME_SOURCE " + audio.toString());
        }
        
        return audio;
    }


    private synchronized boolean getRecording() {
        return recording;
    }

    private synchronized void setRecording(boolean recording) {
        this.recording = recording;
    }

    private synchronized boolean getClosed() {
        return closed;
    }

    private synchronized void setClosed(boolean closed) {
        this.closed = closed;
    }
}
