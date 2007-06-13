/*
 * GUIReader.java
 *
 * Created on October 30, 2006, 9:49 PM
 *
 *  Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui.reader;
import edu.cmu.sphinx.tools.gui.ConfigProperties;

import java.io.File;

/**
 * This class is an abstraction for all reader classes
 *
 * @author Ariani
 */
public interface GUIReader {
      
    /**
     * read method that must be implemented by all readers
     *
     * @param fFile     the file to be read from
     * @return ConfigProperties     the object that holds all the result
     * @throws GUIReaderException   when any error occurs during operation
     */
    public ConfigProperties read(File fFile) throws GUIReaderException;
    
}
