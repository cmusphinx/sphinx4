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


package edu.cmu.sphinx.frontend.util;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineEvent;

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
 * <p>
 * A Microphone captures audio data from the system's underlying
 * audio input systems. Converts these audio data into Data
 * objects. When the method <code>startRecording()</code> is called,
 * a new thread will be created and used to capture
 * audio, and will stop when <code>stopRecording()</code>
 * is called. Calling <code>getData()</code> returns the captured audio
 * data as Data objects.
 * </p>
 * <p>
 * This Microphone will attempt to obtain an audio device with the format
 * specified in the configuration. If such a device with that format
 * cannot be obtained, it will try to obtain a device with an audio format
 * that has a higher sample rate than the configured sample rate,
 * while the other parameters of the format (i.e., sample size, endianness,
 * sign, and channel) remain the same. If, again, no such device can be
 * obtained, it flags an error, and a call <code>startRecording</code> 
 * returns false.
 * </p>
 */
public class Microphone extends BaseDataProcessor {

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
     * too well. The default is false for Linux systems, true for others.
     */
    public final static String PROP_CLOSE_BETWEEN_UTTERANCES =
	"closeBetweenUtterances";

    /**
     * Default value for PROP_CLOSE_BETWEEN_UTTERANCES.
     */
    public final static boolean PROP_CLOSE_BETWEEN_UTTERANCES_DEFAULT = true;

    /**
     * The Sphinx property that specifies the number of milliseconds of
     * audio data to read each time from the underlying Java Sound audio 
     * device.
     */
    public final static String PROP_MSEC_PER_READ = "msecPerRead";

    /**
     * The default value of PROP_MSEC_PER_READ.
     */
    public final static int PROP_MSEC_PER_READ_DEFAULT = 10;

    /**
     * SphinxProperty for the number of bits per value.
     */
    public static final String PROP_BITS_PER_SAMPLE = "bitsPerSample";

    /**
     * Default value for PROP_BITS_PER_SAMPLE.
     */
    public static final int PROP_BITS_PER_SAMPLE_DEFAULT = 16;

    /**
     * Property specifying the number of channels.
     */
    public static final String PROP_CHANNELS = "channels";

    /**
     * Default value for PROP_CHANNELS.
     */
    public static final int PROP_CHANNELS_DEFAULT = 1;

    /**
     * Property specify the endianness of the data.
     */
    public static final String PROP_BIG_ENDIAN = "bigEndian";

    /**
     * Default value for PROP_BIG_ENDIAN.
     */
    public static final boolean PROP_BIG_ENDIAN_DEFAULT = true;

    /**
     * Property specify whether the data is signed.
     */
    public static final String PROP_SIGNED = "signed";

    /**
     * Default value for PROP_SIGNED.
     */
    public static final boolean PROP_SIGNED_DEFAULT = true;

    /**
     * The Sphinx property that specifies whether to keep the audio
     * data of an utterance around until the next utterance is recorded.
     */
    public final static String PROP_KEEP_LAST_AUDIO = "keepLastAudio";

    /**
     * The default value of PROP_KEEP_AUDIO.
     */
    public final static boolean PROP_KEEP_LAST_AUDIO_DEFAULT = false;


    private AudioFormat finalFormat;
    private AudioInputStream audioStream = null;
    private TargetDataLine audioLine = null;
    private DataList audioList;
    private Utterance currentUtterance;
    private boolean doConversion = false;
    private int audioBufferSize = 160000;
    private volatile boolean recording = false;
    private volatile boolean utteranceEndReached = true;

    // Configuration data

