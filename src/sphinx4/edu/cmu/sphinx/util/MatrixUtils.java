package edu.cmu.sphinx.util;

import java.text.DecimalFormat;

/** Some simple matrix and vector manipulation methods. */

public class MatrixUtils {

    public static DecimalFormat df = new DecimalFormat("0.00");


    public static String toString(double[][] m) {
        String s = "[";

        for (int r = 0; r < m.length; r++) {
            s += "[";

            for (int c = 0; c < numCols(m); c++)
                s += " " + df.format(m[r][c]);

            s += " ]\n";
        }

        return s + " ]";
    }



    public static int numCols(double[][] m) {
        return m[0].length;
    }



    public static String toString(double[] v) {
        String s = "[";

        for (int r = 0; r < v.length; r++)
            s += " " + df.format(v[r]);

        return s + " ]";
    }


    public static String toString(int[] v) {
        String s = "[";

        for (int r = 0; r < v.length; r++)
            s += " " + v[r];

        return s + " ]";
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
}
