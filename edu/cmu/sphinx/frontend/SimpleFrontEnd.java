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

import edu.cmu.sphinx.frontend.util.PropertiesResolver;

import edu.cmu.sphinx.model.acoustic.AcousticModel;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.InputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pre-processes the audio into Features. The SimpleFrontEnd consists
 * of a series of processors. The SimpleFrontEnd connects all the processors
 * by the input and output points.
 * 
 * <p>The input to the SimpleFrontEnd is a DataSource object.
 * For an audio stream,
 * the <code>StreamAudioSource</code> should be used. The 
 * input can also be a file containing a list of audio files, in which case
 * the <code>BatchFileAudioSource</code> can be used.
 * Currently, this SimpleFrontEnd can also take Cepstrum objects as
 * input using the StreamCepstrumSource implementation of DataSource.
 *
 * <p>The output of the SimpleFrontEnd are FeatureFrames, which is
 * an array of Features. This SimpleFrontEnd enforces the condition that
 * every utterance starts with a Feature that contains the signal 
 * <code>Signal.UTTERANCE_START</code>, and ends with a Feature that 
 * contains the signal <code>Signal.UTTERANCE_END</code>.
 *
 * <p>To obtain a <code>FeatureFrame</code> with 10 Features from the 
 * SimpleFrontEnd, you do:
 * <pre>
 * SimpleFrontEnd.getFeatureFrame(10);
 * </pre>
 * If there are only 5 Features left, it will return a FeatureFrame with
 * 5 Features, followed by a Feature with a <code>Signal.UTTERANCE_END</code>
 * Signal.
 *
 * This SimpleFrontEnd contains all the "standard" frontend processors like
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
public class SimpleFrontEnd extends DataProcessor implements FrontEnd {

    /**
     * The logger for this class
     */
    private static Logger logger =
            Logger.getLogger("edu.cmu.sphinx.frontend.SimpleFrontEnd");

    private static Map frontends = new HashMap();

    private String amName;                // Acoustic model name
    private Map processors;               // all frontend modules
    private DataSource dataSource;        // source of data to decode
    private FeatureSource featureSource;  // the end of the pipeline

    // configuration information
    private String filterBankClass;
    private String cepstrumProducerClass;
    private String endPointerClass;
    private String cmnClass;
    private String featureExtractorClass;

    private boolean useAcousticModelProperties;
    

    /**
     * Constructs a default SimpleFrontEnd.
     *
     * @param name the name of this SimpleFrontEnd
     * @param context the context of this SimpleFrontEnd
     * @param dataSource the place to pull data from
     */
    public SimpleFrontEnd(String name, String context, DataSource dataSource)
        throws IOException {
	this(name, context, null, dataSource);
    }