    private AudioFormat desiredFormat;
    private Logger logger;
    private boolean closeBetweenUtterances;
    private boolean keepDataReference;
    private boolean signed;
    private int frameSizeInBytes;
    private int msecPerRead;

    
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
        throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_SAMPLE_RATE, PropertyType.INT);
	registry.register(PROP_CLOSE_BETWEEN_UTTERANCES, PropertyType.BOOLEAN);
        registry.register(PROP_MSEC_PER_READ, PropertyType.INT);
        registry.register(PROP_BITS_PER_SAMPLE, PropertyType.INT);
        registry.register(PROP_CHANNELS, PropertyType.INT);
        registry.register(PROP_BIG_ENDIAN, PropertyType.BOOLEAN);
        registry.register(PROP_SIGNED, PropertyType.BOOLEAN);
        registry.register(PROP_KEEP_LAST_AUDIO, PropertyType.BOOLEAN);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        logger = ps.getLogger();

        int sampleRate = ps.getInt(PROP_SAMPLE_RATE, PROP_SAMPLE_RATE_DEFAULT);

        int sampleSizeInBits = ps.getInt
            (PROP_BITS_PER_SAMPLE, PROP_BITS_PER_SAMPLE_DEFAULT);

        int channels = ps.getInt(PROP_CHANNELS, PROP_CHANNELS_DEFAULT);

        boolean bigEndian = 
            ps.getBoolean(PROP_BIG_ENDIAN, PROP_BIG_ENDIAN_DEFAULT);

        signed = ps.getBoolean(PROP_SIGNED, PROP_SIGNED_DEFAULT);

        desiredFormat = new AudioFormat
            ((float)sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        
	closeBetweenUtterances = ps.getBoolean
            (PROP_CLOSE_BETWEEN_UTTERANCES,
             PROP_CLOSE_BETWEEN_UTTERANCES_DEFAULT);
        
        msecPerRead = ps.getInt(PROP_MSEC_PER_READ, 
                                PROP_MSEC_PER_READ_DEFAULT);

        keepDataReference = ps.getBoolean
            (PROP_KEEP_LAST_AUDIO, PROP_KEEP_LAST_AUDIO_DEFAULT);
    }

    /**
     * Constructs a Microphone with the given InputStream.
     *
     * @throws IOException if an I/O error occurs
     */
    public void initialize() {
        super.initialize();
        audioList = new DataList();

        DataLine.Info  info 
                = new DataLine.Info(TargetDataLine.class, desiredFormat);
       
        if (!AudioSystem.isLineSupported(info)) {
            logger.info(desiredFormat + " not supported");
            AudioFormat nativeFormat = getNativeAudioFormat(desiredFormat);
            if (nativeFormat == null) {
                logger.severe("couldn't find suitable target audio format");
                return;
            } else {
                finalFormat = nativeFormat;
                
                /* convert from native to the desired format if supported */
                doConversion = AudioSystem.isConversionSupported
                    (desiredFormat, nativeFormat);
                
                if (doConversion) {
                    logger.info
                        ("Converting from " + finalFormat.getSampleRate()
                         + "Hz to " + desiredFormat.getSampleRate() + "Hz");
                } else {
                    logger.info
                        ("Using native format: Cannot convert from " +
                         finalFormat.getSampleRate() + "Hz to " +
                         desiredFormat.getSampleRate() + "Hz");
                }
            }
        } else {
            logger.info("Desired format: " + desiredFormat + " supported.");
            finalFormat = desiredFormat;
        }

        /* Obtain and open the line and stream. */
        try {
            logger.info("Final format: " + finalFormat);
            info = new DataLine.Info(TargetDataLine.class, finalFormat);
            audioLine = (TargetDataLine) AudioSystem.getLine(info);

            // add a line listener that just traces
            // the line states
            audioLine.addLineListener(new LineListener() {
                    public  void update(LineEvent event) {
                        logger.info("line listener " + event);
                    }
            });
        } catch (LineUnavailableException e) {
            logger.severe("microphone unavailable " + e.getMessage());
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
            if (!audioLine.isOpen()) {

                /* open the audio line */
                logger.info("open");
                try {
                    audioLine.open(finalFormat, audioBufferSize);
                } catch (LineUnavailableException e) {
                    logger.severe("Can't open microphone " + e.getMessage());
                    return false;
                }

                audioStream = new AudioInputStream(audioLine);
                if (doConversion) {
                    audioStream = AudioSystem.getAudioInputStream
                        (desiredFormat, audioStream);
                    assert (audioStream != null);
                }

                /* Set the frame size depending on the sample rate. */
                float sec = ((float) msecPerRead) / 1000.f;
                frameSizeInBytes =
                    (audioStream.getFormat().getSampleSizeInBits() / 8) *
                    (int) (sec * audioStream.getFormat().getSampleRate());

                logger.info("Frame size: " + frameSizeInBytes + " bytes");
            } 
            return true;
        } else {
            logger.severe("Can't find microphone");
            return false;
        }
    }


    /**
     * Returns the format of the audio recorded by this Microphone.
     * Note that this might be different from the configured format.
     *
     * @return the current AudioFormat
     */
    public AudioFormat getAudioFormat() {
        return finalFormat;
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
     * Returns true if this Microphone is recording.
     *
     * @return true if this Microphone is recording, false otherwise
     */
    public boolean isRecording() {
        return recording;
    }


    /**
     * Starts recording audio. This method will return only
     * when a START event is received, meaning that this Microphone
     * has started capturing audio.
     *
     * @return true if the recording started successfully; false otherwise
     */
    public synchronized boolean startRecording() {
	if (recording) {
	    return false;
	}
        if (!open()) {
            return false;
        }
	utteranceEndReached = false;
	recording = true;
	if (audioLine.isRunning()) {
	    logger.severe("Whoops: audio line is running");
	}
	RecordingThread recorder = new RecordingThread("Microphone");
	recorder.start();
	return true;
    }


    /**
     * Stops recording audio.
     */
    public synchronized void stopRecording() {
        if (audioLine != null) {
            recording = false;
	    audioLine.stop();
        }
    }


    /**
     * This Thread records audio, and caches them in an audio buffer.
     */
    class RecordingThread extends Thread {

        private boolean endOfStream = false;
        private volatile boolean started = false;
        private long totalSamplesRead = 0;

        /**
         * Creates the thread with the given name
         *
         * @param name the name of the thread
         */
        public RecordingThread(String name) {
            super(name);
        }

        /**
         * Starts the thread, and waits for recorder to be ready
         */
        public void start() {
            started = false;
            super.start();
            waitForStart();
        }


        /**
         * Implements the run() method of the Thread class.
         * Records audio, and cache them in the audio buffer.
         */
        public void run() {            
	    totalSamplesRead = 0;
	    logger.info("started recording");
	    
	    if (keepDataReference) {
		currentUtterance = new Utterance
                    ("Microphone", audioStream.getFormat());
	    }
	    
	    audioList.add(new DataStartSignal());
	    logger.info("DataStartSignal added");
	    try {
		audioLine.start();
		while (!endOfStream) {
                    Data data = readData(currentUtterance);
		    if (data == null) {
			break;
		    }
		    audioList.add(data);
		}
                audioLine.flush();
                if (closeBetweenUtterances) {
                    audioStream.close();
                }
	    } catch (IOException ioe) {
                logger.warning("IO Exception " + ioe.getMessage());
	    } 
	    long duration = (long)
		(((double)totalSamplesRead/
		  (double)audioStream.getFormat().getSampleRate())*1000.0);
	    
	    audioList.add(new DataEndSignal(duration));
	    logger.info("DataEndSignal ended");
	    logger.info("stopped recording");	    
	}

        /**
         * Waits for the recorder to start
         */
        private synchronized void  waitForStart() {
            // note that in theory we coulde use a LineEvent START
            // to tell us when the microphone is ready, but we have
            // found that some javasound implementations do not always
            // issue this event when a line  is opened, so this is a
            // WORKAROUND.

            try {
                while (!started) {
                    wait();
                }
            } catch (InterruptedException ie) {
                logger.warning("wait was interrupted");
            }
        }

        /**
         * Reads one frame of audio data, and adds it to the given Utterance.
         *
         * @return an Data object containing the audio data
         */
        private Data readData(Utterance utterance) throws IOException {
            // Read the next chunk of data from the TargetDataLine.
            byte[] data = new byte[frameSizeInBytes];
            long collectTime = System.currentTimeMillis();
            long firstSampleNumber = totalSamplesRead;
            
            int numBytesRead = audioStream.read(data, 0, data.length);

            //  notify the waiters upon start
            if (!started) {
                synchronized (this) {
                    started = true;
                    notifyAll();
                }
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.info("Read " + numBytesRead 
                        + " bytes from audio stream.");
            }
            if (numBytesRead <= 0) {
                endOfStream = true;
                return null;
            }
            int sampleSizeInBytes = 
                audioStream.getFormat().getSampleSizeInBits() / 8;
            totalSamplesRead += (numBytesRead / sampleSizeInBytes);
            
            if (numBytesRead != frameSizeInBytes) {
                
                if (numBytesRead % sampleSizeInBytes != 0) {
                    throw new Error("Incomplete sample read.");
                }
                
                byte[] shrinked = new byte[numBytesRead];
                System.arraycopy(data, 0, shrinked, 0, numBytesRead);
                data = shrinked;
            }
            
            if (keepDataReference) {
                utterance.add(data);
            }
            
            double[] samples = DataUtil.bytesToValues
                (data, 0, data.length, sampleSizeInBytes, signed);
            
            return (new DoubleData
                    (samples, (int) audioStream.getFormat().getSampleRate(),
                     collectTime, firstSampleNumber));
        }
    }

    /**
     * Returns a native audio format that has the same encoding, number
     * of channels, endianness and sample size as the given format,
     * and a sample rate that is larger than the given sample rate.
     *
     * @return a suitable native audio format
     */
    private static AudioFormat getNativeAudioFormat(AudioFormat format) {
        // try to do sample rate conversion
        Line.Info[] lineInfos = AudioSystem.getTargetLineInfo
            (new Line.Info(TargetDataLine.class));

        AudioFormat nativeFormat = null;

        // find a usable target line
        for (int i = 0; i < lineInfos.length; i++) {
            
            AudioFormat[] formats = 
                ((TargetDataLine.Info)lineInfos[i]).getFormats();
            
            for (int j = 0; j < formats.length; j++) {
                
                // for now, just accept downsampling, not checking frame
                // size/rate (encoding assumed to be PCM)
                
                AudioFormat thisFormat = formats[j];
                if (thisFormat.getEncoding() == format.getEncoding()
                    && thisFormat.getChannels() == format.getChannels()
                    && thisFormat.isBigEndian() == format.isBigEndian()
                    && thisFormat.getSampleSizeInBits() == 
                    format.getSampleSizeInBits()
                    && thisFormat.getSampleRate() > format.getSampleRate()) {
                    nativeFormat = thisFormat;
                    break;
                }
            }
            if (nativeFormat != null) {
                //no need to look through remaining lineinfos
                break;
            }
        }
        return nativeFormat;
    }



    /**
     * Clears all cached audio data.
     */
    public void clear() {
        audioList = new DataList();
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
            output = (Data) audioList.remove();
            if (output instanceof DataEndSignal) {
                utteranceEndReached = true;
            }
        }

        getTimer().stop();

        // signalCheck(output);

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
}


/**
 * Manages the data as a FIFO queue
 */
class DataList {

    private List list;

    /**
     * Creates a new data list
     */
    public DataList() {
        list = new LinkedList();
    }

    /**
     * Adds a data to the queue
     *
     * @param data the data to add
     */
    public synchronized void add(Data data) {
        list.add(data);
        notify();
    }

    /**
     * Returns the current size of the queue
     *
     * @return the size of the queue
     */
    public synchronized int size() {
        return list.size();
    }

    /**
     * Removes the oldest item on the queue
     *
     * @return the oldest item
     */
    public synchronized Data remove() {
        try {
            while (list.size() == 0) {
                // System.out.println("Waiting...");
                wait();
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        Data data = (Data) list.remove(0);
        if (data == null) {
            System.out.println("DataList is returning null.");
        }
        return data;
    }
}
