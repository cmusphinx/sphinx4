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

import edu.cmu.sphinx.frontend.endpoint.Endpointer;
import edu.cmu.sphinx.frontend.endpoint.NonSpeechFilter;

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
 * Pre-processes the audio into Cepstra. The CepstrumExtractor consists
 * of a series of processors. The CepstrumExtractor connects all the processors
 * by the input and output points.
 * 
 * <p>The input to the CepstrumExtractor is a DataSource object.
 * For an audio stream, the <code>StreamAudioSource</code> should be used. The 
 * input can also be a file containing a list of audio files, in which case
 * the <code>BatchFileAudioSource</code> can be used.
 *
 * <p>Currently, this CepstrumExtractor can also take Cepstrum objects as
 * input using the StreamCepstrumSource implementation of DataSource.
 *
 * <p>For each utterance, this CepstrumExtractor expects either Audio or
 * Cepstrum frames (or objects) in the following sequence: <code>
 * Signal.UTTERANCE_START
 * Audio or Cepstrum
 * Audio or Cepstrum
 * ...
 * Signal.UTTERANCE_END
 *
 * <p>Any other sequence will cause this CepstrumExtractor to throw errors.
 *
 * <p>If the front end gets a null frame OUTSIDE an utterance, 
 * it just returns a null frame. If the front end gets a null frame
 * INSIDE an utterance, it throws an Error. Therefore, there cannot be
 * a null frame (or object) in between the Signal.UTTERANCE_START and
 * Signal.UTTERANCE_END. The implementation of DataSource given to
 * the CepstrumExtractor in the constructor must make sure that these
 * conditions hold.
 *
 * <p>The output of the CepstrumExtractor are Cepstra.
 *
 * <p>This CepstrumExtractor contains all the "standard" frontend
 * processors like Preemphasizer, HammingWindower, SpectrumAnalyzer, ..., etc.
 *
 * @see AudioSource
 * @see BatchCMN
 * @see LiveCMN
 * @see CepstrumProducer
 * @see Filterbank
 * @see SpectrumAnalyzer
 * @see Windower
 */
