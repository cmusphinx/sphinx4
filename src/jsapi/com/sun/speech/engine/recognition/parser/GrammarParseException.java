 /**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine.recognition.parser;

public class GrammarParseException extends Exception
{

	private static final long serialVersionUID = 1L;
	public int lineNumber;
	public int charNumber;
	public String message;
	public String details;

	public GrammarParseException (int lineNumber, int charNumber, String message, String details) {
		this.lineNumber = lineNumber;
		this.charNumber = charNumber;
		this.message = message;
		this.details = details;
	}
	public GrammarParseException (String message) {
     	this.message = message;
    }
}
