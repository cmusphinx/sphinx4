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

package edu.cmu.sphinx.decoder;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Date;
import java.net.URL;
import java.io.PrintStream;
import java.io.FileOutputStream;
import edu.cmu.sphinx.util.SphinxProperties;

/**
 * Attempts to optmize tuning parameters for sphinx 4.  Method of
 * tuning is described in the tuning props file
 */
public class AutoTuner {

    /** Property names */
    public final static String TP_OUTPUT_NAME = "outputFile";
    public final static String TP_TYPE = "type";
    public final static String TP_SPHINX_PROPERTIES = "propsFile";
    public final static String TP_BATCH_FILE = "batchFile";
    public final static String TP_PROPS_FILE = "propsFile";


    /** test types */
    public final static String TYPE_BRUTE_FORCE = "bruteForce";


    public final static String CONTEXT = "AutoTuner";

    private PrintStream outputFile;
    private Properties tunerProps;
    private SphinxProperties standardSphinxProperties;
    private boolean isKeyDumped = false;


    /**
     * Creats an autotuner with the given auto tumer properties
     *
     * @param tunerProps the name of the auto tuner properties (this
     * is not a sphinx property file).
     *
     * @throws IOException if an error occurs while reading the tuner
     * properties
     */
    public AutoTuner(String tunerProps) throws IOException {
        readTunerProperties(tunerProps);
    }


    /**
     * runs the tuner
     */
    public void tune() throws IOException {
        Properties tuningProperties = new Properties();
        open();
        Tuner tuner = createTuningChain();

        tuner.tune(tuningProperties);
        close();
    }


    /**
     * Main method of this Autotuner
     *
     * @param argv argv[0] : Tuner file
     *             argv[1] : a file listing all the audio files to decode
     */
    public static void main(String[] argv) {

        if (argv.length < 1) {
            System.out.println
                ("Usage: AutoTuner tunerFile");
            System.exit(1);
        }

        try {
            AutoTuner tuner = new AutoTuner(argv[0]);
            tuner.tune();
        } catch (IOException ioe) {
             System.out.println("Couldn't  run tuner: " + ioe);
        }
    }


    /**
     * Reads the tuner properties from the given URL
     *
     * @param name a string representing the URL to the tuning props
     *
     * @throws IOException if an error occurs while reading the props
     */
    private void readTunerProperties(String name) throws IOException {
        URL url = new URL(name);
	tunerProps = new Properties();
        InputStream is = url.openStream();
        tunerProps.load(is);
        is.close();

        String sphinxPropertiesFile =
            tunerProps.getProperty(TP_SPHINX_PROPERTIES, "s4.props");
        String pwd = System.getProperty("user.dir");

        System.out.println("Opening " + sphinxPropertiesFile);
        SphinxProperties.initContext
            (CONTEXT, new URL("file://" + pwd +  "/" + sphinxPropertiesFile));
	standardSphinxProperties 
            = SphinxProperties.getSphinxProperties(CONTEXT);
    }

    /**
     * Opens the output file
     */
    private void open() throws IOException {
        String outputFileName = 
            tunerProps.getProperty(TP_OUTPUT_NAME, "tuner.out");
        outputFile = new PrintStream(new
                FileOutputStream(outputFileName));
        outputFile.println("# Date: " + new Date());
    }


    /**
     * Closes the output file
     */
    private void close() throws IOException {
        outputFile.close();
    }

    /**
     * Creates the chain of tuner objects
     */
    private Tuner  createTuningChain() throws IOException {
        if (!tunerProps.getProperty(TP_TYPE,
                    TYPE_BRUTE_FORCE).equals(TYPE_BRUTE_FORCE)) {
            throw new IOException("unknown test type");
        }

        Tuner batchTuner = new BatchTuner();
        return batchTuner;
    }


    /**
     * Standard interface for tuning objects
     */
    private interface Tuner {
        void tune(Properties tuningParams) throws IOException ;
    }


    /**
     * A tuner that invokes that BatchDecoder
     */
    class BatchTuner implements Tuner {

        /**
         * Apply the set of tuning params to the sphinx properties and
         * run the batch decoder. print summary results to the output
         * file when done.
         */
        public void tune(Properties tuningParams) throws IOException {
            String batchFile = tunerProps.getProperty(TP_BATCH_FILE);

            applyProperties(tuningParams);
            BatchDecoder decoder = new
                BatchDecoder(standardSphinxProperties,
                    batchFile);

            decoder.decode();
            dumpResults(tuningParams, decoder.getBatchResults());
        }

        /**
         * Dumps the tuning results
         *
         * @param tuningParams the set of tuning params
         *
         * @param results the results
         */
        private void dumpResults(Properties tuningParams,
                BatchResults results) {

            List keys = getSortedKeys(tuningParams);

            if (!isKeyDumped) {
                isKeyDumped = true;
                int column = 0;
                for (Iterator i = keys.iterator(); i.hasNext(); ) {
                    String key = (String) i.next();
                    outputFile.print("#  column " +  (column++) + " " + key);
                }
                outputFile.print(" # column " + (column++) + " num words");
                outputFile.print(" # column " + (column++) + " WER%");
                outputFile.print(" # column " + (column++) + " tot errs");
                outputFile.print(" # column " + (column++) + " sub errs");
                outputFile.print(" # column " + (column++) + " ins errs");
                outputFile.print(" # column " + (column++) + " del errs");
                outputFile.print(" # column " + (column++) + " sent errs");
            }

            for (Iterator i = keys.iterator(); i.hasNext(); ) {
                String key = (String) i.next();
                String value = tuningParams.getProperty(key);
                outputFile.print(value);
                outputFile.print(" ");
            }
            outputFile.print(" " + results);
        }


        /**
         * Applies the set of tuning parameters to the set of sphinx
         * properties
         *
         * @param tuningParams the set of tuning parameters
         */
        private void applyProperties(Properties tuningParams) {
            Properties sp = standardSphinxProperties.getProperties();

            for (Iterator keys = tuningParams.keySet().iterator(); 
                    keys.hasNext(); ) {
                String key = (String) keys.next();
                sp.setProperty(key, (String) tuningParams.get(key));
            }
        }
    }

    /**
     * Gets a list of sorted property keys
     *
     * @param props a property set
     *
     * @return a list of sorted properti keys
     */
    private List getSortedKeys(Properties props) {
        List keys = new LinkedList(props.keySet());
        Collections.sort(keys);
        return keys;
    }

}
