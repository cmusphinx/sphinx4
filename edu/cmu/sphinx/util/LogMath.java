/**
 * [[[copyright]]]
 */
package edu.cmu.sphinx.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a set of methods for performing simple math in
 * the log domain.  The logarithmic base can be set by the
 * SphinxProperty:
 * <br><code>
 * edu.cmu.sphinx.util.LogMath.logBase
 * </code><br>
 * 
 */
public final class LogMath {

    static Map contextMap = new HashMap();

    /**
     * Sphinx3 property to get the Log base
     */

    public final static String PROP_LOG_BASE 
	= "edu.cmu.sphinx.util.LogMath.logBase";

     private double logBase = 1.0001;
     private double inverseNaturalLogBase;
     private double logZero;

     /**
      * Creates a log math with the given base
      *
      * @param base the base for the log math
      */
     public static  LogMath getLogMath(double base) {
	 return new LogMath(base);
     }

     /**
      * Gets a  LogMath for the given context
      *
      * @param context the context of interest
      *
      * @return the LogMath for the context
      */
     public static LogMath getLogMath(String  context) {
	 LogMath logMath = (LogMath) contextMap.get(context);
	 if (logMath == null) {
	    SphinxProperties props = 
		    SphinxProperties.getSphinxProperties(context);
	    double base = props.getDouble(PROP_LOG_BASE, Math.E);
	    logMath = new LogMath(base);
	    contextMap.put(context, logMath);
	 } 
	 return logMath;
     }

     /**
      * create a log math class 
      *
      * @param base the log base
      */
     private LogMath(double base) {
	 logBase = base;
	 inverseNaturalLogBase = 1.0/Math.log(logBase);;

	 // [[[ TODO: probably not right way to get logZero ]]]
	 // EBG: Probably the right way is just to assign 
	 // -MAX_VALUE to logZero

	 // logZero = Math.log(Double.MIN_VALUE);
	 logZero = -Double.MAX_VALUE;

	 // System.out.println("Logz is " + logZero);
     }



    /**
     * Multiplies the two log values. 
     *
     * Will check for underflow and
     * constrain values to be no lower than LOG_MIN_VALUE. 
     *
     * Will check for overflow and
     * constrain values to be no higher than LOG_MAX_VALUE. 
     *
     *
     * @param val1 value in log domain to multiply
     * @param val2 value in log domain to multiply
     *
     * @return product of val1 and val2 in the log domain
     *
     * Questions: Any constraints
     * [[[ TODO: need to have some overflow underflow checks ]]]
     *
     * [ EBG: how about... test the sign of each of val1 and val2
     * and, if needed, the result. If val1 and val2 have the same
     * sign, then overflow isn't an issue. If they have the same
     * sign and the result also has the same sign, then operation
     * was successful. if not, then an overflow occured, in which case
     * we can signal or just return MAX_VALUE ]
     */
    public final double multiply(double val1, double val2) {
	return val1 + val2;
    }


    /**
     * Adds the two log values.
     *
     * <p>This function makes use of the equality:</p>
     *
     * <p><b>log(a + b) = log(a) + log (1 + exp(log(b) - log(a)))</b></p>
     *
     * <p>which is derived from:</p>
     *
     * <p><b>a + b = a * (1 + (b / a))</b></p>
     *
     * <p>which in turns makes use of:</p>
     *
     * <p><b>b / a = exp (log(b) - log(a))</b></p>
     *
     * <p>Important to notice that <code>subtract(a, b)</code> is *not* the
     * same as <code>add(a, -b)</code>, since we're in the log domain,
     * and -b is in fact the inverse.</p>
     *
     * Will check for underflow and
     * constrain values to be no lower than LOG_MIN_VALUE. 
     *
     * Will check for overflow and
     * constrain values to be no higher than LOG_MAX_VALUE. 
     *
     * @param val1 value in log domain to add
     * @param val2 value in log domain to add
     *
     * @return sum of val1 and val2 in the log domain
     *
     * <br>[[[ TODO: This is a very slow way to do this ]]]
     * [[[ TODO: need to have some overflow underflow checks ]]]
     * [ EBG: maybe we should also have a function to add many numbers,
     * say, return the summation of all terms in a given vector, if 
     * efficiency becomes an issue.
     */
    public final double add(double val1, double val2) {
	double highestValue;
	double difference;

	if (val1 > val2) {
	    highestValue = val1;
	    difference = val2 - val1;
	} else {
	    highestValue = val2;
	    difference = val1 - val2;
	}
	return (highestValue + addTable(difference));
	//	return linearToLog(logToLinear(val1) + logToLinear(val2));
    }

    /**
     * Function used by add() internally. It returns the difference
     * between the highest number and the total summation of two numbers.
     *
     * Considering the expression (in which we assume natural log)
     *
     * <p><b>log(a + b) = log(a) + log(1 + exp(log(b) - log(a)))</b></p>
     *
     * the current function returns the second term of the right hand
     * side of the equality above, generalized for the case of any log
     * base. This function can be contructed as a table, if table
     * lookup is faster than actual computation.
     *
     * @param index the index into the addTable
     *
     * @return the value pointed to by index
     */
    private final double addTable(double index) {
	double innerSummation;

	innerSummation = logToLinear(index);
	innerSummation += 1.0f;
	return linearToLog(innerSummation);
    }

