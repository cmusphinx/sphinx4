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


package edu.cmu.sphinx.frontend.util;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;


/**
 * A Microphone captures audio data from the system's underlying
 * audio input systems. Converts these audio data into Data
 * objects. When the method <code>startRecording()</code> is called,
 * a new thread will be created and used to capture
 * audio, and will stop when <code>stopRecording()</code>
 * is called. Calling <code>getData()</code> returns the captured audio
 * data as Data objects.
 */
public class BufferedMicrophone extends BaseDataProcessor {

    /**
     * SphinxProperty for the size of the audio buffer in seconds.
     */
    public static final String PROP_BUFFER_SIZE_SECS = "bufferSizeInSecs";

    /**
     * Default value for PROP_BUFFER_SIZE_SECS.
     */
    public static final int PROP_BUFFER_SIZE_SECS_DEFAULT = 5;

    /**
     * SphinxProperty for the sample rate of the data.
     */
    public static final String PROP_SAMPLE_RATE = "sampleRate";

    /**
     * Default value for PROP_SAMPLE_RATE.
     */
    public static final int PROP_SAMPLE_RATE_DEFAULT = 16000;

    /**
     * Sphinx property that specifies whether or not the microphone
     * will release the audio between utterances.  On certain systems
     * (linux for one), closing and reopening the audio does not work
     * too well.
     */
    public final static String PROP_CLOSE_BETWEEN_UTTERANCES =
	"closeBetweenUtterances";

    /**
     * The default value for the PROP_CLOSE_BETWEEN_UTTERANCES
     * property.
     */
    public final static boolean PROP_CLOSE_BETWEEN_UTTERANCES_DEFAULT = true;

    /**
     * The Sphinx property that specifies whether debug statements should
     * be printed.
     */
    public final static String PROP_DEBUG = "debug";

    /**
     * The default value of PROP_DEBUG.
     */
    public final static boolean PROP_DEBUG_DEFAULT = false;

    /**
     * The Sphinx property that specifies the number of bytes to
     * read each time from the underlying Java Sound audio device.
     */
    public final static String PROP_BYTES_PER_READ
        = "bytesPerRead";

    /**
     * The default value of PROP_BYTES_PER_READ. The current default
     * is 320 bytes. Assuming that the sample rate is 16kHz and it is
     * 2 bytes per samples, 320 bytes corresponds to 10 milliseconds
     * of audio.
     */
    public final static int PROP_BYTES_PER_READ_DEFAULT = 320;

    /**
     * SphinxProperty for the number of bits per value.
     */
    public static final String PROP_BITS_PER_SAMPLE =
        "bitsPerSample";

    /**
     * Default value for PROP_BITS_PER_SAMPLE.
     */
    public static final int PROP_BITS_PER_SAMPLE_DEFAULT = 16;

    /**
     * The Sphinx property that specifies whether to keep the audio
     * data of an utterance around until the next utterance is recorded.
     */
    public final static String PROP_KEEP_LAST_AUDIO
        = "keepLastAudio";

    /**
     * The default value of PROP_KEEP_AUDIO.
     */
    public final static boolean PROP_KEEP_LAST_AUDIO_DEFAULT = false;


    /**
     * Parameters for audioFormat
     */
    private AudioFormat audioFormat;
    private int bufferSizeInSecs;
    private int sampleRate;
    private int sampleSizeInBytes;
    private int channels = 1;
    private boolean signed = true;
    private boolean bigEndian = true;

    /**
     * Variables for performing format conversion from a 
     * hardware supported format to the speech recognition audio format
     */
    private boolean doConversion = false;
    private AudioInputStream nativelySupportedStream;

    /**
     * The audio capturing device.
     */
    private TargetDataLine audioLine = null;
    private AudioInputStream audioStream = null;
    private List audioList;
    private Utterance currentUtterance;

    private long totalSamplesRead;
    private int frameSizeInBytes;

    private volatile boolean closed = false;
    private volatile boolean utteranceEndReached = true;

    private boolean debug = false;
    private boolean closeBetweenUtterances = true;
    private boolean keepDataReference = true;

