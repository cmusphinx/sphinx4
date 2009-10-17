/*
 * GUIOperationException.java
 *
 * Created on November 15, 2006, 4:30 PM
 *
 * Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui;

/**
 * This exception is thrown when there is any error during GUI operations
 *
 * @author Ariani
 */
public class GUIOperationException extends java.lang.Exception {
    
    public final static int EXCEPTION_GLOBAL = 1;
    public final static int EXCEPTION_DECODER = 2;
    public final static int EXCEPTION_LINGUIST = 3;
    public final static int EXCEPTION_FRONTEND = 4;
    public final static int EXCEPTION_MISC = 5;
    
    
    private final int _mode;
    
    
    /**
     * Creates a new instance of <code>GUIOperationException</code> with detail message
     * and mode as the type of error
     */
    public GUIOperationException(int mode, String msg) {
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
