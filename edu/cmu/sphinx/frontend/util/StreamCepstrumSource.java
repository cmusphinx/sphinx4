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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.ExtendedStreamTokenizer;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;


/**
 * Produces Mel-cepstrum data from an InputStream.
 * To set the inputstream with cepstral data, use the
 * {@link #setInputStream(InputStream,boolean) setInputStream} method,
 * and then call {@link #getData} to obtain the Data objects that
 * have cepstra data in it.
 */
public class StreamCepstrumSource extends BaseDataProcessor {
    /**
     * The SphinxProperty specifying whether the input is in binary.
     */
    public final static String PROP_BINARY = "binary";
    /**
     * The default value for PROP_BINARY.
     */
    public final static boolean PROP_BINARY_DEFAULT = true;
    /**
     * The sphinx property  name for frame size in milliseconds.
     */
    public static final String PROP_FRAME_SIZE_MS = "frameSizeInMs";
    /**
     * The default value for PROP_FRAME_SIZE_MS.
     */
    public static final float PROP_FRAME_SIZE_MS_DEFAULT = 25.625f;
    /**
     * The sphinx property  name for frame shift in milliseconds, which has a
     * default value of 10F.
     */
    public static final String PROP_FRAME_SHIFT_MS = "frameShiftInMs";
    /**
     * The default value for PROP_FRAME_SHIFT_MS.
     */
    public static final float PROP_FRAME_SHIFT_MS_DEFAULT = 10;
    /**
     * The sphinx property  specifying the length of the cepstrum data.
     */
    public static final String PROP_CEPSTRUM_LENGTH = "cepstrumLength";
    /**
     * The default value of PROP_CEPSTRUM_LENGTH.
     */
    public static final int PROP_CEPSTRUM_LENGTH_DEFAULT = 13;
    /**
     * The sphinx property that defines the sample rate
     */
    public static final String PROP_SAMPLE_RATE = "sampleRate";
    /**
     * The default value for PROP_SAMPLE_RATE
     */
    public static final int PROP_SAMPLE_RATE_DEFAULT = 16000;
    private boolean binary;
    private ExtendedStreamTokenizer est; // for ASCII files
    private DataInputStream binaryStream; // for binary files
    private int numPoints;
    private int curPoint;
    private int cepstrumLength;
    private int frameShift;
    private int frameSize;
    private int sampleRate;
    private long firstSampleNumber;
    private boolean bigEndian = true;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_CEPSTRUM_LENGTH, PropertyType.INT);
        registry.register(PROP_BINARY, PropertyType.BOOLEAN);
        registry.register(PROP_FRAME_SHIFT_MS, PropertyType.INT);
        registry.register(PROP_FRAME_SIZE_MS, PropertyType.INT);
        registry.register(PROP_SAMPLE_RATE, PropertyType.INT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        cepstrumLength = ps.getInt(PROP_CEPSTRUM_LENGTH,
                PROP_CEPSTRUM_LENGTH_DEFAULT);
        binary = ps.getBoolean(PROP_BINARY, PROP_BINARY_DEFAULT);
        float frameShiftMs = ps.getFloat(PROP_FRAME_SHIFT_MS,
                PROP_FRAME_SHIFT_MS_DEFAULT);
        float frameSizeMs = ps.getFloat(PROP_FRAME_SIZE_MS,
                PROP_FRAME_SIZE_MS_DEFAULT);
        sampleRate = ps.getInt(PROP_SAMPLE_RATE, PROP_SAMPLE_RATE_DEFAULT);
        frameShift = DataUtil.getSamplesPerWindow(sampleRate, frameShiftMs);
        frameSize = DataUtil.getSamplesPerShift(sampleRate, frameSizeMs);
    }

    /**
     * Constructs a StreamCepstrumSource that reads MelCepstrum data from the
     * given path.
     *  
     */
    public void initialize() {
        super.initialize();
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
            // send a DataEndSignal
            int numberFrames = curPoint / frameSize;
            int totalSamples = (numberFrames - 1) * frameShift + frameSize;
            long duration = (long)
                (((double)totalSamples/(double)sampleRate) * 1000.0);

            data = new DataEndSignal(duration);

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
	    data  = new DoubleData
                (vectorData, sampleRate, collectTime, firstSampleNumber);
            firstSampleNumber += frameShift;
	    // System.out.println(data);
	}
	return data;
    }
}
