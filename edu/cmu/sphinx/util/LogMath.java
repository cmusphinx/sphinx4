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
 * </code>
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
	 logZero = Math.log(Double.MIN_VALUE);

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
     */
    public double multiply(double val1, double val2) {
	return val1 + val2;
    }


    /**
     * adds the two log values
     *
     * Will check for underflow and
     * constrain values to be no lower than LOG_MIN_VALUE. 
     *
     * Will check for overflow and
     * constrain values to be no higher than LOG_MAX_VALUE. 
     *
     * @param val1 value in log domain to multiply
     * @param val2 value in log domain to multiply
     *
     * @return sum of val1 and val2 in the log domain
     *
     * [[[ TODO: This is a very slow way to do this ]]]
     * [[[ TODO: need to have some overflow underflow checks ]]]
     */
    public double add(double val1, double val2) {
	return linearToLog(logToLinear(val1) + logToLinear(val2));
    }

    /**
     * Returns the difference between two log domain values
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
     * @return differnce between minuend and the subtrahend 
     * in the log domain
     *
     * [[[ TODO: This is a very slow way to do this ]]]
     * [[[ TODO: need to have some overflow underflow checks ]]]
     */
    public double subtract(double minuend, double
	    subtrahend) {
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
    * @param source log value whose base is sourceBase
    * @param sourceBase the base of the log the source
    * @param resultBase the base to convert the source log to
    *
    */
    //  [[[ TODO: This is slow, but it probably doesn't need
    //  to be too fast ]]]
    public static double logToLog(double source, 
	    double sourceBase, double resultBase) {
	  double linearSource = Math.pow(sourceBase, source);
	  return Math.log(linearSource) / Math.log(resultBase);
    }


    /**
     * Converts the value from linear domain to log domain
     *
     * @param linearValue the value to be converted to log domain
     *
     * @return the value in log domain
     */
    public double linearToLog(double linearValue) {
	if (linearValue < 0.0) {
	    throw new IllegalArgumentException(
		    "linearToLog: param must be >= 0");
	} else if (linearValue == 0.0) {
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
    public double logToLinear(double logValue) {
	return Math.pow(logBase, logValue);
    }

    /**
     * Returns the zero value in the log domain
     *
     * @return zero value in the log domain
     */
    public double getLogZero() {
	return logZero;
    }

     /**
      * Returns the log (base 10) of value
      *
      * @param value the value to take the log of
      *
      * @return the log (base 10) of value
      */
     public static double log10(double value) {
	  return (0.4342944819 * java.lang.Math.log(value));
     }
}