    private  Logger logger;
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry) throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_BUFFER_SIZE_SECS, PropertyType.INT);
        registry.register(PROP_SAMPLE_RATE, PropertyType.INT);
	registry.register(PROP_CLOSE_BETWEEN_UTTERANCES, 
                          PropertyType.BOOLEAN);
        registry.register(PROP_BYTES_PER_READ, PropertyType.INT);
        registry.register(PROP_BITS_PER_SAMPLE, PropertyType.INT);
        registry.register(PROP_KEEP_LAST_AUDIO, PropertyType.BOOLEAN);
        registry.register(PROP_DEBUG, PropertyType.BOOLEAN);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        logger = ps.getLogger();

        bufferSizeInSecs = ps.getInt
            (PROP_BUFFER_SIZE_SECS, PROP_BUFFER_SIZE_SECS_DEFAULT);

        sampleRate = ps.getInt
            (PROP_SAMPLE_RATE, PROP_SAMPLE_RATE_DEFAULT);
        
	closeBetweenUtterances = ps.getBoolean
            (PROP_CLOSE_BETWEEN_UTTERANCES,
             PROP_CLOSE_BETWEEN_UTTERANCES_DEFAULT);

        frameSizeInBytes = ps.getInt
            (PROP_BYTES_PER_READ, PROP_BYTES_PER_READ_DEFAULT);

        if (frameSizeInBytes % 2 == 1) {
            frameSizeInBytes++;
        }

        sampleSizeInBytes = ps.getInt
            (PROP_BITS_PER_SAMPLE, PROP_BITS_PER_SAMPLE_DEFAULT)/8;

        keepDataReference = ps.getBoolean
            (PROP_KEEP_LAST_AUDIO, PROP_KEEP_LAST_AUDIO_DEFAULT);
        
        debug = ps.getBoolean(PROP_DEBUG, PROP_DEBUG_DEFAULT);
    }

    /**
     * Constructs a Microphone with the given InputStream.
     *
     * @throws IOException if an I/O error occurs
     */
    public void initialize() {
        super.initialize();
        audioFormat = new AudioFormat
            ((float) sampleRate, sampleSizeInBytes * 8, channels,
             signed, bigEndian);
        audioList = Collections.synchronizedList(new LinkedList());
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
	if (debug) {
	    System.out.println("Microphone: " + message);
	}
    }


    /**
     * Opens the audio capturing device so that it will be ready
     * for capturing audio. Attempts to create a converter if the
     * requested audio format is not directly available.
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

        AudioFormat nativeFormat = null;        
        if (!AudioSystem.isLineSupported(info)) {
            logger.warning(audioFormat + " not supported");
            printMessage(audioFormat + " not supported");
             
            nativeFormat = Microphone.getNativeAudioFormat(audioFormat);
            
            if (nativeFormat == null) {
                logger.severe("couldn't find suitable target " +
                              "audio format for conversion");
                return false;
            } else {
                logger.info("accepting " + nativeFormat + 
                            " as natively supported format");
                info = new DataLine.Info(TargetDataLine.class, nativeFormat);
                doConversion = true;
            }
        } else {
            doConversion = false;
        }


        // Obtain and open the line and stream.
        try {
            audioLine = (TargetDataLine) AudioSystem.getLine(info);
            if (doConversion) {
                try {
                    nativelySupportedStream = new AudioInputStream(audioLine);
                    audioStream = AudioSystem.getAudioInputStream
                        (audioFormat, nativelySupportedStream);
                } catch (IllegalArgumentException e) {
                    logger.severe("couldn't construct converter " +
                                  "from native audio format");
                    audioLine.close();
                    audioLine = null;
                    e.printStackTrace();
                    return false;
                }
            } else {
                audioStream = new AudioInputStream(audioLine);
            }

            int bufferSize = 
                sampleSizeInBytes * sampleRate * bufferSizeInSecs;

            audioLine.open(audioFormat, bufferSize);

            return true;
        } catch (LineUnavailableException ex) {
            audioLine = null;
            ex.printStackTrace();
            return false;
        }
    }


    /**
     * Reads one frame of audio data, and adds it to the given Utterance.
     *
     * @return an Data object containing the audio data
     */
    private Data readData(Utterance utterance) {
        // Read the next chunk of data from the TargetDataLine.
        byte[] data = new byte[frameSizeInBytes];
        long collectTime = System.currentTimeMillis();
        long firstSampleNumber = totalSamplesRead;
        
        try {
            printMessage("reading from audio stream...");
            int numBytesRead = audioStream.read(data, 0, data.length);
            printMessage("... finished reading " + numBytesRead + 
                         " from audio stream.");

            if (numBytesRead == 0) {
                return null;
            }

            totalSamplesRead += (numBytesRead / sampleSizeInBytes);

            if (numBytesRead != frameSizeInBytes) {
                numBytesRead = (numBytesRead % 2 == 0) ?
                    numBytesRead + 2 : numBytesRead + 3;
                
                byte[] shrinked = new byte[numBytesRead];
                System.arraycopy(data, 0, shrinked, 0, numBytesRead);
                data = shrinked;
            }

            printMessage("recorded 1 frame (" + numBytesRead + ") bytes");

        } catch(IOException e) {
            audioLine.stop();
            audioLine = null;
            e.printStackTrace();
            return null;
        }

        if (keepDataReference) {
            utterance.add(data);
        }

        double[] samples = DataUtil.bytesToValues
            (data, 0, data.length, sampleSizeInBytes, signed);
        return (new DoubleData(samples, sampleRate, collectTime,
                               firstSampleNumber));
    }


    /**
     * Clears all cached audio data.
     */
    public void clear() {
        audioList = Collections.synchronizedList(new LinkedList());
    }


    /**
     * Starts recording audio. This method will return only
     * when a START event is received, meaning that this Microphone
     * has started capturing audio.
     *
     * @return true if the recording started successfully; false otherwise
     */
    public synchronized boolean startRecording() {
        if (!getRecording() && open()) {
            utteranceEndReached = false;
            audioList.add(new DataStartSignal());
            audioLine.start();
            totalSamplesRead = 0;
            if (keepDataReference) {
                currentUtterance = new Utterance("Microphone", audioFormat);
            }
            printMessage("started");
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
            audioLine.stop();
            Data data = null;
            do {
                data = readData(currentUtterance);
                if (data != null) {
                    audioList.add(data);
                } else {
                    long duration = (long)
                        (((double)totalSamplesRead/(double)sampleRate) * 
                         1000.0);
                    audioList.add(new DataEndSignal(duration));
                    printMessage("DataEnd added");
                }
            } while (data != null);
         
            if (closeBetweenUtterances) {
                audioLine.close();
                printMessage("Audio line closed.");
                try {
                    audioStream.close();
                    printMessage("Audio stream closed.");
                    if (doConversion) {
                        nativelySupportedStream.close();
                        printMessage("Native stream closed.");
                    }                        
                } catch(IOException e) {
                    logger.warning("IOException closing audio streams");
                }
                audioLine = null;
            }
        }
    }

    
    /**
     * Reads and returns the next Data object from this
     * Microphone, return null if there is no more audio data.
     * All audio data captured in-between <code>startRecording()</code>
     * and <code>stopRecording()</code> is cached in an Utterance
     * object. Calling this method basically returns the next
     * chunk of audio data cached in this Utterance.
     *
     * @return the next Data or <code>null</code> if none is
     *         available
     *
     * @throws DataProcessingException if there is a data processing error
     */
    public Data getData() throws DataProcessingException {

        getTimer().start();

        Data output = null;

        if (!utteranceEndReached) {
            if (audioList.size() > 0) {
                output = (Data) audioList.remove(0);
            }
            if (output == null) {
                output = readData(currentUtterance);
            } else if (output instanceof DataStartSignal) {
                printMessage("DataStart");
            } else if (output instanceof DataEndSignal) {
                printMessage("DataEnd");
                utteranceEndReached = true;
            }
        }

        getTimer().stop();

        return output;
    }


    /**
     * Returns true if there is more data in the Microphone.
     * This happens either if getRecording() return true, or if the
     * buffer in the Microphone has a size larger than zero.
     *
     * @return true if there is more data in the Microphone
     */
    public boolean hasMoreData() {
        boolean moreData;
        synchronized (audioList) {
            moreData = (!utteranceEndReached || audioList.size() > 0);
        }
        return moreData;
    }


    /**
     * Returns true if this Microphone is currently
     * in a recording state, false otherwise.
     *
     * @return true if recording, false if not recording
     */ 
    public boolean getRecording() {
        if (audioLine != null) {
            return audioLine.isActive();
        } else {
            return false;
        }
    }
}
