package edu.cmu.sphinx.util.machlearn;

import java.util.Arrays;

/** An real-valued observation. */
public class OVector implements Observation, Cloneable {

    protected double[] values;


    /** Constructs a new observation for a given feature-vector. */
    public OVector(double[] values) {
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


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OVector) {
            return Arrays.equals(values, ((OVector) obj).values);
        }

        return false;
    }


    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }


    @Override
    public String toString() {
        return Arrays.toString(values);
    }
}
