/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.IOException;


/**
 * Applies a melcosine filter bank to the given MelSpectrum.
 * Outputs a Cepstrum.
 */
public class MelCepstrumProducer extends DataProcessor {

    /**
     * The name of the SphinxProperty for the number of mel-filters.
     */
    public static final String PROP_NUM_MEL_FILTERS =
	"edu.cmu.sphinx.melcepstrum.filters";

    
    private int cepstrumSize;
    private int numberMelFilters;
    private float[][] melcosine;


    /**
     * Constructs a default MelCepstrumProducer with the given
     * SphinxProperties context.
     *
     * @param context the context of the SphinxProperties to use
     */
    public MelCepstrumProducer(String context) {
	initSphinxProperties(context);
        computeMelCosine();
        setTimer(Timer.getTimer(context, "MelCepstrum"));
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     *
     * @param context the context of the SphinxProperties used
     */
    private void initSphinxProperties(String context) {
        setSphinxProperties(context);
	SphinxProperties properties = getSphinxProperties();

        cepstrumSize = properties.getInt(FrontEnd.PROP_CEPSTRUM_SIZE, 13);
        numberMelFilters = properties.getInt(PROP_NUM_MEL_FILTERS, 31);
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
     * Returns the next Data object, which is the mel cepstrum of the
     * input frame. However, it can also be other Data objects
     * like a EndPointSignal.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException {

	Data input = getSource().read();
        
        if (input instanceof Spectrum) {
            input = process((Spectrum) input);
        }

	return input;
    }


    /**
     * Process data, creating the mel cepstrum from an input
     * audio frame.
     *
     * @param input a MelSpectrum frame
     *
     * @return a mel Cepstrum frame
     */
    private Cepstrum process(Spectrum input) throws IllegalArgumentException {

        getTimer().start();

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
                melspectrum[i] = -1.0e+5;
            }
        }

        // create the cepstrum by apply the melcosine filter
        float[] cepstrumData = applyMelCosine(melspectrum);

        getTimer().stop();
	
        if (getDump()) {
            System.out.println(Util.dumpFloatArray(cepstrumData,
                                                   "MEL_CEPSTRUM   "));
        }

        return (new Cepstrum(cepstrumData));
    }

    
    /**
     * Apply the MelCosine filter to the given melspectrum.
     */
    private float[] applyMelCosine(double[] melspectrum) {

        // create the cepstrum
        float[] cepstrumData = new float[cepstrumSize];
        float period = (float) numberMelFilters;
        float beta = 0.5f;
        
        // apply the melcosine filter
        for (int i = 0; i < cepstrumSize; i++) {
            
            if (numberMelFilters > 0) {
                float[] melcosine_i = melcosine[i];
                int j = 0;
                cepstrumData[i] += (beta * melspectrum[j] * melcosine_i[j]);

                for (j = 1; j < numberMelFilters; j++) {
                    cepstrumData[i] += (melspectrum[j] * melcosine_i[j]);
                }
                cepstrumData[i] /= period;
            }
        }
        
        return cepstrumData;
    }
}
