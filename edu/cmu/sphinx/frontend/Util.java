/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import java.text.DecimalFormat;


/**
 * Defines utility methods for the FrontEnd.
 */
public class Util {


    /**
     * Uninstantiable class.
     */
    private Util() {}


    /**
     * Returns the number of windows in the given array, given the windowSize
     * and windowShift.
     *
     * @param arraySize the size of the array
     * @param windowSize the window size
     * @param windowShift the window shift
     *
     * @return the number of windows
     */
    public static int getWindowCount(int arraySize, int windowSize,
				     int windowShift) {
	if (arraySize < windowSize) {
	    return 0;
	} else {
	    int windowCount = 1;
	    for (int windowEnd = windowSize;
		 windowEnd + windowShift <= arraySize;
		 windowEnd += windowShift) {
		windowCount++;
	    }
	    return windowCount;
	}
    }


    /**
     * Given an array size, a window size, and a window shift, return
     * the number of elements of the array that the window(s) occupy. For
     * example, if the array is of size 10, the window size is 3, and the
     * window shift is 2, then the total number of elements occupied equals
     * <code>2 + 2 + 2 + 3 = 9</code>.
     *
     * @param arraySize the length of the array
     * @param windowSize the window size
     * @param windowShift the window shift
     *
     * @return the number of occupied array elements
     */
    public static int getOccupiedElements(int arraySize, int windowSize,
					  int windowShift) {
	if (arraySize < windowSize) {
	    return 0;
	} else {
	    int windowEnd = windowSize;
	    while ((windowEnd + windowShift) <= arraySize) {
		windowEnd += windowShift;
	    }
	    return windowEnd;
	}
    }


    /**
     * Converts a byte array into a short array. Since a byte is 8-bits,
     * and a short is 16-bits, the returned short array will be half in
     * length than the byte array. If the length of the byte array is odd,
     * the length of the short array will be
     * <code>(byteArray.length - 1)/2</code>, i.e., the last byte is
     * discarded.
     *
     * @param byteArray a byte array
     * @param offset which byte to start from
     * @param length how many bytes to convert
     *
     * @return a short array, or <code>null</code> if byteArray is of zero
     *    length
     *
     * @throws java.lang.ArrayIndexOutOfBoundsException
     */
    public static short[] byteToShortArray
	(byte[] byteArray, int offset, int length)
	throws ArrayIndexOutOfBoundsException {

	if (0 < length && (offset + length) <= byteArray.length) {
	    int shortLength = length / 2;
	    short[] shortArray = new short[shortLength];
	    int temp;
	    for (int i = offset, j = 0; j < shortLength ; 
		 j++, temp = 0x00000000) {
		temp = (int) (byteArray[i++] << 8);
		temp |= (int) (0x000000FF & byteArray[i++]);
		shortArray[j] = (short) temp;
	    }
	    return shortArray;
	} else {
	    throw new ArrayIndexOutOfBoundsException
		("offset: " + offset + ", length: " + length
		 + ", array length: " + byteArray.length);
	}
    }


    /**
     * Converts a byte array into a double array. Since a byte is 8-bits,
     * and a short is 64-bits. Each two consecutive bytes in the byte array
     * are converted into a double, and becomes the next element in the double
     * array. As a result, the returned double array will be half in
     * length than the byte array. If the length of the byte array is odd,
     * the length of the double array will be
     * <code>(byteArray.length - 1)/2</code>, i.e., the last byte is
     * discarded.
     *
     * @param byteArray a byte array
     * @param offset which byte to start from
     * @param length how many bytes to convert
     *
     * @return a double array, or <code>null</code> if byteArray is of zero
     *    length
     *
     * @throws java.lang.ArrayIndexOutOfBoundsException
     */
    public static double[] byteToDoubleArray (byte[] byteArray, int offset, int length) throws ArrayIndexOutOfBoundsException {
        
        if (0 < length && (offset + length) <= byteArray.length) {
            int doubleLength = length / 2;
            double[] doubleArray = new double[doubleLength];
	    int temp;
	    for (int i = offset, j = 0; j < doubleLength ; 
		 j++, temp = 0x00000000) {
		temp = (int) (byteArray[i++] << 8);
		temp |= (int) (0x000000FF & byteArray[i++]);
		doubleArray[j] = (double) temp;
	    }
	    return doubleArray;
	} else {
	    throw new ArrayIndexOutOfBoundsException
		("offset: " + offset + ", length: " + length
		 + ", array length: " + byteArray.length);
	}
    }


    /**
     * Convert the two bytes starting at the given offset to a short
     *
     * @param byteArray the byte array
     * @param offset where to start
     *
     * @return a short
     *
     * @throws java.lang.ArrayIndexOutOfBoundsException
     */
    public static short bytesToShort(byte[] byteArray, int offset)
	throws ArrayIndexOutOfBoundsException {
	short result = (short) 
	    ((byteArray[offset++] << 8) |
	     (0x000000FF & byteArray[offset]));
	return result;
    }


