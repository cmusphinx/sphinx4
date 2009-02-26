package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Double;

import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.Random;


/** Adds a uniformly distribued dithering to a signal. */
public class Dither extends BaseDataProcessor {

    /** The maximal value which could be added/substracted to/from the signal*/
    @S4Double(defaultValue = 0.0)
    public static final String PROP_MAX_DITHER = "maxDither";
    private double ditherMax;

    /** The maximal value of dithered values. */
    @S4Double(defaultValue = Double.MAX_VALUE)
    public static final String PROP_MAX_VAL = "upperValueBound";
    private double maxValue;

    /** The minimal value of dithered values. */
    @S4Double(defaultValue = -Double.MAX_VALUE)
    public static final String PROP_MIN_VAL = "lowerValueBound";
    private double minValue;


    /** The name of the sphinx property about using random seed or not */
    @S4Boolean(defaultValue = false)
    public static final String PROP_USE_RANDSEED = "useRandSeed";
    private boolean useRandSeed;


    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        
        ditherMax = ps.getDouble(PROP_MAX_DITHER);
        useRandSeed = ps.getBoolean(PROP_USE_RANDSEED);

        maxValue = ps.getDouble(PROP_MAX_VAL);
        minValue = ps.getDouble(PROP_MIN_VAL);
    }


    @Override
    public void initialize() {
        super.initialize();
    }


    /**
     * Returns the next DoubleData object, which is a dithered version of the input
     *
     * @return the next available DoubleData object, or null if no Data is available
     * @throws edu.cmu.sphinx.frontend.DataProcessingException
     *          if a data processing error occurred
     */
    public Data getData() throws DataProcessingException {
        Data input = getPredecessor().getData(); // get the spectrum
        getTimer().start();
        if (input != null && ditherMax != 0) {
            if (input instanceof DoubleData || input instanceof FloatData) {
                input = process(input);
            }
        }

        getTimer().stop();
        return input;
    }


    /**
     * Process data, adding dither
     *
     * @param input a MelSpectrum frame
     * @return a mel Cepstrum frame
     */
    private DoubleData process(Data input) throws IllegalArgumentException {
        DoubleData output;
        Random r;
        if (useRandSeed)
            r = new Random();
        else
            r = new Random(12345);

        assert input instanceof DoubleData;
        double[] inFeatures;

        DoubleData doubleData = (DoubleData) input;
        inFeatures = doubleData.getValues();
        double[] outFeatures = new double[inFeatures.length];
        for (int i = 0; i < inFeatures.length; ++i) {
            outFeatures[i] = r.nextFloat() * 2 * ditherMax - ditherMax + inFeatures[i];
            outFeatures[i] = max(min(outFeatures[i], maxValue), minValue);
        }

        output = new DoubleData(outFeatures, doubleData.getSampleRate(),
                doubleData.getCollectTime(),
                doubleData.getFirstSampleNumber());

        return output;
    }
}
