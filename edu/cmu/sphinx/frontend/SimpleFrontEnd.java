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

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.InputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

/**
 * Pre-processes the audio into Features. The SimpleFrontEnd consists
 * of a series of processors. The SimpleFrontEnd connects all the processors
 * by the input and output points.
 * 
 * <p>The input to the SimpleFrontEnd is an audio data. For an audio stream,
 * the <code>StreamAudioSource</code> should be used. The 
 * input can also be a file containing a list of audio files, in which case
 * the <code>BatchFileAudioSource</code> can be used.
 *
 * <p>The output of the SimpleFrontEnd are Features.
 * To obtain <code>Feature</code>(s) from the SimpleFrontEnd, you would use:
 * <pre>
 * fe.getFeatureFrame(10);
 * </pre>
 * which will return a <code>FeatureFrame</code> with 10 <code>Feature</code>s
 * in it. If there are only 5 more features before the end of utterance,
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

    // Error message displayed when we don't get an UTTERANCE_START initially
    private static final String NO_UTTERANCE_START_ERROR =
	"I must receive a Feature with the UTTERANCE_START Signal before " +
	"I receive any other Features.";

    // Error message displayed when we get too many UTTERANCE_START Signals
    private static final String TOO_MANY_UTTERANCE_START_ERROR =
	"I cannot receive a Feature with the UTTERANCE_START Signal in " +
	"the middle of an utterance, i.e., before I receive an UTTERANCE_END.";


    private static Map frontends = new HashMap();

    private String amName;                // Acoustic model name
    private Map processors;               // all frontend modules
    private DataSource dataSource;        // source of data to decode
    private FeatureSource featureSource;  // the end of the pipeline
    private boolean useAcousticModelProperties;

    // configuration information
    private String filterBankClass;
    private String cepstrumProducerClass;
    private String endPointerClass;
    private String cmnClass;
    private String featureExtractorClass;
    
    // state of this frontend
    private boolean inUtterance;


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
	this.inUtterance = false;
        processors = new HashMap();
        frontends.put(context, this);
        setDataSource(dataSource);
	useAcousticModelProperties = getSphinxProperties().getBoolean
	    (PROP_PREFIX + "useAcousticModelProperties", true);

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
     * Returns the appropriate SphinxProperties to use based on the
     * "useAcousticModelProperties" property.
     *
     * @return the appropriate SphinxProperties
     *
     * @throw java.io.IOException if an I/O error occurred
     */
    private SphinxProperties getCorrectProperties() throws IOException {
	SphinxProperties props = null;
	if (useAcousticModelProperties) {
	    props = getAcousticProperties();
	}
	if (props == null) {
	    props = getSphinxProperties();
	}
	return props;
    }


    /**
     * Returns the properties of the relevant acoustic model.
     *
     * @return the properties of the relevant acoustic model
     *
     * @throw java.io.IOException if an I/O error occurred
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

	SphinxProperties props = getCorrectProperties();

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
	    filterBankClass = getSphinxProperties().getString
		(PROP_FILTERBANK, "edu.cmu.sphinx.frontend.mfc.MelFilterbank");
	    Filterbank bank = (Filterbank) 
		Class.forName(filterBankClass).newInstance();
	    bank.initialize("Filterbank", getContext(), getCorrectProperties(),
			    predecessor);
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
	    cepstrumProducerClass = getSphinxProperties().getString
		(PROP_CEPSTRUM_PRODUCER,
		 "edu.cmu.sphinx.frontend.mfc.MelCepstrumProducer");
	    CepstrumProducer producer = (CepstrumProducer)
		Class.forName(cepstrumProducerClass).newInstance();
	    producer.initialize("CepstrumProducer", getContext(), 
				getCorrectProperties(), predecessor);
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
    private CepstrumSource getEndpointer(CepstrumSource predecessor) {
	endPointerClass = getSphinxProperties().getString
	    (PROP_ENDPOINTER, null);

        if (endPointerClass != null) {
	    CepstrumSource endpointer = null;

	    /*
            EnergyEndpointer energyEndpointer = new EnergyEndpointer
                ("EnergyEndpointer", context, melCepstrum);
            NonSpeechFilter nonSpeechFilter = new NonSpeechFilter
                ("NonSpeechFilter", context, energyEndpointer);
	    */

	    return endpointer;
        } else {
	    return null;
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
	cmnClass = getSphinxProperties().getString
	    (PROP_CMN, "edu.cmu.sphinx.frontend.BatchCMN");

	CepstrumSource cmn = null;

	if (cmnClass.equals("edu.cmu.sphinx.frontend.LiveCMN")) {
	    cmn = new LiveCMN("LiveCMN", getContext(), 
			      getCorrectProperties(), predecessor);
	} else if (cmnClass.equals("edu.cmu.sphinx.frontend.BatchCMN")) {
	    cmn = new BatchCMN("BatchCMN", getContext(), 
			       getCorrectProperties(), predecessor);
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
        featureExtractorClass = getSphinxProperties().getString
            (PROP_FEATURE_EXTRACTOR, 
	     "edu.cmu.sphinx.frontend.DeltasFeatureExtractor");
        try {
	    FeatureExtractor extractor = (FeatureExtractor) 
		Class.forName(featureExtractorClass).newInstance();
            extractor.initialize("FeatureExtractor", getContext(),
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
	String description = ("FrontEnd: " + getName() + "\n");
	description += ("------------------\n");
	description += ("Context          = " + getContext() + "\n");
	description += ("AM               = " + amName + "\n");
	description += ("useAMProperties  = " +
			useAcousticModelProperties+"\n");
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
