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

import java.io.IOException;
import java.io.InputStream;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.SphinxProperties;


/**
 * A StreamDataSource converts data from an InputStream into
 * Data objects. One would call
 * {@link #setInputStream(InputStream,String) setInputStream} to set
 * the input stream, and call {@link #getData} to obtain the
 * Data object.
 */
public class StreamDataSource extends BaseDataProcessor {

    /**
     * SphinxProperty prefix for StreamDataSource.
     */
    public static final String PROP_PREFIX =
        "edu.cmu.sphinx.frontend.util.StreamDataSource.";

    /**
     * SphinxProperty for the number of bytes to read from the InputStream
     * each time.
     */
    public static final String PROP_BYTES_PER_READ =
        PROP_PREFIX + "bytesPerRead";

    /**
     * Default value for PROP_BYTES_PER_READ.
     */
    public static final int PROP_BYTES_PER_READ_DEFAULT = 3200;

    /**
     * SphinxProperty for the number of bits per value.
     */
    public static final String PROP_BITS_PER_SAMPLE =
        PROP_PREFIX + "bitsPerSample";

    /**
     * Default value for PROP_BITS_PER_SAMPLE.
     */
    public static final int PROP_BITS_PER_SAMPLE_DEFAULT = 16;

    /**
     * The SphinxProperty specifying whether the input data is big-endian.
     */
    public static final String PROP_BIG_ENDIAN_DATA 
        = PROP_PREFIX + "bigEndianData";

    /**
     * The default value for PROP_IS_DATA_BIG_ENDIAN.
     */
    public static final boolean PROP_BIG_ENDIAN_DATA_DEFAULT = true;

    /**
     * The SphinxProperty specifying whether the input data is signed.
     */
    public static final String PROP_SIGNED_DATA = PROP_PREFIX + "signedData";

    /**
     * The default value of PROP_SIGNED_DATA.
     */
    public static final boolean PROP_SIGNED_DATA_DEFAULT = true;



    private InputStream dataStream;

    private int bytesPerRead;
    private int bytesPerValue;
    private long totalValuesRead;

    private boolean bigEndian;
    private boolean signedData;
    
    private boolean streamEndReached = false;
    private boolean utteranceEndSent = false;
    private boolean utteranceStarted = false;


    /**
     * Constructs a StreamDataSource with the given InputStream.
     *
     * @param name the name of this StreamDataSource
     * @param frontEnd the front end this StreamDataSource belongs to
     * @param props the SphinxProperties to use
     * @param predecessor the predecessor DataProcessor
     */
    public void initialize(String name, String frontEnd,
                           SphinxProperties props, DataProcessor predecessor) {
        super.initialize(name, frontEnd, props, predecessor);
	initSphinxProperties(props);
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     *
     * @param props the SphinxProperties to read from
     */
    private void initSphinxProperties(SphinxProperties props) {

        bytesPerRead = props.getInt(getFullPropertyName(PROP_BYTES_PER_READ),
                                    PROP_BYTES_PER_READ_DEFAULT);
        
        bytesPerValue = props.getInt(getFullPropertyName(PROP_BITS_PER_SAMPLE),
                                     PROP_BITS_PER_SAMPLE_DEFAULT) / 8;

        bigEndian = props.getBoolean(getFullPropertyName(PROP_BIG_ENDIAN_DATA),
                                     PROP_BIG_ENDIAN_DATA_DEFAULT);

        signedData = props.getBoolean(getFullPropertyName(PROP_SIGNED_DATA),
                                      PROP_SIGNED_DATA_DEFAULT);

        if (bytesPerRead % 2 == 1) {
            bytesPerRead++;
        }
    }


    /**
     * Sets the InputStream from which this StreamDataSource reads.
     *
     * @param inputStream  the InputStream from which audio data comes
     * @param streamName   the name of the InputStream
     */
    public void setInputStream(InputStream inputStream, String streamName) {
        dataStream = inputStream;
        streamEndReached = false;
        utteranceEndSent = false;
        utteranceStarted = false;
        totalValuesRead = 0;
    }

    
    /**
     * Reads and returns the next Data from the InputStream of
     * StreamDataSource, return null if no data is read and end of file
     * is reached.
     *
     * @return the next Data or <code>null</code> if none is
     *     available
     *
     * @throws DataProcessingException if there is a data processing error
     */
    public Data getData() throws DataProcessingException {

        getTimer().start();

        Data output = null;

        if (streamEndReached) {
            if (!utteranceEndSent) {
                // since 'firstSampleNumber' starts at 0, the last
                // sample number should be 'totalValuesRead - 1'
                output = new DataEndSignal();
                utteranceEndSent = true;
            }
        } else {
            if (!utteranceStarted) {
                utteranceStarted = true;
                output = new DataStartSignal();
            } else {
                if (dataStream != null) {
                    output = readNextFrame();
                    if (output == null) {
                        if (!utteranceEndSent) {
                            output = new DataEndSignal();
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
     * Returns the next Data from the input stream, or null if
     * there is none available
     *
     * @return a Data or null
     *
     * @throws java.io.IOException
     */
    private Data readNextFrame() throws DataProcessingException {

        // read one frame's worth of bytes
	int read = 0;
	int totalRead = 0;
        final int bytesToRead = bytesPerRead;
        byte[] samplesBuffer = new byte[bytesPerRead];
        long collectTime = System.currentTimeMillis();
        long firstSample = totalValuesRead;

        try {
            do {
                read = dataStream.read
                    (samplesBuffer, totalRead, bytesToRead - totalRead);
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
                totalRead = (totalRead % 2 == 0) ?
                    totalRead + 2 : totalRead + 3;
                byte[] shrinkedBuffer = new byte[totalRead];
                System.arraycopy
                    (samplesBuffer, 0, shrinkedBuffer, 0, totalRead);
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
            doubleData = DataUtil.bytesToValues
                (samplesBuffer, 0, totalRead, bytesPerValue, signedData);
        } else {
            doubleData = DataUtil.littleEndianBytesToValues
                (samplesBuffer, 0, totalRead, bytesPerValue, signedData);
        }

        Data audio = new DoubleData(doubleData, collectTime, firstSample);
        
        return audio;
    }

    private void closeDataStream() throws IOException {
        streamEndReached = true;
        if (dataStream != null) {
            dataStream.close();
        }
    }
}