    /**
     * Constructs a SimpleFrontEnd with the given name, context, acoustic 
     * model name, and DataSource.
     *
     * @param name the name of this SimpleFrontEnd
     * @param context the context of interest
     * @param amName the name of the acoustic model
     * @param dataSource the source of data
     */
    public SimpleFrontEnd(String name, String context, String amName,
			  DataSource dataSource) throws IOException {
	super(name, context);
	this.amName = amName;
        processors = new HashMap();
        frontends.put(context, this);
        setDataSource(dataSource);

	SphinxProperties props = SphinxProperties.getSphinxProperties(context);
	useAcousticModelProperties = props.getBoolean
	    (FrontEnd.PROP_USE_ACOUSTIC_MODEL_PROPERTIES, true);
	
	setSphinxProperties(getCorrectProperties());

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
     * Returns the appropriate SphinxProperties by combining the
     * acoustic model properties and the default properties.
     *
     * @return the appropriate SphinxProperties
     *
     * @throws java.io.IOException if an I/O error occurred
     */
    private SphinxProperties getCorrectProperties() throws IOException {
	SphinxProperties sphinxProperties = 
	    SphinxProperties.getSphinxProperties(getContext());
	if (useAcousticModelProperties) {
	    return (PropertiesResolver.resolve
		    (sphinxProperties, getAcousticProperties(), 
                     getContext() + "." + getName()));
	} else {
	    return sphinxProperties;
	}
    }


    /**
     * Returns the properties of the relevant acoustic model.
     *
     * @return the properties of the relevant acoustic model
     *
     * @throws java.io.IOException if an I/O error occurred
     */
    public SphinxProperties getAcousticProperties() throws IOException {
	AcousticModel am;
	if (amName != null) {
	    am = AcousticModel.getAcousticModel(amName, getContext());
	} else {
	    am = AcousticModel.getAcousticModel(getContext());
	}
	if (am != null) {
	    return am.getProperties();
	} else {
	    return null;
	}
    }

    
    /**
     * Initializes this SimpleFrontEnd with the given AudioSource.
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
     * Initializes this SimpleFrontEnd with the given CepstrumSource.
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
                (PROP_FILTER_NON_SPEECH, false);
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
	    SphinxProperties props = getSphinxProperties();
	    filterBankClass = props.getString
		(PROP_FILTERBANK, "edu.cmu.sphinx.frontend.mfc.MelFilterbank");
	    Filterbank bank = (Filterbank) 
		Class.forName(filterBankClass).newInstance();
	    bank.initialize("Filterbank", getContext(), props, predecessor);
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
	    SphinxProperties props = getSphinxProperties();
	    cepstrumProducerClass = props.getString
		(PROP_CEPSTRUM_PRODUCER,
		 "edu.cmu.sphinx.frontend.mfc.MelCepstrumProducer");
	    CepstrumProducer producer = (CepstrumProducer)
		Class.forName(cepstrumProducerClass).newInstance();
	    producer.initialize("CepstrumProducer", getContext(), 
				props, predecessor);
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
        SphinxProperties props = getSphinxProperties();
	endPointerClass = props.getString(PROP_ENDPOINTER, null);
        
        if (endPointerClass != null) {
            if (endPointerClass.equals
                ("edu.cmu.sphinx.frontend.EnergyEndpointer")) {
                endpointer = new EnergyEndpointer
                    ("EnergyEndpointer", getContext(), props, predecessor);
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
	SphinxProperties props = getSphinxProperties();
	cmnClass = props.getString
	    (PROP_CMN, "edu.cmu.sphinx.frontend.BatchCMN");

	CepstrumSource cmn = null;
	if (cmnClass.equals("edu.cmu.sphinx.frontend.LiveCMN")) {
	    cmn = new LiveCMN("LiveCMN", getContext(), props, predecessor);
	    logger.info("Live CMN enabled");
	} else if (cmnClass.equals("edu.cmu.sphinx.frontend.BatchCMN")) {
	    cmn = new BatchCMN("BatchCMN", getContext(), props, predecessor);
	    logger.info("Batch CMN enabled");
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
	SphinxProperties props = getSphinxProperties();
        featureExtractorClass = props.getString
	    (PROP_FEATURE_EXTRACTOR, 
	     "edu.cmu.sphinx.frontend.DeltasFeatureExtractor");
        try {
	    FeatureExtractor extractor = (FeatureExtractor) 
		Class.forName(featureExtractorClass).newInstance();
            extractor.initialize("FeatureExtractor", getContext(), props,
				 predecessor);
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
     * Returns the DataProcessor in the SimpleFrontEnd with the given name.
     *
     * @return the DataProcessor with the given name, or null if no
     *    such DataProcessor
     */
    public DataProcessor getProcessor(String processorName) {
        Object object = processors.get(processorName);
        if (object == null) {
            System.out.println
                ("WARNING: SimpleFrontEnd does not have " + processorName);
            return null;
        } else {
            return (DataProcessor) object;
        }
    }


    /**
     * Returns the SimpleFrontEnd with the given context, or null 
     * if there is no SimpleFrontEnd with that context.
     *
     * @return the SimpleFrontEnd with the given context, or null if there
     *    is no SimpleFrontEnd with that context
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
     * Returns the DataSource of this SimpleFrontEnd. The DataSource of
     * the front end is where it gets its data data.
     *
     * @return the DataSource
     */
    private DataSource getDataSource() {
        return dataSource;
    }


    /**
     * Sets the DataSource of this SimpleFrontEnd.  The DataSource of
     * the front end is where it gets its audio data
     *
     * @param audioSource the DataSource
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    /**
     * Returns the FeatureSource of this SimpleFrontEnd.
     *
     * @return the FeatureSource of this SimpleFrontEnd
     */
    private FeatureSource getFeatureSource() {
        return featureSource;
    }


    /**
     * Sets the FeatureSource of this SimpleFrontEnd.
     *
     * @param featureSource the FeatureSource
     */
    private void setFeatureSource(FeatureSource featureSource) {
        this.featureSource = featureSource;
    }


    /**
     * Returns the next N number of Features produced by this SimpleFrontEnd
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
     * Returns a description of this SimpleFrontEnd.
     *
     * @return a description of this SimpleFrontEnd
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
	description += ("Filterbank       = " + filterBankClass + "\n");
	description += ("CepstrumProducer = " + cepstrumProducerClass + "\n");
	description += ("Endpointer       = " + endPointerClass + "\n");
	description += ("CMN              = " + cmnClass + "\n");
	description += ("FeatureExtractor = " + featureExtractorClass + "\n");
	return description;
    }

}
