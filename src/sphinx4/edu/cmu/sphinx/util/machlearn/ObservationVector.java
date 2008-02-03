package edu.cmu.sphinx.util.machlearn;

/** An real-valued observation. */
public class ObservationVector implements Observation, Cloneable {

    protected double[] values;


    /** Constructs a new observation for a given feature-vector. */
    public ObservationVector(double[] values) {
        this.values = values;
    }


    /**
     * Returns the values of this observation.
     *
     * @return the values
     */
    public double[] getValues() {
        return values;
    }


    /** Returns the dimension of this observation. */
    public int dimension() {
        return getValues().length;
    }
}
