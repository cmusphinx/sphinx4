/*
 * Copyright 2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;


/**
 * An abstract DataProcessor implementing elements common to all
 * concrete DataProcessors, such as name, front end name, SphinxProperties,
 * predecessor, and timer.
 */
public abstract class BaseDataProcessor implements DataProcessor {

    private String name;
    private String frontEndName;
    private SphinxProperties props;
    private DataProcessor predecessor;
    private Timer timer;

    /**
     * Initializes this DataProcessor.
     *
     * @param name the name of this DataProcessor
     * @param frontEndName the name of the front-end pipeline this
     *                     DataProcessor is in
     * @param sphinxProperties the SphinxProperties to use
     * @param predecessor the predecessor of this DataProcessor
     */
    public void initialize(String name, String frontEndName,
                           SphinxProperties sphinxProperties,
                           DataProcessor predecessor) {
        this.name = name;
        this.frontEndName = frontEndName;
        this.props = sphinxProperties;
        this.predecessor = predecessor;
        this.timer = Timer.getTimer(name, props.getContext());
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
     * Returns the name of this DataProcessor.
     *
     * @return the name of this DataProcessor
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the name of the front end this DataProcessor is in.
     *
     * @return the name of the front end this DataProcessor is in.
     */
    public String getFrontEndName() {
        return frontEndName;
    }

    /**
     * Returns the context of this BaseDataProcessor.
     *
     * @return the context
     */
    public String getContext() {
        return props.getContext();
    }

    /**
     * Returns the SphinxProperties.
     *
     * @return the SphinxProperties this DataProcessor uses
     */
    public SphinxProperties getSphinxProperties() {
        return props;
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
     * Returns the full property name string. If the front end name
     * is not null, then the returned string is
     * <i>getFrontEndName();propertyName</i>.
     * If the front end name is null, then the returned string is propertyName.
     *
     * @param propertyName the original property name
     *
     * @return the original property name or 
     * <i>getFrontEndName();propertyName</i>
     */
    public String getFullPropertyName(String propertyName) {
        return (getFrontEndName() == null ? propertyName :
                (getFrontEndName() + ";" + propertyName));
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
