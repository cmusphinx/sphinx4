/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;

import java.util.Iterator;
import java.util.Vector;

/**
 * A wrapper for the chain of front end processors. It is created by
 * passing in the first and last DataProcessor in the chain to the
 * constructor. Calling <code>setDataSource()</code> or
 * <code>setPredecessor()</code> on the FrontEnd will set the data
 * source from which the front end reads the data to perform signal
 * processing.
 */
public class FrontEnd extends BaseDataProcessor {

    private DataProcessor first;
    private DataProcessor last;

    private Vector signalListeners = new Vector();


    /**
     * Constructs a FrontEnd with the given first and last DataProcessors,
     * which encloses a chain of DataProcessors.
     *
     * @param firstProcessor first processor in the processor chain
     * @param lastProcessor last processor in the processor chain
     */
    public FrontEnd(DataProcessor firstProcessor,
                    DataProcessor lastProcessor) {
        this.first = firstProcessor;
        this.last = lastProcessor;
    }


    /**
     * Initializes this Front End.
     *
     * @param name         the name of this front end
     * @param frontEndName the name of the front-end pipeline this
     *                     front end is in
     * @param props        the SphinxProperties to use
     * @param predecessor  the predecessor of this Front End
     */
    public void initialize(String name, String frontEndName,
                           SphinxProperties props,
                           DataProcessor predecessor) {
        super.initialize(name, frontEndName, props, predecessor);
    }


    /**
     * Sets the source of data for this front end.
     * It basically sets the predecessor of the first DataProcessor
     * of this front end.
     *
     * @param dataSource the source of data 
     */
    public void setDataSource(DataProcessor dataSource) {
        first.setPredecessor(dataSource);
    }


    /**
     * Returns the processed Data output, basically calls
     * <code>getData()</code> on the last processor.
     *
     * @return an Data object that has been processed by this front end
     *
     * @throws DataProcessingException if a data processor error occurs
     */
    public Data getData() throws DataProcessingException {
        Data data = last.getData();
        
        // fire the signal listeners if its a signal
        if (data instanceof Signal) {
            fireSignalListeners((Signal) data);
        }

        return data;
    }


    /**
     * Sets the source of data for this front end.
     * It basically calls <code>setDataSource(dataSource)</code>.
     *
     * @param dataSource the source of data 
     */
    public void setPredecessor(DataProcessor dataSource) {
        setDataSource(dataSource);
    }

    
    /**
     * Finds the DataProcessor with the given name.
     *
     * @param name the name of the DataProcessor to find
     *
     * @return the DataProcessor with the given name, or null if no
     *         DataProcessor with the given name was found
     */
    public DataProcessor findDataProcessor(String processorName) {
        DataProcessor current = last;
        while (current != null) {
            if (current.getName().equals(processorName)) {
                return current;
            } else {
                current = current.getPredecessor();
            }
        }
        return null;
    }


    /**
     * Add a listener to be called when a feature with non-content
     * signal is detected.
     *
     * @param listener the listener to be added
     */
    public void addSignalListener(SignalListener listener) {
        signalListeners.add(listener);
    }


    /**
     * Removes a listener for features with non-content signals.
     *
     * @param listener the listener to be removed
     */
    public void removeSignalListener(SignalListener listener) {
        signalListeners.remove(listener);
    }


    /**
     * Fire all listeners for features with non-content signals.
     *
     * @param feature the feature with non-content signal
     */
    protected void fireSignalListeners(Signal signal) {
        Vector copy = (Vector) signalListeners.clone();
        for (Iterator i = copy.iterator(); i.hasNext(); ) {
            SignalListener listener = (SignalListener) i.next();
            listener.signalOccurred(signal);
        }
    }
}
