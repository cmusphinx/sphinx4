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


package tests.frontend;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.util.SphinxProperties;


/**
 * This program takes in an audio file, does frontend signal processing to it,
 * and then dumps the resulting Cepstra into a separate binary file.
 *
 * This program takes three arguments:
 *
 * 1. propsFile - the Sphinx properties file
 * 2. audioFile - the name of the audio file
 * 3. outputFile - the name of the binary output file
 */
public class CepstrumFileDumper {

    private FrontEnd frontend;
    private List allFeatures;
    private int featureLength;
    
    /**
     * Constructs a CepstrumFileDumper.
     *
     * @param propsFile the Sphinx properties file
     * @param inputAudioFile the input audio file
     */
    public CepstrumFileDumper(String propsFile, String inputAudioFile)
	throws FileNotFoundException, IOException {
        try {
	    String context = "cepstralDumping";
	    String pwd = System.getProperty("user.dir");
	    SphinxProperties.initContext
		(context, new URL("file://" + pwd + File.separatorChar +
				  propsFile));
	    SphinxProperties props = SphinxProperties.getSphinxProperties
		(context);
	    featureLength = props.getInt
		(FeatureExtractor.PROP_FEATURE_LENGTH, 13);

	    AudioSource audioSource = new StreamAudioSource
		("BatchFileAudioSource", context,
		 (new FileInputStream(inputAudioFile)), inputAudioFile);

	    frontend = new SimpleFrontEnd();
            frontend.initialize("frontend", context, audioSource);

	    allFeatures = new LinkedList();
	    getAllFeatures();

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Retrieve all Features from the frontend, and cache all those
     * with actual feature data.
     */
    private void getAllFeatures() throws IOException {
	Feature feature = null;
	FeatureFrame frame = null;
	while ((frame = frontend.getFeatureFrame(1, null)) != null) {
	    feature = frame.getFeatures()[0];
	    if (feature.hasContent()) {
		allFeatures.add(feature);
	    }
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
     * Dumps the Cepstra to the given binary output.
     *
     * @param outputFile the binary output file
     */
    public void dumpBinary(String outputFile) throws IOException {
	DataOutputStream outStream = new DataOutputStream
	    (new FileOutputStream(outputFile));
	outStream.writeInt(getNumberDataPoints());

	for (Iterator i = allFeatures.iterator(); i.hasNext();) {
	    Feature feature = (Feature) i.next();
	    float[] data = feature.getFeatureData();
	    for (int d = 0; d < data.length; d++) {
		outStream.writeFloat(data[d]);
	    }
	}

	outStream.close();
    }

    /**
     * Dumps the Cepstra to the given ascii output file.
     *
     * @param outputFile the ascii output file
     */
    public void dumpAscii(String outputFile) throws IOException {
	PrintStream ps = new PrintStream(new FileOutputStream(outputFile),
					 true);
	ps.print(getNumberDataPoints());
	ps.print(' ');

        for (Iterator i = allFeatures.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            float[] data = feature.getFeatureData();
            for (int d = 0; d < data.length; d++) {
		ps.print(data[d]);
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

	String propsFile = argv[0];
	String inputFile = argv[1];
	String outputFile = argv[2];
	String format = argv[3];
	
	try {
	    CepstrumFileDumper dumper = new CepstrumFileDumper
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
