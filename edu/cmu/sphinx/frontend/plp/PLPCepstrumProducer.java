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


package edu.cmu.sphinx.frontend.plp;

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumProducer;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.Spectrum;
import edu.cmu.sphinx.frontend.SpectrumSource;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;


/**
 * Computes a PLP cepstrum from a given PLP Spectrum.
 * The procedure has the following steps: <br>
 * 1. Compute the cube root of the PLP spectrum <br>
 * 2. Compute the IDCT of the cube-rooted PLP spectrum to get 
 *    autocorrelation values <br>
 * 3. Compute a linear prediction filter from the autocorrelation values <br>
 * 4. Compute the linear prediction cepstrum from the LP filter.<br>
 *
 * Created: Wed Dec 25 10:22:00 2002
 *
 * @author <a href="mailto:rsingh@cs.cmu.edu">rsingh</a>
 * @version 1.0
 */
public class PLPCepstrumProducer extends DataProcessor implements
CepstrumProducer {
    
    private int cepstrumSize;       // size of a Cepstrum
    private int LPCOrder;           // LPC Order to compute cepstrum
    private int numberPLPFilters;   // number of PLP filters
    private double[][] cosine;
    private SpectrumSource predecessor;


    /**
     * Constructs a default PLPCepstrumProducer.
     */
    public PLPCepstrumProducer() {
	super();
    }


    /**
     * Constructs a PLPCepstrumProducer with the given
     * SphinxProperties context.
     *
     * @param name the name of this PLCepstrumProducer
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
        computeCosine();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     *
     * @param props the SphinxProperties to read properties from
     */
    public void setProperties(SphinxProperties props) {
        numberPLPFilters = props.getInt(PLPFilterbank.PROP_NUMBER_FILTERS, 40);
        cepstrumSize = getSphinxProperties().getInt
	    (FrontEnd.PROP_CEPSTRUM_SIZE, 13);
	// Just hardcoding this for now
	LPCOrder = 14;
    }


    /**
     * Compute the Cosine values for IDCT.
     */
    private void computeCosine() {
        cosine = new double[LPCOrder+1][numberPLPFilters];

        double period = (double) 2 * numberPLPFilters;

        for (int i = 0; i <= LPCOrder; i++) {
            double frequency = 2 * ((double) Math.PI) * (double) i/period; 
            
            for (int j = 0; j < numberPLPFilters; j++) {
                cosine[i][j] = (double) Math.cos
                    ((double) (frequency * (j + 0.5)));
            }
        }
    }

    /**
     * Applies the intensity loudness power law. 
     * This operation is an approximation to the power law of hearing
     * and simulates the non-linear relationship between sound intensity
     * and percieved loudness. 
     * Computationally, this operation is used to reduce the spectral
     * amplitude of the critical band to enable all-pole modeling with
     * relatively low order AR filters
     */
    private double[] powerLawCompress(double[] inspectrum){
	double[] compressedspectrum = new double[inspectrum.length];

	for (int i = 0; i < inspectrum.length; i++){
	    compressedspectrum[i] = Math.pow(inspectrum[i], 0.33333333);
	}
	return compressedspectrum;
    }

    /**
     * Returns the next Cepstrum object, which is the PLP cepstrum of the
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
     * Process data, creating the PLP cepstrum from an input
     * audio frame.
     *
     * @param input a PLP Spectrum frame
     *
     * @return a PLP Cepstrum frame
     */
    private Cepstrum process(Spectrum input) throws
    IllegalArgumentException {

        double[] plpspectrum = input.getSpectrumData();

        if (plpspectrum.length != numberPLPFilters) {
            throw new IllegalArgumentException
                ("PLPSpectrum size is incorrect: plpspectrum.length == " +
                 plpspectrum.length + ", numberPLPFilters == " +
                 numberPLPFilters);
        }

	// power law compress spectrum
	double[] compressedspectrum = powerLawCompress(plpspectrum);

        // compute autocorrelation values
        double[] autocor = applyCosine(compressedspectrum);

	LinearPredictor LPC = new LinearPredictor(LPCOrder);
	// Compute LPC Parameters
	double[] LPCcoeffs = LPC.getARFilter(autocor);
	// Compute LPC Cepstra
	double[] cepstrumdouble = LPC.getCepstrum(cepstrumSize);
	float[] cepstrumCepstrum = new float[cepstrumSize];
	for (int i=0; i<cepstrumSize; i++)
	    cepstrumCepstrum[i] = (float)cepstrumdouble[i];

	Cepstrum cepstrum = new Cepstrum
            (cepstrumCepstrum, input.getUtterance());

        if (getDump()) {
            System.out.println("PLP_CEPSTRUM   " + cepstrum.toString());
        }

        return cepstrum;
    }

    
    /**
     * Compute the discrete Cosine transform for the given power spectrum
     *
     * @param plpspectrum the PLPSpectrum data
     *
     * @return autocorrelation computed from PLP spectral values
     */
    private double[] applyCosine(double[] plpspectrum) {

        double[] autocor = new double[LPCOrder+1];
        double period = (double) numberPLPFilters;
        double beta = 0.5f;
        
        // apply the idct
        for (int i = 0; i <= LPCOrder; i++) {

            if (numberPLPFilters > 0) {
                double[] cosine_i = cosine[i];
                int j = 0;
                autocor[i] += (beta * plpspectrum[j] * cosine_i[j]);

                for (j = 1; j < numberPLPFilters; j++) {
                    autocor[i] += (plpspectrum[j] * cosine_i[j]);
                }
                autocor[i] /= period;
            }
        }
        
        return autocor;
    }
}
