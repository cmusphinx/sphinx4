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


package edu.cmu.sphinx.frontend.mfc;

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumProducer;
import edu.cmu.sphinx.frontend.CepstrumSource;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Spectrum;
import edu.cmu.sphinx.frontend.SpectrumSource;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;


/**
 * Applies a melcosine filter bank to the given Spectrum.
 * Outputs a Cepstrum.
 */
public class MelCepstrumProducer extends DataProcessor implements
CepstrumProducer {
    
    private int cepstrumSize;       // size of a Cepstrum
    private int numberMelFilters;   // number of mel-filters
    private float[][] melcosine;
    private SpectrumSource predecessor;


    /**
     * Constructs a default MelCepstrumProducer.
     */
    public MelCepstrumProducer() {
	super();
    }


    /**
     * Constructs a MelCepstrumProducer.
     *
     * @param name the name of this MelCepstrumProducer
     * @param context the context of the SphinxProperties to use
     * @param props the SphinxProperties to read properties from
     * @param predecessor the SpectrumSource to get Spectrum objects from
     *
     * @throws IOException if an I/O error occurs
     */
    public MelCepstrumProducer(String name, String context, 
			       SphinxProperties props,
			       SpectrumSource predecessor) throws IOException {
        initialize(name, context, props, predecessor);
    }


    /**
     * Initializes this MelCepstrumProducer.
     *
     * @param name the name of this MelCepstrumProducer
     * @param context the context of the SphinxProperties to use
     * @param props the SphinxProperties to read properties from
     * @param predecessor the SpectrumSource to get Spectrum objects from
     *
     * @throws IOException if an I/O error occurs
     */
    public void initialize(String name, String context, SphinxProperties props,
			   SpectrumSource predecessor) throws IOException {
        super.initialize(name, context);
	setProperties(props);
        this.predecessor = predecessor;
        computeMelCosine();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     *
     * @param props the SphinxProperties to read properties from
     */
    public void setProperties(SphinxProperties props) {
        numberMelFilters = props.getInt(MelFilterbank.PROP_NUMBER_FILTERS, 40);
        cepstrumSize = getSphinxProperties().getInt
	    (FrontEnd.PROP_CEPSTRUM_SIZE, 13);
    }


    /**
     * Compute the MelCosine filter bank.
     */
    private void computeMelCosine() {
        melcosine = new float[cepstrumSize][numberMelFilters];

        float period = (float) 2 * numberMelFilters;

        for (int i = 0; i < cepstrumSize; i++) {
            float frequency = 2 * ((float) Math.PI) * (float) i/period; 
            
            for (int j = 0; j < numberMelFilters; j++) {
                melcosine[i][j] = (float) Math.cos
                    ((double) (frequency * (j + 0.5)));
            }
        }
    }


    /**
     * Returns the next Cepstrum object, which is the mel cepstrum of the
     * input frame. However, it can also be other Cepstrum objects
     * like a EndPointSignal.
     *
     * @return the next available Cepstrum object, returns null if no
     *     Cepstrum object is available
     *
     * @throws java.io.IOException if there is an error reading
     * the Cepstrum objects
     */
    public Cepstrum getCepstrum() throws IOException {

	Spectrum input = predecessor.getSpectrum();
        Cepstrum output = null;

        getTimer().start();

        if (input != null) {
            if (input.hasContent()) {
                output = process(input);
            } else {
                output = new Cepstrum(input.getSignal());
            }
        }

        getTimer().stop();

	return output;
    }


    /**
     * Process data, creating the mel cepstrum from an input
     * audio frame.
     *
     * @param input a MelSpectrum frame
     *
     * @return a mel Cepstrum frame
     */
    private Cepstrum process(Spectrum input) throws
    IllegalArgumentException {

        double[] melspectrum = input.getSpectrumData();

        if (melspectrum.length != numberMelFilters) {
            throw new IllegalArgumentException
                ("MelSpectrum size is incorrect: melspectrum.length == " +
                 melspectrum.length + ", numberMelFilters == " +
                 numberMelFilters);
        }

        for (int i = 0; i < melspectrum.length; ++i) {
            if (melspectrum[i] > 0) {
                melspectrum[i] = Math.log(melspectrum[i]);
            } else {
		// in case melspectrum[i] isn't greater than 0
		// instead of trying to compute a log we just
		// assign a very small number
                melspectrum[i] = -1.0e+5;
            }
        }

        // create the cepstrum by apply the melcosine filter
        float[] cepstrumCepstrum = applyMelCosine(melspectrum);

	Cepstrum cepstrum = new Cepstrum
            (cepstrumCepstrum, input.getUtterance());

        if (getDump()) {
            System.out.println("MEL_CEPSTRUM   " + cepstrum.toString());
        }

        return cepstrum;
    }

    
    /**
     * Apply the MelCosine filter to the given melspectrum.
     *
     * @param melspectrum the MelSpectrum data
     *
     * @return MelCepstrum data produced by apply the MelCosine filter
     *    to the MelSpectrum data
     */
    private float[] applyMelCosine(double[] melspectrum) {

        // create the cepstrum
        float[] cepstrum = new float[cepstrumSize];
        float period = (float) numberMelFilters;
        float beta = 0.5f;
        
        // apply the melcosine filter
        for (int i = 0; i < cepstrum.length; i++) {

            if (numberMelFilters > 0) {
                float[] melcosine_i = melcosine[i];
                int j = 0;
                cepstrum[i] += (beta * melspectrum[j] * melcosine_i[j]);

                for (j = 1; j < numberMelFilters; j++) {
                    cepstrum[i] += (melspectrum[j] * melcosine_i[j]);
                }
                cepstrum[i] /= period;
            }
        }
        
        return cepstrum;
    }
}
