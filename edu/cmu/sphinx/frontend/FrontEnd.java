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


package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.model.acoustic.AcousticModel;

import java.io.IOException;


/**
 * A frontend where FeatureFrames can be obtained.
 */
public interface FrontEnd {

    /**
     * The prefix for all Frontend SphinxProperties names.
     * Its value is currently <code>"edu.cmu.sphinx.frontend."</code>.
     */
    public static final String PROP_PREFIX = "edu.cmu.sphinx.frontend.";


    /**
     * The SphinxProperty name for sample rate in Hertz (i.e.,
     * number of times per second), which has a default value of 8000.
     */
    public static final String PROP_SAMPLE_RATE = PROP_PREFIX + "sampleRate";

    
    /**
     * The SphinxProperty name for the number of bits per sample.
     */
    public static final String PROP_BITS_PER_SAMPLE = PROP_PREFIX +
	"bitsPerSample";


    /**
     * The SphinxProperty name for the number of bytes per frame.
     */
    public static final String PROP_BYTES_PER_AUDIO_FRAME = PROP_PREFIX +
	"bytesPerAudioFrame";


    /**
     * The SphinxProperty name for window size in milliseconds.
     */
    public static final String PROP_WINDOW_SIZE_MS = PROP_PREFIX + 
	"windowSizeInMs";


    /**
     * The SphinxProperty name for window shift in milliseconds,
     * which has a default value of 10F.
     */
    public static final String PROP_WINDOW_SHIFT_MS = PROP_PREFIX +
	"windowShiftInMs";

    
    /**
     * The SphinxProperty name for the size of a cepstrum, which is
     * 13 by default.
     */
    public static final String PROP_CEPSTRUM_SIZE = PROP_PREFIX + 
	"cepstrumSize";


    /**
     * The SphinxProperty name that indicates whether Features
     * should retain a reference to the original raw audio bytes. The
     * default value is true.
     */
    public static final String PROP_KEEP_AUDIO_REFERENCE = PROP_PREFIX +
	"keepAudioReference";

    
    /**
     * The SphinxProperty name that specifies the Filterbank class.
     */
    public static final String PROP_FILTERBANK = PROP_PREFIX + "filterbank";


    /**
     * The SphinxProperty name that specifies the CepstrumProducer class.
     */
    public static final String PROP_CEPSTRUM_PRODUCER = PROP_PREFIX + 
	"cepstrumProducer";


    /**
     * The SphinxProperty name that specifies the Endpointer class.
     */
    public static final String PROP_ENDPOINTER = PROP_PREFIX + "endpointer";


    /**
     * The SphinxProperty name that specifies the CMN class.
     */
    public static final String PROP_CMN = PROP_PREFIX + "cmn";


    /**
     * The SphinxProperty name that specifies the FeatureExtractor class.
     */
    public static final String PROP_FEATURE_EXTRACTOR = PROP_PREFIX + 
	"featureExtractor";


    /**
     * The SphinxProperty name that specifies whether to use the
     * properties from the acoustic model.
     */
    public static final String PROP_USE_ACOUSTIC_MODEL_PROPERTIES =
	PROP_PREFIX + "useAcousticModelProperties";


    /**
     * The prefix for acoustic model properties.
     */
    public static final String ACOUSTIC_PROP_PREFIX = 
	AcousticModel.PROP_PREFIX;


    /**
     * Returns the next N feature (of the given acoustic model) 
     * produced by this FrontEnd, in a FeatureFrame object.
     * The number of Features return maybe less than N, in which
     * case the last Feature will contain a Signal.UTTERANCE_END signal.
     *
     * @param numberFeatures the number of FeatureFrames to return
     *
     * @return N number of FeatureFrames, or null
     *    if no more FeatureFrames available
     *
     * @see FeatureFrame
     *
     * @throws java.io.IOException if an I/O error occurred
     */
    public FeatureFrame getFeatureFrame(int numberFeatures, 
					String acousticModelName) 
	throws IOException;

}