public class CepstrumExtractor extends DataProcessor 
implements CepstrumSource {

    private String amName;                     // Acoustic model name
    private Map processors;                    // all frontend modules
    private DataSource stubDataSource;         // the source of data to decode
    private CepstrumSource lastCepstrumSource; // the last processor


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
     * Initializes a CepstrumExtractor with the given name, context, acoustic 
     * model name, and DataSource.
     *
     * @param name the name of this CepstrumExtractor
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

        // add other data types here if necessary

        if (dataSource instanceof AudioSource) {
            stubDataSource = new StubAudioSource((AudioSource)dataSource);
            initialize((AudioSource)stubDataSource);
        } else if (dataSource instanceof CepstrumSource) {
            stubDataSource = 
                new StubCepstrumSource((CepstrumSource)dataSource);
            lastCepstrumSource = (CepstrumSource)stubDataSource;
        } else if (dataSource == null) {
            stubDataSource = new StubAudioSource();
            initialize((AudioSource)stubDataSource);            
        } else {
            throw new IllegalArgumentException
                ("Unsupported Data type: " + dataSource.getClass().getName());
        }
    }

    
    /**
     * Initializes this CepstrumExtractor with the given AudioSource.
     *
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

        lastCepstrumSource = cepstrumProducer;

        addProcessor(preemphasizer);
        addProcessor(windower);
        addProcessor(spectrumAnalyzer);
        addProcessor((DataProcessor) filterbank);
        addProcessor((DataProcessor) cepstrumProducer);

        initialize(lastCepstrumSource);
    }


    /**
     * Initializes this CepstrumExtractor with the given CepstrumSource.
     *
     * @param cepstrumSource the source of Cepstrum objects
     */
    private void initialize(CepstrumSource cepstrumSource) throws IOException {

	Endpointer endpointer = getEndpointer(cepstrumSource);
        	
	if (endpointer != null) { // if we're using an endpointer
            addProcessor((DataProcessor) endpointer);
	    lastCepstrumSource = endpointer;

            // if we are filtering out the non-speech regions,
            // initialize a non-speech filter
            boolean filterNonSpeech = getSphinxProperties().getBoolean
                (FrontEnd.PROP_FILTER_NON_SPEECH, 
                 FrontEnd.PROP_FILTER_NON_SPEECH_DEFAULT);
            if (filterNonSpeech) {
                CepstrumSource nonSpeechFilter = 
                    getNonSpeechFilter(lastCepstrumSource);
                addProcessor((DataProcessor) nonSpeechFilter);
                lastCepstrumSource = nonSpeechFilter;
            }
	}
    }


    /**
     * Returns the next Cepstrum object produced by this CepstrumSource.
     * The Cepstra objects of an Utterance should be preceded by
     * a Cepstrum object with Signal.UTTERANCE_START and ended by
     * a Cepstrum object with Signal.UTTERANCE_END.
     *
     * @return the next available Cepstrum object, returns null if no
     *     Cepstrum object is available
     *
     * @throws java.io.IOException
     */
    public Cepstrum getCepstrum() throws IOException {
        return lastCepstrumSource.getCepstrum();
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
    private Endpointer getEndpointer(CepstrumSource predecessor) 
	throws IOException {
        try {
            Endpointer endpointer = null;
            String endPointerClass = getEndpointerName();
            
            if (endPointerClass != null) {
                endpointer = (Endpointer)
                    Class.forName(getEndpointerName()).newInstance();
                
                endpointer.initialize("Endpointer", getContext(), 
                                      getSphinxProperties(), predecessor);
            }
            return endpointer;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Can't create Endpointer: " + e);
        }
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
     * Adds the given DataProcessor into the list of processors,
     * with the name of the processor as key.
     *
     * @param processor the DataProcessor to add
     *
     * @return the added DataProcessor
     */
    protected DataProcessor addProcessor(DataProcessor processor) {
        processors.put(processor.getName(), processor);
        return processor;
    }


    /**
     * Returns the DataProcessor in the CepstrumExtractor with the given name.
     *
     * @return the DataProcessor with the given name, or null if no
     *    such DataProcessor
     */
    public DataProcessor getProcessor(String processorName) {
        Object object = processors.get(processorName);
        if (object == null) {
            System.out.println
                ("WARNING: CepstrumExtractor does not have " + processorName);
            return null;
        } else {
            return (DataProcessor) object;
        }
    }


    /**
     * Sets the DataSource of this CepstrumExtractor.  The DataSource of
     * the front end is where it gets its audio data
     *
     * @param dataSource the DataSource
     */
    public void setDataSource(DataSource dataSource) {
        if (dataSource instanceof AudioSource) {
            if (stubDataSource instanceof StubAudioSource) {
                ((StubAudioSource)stubDataSource).setAudioSource
                    ((AudioSource)dataSource);
            } else {
                throw new IllegalArgumentException
                    ("Error: CepstrumExtractor.setDataSource() should not " +
                     "be called with an AudioSource,");
            }
        } else if (dataSource instanceof CepstrumSource) {
            if (stubDataSource instanceof StubCepstrumSource) {
                ((StubCepstrumSource)stubDataSource).setCepstrumSource
                    ((CepstrumSource)dataSource);
            } else {
                throw new IllegalArgumentException
                    ("Error: CepstrumExtractor.setDataSource() should not " +
                     "be called with an CepstrumSource,");
            }
        } else {
            throw new IllegalArgumentException
                ("Unsupported Data type: " + dataSource.getClass().getName());
        }
    }


    /**
     * Returns a description of this CepstrumExtractor.
     *
     * @return a description of this CepstrumExtractor
     */
    public String toString() {
	// getSphinxProperties().list(System.out);
        String description = ("Context          = " + getContext() + "\n");
	description += ("AM               = " + amName + "\n");
	description += ("DataSource       = " +
                        stubDataSource.toString() + "\n");
	description += ("Preemphasizer    = Preemphasizer\n");
	description += ("Windower         = Windower\n");
	description += ("FFT              = SpectrumAnalyzer\n");
	description += ("Filterbank       = " + getFilterBankName() + "\n");
	description += ("CepstrumProducer = " + getCepstrumProducerName() + 
                        "\n");
	description += ("Endpointer       = " + getEndpointerName() + "\n");
	return description;
    }


    /**
     * Returns the name of the FilterBank class.
     *
     * @return the name of the FilterBank class
     */
    public String getFilterBankName() {
        return getSphinxProperties().getString
            (FrontEnd.PROP_FILTERBANK, FrontEnd.PROP_FILTERBANK_DEFAULT);
    }


    /**
     * Returns the name of the CepstrumProducer class.
     *
     * @return the name of the CepstrumProducer class
     */
    public String getCepstrumProducerName() {
        return getSphinxProperties().getString
            (FrontEnd.PROP_CEPSTRUM_PRODUCER, 
             FrontEnd.PROP_CEPSTRUM_PRODUCER_DEFAULT);
    }


    /**
     * Returns the name of the Endpointer class.
     *
     * @return the name of the Endpointer class
     */
    public String getEndpointerName() {
        return getSphinxProperties().getString(FrontEnd.PROP_ENDPOINTER, null);
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

