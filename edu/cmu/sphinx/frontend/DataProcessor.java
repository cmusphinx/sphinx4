/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Timer;


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
}
