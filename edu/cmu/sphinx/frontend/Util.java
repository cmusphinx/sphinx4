/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;


/**
 * Defines utility methods for the FrontEnd.
 */
public class Util {

    private static final int HEXADECIMAL = 1;
    private static final int SCIENTIFIC = 2;
    private static final int DECIMAL = 3;


    /**
     * DecimalFormat object to be used by all the methods.
     */
    private static DecimalFormat format = new DecimalFormat();
    

    private static int decimalIntegerDigits = 10;
    private static int decimalFractionDigits = 5;

    private static int floatScientificFractionDigits = 8;
    private static int doubleScientificFractionDigits = 8;


    /**
     * The number format to be used by *ArrayToString() methods.
     * The default is scientific.
     */
    private static int dumpFormat = SCIENTIFIC;


    /**
     * Static initialization of dumpFormat
     */
    static {
        String formatProperty = System.getProperty("frontend.util.dumpformat",
                                                   "SCIENTIFIC");
        if (formatProperty.compareToIgnoreCase("DECIMAL") == 0) {
            dumpFormat = DECIMAL;
        } else if (formatProperty.compareToIgnoreCase("HEXADECIMAL") == 0) {
            dumpFormat = HEXADECIMAL;
        } else if (formatProperty.compareToIgnoreCase("SCIENTIFIC") == 0) {
            dumpFormat = SCIENTIFIC;
        }
    }


