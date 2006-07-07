/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.frontend.util;

import java.text.DecimalFormat;
import java.util.logging.Logger;
import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * Dumps the data
 */
public class DataDumper extends BaseDataProcessor {
    /**
     * The Sphinx property that specifies whether data dumping is enabled
     */
    public final static String PROP_ENABLE = "enable";
    /**
     * The default value of PROP_ENABLE.
     */
    public final static boolean PROP_ENABLE_DEFAULT = true;
    /**
     * The Sphinx property that specifies the format of the output.
     */
    public final static String PROP_OUTPUT_FORMAT = "outputFormat";
    /**
     * The default value of PROP_OUTPUT_FORMAT.
     */
    public final static String PROP_OUTPUT_FORMAT_DEFAULT = "0.00000E00;-0.00000E00";
    /**
     * The Sphinx property that enables the output of signals.
     */
    public final static String PROP_OUTPUT_SIGNALS = "outputSignals";
    /**
     * The default value of PROP_OUTPUT_SIGNALS.
     */
    public final static boolean PROP_OUTPUT_SIGNALS_DEFAULT = true;

    // --------------------------
    // Configuration data
    // --------------------------
    private int frameCount;
    private boolean enable;
    private boolean outputSignals;
    private DecimalFormat formatter;
    private  Logger logger;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        registry.register(PROP_ENABLE, PropertyType.BOOLEAN);
        registry.register(PROP_OUTPUT_FORMAT, PropertyType.STRING);
        registry.register(PROP_OUTPUT_SIGNALS, PropertyType.BOOLEAN);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        logger = ps.getLogger();
        enable = ps.getBoolean(PROP_ENABLE, PROP_ENABLE_DEFAULT);
        String format = ps.getString(PROP_OUTPUT_FORMAT,
                PROP_OUTPUT_FORMAT_DEFAULT);
        formatter = new DecimalFormat(format);
        outputSignals = ps.getBoolean(PROP_OUTPUT_SIGNALS,
                PROP_OUTPUT_SIGNALS_DEFAULT);
    }

    /**
     * Constructs a DataDumper
     */
    public void initialize() {
        super.initialize();
    }

    /**
     * Reads and returns the next Data object from this DataProcessor, return
     * null if there is no more audio data.
     * 
     * @return the next Data or <code>null</code> if none is available
     * 
     * @throws DataProcessingException
     *                 if there is a data processing error
     */
    public Data getData() throws DataProcessingException {
        Data input = getPredecessor().getData();
        if (enable) {
            dumpData(input);
        }
        return input;
    }

    /**
     * Dumps the given input data
     * 
     * @param input
     *                the data to dump
     */
    private void dumpData(Data input) {
        if (input instanceof Signal) {
            if (outputSignals) {
                System.out.println("Signal: " + input);
                if (input instanceof DataStartSignal) {
                    frameCount = 0;
                }
            }
        } else if (input instanceof DoubleData) {
            DoubleData dd = (DoubleData) input;
            double[] values = dd.getValues();
            System.out.print("Frame " + values.length );
            for (int i = 0; i < values.length; i++) {
                System.out.print(" " + formatter.format(values[i]));
            }
            System.out.println();
        } else if (input instanceof FloatData) {
            FloatData fd = (FloatData) input;
            float[] values = fd.getValues();
            System.out.print("Frame " + values.length );
            for (int i = 0; i < values.length; i++) {
                System.out.print(" " + formatter.format(values[i]));
            }
            System.out.println();
        }
        frameCount++;
    }
}
