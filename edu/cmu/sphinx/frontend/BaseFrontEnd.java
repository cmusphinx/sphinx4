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
 * Pre-processes the audio into Features. The BaseFrontEnd consists
 * of a series of processors. The BaseFrontEnd connects all the processors
 * by the input and output points.
 * 
 * <p>The input to the BaseFrontEnd is a DataSource object.
 * For an audio stream,
 * the <code>StreamAudioSource</code> should be used. The 
 * input can also be a file containing a list of audio files, in which case
 * the <code>BatchFileAudioSource</code> can be used.
 * Currently, this BaseFrontEnd can also take Cepstrum objects as
 * input using the StreamCepstrumSource implementation of DataSource.
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
public class BaseFrontEnd extends DataProcessor implements FrontEnd {

    private static Map frontends = new HashMap();

    private String amName;                // Acoustic model name
    private Map processors;               // all frontend modules
    private DataProcessor firstProcessor; // the first front-end processor
    private DataSource dataSource;        // the source of data to decode
    private FeatureSource featureSource;  // the end of the pipeline


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
     * Initializes a BaseFrontEnd with the given name, context, acoustic 
     * model name, and DataSource.
     *
     * @param name the name of this BaseFrontEnd
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
        processors = new HashMap();
        frontends.put(context, this);

        // add other data types here if necessary

