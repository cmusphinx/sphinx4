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

import java.io.IOException;


/**
 * Filters out the attenuation of audio data. Speech signals have an
 * attenuation of 20 dB/dec. Preemphasis flatten the signal to make it
 * less susceptible to finite precision effects later in the signal
 * processing. The Preemphasizer takes a Audio as input
 * and outputs the same Audio, but with preemphasis applied.
 *
 * The SphinxProperties of this Preemphasizer are: <pre>
 * edu.cmu.sphinx.frontend.preemphasis.dump
 * edu.cmu.sphinx.frontend.preemphasis.factor
 * </pre>
 * This Preemphasizer also processes the PreemphasisPriorSignal signal.
 * This type of signal contain the value of the previous sample, which
 * is used in applying preemphasis.
 *
 * Other Data objects are passed along unchanged through this Preemphasizer.
 *
 * @see Audio
 */
public class Preemphasizer extends DataProcessor implements AudioSource {


    /**
     * The name of the SphinxProperty for preemphasis factor/alpha, which
     * has a default value of 0.97F.
     */
    public static final String PROP_PREEMPHASIS_FACTOR = 
	"edu.cmu.sphinx.frontend.Preemphasizer.factor";


    /**
     * The default value of PROP_PREEMPHASIS_FACTOR.
     */
    public static final float PROP_PREEMPHASIS_FACTOR_DEFAULT = 0.97f;
    

    private float preemphasisFactor;
    private double prior;
    private AudioSource predecessor;


    /**
     * Constructs a default Preemphasizer.
     *
     * @param name the name of this Preemphasizer
     * @param context the context of SphinxProperty this Preemphasizer uses
     * @param props the SphinxProperties object to use
     * @param predecessor the AudioSource from which it obtains Audio objects
     */
    public Preemphasizer(String name, String context, SphinxProperties props,
			 AudioSource predecessor) {
        super(name, context, props);
        setProperties();
        this.predecessor = predecessor;
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void setProperties() {
        preemphasisFactor = getSphinxProperties().getFloat
	    (PROP_PREEMPHASIS_FACTOR, PROP_PREEMPHASIS_FACTOR_DEFAULT);
    }


    /**
     * Returns the next Data object, which is usually a Audio,
     * produced by this Preemphasizerm, though it can also be Data objects
     * like EndPoint.UTTERANCE_START.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     *
     * @throws java.io.IOException if there is an error reading
     * the Data objects
     *
     * @see Audio
     */
    public Audio getAudio() throws IOException {

	Audio input = predecessor.getAudio();

        getTimer().start();
        
        if (input != null) {
            if (input.hasContent()) {
                applyPreemphasis(input);
            } else if (input.hasSignal(Signal.UTTERANCE_END)) {
                prior = 0;
            }
        }

        getTimer().stop();

        return input;
    }	


    /**
     * Applies pre-emphasis filter to the given Audio. The preemphasis
     * is applied in place.
     *
     * @param input a Audio of audio data
     */
    private void applyPreemphasis(Audio input) {

	double[] in = input.getSamples();
	
        // set the prior value for the next Audio
        double nextPrior = prior;
        if (in.length > 0) {
            nextPrior = in[in.length - 1];
        }

	if (in.length > 1 && preemphasisFactor != 0.0) {
	    
            // do preemphasis
            double current;
            double previous = in[0];
            in[0] = previous - preemphasisFactor * prior;

	    for (int i = 1; i < in.length; i++) {
                current = in[i];
		in[i] = current - preemphasisFactor * previous;
                previous = current;
	    }
	}

        prior = nextPrior;

	if (getDump()) {
	    System.out.println("PREEMPHASIS " + input.toString());
	}
    }
}
