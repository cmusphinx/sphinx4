
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

package edu.cmu.sphinx.knowledge.acoustic;

import edu.cmu.sphinx.util.LogMath;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;

/**
 * Used to accumulate data for updating of models.
 */
class Buffer {
    private float[] numerator;
    private float denominator;

    /**
     * Creates a new buffer
     */
    Buffer() {
    }

    /**
     * Creates a new buffer
     *
     * @param size the number of elements in this buffer
     */
    Buffer(int size) {
	numerator = new float[size];
    }

    /**
     * Accumulates data to this buffer. Data are accumulated to a
     * given numerator buffer and to the denominator buffer.
     *
     * @param data the data to be added
     * @param entry the numerator entry to be accumulated to
     */
    void accumulate(float data, int entry) {
	assert numerator != null;
	numerator[entry] += data;
	denominator += data;
    }


    /**
     * Accumulates data to this buffer. Data are accumulated to a
     * given numerator buffer and to the denominator buffer.
     *
     * @param data the data to be added
     * @param entry the numerator entry to be accumulated to
     * @param logMath the logMath to use
     */
    void logAccumulate(float data, int entry, LogMath logMath) {
	assert numerator != null;
	numerator[entry] = logMath.addAsLinear(numerator[entry], data);
	denominator = logMath.addAsLinear(denominator, data);
    }

    /**
     * Accumulates data to this buffer. Data are accumulated to a
     * given numerator buffer and to the denominator buffer.
     *
     * @param numeratorData the data to be added to the numerator
     * @param denominatorData the data to be added to the denominator
     */
    void accumulate(float[] numeratorData, float denominatorData) {
	assert numerator != null;
	assert numeratorData != null;
	assert numerator.length == numeratorData.length;
	for (int i = 0; i < numerator.length; i++) {
	    numerator[i] += numeratorData[i];
	}
	denominator += denominatorData;
    }


    /**
     * Accumulates data to this buffer. Data are accumulated to a
     * given numerator buffer and to the denominator buffer.
     *
     * @param logNumeratorData the data to be added to the numerator
     * @param logDenominatorData the data to be added to the denominator
     * @param entry the numerator entry to be accumulated to
     */
    void logAccumulate(float[] logNumeratorData, float logDenominatorData, 
		       int entry, LogMath logMath) {
	assert numerator != null;
	assert logNumeratorData != null;
	assert numerator.length == logNumeratorData.length;
	for (int i = 0; i < numerator.length; i++) {
	    numerator[i] = 
		logMath.addAsLinear(numerator[i], logNumeratorData[i]);
	}
	denominator = logMath.addAsLinear(denominator, logDenominatorData);
    }

    /**
     * Normalize the buffer. This method divides the numerator by the
     * denominator, storing the result in the numerator, and setting
     * denominator to 1.
     */
    void normalize() {
	float invDenominator = 1.0f / denominator;
	for (int i = 0; i < numerator.length; i++) {
	    numerator[i] *= invDenominator;
	}
	denominator = 1.0f;
    }

    /**
     * Normalize the buffer in log scale. This method divides the
     * numerator by the denominator, storing the result in the
     * numerator, and setting denominator to log(1) = 0.
     */
    void logNormalize() {
	for (int i = 0; i < numerator.length; i++) {
	    numerator[i] -= denominator;
	}
	denominator = 0.0f;
    }

    /**
     * Normalize the buffer. The normalization is done so that the
     * summation of elements in the buffer is 1.
     */
    void normalizeToSum() {
	float den = 0.0f;
	for (int i = 0; i < numerator.length; i++) {
	    den += numerator[i];
	}
	float invDenominator = 1.0f / den;
	for (int i = 0; i < numerator.length; i++) {
	    numerator[i] *= invDenominator;
	}
	denominator = 1.0f;
    }

    /**
     * Normalize the buffer in log scale. The normalization is done so
     * that the summation of elements in the buffer is log(1) = 0. In
     * this, we assume that if an element has a value of zero, it
     * won't be updated.
     *
     * @param logMath the logMath to use
     */
    void logNormalizeToSum(LogMath logMath) {
	float logZero = logMath.getLogZero();
	float den = logZero;
	for (int i = 0; i < numerator.length; i++) {
	    if (numerator[i] != logZero) {
		den = logMath.addAsLinear(den, numerator[i]);
	    }
	}
	for (int i = 0; i < numerator.length; i++) {
	    if (numerator[i] != logZero) {
		numerator[i] -= den;
	    }
	}
	denominator = 0.0f;
    }

    /**
     * Floor the buffer.
     *
     * @param floor the floor for this buffer
     *
     * @return if true, the buffer was modified
     */
    protected boolean floor(float floor) {
	boolean wasModified = false;
	for (int i = 0; i < numerator.length; i++) {
	    if (numerator[i] < floor) {
		wasModified = true;
		numerator[i] = floor;
	    }
	}
	return wasModified;
    }

    /**
     * Floor the buffer in log scale.
     *
     * @param logFloor the floor for this buffer, in log scale
     *
     * @return if true, the buffer was modified
     */
    protected boolean logFloor(float logFloor) {
	boolean wasModified = false;
	for (int i = 0; i < numerator.length; i++) {
	    if (numerator[i] < logFloor) {
		wasModified = true;
		numerator[i] = logFloor;
	    }
	}
	return wasModified;
    }

    /**
     * Retrieves a value from this buffer. Make sure you normalize the
     * buffer first.
     *
     * @param entry the index into the buffer
     *
     * @return the value
     */
    protected float getValue(int entry) {
	return numerator[entry];
    }

    /**
     * Set the entry in this buffer to a value.
     *
     * @param entry the index into the buffer
     * @param value the value
     */
    protected void setValue(int entry, float value) {
	numerator[entry] = value;
    }

    /**
     * Retrieves a vector from this buffer. Make sure you normalize the
     * buffer first.
     *
     * @return the value
     */
    protected float[] getValues() {
	return numerator;
    }
}