        if (dataSource instanceof AudioSource) {
            initialize((AudioSource) dataSource);
        } else if (dataSource instanceof CepstrumSource) {
            initialize((CepstrumSource) dataSource);
        } else {
            throw new Error("Unsupported Data type: " +
                            dataSource.getClass().getName());
        }
    }

    
    /**
     * Initializes this BaseFrontEnd with the given AudioSource.
     *
     * @param amName the name of the acoustic model
     * @param audioSource the source of Audio objects
     */
    private void initialize(AudioSource audioSource) throws IOException {

        // initialize all the frontend processors

	SphinxProperties props = getSphinxProperties();

        Preemphasizer preemphasizer = new Preemphasizer
            ("Preemphasizer", getContext(), props, audioSource);

        Windower windower = new Windower
            ("HammingWindow", getContext(), props, preemphasizer);

        SpectrumAnalyzer spectrumAnalyzer = new SpectrumAnalyzer
            ("FFT", getContext(), props, windower);

	Filterbank filterbank = getFilterbank(spectrumAnalyzer);

        CepstrumSource cepstrumProducer = getCepstrumProducer(filterbank);

        addProcessor(preemphasizer);
        addProcessor(windower);
        addProcessor(spectrumAnalyzer);
        addProcessor((DataProcessor) filterbank);
        addProcessor((DataProcessor) cepstrumProducer);
	initialize(cepstrumProducer);
    }


    /**
     * Initializes this BaseFrontEnd with the given CepstrumSource.
     *
     * @param cepstrumProducer the place where Cepstra comes from
     */
    private void initialize(CepstrumSource cepstrumProducer) 
	throws IOException {

	CepstrumSource lastCepstrumSource = cepstrumProducer;
	
	CepstrumSource endpointer = getEndpointer(lastCepstrumSource);

	if (endpointer != null) { // if we're using an endpointer
            addProcessor((DataProcessor) endpointer);
	    lastCepstrumSource = endpointer;

            // if we are filtering out the non-speech regions,
            // initialize a non-speech filter
            boolean filterNonSpeech = getSphinxProperties().getBoolean
                (PROP_FILTER_NON_SPEECH, PROP_FILTER_NON_SPEECH_DEFAULT);
            if (filterNonSpeech) {
                CepstrumSource nonSpeechFilter = 
                    getNonSpeechFilter(lastCepstrumSource);
                addProcessor((DataProcessor) nonSpeechFilter);
                lastCepstrumSource = nonSpeechFilter;
            }
	}

        CepstrumSource cmn = getCMN(lastCepstrumSource);
	if (cmn != null) { // if we're using a CMN
	    addProcessor((DataProcessor) cmn);
	    lastCepstrumSource = cmn;
	}

        FeatureExtractor extractor = getFeatureExtractor(lastCepstrumSource);
        setFeatureSource(extractor);
	addProcessor((DataProcessor) extractor);
    }


    /**
     * Returns the appropriate Filterbank and initializes it with
     * the given predecessor.
     *
     * @param predecessor the predecessor of this Filterbank
     *
     * @return the appropriate Filterbank
     */
    private Filterbank getFilterbank(SpectrumSource predecessor) {
	try {
	    Filterbank bank = (Filterbank) 
		Class.forName(getFilterBankName()).newInstance();
	    bank.initialize("Filterbank", getContext(), 
                            getSphinxProperties(), predecessor);
	    return bank;
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new Error("Can't create Filterbank: " + e);
	}
    }


    /**
     * Returns the appropriate CepstrumProducer, and initializes it
     * with the given predecessor.
     *
     * @param predecessor the predecessor
     *
     * @return the appropriate CepstrumProducer
     */
    private CepstrumProducer getCepstrumProducer(SpectrumSource predecessor) {
	try {
	    CepstrumProducer producer = (CepstrumProducer)
		Class.forName(getCepstrumProducerName()).newInstance();
	    producer.initialize("CepstrumProducer", getContext(), 
				getSphinxProperties(), predecessor);
	    return producer;
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new Error("Can't create CepstrumProducer: " + e);
	}
    }


    /**
     * Returns the appropriate Endpointer, if any, and initializes it
     * with the given predecessor.
     *
     * @param predecessor the predecessor of this Endpointer
     */
    private CepstrumSource getEndpointer(CepstrumSource predecessor) 
	throws IOException {
        
        CepstrumSource endpointer = null;
	String endPointerClass = getEndpointerName();
        
        if (endPointerClass != null) {
            if (endPointerClass.equals
                ("edu.cmu.sphinx.frontend.EnergyEndpointer")) {
                endpointer = new EnergyEndpointer
                    ("EnergyEndpointer", getContext(), 
                     getSphinxProperties(), predecessor);
            }
        }

        return endpointer;
    }


    /**
     * Returns a filter that filters out regions of an Utterance that
     * are marked as non-speech.
     *
     * @param predecessor the predecessor of this filter
     *
     * @return a NonSpeechFilter
     */
    private CepstrumSource getNonSpeechFilter(CepstrumSource predecessor)
        throws IOException {
        return (new NonSpeechFilter
                ("NonSpeechFilter", getContext(), getSphinxProperties(), 
                 predecessor));
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
     * Adds the given DataProcessor into the list of processors,
     * with the name of the processor as key.
     *
     * @param processor the DataProcessor to add
     *
     * @return the added DataProcessor
     */
    private DataProcessor addProcessor(DataProcessor processor) {
        processors.put(processor.getName(), processor);
        return processor;
    }


    /**
     * Returns the DataProcessor in the BaseFrontEnd with the given name.
     *
     * @return the DataProcessor with the given name, or null if no
     *    such DataProcessor
     */
    public DataProcessor getProcessor(String processorName) {
        Object object = processors.get(processorName);
        if (object == null) {
            System.out.println
                ("WARNING: BaseFrontEnd does not have " + processorName);
            return null;
        } else {
            return (DataProcessor) object;
        }
    }


    /**
     * Returns the BaseFrontEnd with the given context, or null 
     * if there is no BaseFrontEnd with that context.
     *
     * @return the BaseFrontEnd with the given context, or null if there
     *    is no BaseFrontEnd with that context
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
     * @param audioSource the DataSource
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    /**
     * Returns the FeatureSource of this BaseFrontEnd.
     *
     * @return the FeatureSource of this BaseFrontEnd
     */
    private FeatureSource getFeatureSource() {
        return featureSource;
    }


    /**
     * Sets the FeatureSource of this BaseFrontEnd.
     *
     * @param featureSource the FeatureSource
     */
    private void setFeatureSource(FeatureSource featureSource) {
        this.featureSource = featureSource;
    }


    /**
     * Returns the next N number of Features produced by this BaseFrontEnd
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
		if (amName != null) {
		    feature.setType(amName);
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
	description += ("Context          = " + getContext() + "\n");
	description += ("AM               = " + amName + "\n");
	description += ("DataSource       = " +
			dataSource.getClass().getName() + "\n");
	description += ("Preemphasizer    = Preemphasizer\n");
	description += ("Windower         = Windower\n");
	description += ("FFT              = SpectrumAnalyzer\n");
	description += ("Filterbank       = " + getFilterBankName() + "\n");
	description += ("CepstrumProducer = " + getCepstrumProducerName() + 
                        "\n");
	description += ("Endpointer       = " + getEndpointerName() + "\n");
	description += ("CMN              = " + getCmnName() + "\n");
	description += ("FeatureExtractor = " + getFeatureExtractorName() +
                        "\n");
	return description;
    }


    /**
     * Returns the name of the FilterBank class.
     *
     * @return the name of the FilterBank class
     */
    public String getFilterBankName() {
        return getSphinxProperties().getString
            (PROP_FILTERBANK, PROP_FILTERBANK_DEFAULT);
    }


    /**
     * Returns the name of the CepstrumProducer class.
     *
     * @return the name of the CepstrumProducer class
     */
    public String getCepstrumProducerName() {
        return getSphinxProperties().getString
            (PROP_CEPSTRUM_PRODUCER, PROP_CEPSTRUM_PRODUCER_DEFAULT);
    }


    /**
     * Returns the name of the Endpointer class.
     *
     * @return the name of the Endpointer class
     */
    public String getEndpointerName() {
        return getSphinxProperties().getString(PROP_ENDPOINTER, null);
    }


    /**
     * Returns the name of the CMN class.
     *
     * @return the name of the CMN class
     */
    public String getCmnName() {
        return getSphinxProperties().getString(PROP_CMN, PROP_CMN_DEFAULT);
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
