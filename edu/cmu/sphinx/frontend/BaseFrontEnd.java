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

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.InputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Pre-processes the audio into Features. The BaseFrontEnd extends
 * the CepstrumExtractor, with additional processors for cepstral mean
 * normalization (CMN) and feature extraction.
 * 
 * <p>Like the CepstrumExtractor, the input to the BaseFrontEnd 
 * is a DataSource object. For an audio stream,
 * the <code>StreamAudioSource</code> should be used. The 
 * input can also be a file containing a list of audio files, in which case
 * the <code>BatchFileAudioSource</code> can be used.
 * Currently, this BaseFrontEnd can also take Cepstrum objects as
 * input using the StreamCepstrumSource implementation of DataSource.
 *
 * <p>For each utterance, this BaseFrontEnd expects either Audio or
 * Cepstrum frames (or objects) in the following sequence: <code>
 * Signal.UTTERANCE_START
 * Audio or Cepstrum
 * Audio or Cepstrum
 * ...
 * Signal.UTTERANCE_END
 * </code>
 *
 * <p>Any other sequence will cause this BaseFrontEnd to throw errors.
 *
 * <p>If the front end gets a null frame OUTSIDE an utterance, 
 * it just returns a null frame. If the front end gets a null frame
 * INSIDE an utterance, it throws an Error. Therefore, there cannot be
 * a null frame (or object) in between the Signal.UTTERANCE_START and
 * Signal.UTTERANCE_END. The implementation of DataSource given to
 * the BaseFrontEnd in the constructor must make sure that these
 * conditions hold.
 *
 * <p>The output of the BaseFrontEnd are FeatureFrames, which is
 * an array of Features. This BaseFrontEnd enforces the condition that
 * every utterance starts with a Feature that contains the signal 
 * <code>Signal.UTTERANCE_START</code>, and ends with a Feature that 
 * contains the signal <code>Signal.UTTERANCE_END</code>.
 *
 * <p>To obtain a <code>FeatureFrame</code> with 10 Features from the 
 * BaseFrontEnd, you do:
 * <pre>
 * BaseFrontEnd.getFeatureFrame(10);
 * </pre>
 * If there are only 5 Features left, it will return a FeatureFrame with
 * 5 Features, followed by a Feature with a <code>Signal.UTTERANCE_END</code>
 * Signal.
 *
 * This BaseFrontEnd contains all the "standard" frontend processors like
 * Preemphasizer, HammingWindower, SpectrumAnalyzer, ..., etc.
 *
 * @see AudioSource
 * @see BatchCMN
 * @see Feature
 * @see FeatureExtractor
 * @see FeatureFrame
 * @see FeatureSource
 * @see LiveCMN
 * @see CepstrumProducer
 * @see Filterbank
 * @see SpectrumAnalyzer
 * @see Windower
 */
public class BaseFrontEnd extends CepstrumExtractor implements FrontEnd {

    private static Map frontends = new HashMap();

    private FeatureSource featureSource;  // the end of the pipeline


    /**
     * Constructs a default BaseFrontEnd.
     *
     * @param name the name of this BaseFrontEnd
     * @param context the context of this BaseFrontEnd
     * @param dataSource the place to pull data from
     *
     * @throws IOException if an I/O error occurs
     */
    public void initialize(String name, String context, 
                           DataSource dataSource)
        throws IOException {
        initialize(name, context, 
                   SphinxProperties.getSphinxProperties(context),
                   null, dataSource);
    }


    /**
     * Initializes a BaseFrontEnd with the given name, context, acoustic 
     * model name, and DataSource.
     *
     * @param name the name of this BaseFrontEnd
     * @param context the context of interest
     * @param props the SphinxProperties to use
     * @param amName the name of the acoustic model
     * @param dataSource the source of data; can be null, in which
     *    case the setDataSource() method must be called later
     *
     * @throws IOException if an I/O error occurs
     */
    public void initialize(String name, String context, 
                           SphinxProperties props,
                           String amName,
                           DataSource dataSource) throws IOException {
	super.initialize(name, context, props, amName, dataSource);

	CepstrumSource lastCepstrumSource = this;
	
        CepstrumSource cmn = getCMN(lastCepstrumSource);
	if (cmn != null) { // if we're using a CMN
	    addProcessor((DataProcessor) cmn);
	    lastCepstrumSource = cmn;
	}

        FeatureExtractor extractor = 
            getFeatureExtractor(lastCepstrumSource);

	addProcessor((DataProcessor) extractor);
        
        FrameDropper frameDropper = getFrameDropper(extractor);
        
        if (frameDropper == null) {
            this.featureSource = extractor;
        } else {
            this.featureSource = frameDropper;
            addProcessor((DataProcessor) frameDropper);
        }
    }


    /**
     * Returns the appropriate Cepstral Mean Normalizer (CMN)
     * as specified in the SphinxProperties, with the given predecessor.
     *
     * @param predecessor the predecessor of the CMN step
     */
    private CepstrumSource getCMN(CepstrumSource predecessor) throws 
	IOException {
	String cmnClass = getCmnName();
	CepstrumSource cmn = null;
	if (cmnClass.equals("edu.cmu.sphinx.frontend.LiveCMN")) {
	    cmn = new LiveCMN("LiveCMN", getContext(), 
                              getSphinxProperties(), predecessor);
	} else if (cmnClass.equals("edu.cmu.sphinx.frontend.BatchCMN")) {
	    cmn = new BatchCMN("BatchCMN", getContext(), 
                               getSphinxProperties(), predecessor);
        }
	return cmn;
    }



