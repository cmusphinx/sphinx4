/*
 * Copyright 2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.Registry;


/**
 * An abstract DataProcessor implementing elements common to all
 * concrete DataProcessors, such as name, predecessor, and timer.
 */
public  abstract class BaseDataProcessor implements DataProcessor {
    private String name;
    private DataProcessor predecessor;
    private Timer timer;
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        setName(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
    }
    /**
     * Returns the processed Data output.
     *
     * @return an Data object that has been processed by this DataProcessor 
     *
     * @throws DataProcessingException if a data processor error occurs
     */
    public abstract Data getData() throws DataProcessingException;
    
    
    
    /**
     * Initializes this DataProcessor. This is typically called after the
     * DataProcessor has been configured.
     * 
     */
    public  void initialize() {
        this.timer = Timer.getTimer(name); 
    }

    /**
     * Returns the name of this DataProcessor.
     *
     * @return the name of this DataProcessor
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the name for this front end
     *
     * @param name the name
     */
     private void setName(String name) {
        this.name = name;
    }


    /**
     * Returns the predecessor DataProcessor.
     *
     * @return the predecessor
     */
    public DataProcessor getPredecessor() {
        return predecessor;
    }

    /**
     * Returns the timer this DataProcessor uses.
     *
     * @return the timer
     */
    public Timer getTimer() {
        return timer;
    }

    /**
     * Sets the predecessor DataProcessor. This method allows dynamic
     * reconfiguration of the front end.
     *
     * @param predecessor the new predecessor of this DataProcessor
     */
    public void setPredecessor(DataProcessor predecessor) {
        this.predecessor = predecessor;
    }

    /**
     * Returns the name of this BaseDataProcessor.
     *
     * @return the name of this BaseDataProcessor
     */
    public String toString() {
        return name;
    }

}
