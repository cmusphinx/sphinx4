/*
 * GUIWriter.java
 *
 * Created on October 30, 2006, 4:12 PM
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
import edu.cmu.sphinx.tools.gui.ConfigProperties;

import java.io.File;

/**
 * This class is the general type of all type of GUI Writers.
 * All types of writers should be a subclass of this type
 *
 * @author Ariani
 */
public interface GUIWriter {
    
    /**
     * this method will write the configuration values to the output 
     *
     * @param configProp <code>ConfigProperties</code> that keeps the values
     * @param fFile Output file
     * @throws <code>GUIWriterException</code> when there is error in the writing process 
     */
    public boolean writeOutput(ConfigProperties configProp, File fFile) throws GUIWriterException;
    
    /**
     * this method will return the configuration values as text
     *
     * @param configProp <code>ConfigProperties</code> that keeps the values
     * @return String configuration values as text
     * @throws <code>GUIWriterException</code> when there is error in the writing process 
     */
    public String getOutput(ConfigProperties configProp) throws GUIWriterException;
}
