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

import edu.cmu.sphinx.util.SphinxProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


/**
 * A StreamAudioSource converts data from an InputStream into
 * Audio(s). One would obtain the Audios using
 * the <code>read()</code> method.
 *
 * The size of each Audio returned is specified by:
 * <pre>
 * edu.cmu.sphinx.frontend.bytesPerAudio
 * </pre>
 *
 * @see BatchFileAudioSource
 */
public class StreamAudioSource extends DataProcessor implements AudioSource {

    private InputStream audioStream;
    private Utterance currentUtterance = null;

    private int frameSizeInBytes;
    private int bytesPerSample;
    private long totalSamplesRead;

    private boolean bigEndian;
    private boolean signedData;
    private boolean keepAudioReference;
    
    private boolean streamEndReached = false;
    private boolean utteranceEndSent = false;
    private boolean utteranceStarted = false;


    /**
     * Constructs a StreamAudioSource with the given InputStream.
     *
     * @param name the name of this StreamAudioSource
     * @param context the context of this StreamAudioSource
     * @param audioStream the InputStream where audio data comes from
     * @param streamName the name of the InputStream
     */
    public StreamAudioSource(String name, String context,
                             InputStream audioStream, String streamName) {
        super(name, context);
	initSphinxProperties();
        setInputStream(audioStream, streamName);
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties() {
        keepAudioReference = getSphinxProperties().getBoolean
            (FrontEnd.PROP_KEEP_AUDIO_REFERENCE,
             FrontEnd.PROP_KEEP_AUDIO_REFERENCE_DEFAULT);

        frameSizeInBytes = getSphinxProperties().getInt
	    (FrontEnd.PROP_BYTES_PER_AUDIO_FRAME,
             FrontEnd.PROP_BYTES_PER_AUDIO_FRAME_DEFAULT);
        
        bytesPerSample = getSphinxProperties().getInt
            (FrontEnd.PROP_BITS_PER_SAMPLE, 
             FrontEnd.PROP_BITS_PER_SAMPLE_DEFAULT) / 8;

        bigEndian = getSphinxProperties().getBoolean
            (FrontEnd.PROP_BIG_ENDIAN_DATA, 
             FrontEnd.PROP_BIG_ENDIAN_DATA_DEFAULT);

        signedData = getSphinxProperties().getBoolean
            (FrontEnd.PROP_SIGNED_DATA, FrontEnd.PROP_SIGNED_DATA_DEFAULT);

        if (frameSizeInBytes % 2 == 1) {
            frameSizeInBytes++;
        }
    }


    /**
     * Sets the InputStream from which this StreamAudioSource reads.
     *
     * @param inputStream the InputStream from which audio data comes
     * @param streamName the name of the InputStream
     */
    public void setInputStream(InputStream inputStream, String streamName) {

        this.audioStream = inputStream;

        streamEndReached = false;
        utteranceEndSent = false;
        utteranceStarted = false;
        totalSamplesRead = 0;

        if (keepAudioReference) {
            currentUtterance = new Utterance(streamName, getContext());
        } else {
            currentUtterance = null;
        }
    }

    
    /**
     * Reads and returns the next Audio from the InputStream of
     * StreamAudioSource, return null if no data is read and end of file
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

        if (streamEndReached) {
            if (!utteranceEndSent) {
                // since 'firstSampleNumber' starts at 0, the last
                // sample number should be 'totalSamplesRead - 1'
                output = new Audio
                    (Signal.UTTERANCE_END, System.currentTimeMillis(),
                     totalSamplesRead - 1);
                utteranceEndSent = true;
            }
        } else {
            if (!utteranceStarted) {
                utteranceStarted = true;
                output = new Audio
                    (Signal.UTTERANCE_START, System.currentTimeMillis(),
                     totalSamplesRead);
            } else {
                if (audioStream != null) {
                    output = readNextFrame();
                    if (output == null) {
                        if (!utteranceEndSent) {
                            output = new Audio(Signal.UTTERANCE_END,
                                               System.currentTimeMillis(),
                                               totalSamplesRead);
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
     * Returns the next Audio from the input stream, or null if
     * there is none available
     *
     * @return a Audio or null
     *
     * @throws java.io.IOException
     */
    private Audio readNextFrame() throws IOException {

        // read one frame's worth of bytes
	int read = 0;
	int totalRead = 0;
        final int bytesToRead = frameSizeInBytes;
        byte[] samplesBuffer = new byte[frameSizeInBytes];
        long collectTime = System.currentTimeMillis();
        long firstSample = totalSamplesRead;

	do {
	    read = audioStream.read
		(samplesBuffer, totalRead, bytesToRead - totalRead);
	    if (read > 0) {
		totalRead += read;
	    }
	} while (read != -1 && totalRead < bytesToRead);

        if (totalRead <= 0) {
            closeAudioStream();
            return null;
        }

        // shrink incomplete frames

        totalSamplesRead += (totalRead / bytesPerSample);

        if (totalRead < bytesToRead) {
            totalRead = (totalRead % 2 == 0) ? totalRead + 2 : totalRead + 3;
            byte[] shrinkedBuffer = new byte[totalRead];
            System.arraycopy(samplesBuffer, 0, shrinkedBuffer, 0, totalRead);
            samplesBuffer = shrinkedBuffer;
            closeAudioStream();
        }

        // turn it into an Audio object

        double[] doubleAudio;
        if (bigEndian) {
            doubleAudio = Util.bytesToSamples
                (samplesBuffer, 0, totalRead, bytesPerSample, signedData);
        } else {
            doubleAudio = Util.littleEndianBytesToSamples
                (samplesBuffer, 0, totalRead, bytesPerSample, signedData);
        }

        Audio audio;

        if (keepAudioReference) {
            currentUtterance.add(samplesBuffer);
            audio = new Audio(doubleAudio, currentUtterance, 
                              collectTime, firstSample);
        } else {
            audio = new Audio(doubleAudio, collectTime, firstSample);
        }

        if (getDump()) {
            System.out.println("FRAME_SOURCE " + audio.toString());
        }
        
        return audio;
    }

    private void closeAudioStream() throws IOException {
        streamEndReached = true;
        if (audioStream != null) {
            audioStream.close();
        }
    }
}





