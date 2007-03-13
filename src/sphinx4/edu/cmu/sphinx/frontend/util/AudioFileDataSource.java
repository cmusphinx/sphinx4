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

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * An AudioFileDataSource generates a stream of audio data from a given audion file. All required information concerning
 * the audio format are read directly from the file . One would call {@link * #setInputFile(URL audioFileURL, String
 * streamName)}to set the input file, and call {@link #getData}to obtain the Data frames.
 * <p/>
 * Using JavaSound as backend this class is able to handle all sound files supported by JavaSound. Beside the built-in
 * support for .wav, .au and .aiff. Using plugins (cf.  http://www.jsresources.org/ ) it can be extended to support
 * .ogg, .mp3, .speex and others.
 *
 * @author Holger Brandl
 */

public class AudioFileDataSource extends BaseDataProcessor {

    /** SphinxProperty for the number of bytes to read from the InputStream each time. */
    public static final String PROP_BYTES_PER_READ = "bytesPerRead";
    /** Default value for PROP_BYTES_PER_READ. */
    public static final int PROP_BYTES_PER_READ_DEFAULT = 3200;

    protected InputStream dataStream;
    protected int sampleRate;
    protected int bytesPerRead;
    protected int bytesPerValue;
    private long totalValuesRead;
    protected boolean bigEndian;
    protected boolean signedData;
    private boolean streamEndReached = false;
    private boolean utteranceEndSent = false;
    private boolean utteranceStarted = false;

    protected List<NewFileListener> fileListeners = new ArrayList<NewFileListener>();


    public AudioFileDataSource() {
        this(PROP_BYTES_PER_READ_DEFAULT);
    }


    public AudioFileDataSource(int bytesPerRead) {
        super();

        initialize();
        this.bytesPerRead = bytesPerRead;
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
    *      edu.cmu.sphinx.util.props.Registry)
    */
    public void register(String name, Registry registry)
            throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_BYTES_PER_READ, PropertyType.INT);
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        bytesPerRead = ps.getInt(PROP_BYTES_PER_READ, PROP_BYTES_PER_READ_DEFAULT);

        initialize();
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.frontend.DataProcessor#initialize(edu.cmu.sphinx.frontend.CommonConfig)
    */
    public void initialize() {
        super.initialize();

        if (bytesPerRead % 2 == 1) {
            bytesPerRead++;
        }
    }


    /**
     * Sets the audio file from which the data-stream will be generated of.
     *
     * @param audioFile  The location of the audio file to use
     * @param streamName The name of the InputStream. if <code>null</code> the complete path of the audio file will be
     *                   uses as stream name.
     */
    public void setAudioFile(File audioFile, String streamName) {
        try {
            setAudioFile(audioFile.toURI().toURL(), streamName);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Sets the audio file from which the data-stream will be generated of.
     *
     * @param audioFileURL The location of the audio file to use
     * @param streamName   The name of the InputStream. if <code>null</code> the complete path of the audio file will be
     *                     uses as stream name.
     */
    public void setAudioFile(URL audioFileURL, String streamName) {
        // first close the last stream if there's such a one
        if (dataStream != null) {
            try {
                dataStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            dataStream = null;
        }

        assert audioFileURL != null;
        if (streamName != null)
            streamName = audioFileURL.getPath();

        AudioInputStream audioStream = null;
        try {
            audioStream = AudioSystem.getAudioInputStream(audioFileURL);
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (NewFileListener fileListener : fileListeners)
            fileListener.newFileProcessingStarted(new File(audioFileURL.getFile()));

        setInputStream(audioStream, streamName);
    }


    /**
     * Sets the InputStream from which this StreamDataSource reads.
     *
     * @param inputStream the InputStream from which audio data comes
     * @param streamName  the name of the InputStream
     */
    public void setInputStream(AudioInputStream inputStream, String streamName) {
        dataStream = inputStream;
        streamEndReached = false;
        utteranceEndSent = false;
        utteranceStarted = false;

        AudioFormat format = inputStream.getFormat();
        sampleRate = (int) format.getSampleRate();
        bigEndian = format.isBigEndian();

        if (format.getSampleSizeInBits() % 8 != 0)
            throw new Error("StreamDataSource: bits per sample must be a multiple of 8.");
        bytesPerValue = format.getSampleSizeInBits() / 8;

        // test wether all files in the stream have the same format

        AudioFormat.Encoding encoding = format.getEncoding();
        if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED))
            signedData = true;
        else if (encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED))
            signedData = false;
        else
            throw new RuntimeException("used file encoding is not supported");

        totalValuesRead = 0;
    }


    /**
     * Reads and returns the next Data from the InputStream of StreamDataSource, return null if no data is read and end
     * of file is reached.
     *
     * @return the next Data or <code>null</code> if none is available
     * @throws edu.cmu.sphinx.frontend.DataProcessingException
     *          if there is a data processing error
     */
    public Data getData() throws DataProcessingException {
        getTimer().start();
        Data output = null;
        if (streamEndReached) {
            if (!utteranceEndSent) {
                // since 'firstSampleNumber' starts at 0, the last
                // sample number should be 'totalValuesRead - 1'
                output = new DataEndSignal(getDuration());
                utteranceEndSent = true;
            }
        } else {
            if (!utteranceStarted) {
                utteranceStarted = true;
                output = new DataStartSignal(sampleRate);
            } else {
                if (dataStream != null) {
                    output = readNextFrame();
                    if (output == null) {
                        if (!utteranceEndSent) {
                            output = new DataEndSignal(getDuration());
                            utteranceEndSent = true;
                        }
                    }
                }
            }
        }
        getTimer().stop();
        return output;
    }


    /**
     * Returns the next Data from the input stream, or null if there is none available
     *
     * @return a Data or null
     * @throws java.io.IOException
     */
    private Data readNextFrame() throws DataProcessingException {
        // read one frame's worth of bytes
        int read;
        int totalRead = 0;
        final int bytesToRead = bytesPerRead;
        byte[] samplesBuffer = new byte[bytesPerRead];
        long collectTime = System.currentTimeMillis();
        long firstSample = totalValuesRead;
        try {
            do {
                read = dataStream.read(samplesBuffer, totalRead, bytesToRead
                        - totalRead);
                if (read > 0) {
                    totalRead += read;
                }
            } while (read != -1 && totalRead < bytesToRead);
            if (totalRead <= 0) {
                closeDataStream();
                return null;
            }
            // shrink incomplete frames
            totalValuesRead += (totalRead / bytesPerValue);
            if (totalRead < bytesToRead) {
                totalRead = (totalRead % 2 == 0)
                        ? totalRead + 2
                        : totalRead + 3;
                byte[] shrinkedBuffer = new byte[totalRead];
                System
                        .arraycopy(samplesBuffer, 0, shrinkedBuffer, 0,
                                totalRead);
                samplesBuffer = shrinkedBuffer;
                closeDataStream();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new DataProcessingException("Error reading data");
        }
        // turn it into an Data object
        double[] doubleData;
        if (bigEndian) {
            doubleData = DataUtil.bytesToValues(samplesBuffer, 0, totalRead, bytesPerValue, signedData);
        } else {
            doubleData = DataUtil.littleEndianBytesToValues(samplesBuffer, 0, totalRead, bytesPerValue, signedData);
        }

        return new DoubleData(doubleData, sampleRate, collectTime, firstSample);
    }


    private void closeDataStream() throws IOException {
        streamEndReached = true;
        if (dataStream != null) {
            dataStream.close();
        }
    }


    /**
     * Returns the duration of the current data stream in milliseconds.
     *
     * @return the duration of the current data stream in milliseconds
     */
    private long getDuration() {
        return (long) (((double) totalValuesRead / (double) sampleRate) * 1000.0);
    }


    public int getSampleRate() {
        return sampleRate;
    }


    public boolean isBigEndian() {
        return bigEndian;
    }


    /** Adds a new listener for new file events. */
    public void addNewFileListener(NewFileListener l) {
        if (l == null)
            return;

        fileListeners.add(l);
    }


    /** Removes a listener for new file events. */
    public void removeNewFileListener(NewFileListener l) {
        if (l == null)
            return;

        fileListeners.remove(l);
    }
}