    /**
     * Dumps the given short array as a line to stdout.
     * The dump will be in the form:
     * <pre>description data[0] data[1] ... data[data.length]</pre>
     *
     * @param data the short array to dump
     * @param description some comment notes
     */
    public static String dumpShortArray(short[] data, String description) {
        String dump = (description + " " + data.length);
	for (int i = 0; i < data.length; i++) {
	    dump += (" " + data[i]);
	}
        return dump;
    }
	


    /**
     * Dumps the given double array as a string.
     * The dump will be in the form:
     * <pre>description data[0] data[1] ... data[data.length]</pre>where
     * <code>data[i]</code> is formatted by the method
     * <code>Util.formatDouble(data[i], 9, 5)</code>.
     *
     * @param data the double array to dump
     * @param description some comment notes
     *
     * @return a string of the dump
     */
    public static String dumpDoubleArray(double[] data, String description) {
	String dump = (description + " " + data.length);
	for (int i = 0; i < data.length; i++) {
	    dump += (" " + formatDouble(data[i], 9, 5));
	}
        return dump;
    }


    /**
     * Returns a formatted string of the given number, with
     * the given numbers of digit space for the integer and fraction parts.
     * If the integer part has less than <code>integerDigits</code> digits,
     * spaces will be prepended to it. If the fraction part has less than
     * <code>fractionDigits</code>, spaces will be appended to it.
     * Therefore, <code>formatDouble(12345.6789, 6, 6)</code> will give
     * the string <pre>" 12345.6789  "</pre> (one space before 1, two spaces
     * after 9).
     *
     * @param number the number to format
     * @param integerDigits the length of the integer part
     * @param fractionDigits the length of the fraction part
     *
     * @return a formatted number
     */
    public static String formatDouble(double number, int integerDigits,
				      int fractionDigits) {
        String formatter = "";
	for (int i = 0; i < integerDigits; i++) {
	    formatter += "#";
	}
	formatter += ".";
	for (int i = 0; i < fractionDigits; i++) {
	    formatter += "#";
	}

	DecimalFormat format = new DecimalFormat();
	format.applyPattern(formatter);
	String formatted = format.format(number);

	// pad preceding spaces and trailing zeroes
	int dotIndex = formatted.indexOf('.');
	if (dotIndex == -1) {
	    formatted += ".";
	    dotIndex = formatted.length() - 1;
	}
	String result = "";
	for (int i = dotIndex; i < integerDigits; i++) {
	    result += " ";
	}
	result += formatted;
	int fractionLength = formatted.length() - dotIndex - 1;
	for (int i = fractionLength; i < fractionDigits; i++) {
	    result += "0";
	}
	return result;
    }


    /**
     * Dumps the given float array as a string.
     *
     * @param data the float array to dump
     * @param name name of the array
     *
     * @return a string of the given float array
     */
    public static String dumpFloatArray(float[] data, String name) {
        String dump = (name + " " + data.length);
        for (int i = 0; i < data.length; i++) {
            dump += (" " + formatDouble
			     ((new Float(data[i])).doubleValue(), 9, 5));
        }
        return dump;
    }


    /**
     * Dumps the given FeatureFrame as a string.
     *
     * @param featureFrame the FeatureFrame to dump
     *
     * @return a string dump of the given FeatureFrame
     */
    public static String dumpFeatureFrame(FeatureFrame featureFrame) {
	Feature[] features = featureFrame.getFeatures();
	String dump = ("FEATURE_FRAME " + features.length + "\n");
        for (int i = 0; i < features.length; i++) {
	    dump += dumpFloatArray(features[i].getFeatureData(), "FEATURE");
	}
        return dump;
    }


    /**
     * Returns the number of samples per window given the sample rate
     * (in Hertz) and window size (in milliseconds).
     *
     * @param sampleRate the sample rate in Hertz (i.e., frequency per
     *     seconds)
     * @param windowSizeInMs the window size in milliseconds
     *
     * @return the number of samples per window
     */
    public static int getSamplesPerWindow(int sampleRate, 
                                          float windowSizeInMs) {
        return (int) ( ((float) sampleRate) * windowSizeInMs / 1000);
    }
    
    
    /**
     * Returns the number of samples in a window shift given the sample
     * rate (in Hertz) and the window shift (in milliseconds).
     *
     * @param sampleRate the sample rate in Hertz (i.e., frequency per
     *     seconds)
     * @param windowShiftInMs the window shift in milliseconds
     *
     * @return the number of samples in a window shift
     */
    public static int getSamplesPerShift(int sampleRate,
                                         float windowShiftInMs) {
        return (int) (((float) sampleRate) * windowShiftInMs / 1000);
    }
}