    /**
     * Uninstantiable class.
     */
    private Util() {}


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
     * Converts a byte array into a double array. Each two consecutive
     * bytes in the byte array are converted into a double, and
     * becomes the next element in the double
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
    public static double[] byteToDoubleArray(byte[] byteArray, int offset,
                                             int length)
        throws ArrayIndexOutOfBoundsException {
        
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
     * Returns the string representation of the given double value in
     * normalized scientific notation. The <code>fractionDigits</code>
     * argument gives the number of decimal digits in the fraction
     * portion. For example, if <code>fractionDigits</code> is 4, then
     * the 123450 will be "1.2345e+05". There will always be two digits
     * in the exponent portion, and a plus or minus sign before the
     * exponent.
     *
     * @param number the double to convert
     * @param fractionDigits the number of digits in the fraction part,
     *    e.g., 4 in "1.2345e+05".
     *
     * @return the string representation of the double in scientific
     *    notation
     */
    private static String doubleToScientificString(double number,
                                                   int fractionDigits) {
        String formatter = "0.";
        for (int i = 0; i < fractionDigits; i++) {
            formatter += "0";
        }
        formatter += "E00";

        format.applyPattern(formatter);
        String formatted = format.format(number);

        int index = formatted.indexOf('E');
        if (formatted.charAt(index+1) != '-') {
            return formatted.substring(0, index+1) + "+" +
                formatted.substring(index+1);
        } else {
            return formatted;
        }
    }


    /**
     * Returns the string representation of the given short array.
     * The string will be in the form:
     * <pre>data.length data[0] data[1] ... data[data.length-1]</pre>
     *
     * @param data the short array to convert
     *
     * @return a string representation of the short array
     */
    public static String shortArrayToString(short[] data) {
        String dump = String.valueOf(data.length);
	for (int i = 0; i < data.length; i++) {
	    dump += (" " + data[i]);
	}
        return dump;
    }


    /**
     * Returns the given double array as a string.
     * The string will be in the form:
     * <pre>data.length data[0] data[1] ... data[data.length-1]</pre>where
     * <code>data[i]</code>.
     *
     * The doubles can be written as decimal, hexadecimal,
     * or scientific notation. In decimal notation, it is formatted by the
     * method <code>Util.formatDouble(data[i], 10, 5)</code>. Use
     * the System property <code>"frontend.util.dumpformat"</code> to
     * control the dump format (permitted values are "decimal",
     * "hexadecimal", and "scientific".
     *
     * @param data the double array to dump
     *
     * @return a string representation of the double array
     */
    public static String doubleArrayToString(double[] data) {
        return doubleArrayToString(data, dumpFormat);
    }


    /**
     * Returns the given double array as a string.
     * The dump will be in the form:
     * <pre>data.length data[0] data[1] ... data[data.length-1]</pre>where
     * <code>data[i]</code> is formatted by the method
     * <code>Util.formatDouble(data[i], 10, 5)</code>.
     *
     * @param data the double array to dump
     * @param format either HEXADECIMAL, SCIENTIFIC or DECIMAL
     *
     * @return a string representation of the double array
     */
    private static String doubleArrayToString(double[] data, int format) {
        String dump = String.valueOf(data.length);

        for (int i = 0; i < data.length; i++) {
            if (format == DECIMAL) {
                dump += (" " + formatDouble
                         (data[i], decimalIntegerDigits,
                          decimalFractionDigits));
            } else if (format == HEXADECIMAL) {
                long binary = Double.doubleToRawLongBits(data[i]);
                dump += (" 0x" + Long.toHexString(binary));
            } else if (format == SCIENTIFIC) {
                dump += (" " + doubleToScientificString
                         (data[i], doubleScientificFractionDigits));
            }
        }
        return dump;
    }        


    /**
     * Returns the given float array as a string. The string is of the
     * form:
     * <pre>data.length data[0] data[1] ... data[data.length-1]</pre>
     *
     * The floats can be written as decimal, hexadecimal,
     * or scientific notation. In decimal notation, it is formatted by the
     * method <code>Util.formatDouble(data[i], 10, 5)</code>. Use
     * the System property <code>"frontend.util.dumpformat"</code> to
     * control the dump format (permitted values are "decimal",
     * "hexadecimal", and "scientific".
     *
     * @param data the float array to dump
     *
     * @return a string of the given float array
     */
    public static String floatArrayToString(float[] data) {
        return floatArrayToString(data, dumpFormat);
    }


    /**
     * Returns the given float array as a string. The string is of the
     * form:
     * <pre>data.length data[0] data[1] ... data[data.length-1]</pre>
     *
     * @param data the float array to dump
     * @param format either DECIMAL, HEXADECIMAL or SCIENTIFIC
     *
     * @return a string of the given float array
     */
    private static String floatArrayToString(float[] data, int format) {
        String dump = String.valueOf(data.length);

        for (int i = 0; i < data.length; i++) {

            if (format == DECIMAL) {
                dump += (" " + formatDouble
                         ((double) data[i],
                          decimalIntegerDigits, decimalFractionDigits));
            } else if (format == HEXADECIMAL) {
                int binary = Float.floatToRawIntBits(data[i]);
                dump += (" 0x" + Integer.toHexString(binary));
            } else if (format == SCIENTIFIC) {
                dump += (" " + doubleToScientificString
                         ((double) data[i], floatScientificFractionDigits));
            }
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
        String formatter = "0.";
	for (int i = 0; i < fractionDigits; i++) {
	    formatter += "0";
	}

	format.applyPattern(formatter);
	String formatted = format.format(number);

	// pad preceding spaces before the number
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
	return result;
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


    /**
     * Returns the total amount of audio time (in seconds) represented by
     * the first frame to given frame. The first frame number is 0.
     *
     * @param frameNumber the given frame
     * @param properties the SphinxProperties object where information
     *    about window shift, window size, etc., can be retrieved
     *
     * @return the total audio time in seconds
     */
    public static float getAudioTime(int frameNumber, 
                                     SphinxProperties properties) {
        float audioTime = 0.0f;

        if (frameNumber >= 0) {
            float windowSizeInMs = properties.getFloat
                (FrontEnd.PROP_WINDOW_SIZE_MS, 25.625F);
            float windowShiftInMs = properties.getFloat
                (FrontEnd.PROP_WINDOW_SHIFT_MS, 10.0F);
            
            // calculate audio time in milliseconds
            audioTime = frameNumber * windowShiftInMs + windowSizeInMs;
            audioTime /= 1000;
        }

        return audioTime;
    }


    /**
     * Saves the given bytes to the given binary file.
     *
     * @param data the bytes to save
     * @param filename the binary file name
     */
    public static void bytesToFile(byte[] data, String filename) throws
                                     IOException {
        FileOutputStream file = new FileOutputStream(filename);
        file.write(data);
        file.close();
    }
}
