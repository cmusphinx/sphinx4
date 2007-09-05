package edu.cmu.sphinx.util;

import java.text.DecimalFormat;

/** Some simple matrix and vector manipulation methods. */

public class MatrixUtils {

    public static DecimalFormat df = new DecimalFormat("0.00");


    public static String toString(double[][] m) {
        StringBuffer s = new StringBuffer("[");

        for (int r = 0; r < m.length; r++) {
            s.append("[");

            for (int c = 0; c < numCols(m); c++)
                s.append(" ").append(df.format(m[r][c]));

            s.append(" ]\n");
        }

        return s.append(" ]").toString();
    }


    public static int numCols(double[][] m) {
        return m[0].length;
    }


    public static String toString(double[] v) {
        StringBuffer s = new StringBuffer("[");

        for (int r = 0; r < v.length; r++)
            s.append(" ").append(df.format(v[r]));

        return s.append(" ]").toString();
    }


    public static String toString(int[] v) {
        StringBuffer s = new StringBuffer("[");

        for (int r = 0; r < v.length; r++)
            s.append(" ").append(v[r]);

        return s.append(" ]").toString();
    }


    public static String toString(float[] vector) {
        return toString(float2double(vector));
    }


    public static String toString(float[][] matrix) {
        return toString(float2double(matrix));
    }


    public static float[] double2float(double[] values) { // what a mess !!! -> fixme: how to convert number arrays ?
        float[] newVals = new float[values.length];
        for (int i = 0; i < newVals.length; i++) {
            newVals[i] = (float) values[i];
        }

        return newVals;
    }


    public static float[][] double2float(double[][] array) {
        float[][] floatArr = new float[array.length][array[0].length];
        for (int i = 0; i < array.length; i++)
            floatArr[i] = double2float(array[i]);

        return floatArr;
    }


    public static double[] float2double(float[] values) {
        double[] doubArr = new double[values.length];
        for (int i = 0; i < doubArr.length; i++)
            doubArr[i] = values[i];

        return doubArr;
    }


    public static double[][] float2double(float[][] array) {
        double[][] doubArr = new double[array.length][array[0].length];
        for (int i = 0; i < array.length; i++)
            doubArr[i] = float2double(array[i]);

        return doubArr;
    }


    /** Converts a vector from linear domain to logdomain using a given <code>LogMath</code>-instance for conversion. */
    public static float[] linearToLog(double[] vector, LogMath logMath) {
        float[] logMixtureWeights = new float[vector.length];
        int nbGaussians = vector.length;
        for (int i = 0; i < nbGaussians; i++) {
            logMixtureWeights[i] = logMath.linearToLog(vector[i]);
        }
        return logMixtureWeights;
    }
}
