/**
 * [[[copyright]]]
 */
package edu.cmu.sphinx.util;

/**
 * Implements complex types and arythmetics
 */
public class Complex {

     /**
     * The real part of a complex number.
     */
    public double re;
     /**
     * The imaginary part of a complex number.
     */
    public double im;

    /**
     * Create a default complex number
     */
    public Complex() {
	this.re = 0.0f;
	this.im = 0.0f;
    }

    /**
     * Create a complex number from a real one
     */
    public Complex(double real) {
	this.re = real;
	this.im = 0.0f;
    }

    /**
     * Create a complex number from the real and imaginary parts
     */
    public Complex(double real, double imaginary) {
	this.re = real;
	this.im = imaginary;
    }

    /**
     * Method to add two complex numbers.
     * 
     * @param a the first element to be added
     * @param b the second element to be added
     *
     * @return the sum of the complex numbers a and b
     */
    public void addComplex(Complex a, Complex b) {
	this.re = a.re + b.re;
	this.im = a.im + b.im;
    }

    /**
     * Method to subtract two complex numbers.
     * 
     * @param a the element we subtract from
     * @param b the element to be subtracted
     *
     * @return the difference between the complex numbers a and b
     */
    public void subtractComplex(Complex a, Complex b) {
	this.re = a.re - b.re;
	this.im = a.im - b.im;
    }

    /**
     * Method to multiply two complex numbers.
     * 
     * @param a the first element to multiply
     * @param b the second element to multiply
     *
     * @return the product the complex numbers a and b
     */
    public void multiplyComplex(Complex a, Complex b) {
	this.re = a.re * b.re - a.im * b.im;
	this.im = a.re * b.im + a.im * b.re;
    }

    /**
     * Method to divide two complex numbers.
     * To divide two complexes, we multiply by the
     * complex conjugate of the denominator, thus
     * resulting in a real number in the denominator.
     * 
     * @param a the numerator
     * @param b the denominator
     *
     * @return the ratio between the complex numbers a and b
     */
    public void divideComplex(Complex a, Complex b) {
	this.re = a.re * b.re + a.im * b.im;
	this.im = a.im * b.re - a.re * b.im;
	this.scaleComplex(this, b.squaredMagnitudeComplex());
    }

    /**
     * Method to scale a complex number by a real one.
     * The input complex number is modified in place.
     * 
     * @param a the complex number
     * @param b the real scaling factor
     *
     */
    public void scaleComplex(Complex a, double b) {
	this.re = a.re / b;
	this.im = a.re / b;
    }

    /**
     * Method to compute the squared magnitude of a complex number.
     * 
     * @param a the complex number
     *
     * @return the squared magnitude of the complex number
     */
    public double squaredMagnitudeComplex() {
	double squaredMag;
	squaredMag = this.re * this.re + this.im * this.im;
	return squaredMag;
    }

}
