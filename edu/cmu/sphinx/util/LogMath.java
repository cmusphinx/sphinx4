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

package edu.cmu.sphinx.util;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.IOException;


/**
 * Provides a set of methods for performing simple math in
 * the log domain.  The logarithmic base can be set by the
 * SphinxProperty:
 * <br><code>
 * edu.cmu.sphinx.util.LogMath.logBase
 * </code><br>
 * 
 */
public final class LogMath implements Serializable {

    static Map contextMap = new HashMap();

    /**
     * The logger for this class
     */
    private static Logger logger = 
	    Logger.getLogger("edu.cmu.sphinx.util.LogMath");

    /**
     * Sphinx property to get the Log base
     */

    public final static String PROP_LOG_BASE 
	= "edu.cmu.sphinx.util.LogMath.logBase";

    /**
     * Sphinx property that controls whether we use the old, slow (but
     * correct) method of performing the log.add by doing the actual
     * computation.
     */

    public final static String PROP_USE_ADD_TABLE
	= "edu.cmu.sphinx.util.LogMath.useAddTable";

    private double logBase = 1.0001;
    private boolean useAddTable;
    private transient double naturalLogBase;
    private transient double inverseNaturalLogBase;
    private transient double logZero;
    private transient double logOne;
    private transient double theAddTable[];
    private transient double maxLogValue;
    private transient double minLogValue;

