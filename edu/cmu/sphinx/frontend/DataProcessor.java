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


/**
 * DataProcessor contains the common elements of all frontend data
 * processors, namely the name, context, predecessor, timers, and
 * SphinxProperties.
 */
public interface DataProcessor {

    /**
     * Initializes this DataProcessor.
     *
     * @param name the name of this DataProcessor
     * @param pipelineName the name of the front-end pipeline this
     *                     DataProcessor is in
     * @param sphinxProperties the SphinxProperties to use
     * @param predecessor the predecessor of this DataProcessor
     */
    public void initialize(String name, String frontEndName,
                           SphinxProperties sphinxProperties,
                           DataProcessor predecessor);
    
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
    public String getName();

    /**
     * Returns the predecessor DataProcessor.
     *
     * @return the predecessor
     */
    public DataProcessor getPredecessor();

    /**
     * Sets the predecessor DataProcessor. This method allows dynamic
     * reconfiguration of the front end.
     *
     * @param predecessor the new predecessor of this DataProcessor
     */
    public void setPredecessor(DataProcessor predecessor);
}
