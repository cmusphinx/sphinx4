
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

package tests.other;

import edu.cmu.sphinx.util.Timer;

import java.util.Random;

/**
 * 
 * Performas some math timings. Used to compare different modes of
 * math (floating, double, mixed, integer)
 */
public class MathTest {
    private int maxElements;
    private int maxIterations;

    float[] f1Values;
    float[] f2Values;
    float[] f3Values;

    int[] i1Values;
    int[] i2Values;
    int[] i3Values;

    double[] d1Values;
    double[] d2Values;
    double[] d3Values;

    double f = Math.PI;


    Timer floatTimer;
    Timer intTimer;
    Timer doubleTimer;
    Timer pureIntTimer;
    Timer pureFloatTimer;

    Random generator = new Random();

    public MathTest(int elements, int iterations) {
    	maxElements = elements;
	maxIterations = iterations;

	f1Values = new float[maxElements];
	f2Values = new float[maxElements];
	f3Values = new float[maxElements];

	i1Values = new int[maxElements];
	i2Values = new int[maxElements];
	i3Values = new int[maxElements];

	d1Values = new double[maxElements];
	d2Values = new double[maxElements];
	d3Values = new double[maxElements];

	for (int i = 0; i < maxElements; i++) {
	    f1Values[i] = generator.nextFloat();
	    f2Values[i] = generator.nextFloat();
	    f3Values[i] = generator.nextFloat();

	    i1Values[i] = generator.nextInt();
	    i2Values[i] = generator.nextInt();
	    i3Values[i] = generator.nextInt();

	    d1Values[i] = generator.nextDouble();
	    d2Values[i] = generator.nextDouble();
	    d3Values[i] = generator.nextDouble();
	}


	floatTimer = Timer.getTimer("MathTest", "mixed float");
	intTimer = Timer.getTimer("MathTest", "mixed double, float, int");
	doubleTimer = Timer.getTimer("MathTest", "pure double");
	pureIntTimer = Timer.getTimer("MathTest", "pure int");
	pureFloatTimer = Timer.getTimer("MathTest", "pure float");
    }

    /**
     * Mixed double and floating point
     */
    public float doFloatScore() {
	double dval = 0;
        for (int i = 0; i < f1Values.length ; i++) {
	    float diff = f1Values[i] - f2Values[i];
	    dval -= diff * diff * f3Values[i];
	}
	return (float) (dval * f1Values[0]);
    }

    /**
     * Pure double
     */
    public double doDoubleScore() {
	double dval = 0;
        for (int i = 0; i < d1Values.length ; i++) {
	    double diff = d1Values[i] - d2Values[i];
	    dval -= diff * diff * d3Values[i];
	}
	return dval * d1Values[0];
    }

    /*
     * Mixed double, float and int
     */
    public int doIntScore() {
	double dval = 0;
        for (int i = 0; i < f1Values.length ; i++) {
	    double diff = f1Values[i] - f2Values[i];
	    dval -= diff * diff * f3Values[i];
	}
	return (int) ((f * dval) + i1Values[0]);
    }

    /**
     * Pure Int
     */
    public int doPureIntScore() {
	int ival = 0;
        for (int i = 0; i < i1Values.length ; i++) {
	    int diff = i1Values[i] - i2Values[i];
	    ival -= diff * diff * i3Values[i];
	}
	return ival + i1Values[0];
    }

    /**
     * Pure float
     */
    public float doPureFloatScore() {
	float dval = 0;
        for (int i = 0; i < f1Values.length ; i++) {
	    float diff = f1Values[i] - f2Values[i];
	    dval -= diff * diff * f3Values[i];
	}
	return  (dval * f1Values[0]);
    }


    /**
     * Time all of the routines for multiple interations
     */
    public void doScores() {
	floatTimer.start();
	for (int i = 0; i < maxIterations; i++) {
	    doFloatScore();
	}
	floatTimer.stop();

	intTimer.start();
	for (int i = 0; i < maxIterations; i++) {
	    doIntScore();
	}
	intTimer.stop();

	doubleTimer.start();
	for (int i = 0; i < maxIterations; i++) {
	    doDoubleScore();
	}
	doubleTimer.stop();

	pureIntTimer.start();
	for (int i = 0; i < maxIterations; i++) {
	    doPureIntScore();
	}
	pureIntTimer.stop();

	pureFloatTimer.start();
	for (int i = 0; i < maxIterations; i++) {
	    doPureFloatScore();
	}
	pureFloatTimer.stop();
    }


    public void logTest() {
	for (int i = 0; i < maxIterations; i++) {
	    for (int j = 0; j < d1Values.length; j++) {
		Math.log(d1Values[j]);
	    }
	}
    }

    /**
     * Shows the results
     */
    public void dump() {
        Timer.dumpAll();
    }

    /**
     * Runs the timing test
     */
    public static void main(String[] args) {
	MathTest mt = new MathTest(39, 1000000);

	// warm-up
	for (int i = 0; i < 5; i++) {
	    mt.doScores();
	}

	Timer.resetAll();

	// go live
	for (int i = 0; i < 5; i++) {
	    mt.doScores();
	}

	mt.dump();
    }
}