     /**
      * Creates a log math with the given base
      *
      * @param base the base for the log math
      *
      * @param useTable use the addTable (true) or do computation (false)
      */
     public static  LogMath getLogMath(double base, boolean useTable) {
	 return new LogMath(base, useTable);
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
	    boolean useTable = props.getBoolean(PROP_USE_ADD_TABLE, true);
	    logMath = new LogMath(base, useTable);
	    contextMap.put(context, logMath);
	 } 
	 return logMath;
     }

     /**
      * Create a log math class. Also create the addTable table, which
      * depends on the log base.
      *
      * @param base the log base
      *
      * @param useTable use the addTable (true) or do computation (false)
      */
     private LogMath(double base, boolean useTable) {
	 logBase = base;
	 useAddTable = useTable;
	 init();
     }

    /**
     * De-serializes the non-transient fields to the given stream
     *
     * @param s the stream to read the object from
     *
     * @throws IOException if an error occurs during the read.
     */
    private void readObject(ObjectInputStream s) 
	throws IOException, ClassNotFoundException {
	s.defaultReadObject();
	init();
    }

     /**
      * Initializes this log math
      */
     private void init() {

	 logger.info("Log base is " + logBase);
	 if (useAddTable) {
	     logger.info("Using AddTable when adding logs");
	 } else {
	     logger.info("Performing actual computation when adding logs");
	 }
	 naturalLogBase = Math.log(logBase);
	 inverseNaturalLogBase = 1.0/naturalLogBase;

	 // [[[ TODO: probably not right way to get logZero ]]]
	 // EBG: Probably the right way is just to assign 
	 // -MAX_VALUE to logZero

	 // logZero = Math.log(Double.MIN_VALUE);
	 logZero = -Double.MAX_VALUE;
         logOne = 0.0;

	 // System.out.println("Logz is " + logZero);

	 // When converting a number from/to linear, we need to make
	 // sure it's within certain limits to prevent it from
	 // underflowing/overflowing.

	 // We compute the max value by computing the log of the max
	 // value that a double can contain.
	 maxLogValue = linearToLog(Double.MAX_VALUE);

	 // We compute the min value by computing the log of the min
	 // (absolute) value that a double can hold.
	 minLogValue = linearToLog(Double.MIN_VALUE);

	 if (useAddTable) {
	     // Now create the addTable table.

	     // summation needed in the loop
	     double innerSummation;

	     // First decide number of elements.
	     int entriesInTheAddTable;
	     final int veryLargeNumberOfEntries = 150000;
	     final int verySmallNumberOfEntries = 0;

	     // To decide size of table, take into account that a base
	     // of 1.0001 or 1.0003 converts probabilities, which are
	     // numbers less than 1, into integers.  Therefore, a good
	     // approximation for the smallest number in the table,
	     // therefore the value with the highest index, is an
	     // index that maps into 0.5: indices higher than that, if
	     // they were present, would map to less values less than
	     // 0.5, therefore they would be mapped to 0 as
	     // integers. Since the table implements the expression:
	     //
	     // log(1.0 + base^(-index)))
	     //
	     // then the highest index would be:
	     //
	     // topIndex = - log(logBase^(0.5) - 1)
	     //
	     // where log is the log in the appropriate base.

	     // TODO: PBL changed this to get it to compile, also
	     // added -Math.rint(...) to round to nearest
	     // integer. Added the negation to match the preceeding
	     // documentation

	     entriesInTheAddTable = 
		 (int) -Math.rint(linearToLog(logToLinear(0.5) - 1));

	     // We reach this max if the log base is 1.00007. The
	     // closer you get to 1, the higher the number of entries
	     // in the table.

	     if (entriesInTheAddTable > veryLargeNumberOfEntries) {
		 entriesInTheAddTable = veryLargeNumberOfEntries;
	     }

	     if (entriesInTheAddTable <= verySmallNumberOfEntries) {
		 throw new IllegalArgumentException("The log base "
		      + logBase + " yields a very small addTable. "
		      + "Either choose not to use the addTable, "
                      + "or choose a logBase closer to 1.0");
	     }

	     // PBL added this just to see how many entries really are
	     // in the table

	     if (false)  {
		 System.out.println("LogAdd table has " + entriesInTheAddTable
				    + " entries.");
	     }

	     theAddTable = new double[entriesInTheAddTable];
	     for (int index = 0; index < entriesInTheAddTable; index++) {
		 // This loop implements the expression:
		 //
		 // log( 1.0 + power(base, index))
		 //
		 // needed to add two numbers in the log domain.

		 innerSummation = logToLinear(-index);
		 innerSummation += 1.0f;
		 theAddTable[index] = linearToLog(innerSummation);
	     }
	 }
     }



    /**
     * Exponentiate base to the power in log.
     *
     * <p>Will check for underflow and report.</p>
     *
     * <p>Will check for overflow on the lowest bound and constrain
     * values to be no lower than -Double.MAX_VALUE.</p>
     *
     * <p>Will check for overflow on the highest bound and constrain
     * values to be no higher than Double.MAX_VALUE.</p>
     *
     *
     * @param base base to be raised in log domain
     * @param exponent power of the exponentiation
     *
     * @return base raised to exponent
     *
     */
    public final double power(double base, double exponent) {
	double returnValue = base * exponent;
	boolean overflowOccurred = false;

	// if abs(exponent) is > 1, the product may result larger than max
	if (Math.abs(exponent) > 1) {
	    if (Math.abs(base) > (Double.MAX_VALUE / Math.abs(exponent))) {
		overflowOccurred = true;
		// base/exponent will have the same sign as their
		// product, but will not overflow.
		if ((base / exponent > 0)) {
		    returnValue = Double.MAX_VALUE;
		} else {
		    returnValue = -Double.MAX_VALUE;
		}
	    }
	} else if (Math.abs(exponent) < 1) {
	    // Underflow may occur if exponent is less than one (the
	    // power will be less than the base in absolute terms.
	    if (Math.abs(base) < ( Double.MIN_VALUE / Math.abs(exponent))) {
		overflowOccurred = true;
		returnValue = 0.0;
	    }
	}
	if (overflowOccurred) {
	    logger.info("********Overflow or underflow occurred "
			+ "while trying to log.power "
			+  base + " raised to " + exponent);
	}
	return returnValue;
    }


    /**
     * Multiplies the two log values. 
     *
     * <p>Will check for overflow on the lowest bound and constrain
     * values to be no lower than -Double.MAX_VALUE.</p>
     *
     * <p>Will check for overflow on the highest bound and constrain
     * values to be no higher than Double.MAX_VALUE.</p>
     *
     *
     * @param val1 value in log domain to multiply
     * @param val2 value in log domain to multiply
     *
     * @return product of val1 and val2 in the log domain
     *
     */
    public final double multiply(double val1, double val2) {
	double returnValue = val1 + val2;
	boolean overflowOccurred = false;

	// [ EBG: alternative... test the sign of each of val1 and
	// val2 and, if needed, the result. If val1 and val2 have
	// different signs, then overflow isn't an issue. If they have
	// the same sign and the result also has the same sign, then
	// operation was successful. if not, then an overflow occured,
	// in which case we can signal or just return MAX_VALUE ]

	if (val1 > 0) {
	    if (val2 > (Double.MAX_VALUE - val1)) {
		overflowOccurred = true;
		returnValue = Double.MAX_VALUE;
	    }
	} else if (val1 < 0) {
	    if (val2 < ( -Double.MAX_VALUE - val1)) {
		overflowOccurred = true;
		returnValue = -Double.MAX_VALUE;
	    }
	}
	if (overflowOccurred) {
	    logger.info("********Overflow or underflow occurred "
			+ "while trying to log.multiply "
			+ val1 + " and " + val2);
	}
	return returnValue;
    }

    /**
     * Divides the two log values. 
     *
     * @param val1 value in log domain to multiply
     * @param val2 value in log domain to multiply
     *
     * @return division of val1 and val2 in the log domain
     *
     * TODO: Add check for underflow
     */
    public final double divide(double val1, double val2) {
	double returnValue = val1 - val2;
	return returnValue;
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
     * <p>Will check for underflow and constrain values to be no lower
     * than Double.MIN_VALUE.</p>
     *
     * <p>Will check for overflow and constrain values to be no higher
     * than Double.MAX_VALUE.</p>
     *
     * <p>Both underflow and overflow checks are actually performed by
     * the multiply method.</p>
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
	double returnValue;

	// difference is always a positive number
	if (val1 > val2) {
	    highestValue = val1;
	    difference = val1 - val2;
	} else {
	    highestValue = val2;
	    difference = val2 - val1;
	}
	return multiply(highestValue, addTable(difference));
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
    private final double addTableActualComputation(double index) {
	double innerSummation;

	// Negate index, since the derivation of this formula implies
	// the smallest number as a numerator, therefore the log of the
	// ratio is negative
	innerSummation = logToLinear(-index);
	innerSummation += 1.0f;
	return linearToLog(innerSummation);
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
     * base. This function is contructed as a table lookup.
     *
     * @param index the index into the addTable
     *
     * @return the value pointed to by index
     *
     * @throws IllegalArgumentException
     */
    private final double addTable(double index) 
	throws IllegalArgumentException {

	if (!useAddTable) {
	    return addTableActualComputation(index);
	} else {
	    int intIndex = (int) Math.rint(index);
	    // When adding two numbers, the highest one should be
	    // preserved, and therefore the difference should always
	    // be positive.
	    if (intIndex < 0) {
		throw new IllegalArgumentException("addTable index has " 
						   + "to be negative");
	    }
	    if (intIndex  >= theAddTable.length) {
		return 0.0;
	    } else {
		return theAddTable[intIndex];
	    }
	}
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
     * <p>It implements the subtraction as:</p>
     *
     * <p><b>log(a - b) = log(a) + log(1 - exp(log(b) - log(a)))</b></p>
     *
     * <p>No need to check for underflow/overflow.</p>
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
     * <p>This is a very slow way to do this, but this method should
     * rarely be used.</p>
     */
    public final double subtract(double minuend, double
	    subtrahend) throws IllegalArgumentException {
	double innerSummation;
	if (minuend < subtrahend) {
	    throw new IllegalArgumentException("Subtract results in log "
					       + "of a negative number: "
					       + minuend + " - " 
					       + subtrahend);
	}
	innerSummation = 1.0f;
	innerSummation -= logToLinear(subtrahend - minuend);
	return minuend + linearToLog(innerSummation);
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
    public final double lnToLog(double source) {
	return (source * inverseNaturalLogBase);
    }

    /**
     * Converts the source, which is a number in base 10, to a log value
     * which base is the LogBase of this LogMath.
     *
     * @param source the number in base Math.E to convert
     */
    public final double log10ToLog(double source) {
	return logToLog(source, 10.0, logBase);
    }

    /**
     * Converts the given log value into a base Math.E log value.
     *
     * @param logValue the log value to convert
     */
    public final double logToLn(double logValue) {
        return logValue * naturalLogBase;
    }


    /**
     * Converts the value from linear domain to log domain
     *
     * @param linearValue the value to be converted to log domain
     *
     * @return the value in log domain
     *
     * @throws IllegalArgumentException
     *
     */
    public final double linearToLog(double linearValue) 
	throws IllegalArgumentException {
	if (linearValue < 0.0) {
	    throw new IllegalArgumentException(
		    "linearToLog: param must be >= 0: " + linearValue);
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
	// return Math.pow(logBase, logValue);
	double returnValue;
	if (logValue < minLogValue) {
	    returnValue = getLogZero();
	} else if (logValue > maxLogValue) {
	    returnValue = Double.MAX_VALUE;
	} else {
	    returnValue = Math.exp(logToLn(logValue));
	}
	return returnValue;
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
     * Returns the one value in the log domain
     *
     * @return one value in the log domain
     */
    public final double getLogOne() {
	return logOne;
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
