
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

package edu.cmu.sphinx.linguist.acoustic.tiedstate.trainer;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;

import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Pool;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StreamFactory;


/**
 * An acoustic model saver that saves sphinx3 ascii data.
 *
 * Mixture weights and transition probabilities are saved in linear scale.
 */
class Sphinx4Saver extends Sphinx3Saver {

    /**
     * The logger for this class
     */
    private static Logger logger = 
	    Logger.getLogger(AcousticModel.PROP_PREFIX + "AcousticModel");

    protected final static String TMAT_FILE_VERSION = "4.0";

    /**
     * Saves the sphinx4 models.
     *
     * @param modelName  the name of the model as specified in the
     *    props file.
     * @param props  the SphinxProperties object
     * @param binary if <code>true</code> the file is saved in binary
     * format
     * @param loader this acoustic model's loader
     */
    public Sphinx4Saver(String modelName, SphinxProperties props, 
            boolean binary, Loader loader) throws 
	FileNotFoundException, IOException, ZipException {

	super(modelName, props, binary, loader);
    }


    /**
     * Saves the transition matrices
     *
     * @param pool the transition matrices pool
     * @param path the path to the transitions matrices
     * @param append is true, the file will be appended, useful if
     * saving to a ZIP or JAR file
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while saving the data
     */
    protected void saveTransitionMatricesAscii(Pool pool, String path, 
					     boolean append)
        throws FileNotFoundException, IOException {

	String location = super.getLocation();
        OutputStream outputStream = StreamFactory.getOutputStream(location, 
							      path, append);
	if (outputStream == null) {
	    throw new IOException("Error trying to write file "
                                        + location + path);
	}
	PrintWriter pw = new PrintWriter(outputStream, true);

	SphinxProperties acousticProperties = super.getAcousticProperties();
	LogMath logMath = super.getLogMath();
        boolean sparseForm = acousticProperties.getBoolean
	    (TiedStateAcousticModel.PROP_SPARSE_FORM, 
	     TiedStateAcousticModel.PROP_SPARSE_FORM_DEFAULT);
	logger.info("Saving transition matrices to: ");
	logger.info( path);
	int numMatrices = pool.size();
	int numStates;
	float[][] tmat;

	assert numMatrices > 0;
	tmat = (float [][])pool.get(0);

	pw.println("tmat " + numMatrices + " X");

	for (int i = 0; i < numMatrices; i++) {
	    tmat = (float [][])pool.get(i);
	    numStates = tmat[0].length;

	    pw.println("tmat [" + i + "]");
	    pw.println("nstate " + (numStates - 1));
	    for (int j = 0; j < numStates ; j++) {
		for (int k = 0; k < numStates ; k++) {

		    // the last row is just zeros, so we just do
		    // the first (numStates - 1) rows

		    if (j < numStates - 1) {
			if (sparseForm) {
			    if (k < j) {
				pw.print("\t");
			    }
			    if (k == j  || k  == j + 1) {
				pw.print((float)
					 logMath.logToLinear(tmat[j][k]));
			    }
			} else {
			    pw.print((float)logMath.logToLinear(tmat[j][k]));
			}
			if (numStates - 1 == k) {
			    pw.println();
			} else {
			    pw.print(" ");
			}

		    }

		    if (logger.isLoggable(Level.FINE)) {
			logger.fine("tmat j " + j  + " k " 
			    + k + " tm "+ tmat[j][k]);
		    }
		}
	    }
	}
	outputStream.close();
    }



    /**
     * Saves the transition matrices (Binary)
     *
     * @param pool the transition matrices pool
     * @param path the path to the transitions matrices
     * @param append is true, the file will be appended, useful if
     * saving to a ZIP or JAR file
     *
     * @return a pool of transition matrices
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while saving the data
     */
    protected void saveTransitionMatricesBinary(Pool pool, String path, 
					      boolean append)
        throws FileNotFoundException, IOException {

	SphinxProperties acousticProperties = super.getAcousticProperties();
	LogMath logMath = super.getLogMath();

        boolean sparseForm = acousticProperties.getBoolean
	    (TiedStateAcousticModel.PROP_SPARSE_FORM, 
	     TiedStateAcousticModel.PROP_SPARSE_FORM_DEFAULT);
	logger.info("Saving transition matrices to: ");
	logger.info( path);
        Properties props = new Properties();
	float[][] tmat;

	int checkSum = 0;

	String strCheckSum = super.getCheckSum();
	boolean doCheckSum = super.getDoCheckSum();
	props.setProperty("version", TMAT_FILE_VERSION);
	if (doCheckSum) {
	    props.setProperty("chksum0", strCheckSum);
	}


	String location = super.getLocation();

        DataOutputStream dos = writeS3BinaryHeader(location, path, props, 
						   append);


	int numMatrices = pool.size();
	assert numMatrices > 0;
	writeInt(dos, numMatrices);


	// Now we count number of states. Since each model can have an
	// arbitrary number of states, we have to visit all tmats, and
	// count the total number of states.
	int numStates = 0;
	int numValues = 0;
	for (int i = 0; i < numMatrices; i++) {
	    tmat = (float [][])pool.get(i);
	    int nStates = tmat[0].length;
	    numStates += nStates;
	    // Number of elements in each transition matrix is the
	    // number of row, i.e. number of emitting states, times
	    // number of columns, i.e., number of emitting states plus
	    // final non-emitting state.
	    numValues += nStates * (nStates - 1);
	}
	writeInt(dos, numValues);

	for (int i = 0; i < numMatrices; i++) {
	    float[] logTmatRow;
	    float[] tmatRow;

	    tmat = (float [][])pool.get(i);
	    numStates = tmat[0].length;
	    writeInt(dos, numStates);

	    // Last row should be all zeroes
	    logTmatRow = tmat[numStates - 1];
	    tmatRow = new float[logTmatRow.length];

	    for (int j = 0; j < numStates; j++) {
		assert tmatRow[j] == 0.0f;
	    }

	    for (int j = 0; j < numStates - 1; j++) {
		logTmatRow = tmat[j];
		tmatRow = new float[logTmatRow.length];
		convertFromLogMath(logTmatRow, tmatRow);
                writeFloatArray(dos, tmatRow);
	    }
	}
	if (doCheckSum) {
	    assert doCheckSum = false: "Checksum not supported";
	    // writeInt(dos, checkSum);
	}
	dos.close();
    }
}

