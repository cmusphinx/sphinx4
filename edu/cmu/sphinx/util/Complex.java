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
     * Create a default complex number;
     */
    private Complex() {
	this.re = 0.0f;
	this.im = 0.0f;
    }

    /**
     * Method to add two complex numbers.
     * 
     * @param a the first element to be added
     * @param b the second element to be added
     *
     * @return the sum of the complex numbers a and b
     */
    public Complex addComplex(Complex a, Complex b) {
	Complex Sum = new Complex(); 
	Sum.re = a.re + b.re;
	Sum.im = a.im + b.im;
	return Sum;
    }

    /**
     * Method to subtract two complex numbers.
     * 
     * @param a the element we subtract from
     * @param b the element to be subtracted
     *
     * @return the difference between the complex numbers a and b
     */
    public Complex subtractComplex(Complex a, Complex b) {
	Complex Diff = new Complex(); 
	Diff.re = a.re - b.re;
	Diff.im = a.im - b.im;
	return Diff;
    }

    /**
     * Method to multiply two complex numbers.
     * 
     * @param a the first element to multiply
     * @param b the second element to multiply
     *
     * @return the product the complex numbers a and b
     */
    public Complex multiplyComplex(Complex a, Complex b) {
	Complex Mult = new Complex(); 
	Mult.re = a.re * b.re - a.im * b.im;
	Mult.im = a.re * b.im + a.im * b.re;
	return Mult;
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
    public Complex divideComplex(Complex a, Complex b) {
	Complex Div = new Complex(); 
	Div.re = a.re * b.re + a.im * b.im;
	Div.im = a.im * b.re - a.re * b.im;
	scaleComplex(Div, squaredMagnitudeComplex(b));
	return Div;
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
	a.re /= b;
	a.im /= b;
    }

    /**
     * Method to compute the squared magnitude of a complex number.
     * 
     * @param a the complex number
     *
     * @return the squared magnitude of the complex number
     */
    public double squaredMagnitudeComplex(Complex a) {
	double squaredMag;
	squaredMag = a.re * a.re + a.im * a.im;
	return squaredMag;
    }

}
