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

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumSource;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Signal;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.ExtendedStreamTokenizer;
import edu.cmu.sphinx.util.Utilities;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.IOException;


/**
 * Produces MelCepstrum data from a file.
 */
public class StreamCepstrumSource extends DataProcessor implements
CepstrumSource {


    /**
     * The SphinxProperty specifying whether the input is in binary.
     */
    public final static String PROP_BINARY
        = "edu.cmu.sphinx.frontend.util.StreamCepstrumSource.binary";


    /**
     * The default value for PROP_BINARY.
     */
    public final static boolean PROP_BINARY_DEFAULT = true;


    private boolean binary;
    private ExtendedStreamTokenizer est;  // for ASCII files
    private DataInputStream binaryStream; // for binary files
    private int numPoints;
    private int curPoint;
    private int cepstrumLength;
    private boolean bigEndian = true;


    /**
     * Constructs a StreamCepstrumSource that reads
     * MelCepstrum data from the given path.
     *
     * @param name the name of this StreamCepstrumSource
     * @param context the context for the producer
     *
     * @throws IOException if an error occurs while reading the data
     */
    public StreamCepstrumSource(String name, String context) throws
    IOException {
	super(name, context);
	initSphinxProperties();
	curPoint = -1;
	bigEndian = true;
    }


    /**
     * Sets the InputStream to read cepstral data from.
     *
     * @param is the InputStream to read cepstral data from
     * @param bigEndian true if the InputStream data is in big-endian,
     *     false otherwise
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
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties() {
	SphinxProperties properties = getSphinxProperties();
	cepstrumLength = properties.getInt
            (FrontEnd.PROP_CEPSTRUM_SIZE, FrontEnd.PROP_CEPSTRUM_SIZE_DEFAULT);
	binary = properties.getBoolean(PROP_BINARY, PROP_BINARY_DEFAULT);
    }


    /**
     * Returns the next Cepstrum object, which is the mel cepstrum of the
     * input frame. However, it can also be other Cepstrum objects
     * like a EndPointSignal.
     *
     * @return the next available Cepstrum object, returns null if no
     *     Cepstrum object is available
     */
    public Cepstrum getCepstrum() throws IOException {

	Cepstrum data = null;

	if (curPoint == -1) {
	    data = new Cepstrum
                (Signal.UTTERANCE_START, System.currentTimeMillis());
	    curPoint++;
	} else if (curPoint == numPoints) {
            data = new Cepstrum
                (Signal.UTTERANCE_END, System.currentTimeMillis());
	    binaryStream.close();
	    curPoint++;
	} else if (curPoint > numPoints) {
            data = null;
	} else {
            float[] vectorCepstrum = new float[cepstrumLength];
            long collectTime = System.currentTimeMillis();

	    for (int i = 0; i < cepstrumLength; i++) {
		if (binary) {
		    if (bigEndian) {
			vectorCepstrum[i] = binaryStream.readFloat();
		    } else {
			vectorCepstrum[i] = 
			    Utilities.readLittleEndianFloat(binaryStream);
		    }
		} else {
		    vectorCepstrum[i] = est.getFloat("cepstrum data");
		}
		curPoint++;
	    }

	    // System.out.println("Read: " + curPoint);
	    data  = new Cepstrum(vectorCepstrum, collectTime);
	    // System.out.println(data);
	}
	return data;
    }
}
