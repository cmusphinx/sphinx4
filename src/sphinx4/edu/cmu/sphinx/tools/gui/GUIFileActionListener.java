/*
 * GUIFileActionListener.java
 *
 * Created on November 10, 2006, 4:33 PM
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
import edu.cmu.sphinx.tools.gui.util.ConfigurableUtilException;

/**
 * This interface represents the super class of the GUI panels,
 * It is used for synchronizing the GUI panel data and data in the model
 *
 * @author Ariani
 */
public interface GUIFileActionListener {
    /**
     * update GUI with new data 
     * 
     * @param cp <code>ConfigProperties</code> with new data
     */
    public void update (ConfigProperties cp);
    
    /**
     * save the data from GUI 
     * @param cp the holder of new data
     */
    public void saveData(ConfigProperties cp) throws GUIOperationException;
    
    /** 
     * clear all data in the GUI
     */
    public void clearAll();
    
    /**
     * model has just been reloaded
     */
    public void modelRefresh() throws ConfigurableUtilException;
}
