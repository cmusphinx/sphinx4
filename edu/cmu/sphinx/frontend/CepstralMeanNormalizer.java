/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.IOException;


/**
 * Apply Cepstral Mean Normalization (CMN) to a Cepstrum.
 * It subtracts the mean of all the input so far
 * from the cepstrum. The Sphinx properties that affect this processor
 * are: <pre>
 * edu.cmu.sphinx.frontend.cmn.initialCepstralMean
 * edu.cmu.sphinx.frontend.cmn.windowSize
 * edu.cmu.sphinx.frontend.cmn.shiftWindow </pre>
 *
 * The mean of all the input cepstrum so far is not recalculated
 * for each cepstrum. This mean is recalculated after
 * <code>edu.cmu.sphinx.frontend.cmn.shiftWindow</code> cepstra.
 * This mean is calculated by dividing the sum of all input cepstrum so
 * far by the number of input cepstrum. After obtaining the mean,
 * the sum is exponentially by multiplying it by the ratio: <pre>
 * cmnWindow/(cmnWindow + number of frames since the last recalculation)</pre>
 *
 * @see Cepstrum
 */
public class CepstralMeanNormalizer extends DataProcessor {


    /**
     * The name of the SphinxProperty for the initial cepstral mean,
     * which has a default value of 12.0F.
     * This is a front-end dependent magic number.
     */
    public static final String PROP_INITIAL_MEAN =
	"edu.cmu.sphinx.frontend.cmn.initialCepstralMean";

    /**
     * The name of the SphinxProperty for the CMN window size,
     * which has a default value of 500.
     */
    public static final String PROP_CMN_WINDOW =
	"edu.cmu.sphinx.frontend.cmn.windowSize";

    /**
     * The name of the SphinxProperty for the CMN shifting window,
     * which has a default value of 800.
     * The shifting window specifies how many cepstrum after which
     * we re-calculate the cepstral mean.
     */
    public static final String PROP_CMN_SHIFT_WINDOW =
	"edu.cmu.sphinx.frontend.cmn.shiftWindow";


    private float initialMean;
    private int cepstrumLength;
    private int numberFrame;
    private float[] currentMean;
    private float[] sum;
    private int cmnShiftWindow;
    private int cmnWindow;
    

    /**
     * Constructs a default CepstralMeanNormalizer with the given
     * SphinxProperties context.
     *
     * @param context the context of the SphinxProperties to use
     */
    public CepstralMeanNormalizer(String context) {
        super("CMN", context);
        initSphinxProperties();
	initMeansSums();
    }


    /**
     * Initializes the currentMean and sum arrays.
     */
    private void initMeansSums() {
	currentMean = new float[cepstrumLength];
	currentMean[0] = initialMean;
	sum = new float[cepstrumLength];
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void initSphinxProperties() {
        SphinxProperties properties = getSphinxProperties();
	initialMean = properties.getFloat(PROP_INITIAL_MEAN, 12.0f);
	cepstrumLength = properties.getInt(FrontEnd.PROP_CEPSTRUM_SIZE, 13);
	cmnWindow = properties.getInt(PROP_CMN_WINDOW, 500);
	cmnShiftWindow = properties.getInt(PROP_CMN_SHIFT_WINDOW, 800);
    }
	

    /**
     * Returns the next Data object, which is a normalized Cepstrum
     * produced by this class. However, it can also be other Data objects
     * like a EndPointSignal.SEGMENT_START.
     *
     * @return the next available Data object, returns null if no
     *     Data object is available
     */
    public Data read() throws IOException {
	
        Data input = getSource().read();

        if (input instanceof Cepstrum) {
            return process((Cepstrum) input);
        
        } else if (input instanceof EndPointSignal) {
            
            EndPointSignal signal = (EndPointSignal) input;

            if (signal.equals(EndPointSignal.FRAME_END)) {
                
                // Shift buffers down if we have more 
                // than cmnShiftWindow frames
                
                if (numberFrame > cmnShiftWindow) {
                    updateMeanSumBuffers();
                }

            } else if (signal.equals(EndPointSignal.SEGMENT_END)) {
                updateMeanSumBuffers();
            }

            return input;

        } else {
            return input;
        }
    }	


    /**
     * Returns the given Cepstrum, normalized.
     *
     * @param input a Cepstrum
     *
     * @return a normalized Cepstrum
     */
    private Data process(Cepstrum cepstrumObject) {
	
        getTimer().start();

        float[] cepstrum = cepstrumObject.getCepstrumData();
        for (int j = 0; j < cepstrum.length; j++) {
            sum[j] += cepstrum[j];
            cepstrum[j] -= currentMean[j];
        }

        numberFrame++;

        getTimer().stop();

	return cepstrumObject;
    }


    /**
     * Updates the currentMean buffer with the values in the sum buffer.
     * Then decay the sum buffer exponentially, i.e., divide the sum
     * with numberFrames.
     */
    private void updateMeanSumBuffers() {
        
        getTimer().start();

        if (numberFrame > 0) {
            // update the currentMean buffer with the sum buffer
            float sf = (float) (1.0/numberFrame);
            
            System.arraycopy(sum, 0, currentMean, 0, sum.length);
            
            multiplyArray(currentMean, sf);
            
            // decay the sum buffer exponentially
            if (numberFrame >= cmnShiftWindow) {
                multiplyArray(sum, (sf * cmnWindow));
                numberFrame = cmnWindow;
            }
        }

        getTimer().stop();
    }


    /**
     * Multiplies each element of the given array by the multiplier.
     *
     * @param array the array to multiply
     * @param multipler the amount to multiply by
     */
    private static final void multiplyArray(float[] array, float multiplier) {
        int arrayLength = array.length;
        for (int i = 0; i < arrayLength; i++) {
            array[i] *= multiplier;
        }
    }
}
