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
     * @param offset which bytes to start from
     * @param length how many bytes to convert
     *
     * @return a short array, or <code>null</code> if byteArray is of zero
     *    length
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
     * Convert the two bytes starting at the given offset to a short
     *
     * @param byteArray the byte array
     * @param offset where to start
     *
     * @return a short
     */
    public static short bytesToShort(byte[] byteArray, int offset)
	throws ArrayIndexOutOfBoundsException {
	short result = (short) 
	    ((byteArray[offset++] << 8) |
	     (0x000000FF & byteArray[offset]));
	return result;
    }


    /**
     * Dumps the given short array.
     *
     * @param description some comment notes
     */
    public static void dumpShortArray(short[] data, String description) {
	System.out.print(description + " " + data.length);
	for (int i = 0; i < data.length; i++) {
	    System.out.print(" " + data[i]);
	}
	System.out.println();
    }
	


    /**
     * Dumps the given double array.
     *
     * @param description some comment notes
     */
    public static void dumpDoubleArray(double[] data, String description) {
	System.out.print(description + " " + data.length);
	for (int i = 0; i < data.length; i++) {
	    System.out.print(" " + formatDouble(data[i], 9, 5));
	}
	System.out.println();
    }


    private static String formatDouble(double number, int integerDigits,
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
}
