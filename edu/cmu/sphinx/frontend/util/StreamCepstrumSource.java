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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FrontEndFactory;
import edu.cmu.sphinx.util.ExtendedStreamTokenizer;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Utilities;


/**
 * Produces Mel-cepstrum data from an InputStream.
 * To set the inputstream with cepstral data, use the
 * {@link #setInputStream(InputStream,boolean) setInputStream} method,
 * and then call {@link #getData} to obtain the Data objects that
 * have cepstra data in it.
 */
public class StreamCepstrumSource extends BaseDataProcessor {

    private static final String PROP_PREFIX
        = "edu.cmu.sphinx.frontend.util.StreamCepstrumSource.";

    /**
     * The SphinxProperties specifying whether the input is in binary.
     */
    public final static String PROP_BINARY = PROP_PREFIX + "binary";

    /**
     * The default value for PROP_BINARY.
     */
    public final static boolean PROP_BINARY_DEFAULT = true;

    /**
     * The SphinxProperties name for frame size in milliseconds.
     */
    public static final String PROP_FRAME_SIZE_MS
        = PROP_PREFIX + "frameSizeInMs";

    /**
     * The default value for PROP_FRAME_SIZE_MS.
     */
    public static final float PROP_FRAME_SIZE_MS_DEFAULT = 25.625f;

    /**
     * The SphinxProperties name for frame shift in milliseconds,
     * which has a default value of 10F.
     */
    public static final String PROP_FRAME_SHIFT_MS
        = PROP_PREFIX + "frameShiftInMs";

    /**
     * The default value for PROP_FRAME_SHIFT_MS.
     */
    public static final float PROP_FRAME_SHIFT_MS_DEFAULT = 10;

    /**
     * The SphinxProperty specifying the length of the cepstrum data.
     */
    public static final String PROP_CEPSTRUM_LENGTH
        = PROP_PREFIX + "cepstrumLength";

    /**
     * The default value of PROP_CEPSTRUM_LENGTH.
     */
    public static final int PROP_CEPSTRUM_LENGTH_DEFAULT = 13;


    private boolean binary;
    private ExtendedStreamTokenizer est;  // for ASCII files
    private DataInputStream binaryStream; // for binary files
    private int numPoints;
    private int curPoint;
    private int cepstrumLength;
    private int frameShift;
    private int frameSize;
    private long firstSampleNumber;
    private boolean bigEndian = true;


    /**
     * Constructs a StreamCepstrumSource that reads
     * MelCepstrum data from the given path.
     *
     * @param name         the name of this StreamCepstrumSource, if it is
     *                     null, the name "StreamCepstrumSource" will be given
     *                     by default
     * @param frontEnd     the front end this StreamCepstrumSource belongs to
     * @param props        the SphinxProperties used to read properties
     * @param predecessor  the DataProcessor to read Data from, usually
     *                     null for this StreamCepstrumSource
     */
    public void initialize(String name, String frontEnd,
                           SphinxProperties props, DataProcessor predecessor) {
	super.initialize((name == null ? "StreamCepstrumSource" : name),
                         frontEnd, props, predecessor);
	initSphinxProperties(props);
	curPoint = -1;
        firstSampleNumber = 0;
	bigEndian = true;
    }


    /**
     * Sets the InputStream to read cepstral data from.
     *
     * @param is the InputStream to read cepstral data from
     * @param bigEndian true if the InputStream data is in big-endian,
     *     false otherwise
     *
     * @throws IOException if an I/O error occurs
     */
    public void setInputStream(InputStream is, boolean bigEndian) 
	throws IOException {	
	this.bigEndian = bigEndian;
	if (binary) {
	    binaryStream = new DataInputStream(new BufferedInputStream(is));
	    if (bigEndian) {
		numPoints = binaryStream.readInt();
		System.out.println("BigEndian");
	    } else {
		numPoints = Utilities.readLittleEndianInt(binaryStream);
		System.out.println("LittleEndian");
	    }
	    System.out.println("Frames: " + numPoints/cepstrumLength);
	} else {
	    est = new ExtendedStreamTokenizer(is, false);
	    numPoints = est.getInt("num_frames");
	    est.expectString("frames");
	}
	curPoint = -1;
        firstSampleNumber = 0;
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     *
     * @param props the SphinxProperties to read from
     */
    private void initSphinxProperties(SphinxProperties props) {
	
        cepstrumLength = props.getInt
            (getName(), PROP_CEPSTRUM_LENGTH, PROP_CEPSTRUM_LENGTH_DEFAULT);
	
        binary = props.getBoolean
            (getName(), PROP_BINARY, PROP_BINARY_DEFAULT);
        
        float frameShiftMs = props.getFloat
            (getName(), PROP_FRAME_SHIFT_MS, PROP_FRAME_SHIFT_MS_DEFAULT);
        
        float frameSizeMs = props.getFloat
            (getName(), PROP_FRAME_SIZE_MS, PROP_FRAME_SIZE_MS_DEFAULT);
        
        int sampleRate = props.getInt
            (getName(),
             FrontEndFactory.PROP_SAMPLE_RATE,
             FrontEndFactory.PROP_SAMPLE_RATE_DEFAULT);

        frameShift = DataUtil.getSamplesPerWindow(sampleRate, frameShiftMs);
        frameSize = DataUtil.getSamplesPerShift(sampleRate, frameSizeMs);
    }


    /**
     * Returns the next Data object, which is the mel cepstrum of the
     * input frame. However, it can also be other Data objects
     * like DataStartSignal.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     *
     * @throws DataProcessingException if a data processing error occurs
     */
    public Data getData() throws DataProcessingException {

	Data data = null;

	if (curPoint == -1) {
	    data = new DataStartSignal();
	    curPoint++;
	} else if (curPoint == numPoints) {
            if (numPoints > 0) {
                firstSampleNumber =
                    (firstSampleNumber - frameShift + frameSize - 1);
            }
            data = new DataEndSignal();
            try {
                binaryStream.close();
                curPoint++;
            } catch (IOException ioe) {
                throw new DataProcessingException
                    ("IOException closing cepstrum stream.");
            }
	} else if (curPoint > numPoints) {
            data = null;
	} else {
            double[] vectorData = new double[cepstrumLength];
            long collectTime = System.currentTimeMillis();

	    for (int i = 0; i < cepstrumLength; i++) {
                try {
                    if (binary) {
                        if (bigEndian) {
                            vectorData[i] = (double) binaryStream.readFloat();
                        } else {
                            vectorData[i] = (double)
                                Utilities.readLittleEndianFloat(binaryStream);
                        }
                    } else {
                        vectorData[i] = (double) est.getFloat("cepstrum data");
                    }
                    curPoint++;
                } catch (IOException ioe) {
                    throw new DataProcessingException
                        ("IOException reading from cepstrum stream.");
                }
	    }

	    // System.out.println("Read: " + curPoint);
	    data  = new DoubleData(vectorData, collectTime, firstSampleNumber);
            firstSampleNumber += frameShift;
	    // System.out.println(data);
	}
	return data;
    }
}
