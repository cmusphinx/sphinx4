/**
 * Copyright 2001 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.util;

import java.io.PrintWriter;

/**
 * Provides a set of generic utilities
 */
public class Utilities {

    // Unconstructable.
    private Utilities() {}

    /**
     * Returns a string with the given number of
     * spaces.
     *
     * @param padding the number of spaces in the string
     *
     * @return a string of length 'padding' containg only the SPACE
     * char.
     */
    public static String pad(int padding) {
	if (padding > 0) {
	    StringBuffer sb = new StringBuffer(padding);
	    for (int i = 0; i < padding; i++) {
		sb.append(' ');
	    }
	    return sb.toString();
	 } else {
	     return "";
	 }
    }

    /**
     * Pads with spaces or truncates the given string to guarantee that it is
     * exactly the desired length.
     *
     * @param string the string to be padded
     * @param minLength the desired length of the string
     *
     * @return a string of length conntaining string
     * padded with whitespace or truncated
     */
    public static String pad(String string, int minLength) {
	String result = string;
	int pad = minLength - string.length();
	if (pad > 0) {
	    result =  string + pad(minLength - string.length());
	} else if (pad < 0) {
	    result = string.substring(0, minLength);
	}
	return result;
    }

    
    /**
     * Dumps padded text. This is a simple tool for helping dump text 
     * with padding to a Writer.
     *
     * @param pw the stream to send the output
     * @param padding the number of spaces in the string
     * @param string the string to output
     */
    public static void dump(PrintWriter pw, int padding, String string) {
	pw.print(pad(padding));
	pw.println(string);
    }

}

  
