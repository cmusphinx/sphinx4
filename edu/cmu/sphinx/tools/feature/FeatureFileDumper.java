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


package edu.cmu.sphinx.tools.feature;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FrontEndFactory;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.util.SphinxProperties;

/**
 * This program takes in an audio file, does frontend signal processing to it,
 * and then dumps the resulting Feature into a separate file.
 *
 * This program takes three arguments:
 *
 * 1. propsFile - the Sphinx properties file
 * 2. audioFile - the name of the audio file
 * 3. outputFile - the name of the output file
 */
public class FeatureFileDumper {

    private FrontEnd frontEnd;
    private List allFeatures;
    // Initialize this to an invalid number
    private int featureLength = -1;

    /**
     * The logger for this class
     */
    private static Logger logger =
            Logger.getLogger("edu.cmu.sphinx.tools.feature.FeatureFileDumper");

    /**
     * Constructs a FeatureFileDumper.
     *
     * @param propsFile the Sphinx properties file
     * @param inputAudioFile the input audio file
     */
    public FeatureFileDumper(String propsFile, String inputAudioFile)
	throws FileNotFoundException, IOException {
	try {
            /*
	     * Create a new front end and get the feature (cepstra, plp, etc).
             */
	    String context = "featureDumping";
            URL url = new File(propsFile).toURI().toURL();
            SphinxProperties.initContext (context, url);
	    SphinxProperties props = SphinxProperties.getSphinxProperties
		(context);

	    StreamDataSource audioSource = new StreamDataSource();
	    audioSource.initialize("BatchFileAudioSource", context,
				   props, null);
	    audioSource.setInputStream(new FileInputStream(inputAudioFile), 
		"audio");

	    Collection names = FrontEndFactory.getNames(props);
	    assert (names.size() == 1);
	    String feName = (String) names.iterator().next();

            frontEnd = FrontEndFactory.getFrontEnd(props, feName);
            frontEnd.setDataSource(audioSource);


	    allFeatures = new LinkedList();
	    getAllFeatures();
	    logger.info("Frames: " + allFeatures.size());
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Retrieve all Features from the frontend, and cache all those
     * with actual feature data.
     */
    private void getAllFeatures() {
	/* Run through all the data and produce feature.
	 */
	try {
	    assert(allFeatures != null);
	    Data feature = frontEnd.getData();
	    while (!(feature instanceof DataEndSignal)) {
		if (feature instanceof DoubleData) {
		    double[] featureData = ((DoubleData)feature).getValues();
		    if (featureLength < 0) {
			featureLength = featureData.length;
                        logger.info("Feature length: " + featureLength);
		    }
		    allFeatures.add(featureData);
		} else if (feature instanceof FloatData) {
                    float[] featureData = ((FloatData)feature).getValues();
                    if (featureLength < 0) {
                        featureLength = featureData.length;
                        logger.info("Feature length: " + featureLength);
                    }
                    allFeatures.add(featureData);
                }
		feature = frontEnd.getData();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Returns the total number of data points that should be written
     * to the output file.
     *
     * @return the total number of data points that should be written
     */
    private int getNumberDataPoints() {
	return (allFeatures.size() * featureLength);
    }

    /**
     * Dumps the feature to the given binary output.
     *
     * @param outputFile the binary output file
     */
    public void dumpBinary(String outputFile) throws IOException {
	DataOutputStream outStream = new DataOutputStream
	    (new FileOutputStream(outputFile));
	outStream.writeInt(getNumberDataPoints());

	for (Iterator i = allFeatures.iterator(); i.hasNext();) {
            Object data = i.next();
            if (data instanceof double[]) {
                double[] feature = (double []) data;
                for (int d = 0; d < feature.length; d++) {
                    outStream.writeFloat((float)feature[d]);
                }
            } else if (data instanceof float[]) {
                float[] feature = (float[]) data;
                for (int d = 0; d < feature.length; d++) {
                    outStream.writeFloat(feature[d]);
                }
            }
	}

	outStream.close();
    }

    /**
     * Dumps the feature to the given ascii output file.
     *
     * @param outputFile the ascii output file
     */
    public void dumpAscii(String outputFile) throws IOException {
	PrintStream ps = new PrintStream(new FileOutputStream(outputFile),
					 true);
	ps.print(getNumberDataPoints());
	ps.print(' ');

        for (Iterator i = allFeatures.iterator(); i.hasNext();) {
	    double[] feature = (double []) i.next();
            for (int d = 0; d < feature.length; d++) {
		ps.print(feature[d]);
		ps.print(' ');
		}
        }

	ps.close();
    }

    /**
     * Main program for this dumper.
     *
     * This program takes three arguments:
     *
     * 1. propsFile - the Sphinx properties file
     * 2. audioFile - the name of the audio file
     * 3. outputFile - the name of the binary output file
     */
    public static void main(String[] argv) {

	if (argv.length < 3) {
	    System.out.println("Usage: FeatureFileDumper " + 
			       "propsFile inputFile outputFile [(binary|ascii)]");
	    System.exit(1);
	}
	String propsFile = argv[0];
	String inputFile = argv[1];
	String outputFile = argv[2];
	String format = "binary";
	if (argv.length == 4) {
	    format = argv[3];
	}
	logger.info("Input file: " + inputFile);
	logger.info("Output file: " + outputFile);
	logger.info("Format: " + format);

	try {
	    FeatureFileDumper dumper = new FeatureFileDumper
		(propsFile, inputFile);
	    if (format.equals("binary")) {
		dumper.dumpBinary(outputFile);
	    } else if (format.equals("ascii")) {
		dumper.dumpAscii(outputFile);
	    } else {
		System.out.println("ERROR: unknown output format: " + format);
	    }
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}
    }
}
