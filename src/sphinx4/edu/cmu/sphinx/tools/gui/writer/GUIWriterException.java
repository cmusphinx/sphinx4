/*
 * GUIWriterException.java
 *
 * Created on October 31, 2006, 11:46 AM
 * 
 * Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui.writer;

/**
 * This exception is thrown when there is an error in the writing process to output
 *
 * @author Ariani
 */
public class GUIWriterException extends java.lang.Exception {
    
    public final static int EXCEPTION_OTHER = 1;
    public final static int EXCEPTION_IO = 2;
    public final static int EXCEPTION_NO_FILENAME = 3;
    public final static int EXCEPTION_INVALID_TYPE = 4;
            
    private final int _exceptionMode;
    
   
    /**
     * Constructs an instance of <code>GUIWriterException</code> with the 
     * specified detail message and mode as the type of error
     *
     * @param msg the detail message.
     * @param mode Type of error
     */
    public GUIWriterException(String msg, int mode) {
        super(msg);
        _exceptionMode = mode;
        
    }
    
    /**
     * @return Mode of this exception, which explains the type of exception 
     */
    public int getErrorCode(){
        return _exceptionMode;
    }
}
