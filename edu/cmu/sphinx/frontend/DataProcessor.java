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

import edu.cmu.sphinx.model.acoustic.AcousticModel;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;

import java.io.IOException;


/**
 * DataProcessor provides the common elements of all frontend data
 * processors, namely the name, context, timers, SphinxProperties,
 * and dumping. 
 */
public abstract class DataProcessor {


    /**
     * The name of this DataProcessor.
     */
    private String name;


    /**
     * The context of this DataProcessor.
     */
    private String context;


    /**
     * A Timer for timing processing.
     */
    private Timer timer = null;

    
    /**
     * Indicates whether to dump the processed Data
     */
    private boolean dump = false;


    /**
     * The SphinxProperties used by this DataProcessor
     */
    private SphinxProperties sphinxProperties = null;


    /**
     * Constructs a DataProcessor of the given name and at the given context. 
     *
     * @param name the name of this DataProcessor
     * @param context the context of this DataProcessor
     */
    protected DataProcessor(String name, String context) {
        this.name = name;
        this.context = context;
        this.timer = Timer.getTimer(context, name);
        this.sphinxProperties = SphinxProperties.getSphinxProperties(context);
    }


    /**
     * Returns the name of this DataProcessor.
     *
     * @return the name of this DataProcessor
     */
    public final String getName() {
        return name;
    }


    /**
     * Returns the context of this DataProcessor.
     *
     * @return the context of this DataProcessor
     */
    public final String getContext() {
        return context;
    }


    /**
     * Returns the SphinxProperties used by this DataProcessor.
     *
     * @return the SphinxProperties
     */
    public final SphinxProperties getSphinxProperties() {
        return sphinxProperties;
    }


    /**
     * Returns the Timer for metrics collection purposes. 
     *
     * @return the Timer
     */
    public final Timer getTimer() {
        return timer;
    }


    /**
     * Determine whether to dump the output for debug purposes.
     *
     * @return true to dump, false to not dump
     */
    public final boolean getDump() {
	return this.dump;
    }


    /**
     * Set whether we should dump the output for debug purposes.
     *
     * @param dump true to dump the output; false otherwise
     */
    public void setDump(boolean dump) {
	this.dump = dump;
    }


    /**
     * Returns the name of this DataProcessor.
     *
     * @return the name of this DataProcessor
     */
    public String toString() {
        return name;
    }


    /**
     * Returns true if this FrontEnd DataProcessor should use acoustic
     * model properties. This is a convenience method that basically
     * calls FrontEnd.useAcousticModelProperties().
     *
     * @return true if this DataProcessor should use acoustic model
     *    properties, false otherwise
     */
    private boolean useAcousticModelProperties() {
        return getSphinxProperties().getBoolean
            (FrontEnd.PROP_PREFIX + "useAcousticModelProperties", true);
    }


    /**
     * Returns the properties of the AcousticModel with the
     * same context is returned (or null if no such AcousticModel,
     * or if the AcousticModel has no properties).
     *
     * @return the properties of the AcousticModel
     */
    private SphinxProperties getAcousticProperties() throws IOException {
        AcousticModel am = AcousticModel.getAcousticModel(getContext());
        if (am != null) {
            return am.getProperties();
        } else {
            return null;
        }
    }


    /**
     * Returns the given acoustic property with the given name.
     * If no such acoustic property is found, returns the frontend
     * property with the same name.
     *
     * @param propertyName the name of the acoustic property
     * @param defaultValue the value to return if the property is not
     *    found
     *
     * @throws IOException if an I/O error occurs
     */
    public double getDoubleAcousticProperty(String propertyName, 
                                            double defaultValue) throws
    IOException {
        if (useAcousticModelProperties()) {
            return getAcousticProperties().getDouble
                (AcousticModel.PROP_PREFIX + propertyName, defaultValue);
        } else {
            return getSphinxProperties().getDouble
                (FrontEnd.PROP_PREFIX + propertyName, defaultValue);
        }
        
    }
    

    /**
     * Returns the given acoustic property with the given name.
     * If no such acoustic property is found, returns the frontend
     * property with the same name.
     *
     * @param propertyName the name of the acoustic property
     * @param defaultValue the value to return if the property is not
     *    found
     *
     * @throws IOException if an I/O error occurs
     */
    public float getFloatAcousticProperty(String propertyName,
                                          float defaultValue) throws
    IOException {
        if (useAcousticModelProperties()) {
            return getAcousticProperties().getFloat
                (AcousticModel.PROP_PREFIX + propertyName, defaultValue);
        } else {
            return getSphinxProperties().getFloat
                (FrontEnd.PROP_PREFIX + propertyName, defaultValue);
        }
        
    }


    /**
     * Returns the given acoustic property with the given name.
     * If no such acoustic property is found, returns the frontend
     * property with the same name.
     *
     * @param propertyName the name of the acoustic property
     * @param defaultValue the value to return if the property is not
     *    found
     *
     * @throws IOException if an I/O error occurs
     */
    public int getIntAcousticProperty(String propertyName,
                                      int defaultValue) throws
    IOException {
        if (useAcousticModelProperties()) {
            return getAcousticProperties().getInt
                (AcousticModel.PROP_PREFIX + propertyName, defaultValue);
        } else {
            return getSphinxProperties().getInt
                (FrontEnd.PROP_PREFIX + propertyName, defaultValue);
        }
        
    }


    /**
     * Returns the given acoustic property with the given name.
     * If no such acoustic property is found, returns the frontend
     * property with the same name.
     *
     * @param propertyName the name of the acoustic property
     * @param defaultValue the value to return if the property is not
     *    found
     *
     * @throws IOException if an I/O error occurs
     */
    public boolean getBooleanAcousticProperty(String propertyName, 
                                              boolean defaultValue) throws
    IOException {
        if (useAcousticModelProperties()) {
            return getAcousticProperties().getBoolean
                (AcousticModel.PROP_PREFIX + propertyName, defaultValue);
        } else {
            return getSphinxProperties().getBoolean
                (FrontEnd.PROP_PREFIX + propertyName, defaultValue);
        }        
    }
}
