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


package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.DataStartSignal;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;
import java.text.DecimalFormat;

import java.util.logging.Logger;


/**
 * Dumps the data
 */
public class DataDumper extends BaseDataProcessor {
    private int frameCount;
    private boolean enable;
    private DecimalFormat formatter = new DecimalFormat(
            " 0.00000E00;-0.00000E00");

    private static Logger logger = Logger.getLogger
        ("edu.cmu.sphinx.frontend.util.DataDumper");
    private final static String PROP_PREFIX = 
        "edu.cmu.sphinx.frontend.util.DataDumper.";
    
    /**
     * The Sphinx property that specifies whether data dumping is
     * enabled
     */
    public final static String PROP_ENABLE = PROP_PREFIX + "enable";

    /**
     * The default value of PROP_ENABLE.
     */
    public final static boolean PROP_ENABLE_DEFAULT = false;

    /**
     * Constructs a DataDumper 
     *
     * @param name the name of this DataDumper
     * @param frontEnd the frontEnd this DataDumper belongs to
     * @param props the SphinxProperties to read properties from
     * @param predecessor the predecessor DataProcessor of this
     * DataDumper
     *
     * @throws IOException if an I/O error occurs
     */
    public void initialize(String name, String frontEnd,
                           SphinxProperties props, DataProcessor predecessor) {
        super.initialize(name, frontEnd, props, predecessor);
        System.out.println("My name is " + name);
	setProperties(props);
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     *
     * @param props a SphinxProperties object specifying the properties values
     */
    private void setProperties(SphinxProperties props) {
	enable = props.getBoolean
            (getFullPropertyName(PROP_ENABLE),
             PROP_ENABLE_DEFAULT);
    }



    /**
     * Reads and returns the next Data object from this
     * DataProcessor, return null if there is no more audio data.
     *
     * @return the next Data or <code>null</code> if none is
     *         available
     *
     * @throws DataProcessingException if there is a data processing error
     */
    public Data getData() throws DataProcessingException {
	Data input = getPredecessor().getData();
        if (enable) {
            dumpData(input);
        }
        return input;
    }

    /**
     *  Dumps the given input data
     *
     * @param input the data to dump
     */
    private void dumpData(Data input) {
        if (input instanceof Signal) {
            System.out.println("Signal: " + input);
            if (input instanceof DataStartSignal) {
                frameCount = 0;
            }
        } else if (input instanceof DoubleData) {
            System.out.println("Frame " + frameCount);
            DoubleData dd = (DoubleData) input;
            double[] values = dd.getValues();
            for (int i = 0; i < values.length; i++) {
                System.out.print("  " + formatter.format(values[i]));
                if ((i+1) % 4 == 0) {
                    System.out.println();
                }
            }
            System.out.println();
        } else if (input instanceof FloatData) {
            System.out.println("Frame " + frameCount);
            FloatData fd = (FloatData) input;
            float[] values = fd.getValues();
            for (int i = 0; i < values.length; i++) {
                System.out.print("  " + formatter.format(values[i]));
                if ((i+1) % 4 == 0) {
                    System.out.println();
                }
            }
            System.out.println();
        }
        frameCount++;
    }
}