    /**
     * Returns the difference between two log domain values.
     *
     * <p>Implementation is less efficient than add(), since we're less
     * likely to use this function, provided for completeness. Notice
     * however that the result only makes sense if the minuend is
     * higher than the subtrahend. Otherwise, we should return the log
     * of a negative number.</p>
     *
     * Will check for underflow and
     * constrain values to be no lower than LOG_MIN_VALUE. 
     *
     * Will check for overflow and
     * constrain values to be no higher than LOG_MAX_VALUE. 
     *
     * @param minuend value in log domain to be  subtracted from
     * @param subtrahend value in log domain that is being
     * subtracted
     *
     * @return difference between minuend and the subtrahend 
     * in the log domain
     *
     * @throws IllegalArgumentException
     *
     * <br>[[[ TODO: This is a very slow way to do this ]]]
     * [[[ TODO: need to have some overflow underflow checks ]]]
     */
    public final double subtract(double minuend, double
	    subtrahend) throws IllegalArgumentException {

	if (minuend < subtrahend) {
	    throw new IllegalArgumentException("Subtract results in log "
					       + "of a negative number: "
					       + minuend + " - " 
					       + subtrahend);
	}
	return linearToLog(logToLinear(minuend) - logToLinear(subtrahend));
    }

   /**
    * Converts the source, which is assumed to be a log value
    * whose base is sourceBase, to a log value whose base is
    * resultBase.  Possible values for both the source and
    * result bases include Math.E, 10.0, LogMath.getLogBase().
    * If a source or result base is not supported, an
    * IllegalArgumentException will be thrown.
    *
    * <p>It takes advantage of the relation:</p>
    *
    * <p><b>log_a(b) = log_c(b) / lob_c(a)</b></p>
    *
    * <p>or:</p>
    *
    * <p><b>log_a(b) = log_c(b) * lob_a(c)</b></p>
    *
    * <p>where <b>log_a(b)</b> is logarithm of <b>b</b> base <b>a</b>
    * etc.</p>
    *
    * @param source log value whose base is sourceBase
    * @param sourceBase the base of the log the source
    * @param resultBase the base to convert the source log to
    *
    * @throws IllegalArgumentException
    */
    //  [[[ TODO: This is slow, but it probably doesn't need
    //  to be too fast ]]]
    // [ EBG: it can be made more efficient if one of the bases is
    // Math.E. So maybe we should consider two functions logToLn and
    // lnToLog instead of a generic function like this??
    //
    public static double logToLog(double source, 
	    double sourceBase, double resultBase) 
	throws IllegalArgumentException {
	if ((sourceBase <= 0) || (resultBase <= 0)) {
	    throw new IllegalArgumentException("Trying to take log of "
					       + " non-positive number: "
					       + sourceBase + " or " +
					       resultBase);
	}
	double lnSourceBase = Math.log(sourceBase);
	double lnResultBase = Math.log(resultBase);

	return (source * lnSourceBase / lnResultBase);
    }


    /**
     * Converts the source, which is a number in base Math.E, to a log value
     * which base is the LogBase of this LogMath.
     *
     * @param source the number in base Math.E to convert
     */
    public final double logEToLog(double source) {
	return (source * inverseNaturalLogBase);
    }


    /**
     * Converts the value from linear domain to log domain
     *
     * @param linearValue the value to be converted to log domain
     *
     * @return the value in log domain
     */
    public final double linearToLog(double linearValue) {
	if (linearValue < 0.0) {
	    throw new IllegalArgumentException(
		    "linearToLog: param must be >= 0");
	} else if (linearValue == 0.0) {
	    // [EBG] Shouldn't the comparison above be something like
	    // linearValue < "epsilon"? Is it ever going to be 0.0?
	    return getLogZero();
	} else {
	    return Math.log(linearValue) *  inverseNaturalLogBase;
	}
    }

    /**
     * Converts the value from log domain to linear domain
     *
     * @param logValue the value to be converted to the linear  domain
     *
     * @return the value in the linear domain
     */
    public final double logToLinear(double logValue) {
	return Math.pow(logBase, logValue);
    }

    /**
     * Returns the zero value in the log domain
     *
     * @return zero value in the log domain
     */
    public final double getLogZero() {
	return logZero;
    }

    /**
     * Returns the actual log base.
     */
    public final double getLogBase() {
	return logBase;
    }

     /**
      * Returns the log (base 10) of value
      *
      * @param value the value to take the log of
      *
      * @return the log (base 10) of value
      */
    // [ EBG: Shouldn't we be using something like logToLog(value, base, 10)
    // for this? ]
     public static double log10(double value) {
	  return (0.4342944819 * java.lang.Math.log(value));
	  // If you want to get rid of the constant:
	  // return ((1.0f / Math.log(10.0f)) * Math.log(value));
     }
}