    /**
     * Returns the appropriate FeatureExtractor.
     *
     * @param predecessor the CepstrumSource of Cepstrum objects 
     *    from which Features are extracted
     *
     * @return the FeatureExtractor
     */
    private FeatureExtractor getFeatureExtractor
        (CepstrumSource predecessor) 
	throws IOException {
        try {
	    FeatureExtractor extractor = (FeatureExtractor) 
		Class.forName(getFeatureExtractorName()).newInstance();
            extractor.initialize("FeatureExtractor", getContext(), 
                                 getSphinxProperties(), predecessor);
            return extractor;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Can't create FeatureExtractor: " + e);
        }
    }


    /**
     * Returns the FrameDropper, if applicable, or null if the
     * BaseFrontEnd is not configured to drop frames.
     *
     * @param predecessor the FeatureSource from which this
     *    FrameDropper reads Features
     *
     * @return a FrameDropper or null
     */
    private FrameDropper getFrameDropper(FeatureSource predecessor)
        throws IOException {
        int dropEveryNthFrame = getSphinxProperties().getInt
            (FrameDropper.PROP_DROP_EVERY_NTH_FRAME,
             FrameDropper.PROP_DROP_EVERY_NTH_FRAME_DEFAULT);
        if (dropEveryNthFrame > 1) {
            System.out.println("FrameDropper drops one out of every " + 
                               dropEveryNthFrame + " frame(s)");
            return (new FrameDropper("FrameDropper", getContext(),
                                     getSphinxProperties(), predecessor));
        } else {
            return null;
        }
    }


    /**
     * Returns the BaseFrontEnd with the given context, or null 
     * if there is no BaseFrontEnd with that context.
     *
     * @param context the context of the FrontEnd to get
     *
     * @return the BaseFrontEnd with the given context, or null if there
     *    is no BaseFrontEnd with that context
     *
     * @throws IOException if an I/O error occurs
     */
    public static FrontEnd getFrontEnd(String context) {
        Object frontend = frontends.get(context);
        if (frontend != null) {
            return (FrontEnd) frontend;
        } else {
            return null;
        }
    }


    /**
     * Sets the DataSource of this BaseFrontEnd.  The DataSource of
     * the front end is where it gets its audio data
     *
     * @param dataSource the DataSource
     */
    public void setDataSource(DataSource dataSource) {
        super.setDataSource(dataSource);
    }


    /**
     * Returns the next N number of Features produced by this BaseFrontEnd
     * as a single FeatureFrame.
     * The number of Features return maybe less than N, in which
     * case the last Feature will contain a Signal.UTTERANCE_END signal.
     * Consequently, the size of the FeatureFrame will be less than N.
     *
     * @param numberFeatures the number of Features to return
     * @param acousticModelName the name of the acoustic model for each
     *   the returned features are for
     *
     * @return the next N number of Features in a FeatureFrame, or null
     *    if no more FeatureFrames available
     *
     * @throws IOException if an I/O error occurs
     *
     * @see FeatureFrame
     */
    public FeatureFrame getFeatureFrame(int numberFeatures, 
					String acousticModelName) 
	throws IOException {

        getTimer().start();

        FeatureFrame featureFrame = null;
        Feature[] features = new Feature[numberFeatures];
        Feature feature = null;

        int i = 0;
        do {
            feature = featureSource.getFeature();
            if (feature != null) {
		if (getAcousticModelName() != null) {
		    feature.setType(getAcousticModelName());
		}
                features[i++] = feature;
		signalCheck(feature);
            } else {
                break;
            }
        } while (i < numberFeatures);

        // if there are some features
        if (i > 0) {

            // if we hit the end of utterance before getting the
            // desired number of features, shrink the array
            if (i < numberFeatures) {
                Feature[] lessFeatures = new Feature[i];
                for (i = 0; i < lessFeatures.length; i++) {
                    lessFeatures[i] = features[i];
                }
                features = lessFeatures;
            }
            
            featureFrame = new FeatureFrame(features);
        }
        
        getTimer().stop();

        return featureFrame;
    }


    /**
     * Drains all the data in this FrontEnd.
     */
    public void drain() {
        Feature feature = null;
        do {
            try {
                feature = featureSource.getFeature();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } while (feature != null);
    }


    /**
     * Returns a description of this BaseFrontEnd.
     *
     * @return a description of this BaseFrontEnd
     */
    public String toString() {
	// getSphinxProperties().list(System.out);
	String description = ("FrontEnd: " + getName() + "\n");
	description += ("------------------\n");
	description += super.toString();
	description += ("CMN              = " + getCmnName() + "\n");
	description += ("FeatureExtractor = " + getFeatureExtractorName() +
                        "\n");
	return description;
    }


    /**
     * Returns the name of the CMN class.
     *
     * @return the name of the CMN class
     */
    public String getCmnName() {
        return getSphinxProperties().getString(FrontEnd.PROP_CMN, 
                                               FrontEnd.PROP_CMN_DEFAULT);
    }


    /**
     * Returns the name of the FeatureExtractor class.
     *
     * @return the name of the FeatureExtractor class
     */
    public String getFeatureExtractorName() {
        return getSphinxProperties().getString
	    (PROP_FEATURE_EXTRACTOR, PROP_FEATURE_EXTRACTOR_DEFAULT);
    }
}




