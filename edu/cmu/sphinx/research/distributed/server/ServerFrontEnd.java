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


package edu.cmu.sphinx.research.distributed.server;

import edu.cmu.sphinx.frontend.BatchCMN;
import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumSource;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DataSource;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.FeatureExtractor;
import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.frontend.FeatureSource;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.LiveCMN;

import edu.cmu.sphinx.frontend.util.StubAudioSource;
import edu.cmu.sphinx.frontend.util.StubCepstrumSource;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.InputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Reads cepstral data from the network, and converts them into
 * Feature objects by computing their delta and double delta.
 *
 * <p>For each utterance, this ServerFrontEnd expects either Audio or
 * Cepstrum frames (or objects) in the following sequence: <code>
 * Signal.UTTERANCE_START
 * Audio or Cepstrum
 * Audio or Cepstrum
 * ...
 * Signal.UTTERANCE_END
 *
 * <p>Any other sequence will cause this ServerFrontEnd to throw errors.
 *
 * <p>If the front end gets a null frame OUTSIDE an utterance, 
 * it just returns a null frame. If the front end gets a null frame
 * INSIDE an utterance, it throws an Error. Therefore, there cannot be
 * a null frame (or object) in between the Signal.UTTERANCE_START and
 * Signal.UTTERANCE_END. The implementation of DataSource given to
 * the ServerFrontEnd in the constructor must make sure that these
 * conditions hold.
 *
 * <p>The output of the ServerFrontEnd are FeatureFrames, which is
 * an array of Features. This ServerFrontEnd enforces the condition that
 * every utterance starts with a Feature that contains the signal 
 * <code>Signal.UTTERANCE_START</code>, and ends with a Feature that 
 * contains the signal <code>Signal.UTTERANCE_END</code>.
 *
 * <p>To obtain a <code>FeatureFrame</code> with 10 Features from the 
 * ServerFrontEnd, you do:
 * <pre>
 * ServerFrontEnd.getFeatureFrame(10);
 * </pre>
 * If there are only 5 Features left, it will return a FeatureFrame with
 * 5 Features, followed by a Feature with a <code>Signal.UTTERANCE_END</code>
 * Signal.
 *
 * This ServerFrontEnd contains all the "standard" frontend processors like
 * Preemphasizer, HammingWindower, SpectrumAnalyzer, ..., etc.
 *
 * @see Feature
 * @see FeatureExtractor
 * @see FeatureFrame
 * @see FeatureSource
 */
public class ServerFrontEnd extends DataProcessor implements FrontEnd {

    private StubCepstrumSource stubCepstrumSource;
    private FeatureSource featureSource;  // the end of the pipeline
    
    private String amName;                // name of acoustic model


    /**
     * Constructs a default SimpleFrontEnd.
     *
     * @param name the name of this SimpleFrontEnd
     * @param context the context of this SimpleFrontEnd
     * @param dataSource the place to pull data from
     */
    public void initialize(String name, String context, DataSource dataSource)
        throws IOException {
        initialize(name, context, 
                   SphinxProperties.getSphinxProperties(context),
                   null, dataSource);
    }


    /**
     * Initializes a ServerFrontEnd with the given name, context, acoustic 
     * model name, and DataSource.
     *
     * @param name the name of this ServerFrontEnd
     * @param context the context of interest
     * @param props the SphinxProperties to use
     * @param amName the name of the acoustic model
     * @param dataSource the source of data; can be null, in which
     *    case the setDataSource() method must be called later
     */
    public void initialize(String name, String context, 
                           SphinxProperties props,
                           String amName,
                           DataSource dataSource) throws IOException {
        super.initialize(name, context, props);
        this.amName = amName;
        if (dataSource != null) {
            stubCepstrumSource = new StubCepstrumSource
                ((CepstrumSource)dataSource);
        } else {
            stubCepstrumSource = new StubCepstrumSource();
        }


        CepstrumSource cmn = getCMN(stubCepstrumSource);
        FeatureExtractor extractor = getFeatureExtractor(cmn);
        this.featureSource = extractor;
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
    private FeatureExtractor getFeatureExtractor(CepstrumSource predecessor) 
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
     * Sets the CepstrumSource of this ServerFrontEnd. It should be
     * expecting a SocketCepstrumSource.
     *
     * @param audioSource the DataSource
     */
    public void setDataSource(DataSource dataSource) {
        assert (dataSource instanceof CepstrumSource);
        stubCepstrumSource.setCepstrumSource((CepstrumSource)dataSource);
    }


    /**
     * Returns the next N number of Features produced by this ServerFrontEnd
     * as a single FeatureFrame.
     * The number of Features return maybe less than N, in which
     * case the last Feature will contain a Signal.UTTERANCE_END signal.
     * Consequently, the size of the FeatureFrame will be less than N.
     *
     * @param numberFeatures the number of Features to return
     *
     * @return the next N number of Features in a FeatureFrame, or null
     *    if no more FeatureFrames available
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
     * Returns a description of this ServerFrontEnd.
     *
     * @return a description of this ServerFrontEnd
     */
    public String toString() {
	// getSphinxProperties().list(System.out);
	String description = ("FrontEnd: " + getName() + "\n");
	description += ("------------------\n");
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


    /**
     * Returns the name of the acoustic model use.
     *
     * @return the name of the acoustic model use, or null if no name
     */
    public String getAcousticModelName() {
        return amName;
    }
}




