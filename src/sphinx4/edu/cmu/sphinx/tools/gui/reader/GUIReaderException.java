/*
 * GUIReaderException.java
 *
 * Created on October 31, 2006, 11:50 AM
 *
 * Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui.reader;

/**
 * This class is thrown when there is error during reading operations
 *
 * @author Ariani
 */
public class GUIReaderException extends java.lang.Exception {
    
    public final static int EXCEPTION_OTHER = 1;
    public final static int EXCEPTION_IO = 2;
    public final static int EXCEPTION_NO_FILENAME = 3;
    public final static int EXCEPTION_FILE_NOT_FOUND = 4;
    public final static int EXCEPTION_INVALID_TYPE = 5;
    
    private final int _mode;
    
    /**
     * Constructs an instance of <code>GUIReaderException</code> with the specified detail message.
     *
     * @param msg the detail message.
     * @param mode the type of error that occurs
     */
    public GUIReaderException(String msg, int mode) {
        super(msg);
        _mode = mode;
    }
    
    /**
     * @return Mode of this exception, which explains the type of exception 
     */
    public int getErrorCode(){
        return _mode;
    }
}
