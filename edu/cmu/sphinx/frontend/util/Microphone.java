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
     * The Sphinx property that specifies whether to keep the audio
     * data of an utterance around until the next utterance is recorded.
     */
    public final static String PROP_KEEP_LAST_AUDIO = "keepLastAudio";

    /**
     * The default value of PROP_KEEP_AUDIO.
     */
    public final static boolean PROP_KEEP_LAST_AUDIO_DEFAULT = false;


    /**
     * Parameters for audioFormat
     */
    private AudioFormat audioFormat;
    private AudioFormat nativeAudioFormat;
    private AudioFormat finalAudioFormat;
    private int sampleRate;
    private int sampleSizeInBytes;
    private int channels;
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
    private DataList audioList;
    private Utterance currentUtterance;
    private int audioBufferSize = 160000;

    private long totalSamplesRead;
    private int frameSizeInBytes;
    private int msecPerRead;

    private volatile boolean started = false;
    private volatile boolean recording = false;
    private volatile boolean utteranceEndReached = true;

    private boolean closeBetweenUtterances = true;
    private boolean keepDataReference = true;
    private DataLine.Info info;

    private Logger logger;
    
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

        sampleRate = ps.getInt(PROP_SAMPLE_RATE, PROP_SAMPLE_RATE_DEFAULT);
        
	closeBetweenUtterances = ps.getBoolean
            (PROP_CLOSE_BETWEEN_UTTERANCES, true);
        
        sampleSizeInBytes = ps.getInt
            (PROP_BITS_PER_SAMPLE, PROP_BITS_PER_SAMPLE_DEFAULT)/8;

        msecPerRead = ps.getInt(PROP_MSEC_PER_READ, 
                                PROP_MSEC_PER_READ_DEFAULT);

        channels = ps.getInt(PROP_CHANNELS, PROP_CHANNELS_DEFAULT);

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

        audioFormat = new AudioFormat((float) sampleRate, 
                sampleSizeInBytes * 8, channels, signed, bigEndian);

	finalAudioFormat = audioFormat;
        audioList = new DataList();
        nativeAudioFormat = null;

        info = new DataLine.Info(TargetDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(info)) {
            logger.info(audioFormat + " not supported");
            nativeAudioFormat = getNativeAudioFormat(audioFormat);
            
            if (nativeAudioFormat == null) {
                logger.severe("couldn't find suitable target audio format " +
                              "for conversion");
            } else {
                info = new DataLine.Info(TargetDataLine.class, 
                        nativeAudioFormat);
                doConversion = true;
            }
        } else {
            doConversion = false;
        }

        // Obtain and open the line and stream.
        try {
            audioLine = (TargetDataLine) AudioSystem.getLine(info);
            finalAudioFormat = info.getFormats()[0];
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
                if (doConversion) {
                    audioStream = getConvertingStream();
                } else {
                    audioStream = new AudioInputStream(audioLine);
                }

                /* Set the frame size depending on the sample rate. */
                frameSizeInBytes = sampleSizeInBytes *
                    (int) (((float) msecPerRead) / 1000.f * 
                           ((float) audioStream.getFormat().getSampleRate()));
                if (frameSizeInBytes % 2 == 1) {
                    frameSizeInBytes++;
                }
                logger.info("Frame size: " + frameSizeInBytes + " bytes");

                /* open the audio line */
                logger.info("open");
                try {
                    audioLine.open(finalAudioFormat, audioBufferSize);
                    return true;
                } catch (LineUnavailableException e) {
                    logger.severe("Can't open microphine " + e.getMessage());
                    return false;
                }
            } 
            return true;
        } else {
            logger.severe("Can't find microphone");
            return false;
        }
    }

    /**
     * Gets a stream that converts from the natively supprted format
     * to the desired format
     *
     * @return an audio stream of the desired format
     */
    private AudioInputStream getConvertingStream() {
        AudioInputStream convertingStream = null;
        try {
            convertingStream  = AudioSystem.getAudioInputStream
                    (audioFormat, new AudioInputStream(audioLine));
            logger.info ("Converting from " + nativeAudioFormat.getSampleRate()
                        + "Hz to " + audioFormat.getSampleRate() + "Hz");
        } catch (IllegalArgumentException e) {
            logger.info
                ("Using native format: Cannot convert from " +
                 nativeAudioFormat.getSampleRate() + "Hz to " + 
                 audioFormat.getSampleRate() + "Hz");
            convertingStream = nativelySupportedStream;
        }
        return convertingStream;
    }


    /**
     * Returns the format of the audio recorded by this Microphone.
     * Note that this might be different from the configured format.
     *
     * @return the current AudioFormat
     */
    public AudioFormat getAudioFormat() {
        return finalAudioFormat;
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
        open();
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

        public RecordingThread(String name) {
            super(name);
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
		    ("Microphone", finalAudioFormat);
	    }
	    
	    audioList.add(new DataStartSignal());
	    logger.info("DataStartSignal added");
	    try {
		audioLine.flush();
		audioLine.start();
		while (recording || audioStream.available() > 0) {
		    Data data = readData(currentUtterance);
		    if (data == null) {
			break;
		    }
		    audioList.add(data);
		}
                audioStream.close();
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
        logger.info("Read " + numBytesRead + " bytes from audio stream.");
        if (numBytesRead <= 0) {
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

        if (keepDataReference) {
            utterance.add(data);
        }

        double[] samples = DataUtil.bytesToValues
            (data, 0, data.length, sampleSizeInBytes, signed);
        
        return (new DoubleData(samples, 
                               (int) audioStream.getFormat().getSampleRate(),
                               collectTime,
                               firstSampleNumber));
    }

    /**
     * Returns a native audio format that has the same encoding, number
     * of channels, endianness and sample size as the given format,
     * and a sample rate that is larger than the given sample rate
     * (which is given by desiredFormat.getSampleRate()).
     *
     * @return a suitable native audio format
     */
    public static AudioFormat getNativeAudioFormat(AudioFormat format) {
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
            output = (Data) audioList.remove(0);
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

    /**
     * Returns true if this Microphone is currently
     * in a recording state, false otherwise.
     *
     * @return true if recording, false if not recording
     */ 
    public boolean getRecording() {
        return recording;
    }
}

class DataList {

    private List list;

    public DataList() {
        list = new LinkedList();
    }

    public synchronized void add(Data audio) {
        list.add(audio);
        // System.out.println("Data added...");
        notify();
    }

    public synchronized int size() {
        return list.size();
    }

    public synchronized Object remove(int index) {
        try {
            while (list.size() == 0) {
                // System.out.println("Waiting...");
                wait();
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        Object obj = list.remove(index);
        if (obj == null) {
            System.out.println("DataList is returning null.");
        }
        return obj;
    }
}
