/*
 * GUIMediator.java
 *
 * Created on November 9, 2006, 4:24 PM
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

import edu.cmu.sphinx.tools.gui.reader.XMLConfigReader;
import edu.cmu.sphinx.tools.gui.reader.GUIReaderException;
import edu.cmu.sphinx.tools.gui.writer.GUIWriterException;
import edu.cmu.sphinx.tools.gui.writer.XMLConfigWriter;
import edu.cmu.sphinx.tools.gui.util.ModelBuilder;
import edu.cmu.sphinx.tools.gui.util.ConfigurableUtilException;

import java.util.List;
import java.util.ArrayList;
import java.io.File;


/**
 * This is one of the important classes that coordinates between GUI and model
 * Its main operations are : 
 * 1. to start and initialize the GUI and Sphinx model
 * 2. drive the input - output operation, and 
 * 3. to retrieve the most updated data from GUI or to update GUI of the new loaded data
 *
 * @author Ariani
 */
public class GUIMediator {
    
    public static final String OPEN = "open";
    public static final String EXIT = "exit";
    public static final String NEW = "new";
    public static final String SAVE = "save";
    public static final String REFRESH = "refresh_model";
    public static final String SHOW_CONFIG = "show_config";
        
    private MainJFrame _mainJF;
    private final List<GUIFileActionListener> _panelList;
    private final XMLConfigReader _xmlReader;
    private final XMLConfigWriter _xmlWriter;
    private ModelBuilder _mb;

    
    /** 
     * Creates a new instance of GUIMediator 
     */
    public GUIMediator() throws ConfigurableUtilException{
        _panelList = new ArrayList<GUIFileActionListener>(); //for the GUI panels
        
        // load reader and writer
        _xmlReader = XMLConfigReader.getInstance();
        _xmlWriter = XMLConfigWriter.getInstance();
                
        // NOTE: the model should be created before the GUI components,
        // so that GUI TabPenel can be created based on model        
        loadModel();
        
        // create main GUI frame        
        _mainJF = new MainJFrame(this,_mb.getGroups());
        
    }
    
    // part of the constuctor operation - initialize and load Sphinx model
    private void loadModel() throws ConfigurableUtilException{
        _mb = ModelBuilder.getInstance();            
        
        // NOTE: the model should be created before the GUI components,
        // so that GUI TabPenel can be created based on model
        _mb.refresh();       
        // _mb.printModel(); // for debugging
    }
    
    /**
     * A public function to obtain reference to the ModelBuilder, 
     * that holds the complete set of classes and groups in the model
     * 
     * @return ModelBuilder
     **/
    public ModelBuilder getModelBuilder(){
        return _mb;
    }
    
    /**
     * Used by GUI panel to register as one of the notified clients
     *
     * @param c the registering class must implement <code>GUIFileActionListener</code>
     */
    public void registerPanel(GUIFileActionListener c){
        _panelList.add(c);
        
    }
 
    /**
     * Start the GUI -  after load operations is completed successfully
     */
    public void execute(){
        _mainJF.setSize(900,700);
        _mainJF.setLocationRelativeTo(null);
        _mainJF.setVisible(true);
        
        
    }
    
    // read the file and update all GUI panels
    private void updateList (File fFile) throws GUIReaderException, GUIWriterException {
        ConfigProperties cp = _xmlReader.read(fFile);
        _mb.update(cp);
        if (_panelList != null) {
            for (GUIFileActionListener listener : _panelList) {
                listener.update(cp);
            }
         }
    }
        
    
    private void saveToFile(File fFile) throws GUIReaderException, GUIWriterException {
        ConfigProperties cp = new ConfigProperties(); 
        if (readList(cp))
            _xmlWriter.writeOutput(cp,fFile);
    }
    
    /** read the GUI entries and save changes to ConfigProperties */
    private boolean readList ( ConfigProperties cp) throws GUIReaderException, GUIWriterException {
        try {
            _mb.saveData(cp);
            
            if (_panelList != null) {
                for (GUIFileActionListener listener : _panelList) {
                    listener.saveData(cp);
                }                
               return true;
            }
            return true;
        } catch (GUIOperationException oe) {
            return false;
        }
    }
    
    /** inform GUI panels to clear all configuration data
     */
    private void clearAll() {
        _mb.clearAll();
        if (_panelList != null) {
            for (GUIFileActionListener listener : _panelList) {
                listener.clearAll();
            }
        }        
    }
    
    /**refresh the Sphinx model in Model Builder and GUI Panels
     */
    private void refreshModel()throws ConfigurableUtilException{
        _mb.modelRefresh();
        _mb.printModel();
        _mainJF.addTextPanels(_mb.getGroups());
    }
    
    /**
     * all action will call this method, with its specific command
     *
     * @param command Action to be performed
     * @param fFile   File to open/save to
     * @throws GUIReaderException, GUIWriterException
     */
    public void action(String command,File fFile) throws GUIReaderException, GUIWriterException {  
        if (command.equalsIgnoreCase(OPEN))
            updateList(fFile);
        else if (command.equalsIgnoreCase(SAVE))
            saveToFile(fFile);
    }
    
    /**
     * action that needs String return, with its specific command
     *
     * @param command Action to be performed
     * @param outputJTextArea text area to display the output
     * @throws GUIReaderException, GUIWriterException
     */
    public void action(String command, javax.swing.JTextArea outputJTextArea) 
            throws GUIWriterException, GUIReaderException {
        ConfigProperties cp = new ConfigProperties();
        if (readList(cp)) {
            String output = _xmlWriter.getOutput(cp);
            outputJTextArea.setText(output);
        }
    }
    
    /** 
     * action command that does not involve <code>File</code> operation
     *
     * @throws ConfigurableUtilException when there's error while reloading model
     */
    public void action(String command) throws ConfigurableUtilException {
        if (command.equalsIgnoreCase(NEW))
            clearAll();        
        else if (command.equalsIgnoreCase(REFRESH))
            refreshModel();
    }
}
