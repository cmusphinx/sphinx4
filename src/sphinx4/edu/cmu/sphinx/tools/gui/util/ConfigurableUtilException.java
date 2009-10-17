/*
 * ConfigurableUtilException.java
 *
 * Created on December 1, 2006, 4:31 PM
 *
 * Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui.util;

/**
 * This exception class is thrown when there is an error in the operation of 
 * <code>ModelBuilder, ConfigurableComponent</code, and 
 * <code>ConfigurableProperty</code>
 *
 * @author Ariani
 */
public class ConfigurableUtilException extends java.lang.Exception{
    
    public final static int UTIL_COMPONENT = 1;  // "Configurable Component";
    public final static int UTIL_BUILDER = 2;  //"Sphinx Model Builder";
    public final static int UTIL_INIT = 3;  //"System scanner initialization";
    public final static int UTIL_PATHHACKER = 4; //"Classpath loader error"
    
    private final int _mode;
    
    /** 
     * Creates a new instance of <code>ConfigurableUtilException</code> with the 
     * specific mode that describe the reason, and message
     *
     * @param msg Error message
     * @param mode original cause of the exception
     */
    public ConfigurableUtilException(String msg, int mode) {
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
