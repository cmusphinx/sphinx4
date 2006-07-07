
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
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;

import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.Context;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.LeftRightContext;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.HMMManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Pool;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneHMM;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneSequence;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.util.ExtendedStreamTokenizer;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StreamFactory;



/**
 * an acoustic model loader that loads sphinx3 ascii data
 *
 * Mixture weights and transition probabilities are maintained in logMath 
 * log base, 
 */
class Sphinx4Loader extends Sphinx3Loader {

    /**
     * The logger for this class
     */
    private static Logger logger = 
	    Logger.getLogger(AcousticModel.PROP_PREFIX + "AcousticModel");

    protected final static String TMAT_FILE_VERSION = "4.0";

    public final static String MAX_MODEL_SIZE = 
	AcousticModel.PROP_PREFIX + "maxStatePerModel";

    public final static int MAX_MODEL_SIZE_DEFAULT = 10;


    /**
     * Loads the sphinx4 ascii model.
     *
     * @param modelName  the name of the model as specified in the
     *    props file.
     * @param props  the SphinxProperties object
     * @param binary  if <code>true</code> the file is in binary
     * format
     */
    public Sphinx4Loader(String modelName, SphinxProperties props, 
            boolean binary) throws 
	FileNotFoundException, IOException, ZipException {
	super(modelName, props, binary);

    }


    /**
     * Loads the sphinx4 densityfile, a set of density arrays are
     * created and placed in the given pool.
     *
     * @param useCDUnits if true, loads also the context dependent units
     * @param inputStream the open input stream to use
     * @param path the path to a density file
     *
     * @return a pool of loaded densities
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while loading the data
     */
    protected Pool loadHMMPool(boolean useCDUnits,
                             InputStream inputStream,
                             String path) 
            throws FileNotFoundException, IOException {
	int token_type;
	int numBase;
	int numTri;
	int numStateMap;
	int numTiedState;
	int numStatePerHMM;
	int numContextIndependentTiedState;
	int numTiedTransitionMatrices;

	ExtendedStreamTokenizer est = new ExtendedStreamTokenizer
            (inputStream, '#', false);
    	Pool pool = new Pool(path);

	logger.info("Loading HMM file from: ");
	logger.info(path);

	est.expectString(MODEL_VERSION);

	numBase = est.getInt("numBase");
	est.expectString("n_base");

	numTri = est.getInt("numTri");
	est.expectString("n_tri");

	numStateMap = est.getInt("numStateMap");
	est.expectString("n_state_map");

	numTiedState = est.getInt("numTiedState");
	est.expectString("n_tied_state");

	numContextIndependentTiedState = 
	    est.getInt("numContextIndependentTiedState");
	est.expectString("n_tied_ci_state");

	numTiedTransitionMatrices = est.getInt("numTiedTransitionMatrices");
	est.expectString("n_tied_tmat");

	SphinxProperties props = super.getSystemProperties();
	int maxModelSize = 
	    props.getInt(MAX_MODEL_SIZE, MAX_MODEL_SIZE_DEFAULT);
	int[] maxStid = new int[maxModelSize];

	HMMManager hmmManager = super.getHmmManager();
	Pool matrixPool = super.getMatrixPool();
	Pool mixtureWeightsPool = super.getMixtureWeightsPool();
	Map contextIndependentUnits = super.getContextIndependentUnits();

	assert numTiedState == mixtureWeightsPool.getFeature(NUM_SENONES, 0);
	assert numTiedTransitionMatrices == matrixPool.size();

	// Load the base phones
	for (int i = 0; i < numBase; i++) {
	    String name = est.getString();
	    String left = est.getString();
	    String right = est.getString();
	    String position = est.getString();
	    String attribute  = est.getString();
	    int  tmat  = est.getInt("tmat");
	    int numStates = 0;

	    // Read all state ID in the line...
	    for (int j = 0; ; j++) {
		String str = est.getString();

		// ... until we reach a "N"

		if (!str.equals("N")) {
		    int id = Integer.parseInt(str);
		    try {
			maxStid[j] = id;
		    } catch (ArrayIndexOutOfBoundsException aie) {
			throw new Error("Use a larger value for " +
					"maxStatePerModel");
		    }
		    assert maxStid[j] >= 0 && 
			maxStid[j] < numContextIndependentTiedState;
		} else {
		    numStates = j;
		    break;
		}
	    }
	    int[] stid = new int[numStates];
	    System.arraycopy(maxStid, 0, stid, 0, numStates);

	    assert left.equals("-");
	    assert right.equals("-");
	    assert position.equals("-");
	    assert tmat < numTiedTransitionMatrices;

	    Unit unit = Unit.getUnit(name, attribute.equals(FILLER));
	    contextIndependentUnits.put(unit.getName(), unit);

	    if (logger.isLoggable(Level.FINE)) {
		logger.fine("Loaded " + unit);
	    }


	    // The first filler
	    if (unit.isFiller() && unit.getName().equals(SILENCE_CIPHONE)) {
		unit = Unit.SILENCE;
	    }

	    float[][] transitionMatrix = (float[][]) matrixPool.get(tmat);
	    SenoneSequence ss = getSenoneSequence(stid);

	    HMM hmm = new SenoneHMM(unit,  ss, 
		    	transitionMatrix, HMMPosition.lookup(position));
	    hmmManager.put(hmm);
	}

	// Load the context dependent phones.  If the useCDUnits
	// property is false, the CD phones will not be created, but
        // the values still need to be read in from the file.

        String lastUnitName = "";
        Unit lastUnit = null;
        int[] lastStid = null;
        SenoneSequence lastSenoneSequence = null;

	for (int i = 0; i < numTri; i++) {
	    String name = est.getString();
	    String left = est.getString();
	    String right = est.getString();
	    String position = est.getString();
	    String attribute  = est.getString();
	    int  tmat  = est.getInt("tmat");

	    int numStates = 0;

	    // Read all state ID in the line...
	    for (int j = 0; ; j++) {
		String str = est.getString();

		// ... until we reach a "N"

		if (!str.equals("N")) {
		    int id = Integer.parseInt(str);
		    maxStid[j] = id;
		    assert maxStid[j] >= numContextIndependentTiedState && 
			maxStid[j] < numTiedState;
		} else {
		    numStates = j;
		    break;
		}
	    }
	    int[] stid = new int[numStates];
	    System.arraycopy(maxStid, 0, stid, 0, numStates);

	    assert !left.equals("-");
	    assert !right.equals("-");
	    assert !position.equals("-");
	    assert attribute.equals("n/a");
	    assert tmat < numTiedTransitionMatrices;

            if (useCDUnits) {
                Unit unit = null;
                String unitName = (name + " " + left + " " + right);

                if (unitName.equals(lastUnitName)) {
                    unit = lastUnit;
                } else {
                    Unit[] leftContext = new Unit[1];
                    leftContext[0] = (Unit) contextIndependentUnits.get(left);
                    
                    Unit[] rightContext = new Unit[1];
                    rightContext[0] = (Unit) contextIndependentUnits.get(right);
                
                    Context context = LeftRightContext.get(leftContext,
                                                           rightContext);
                    unit = Unit.getUnit(name, false, context);
                }
                lastUnitName = unitName;
                lastUnit = unit;

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Loaded " + unit);
                }

                float[][] transitionMatrix = (float[][]) matrixPool.get(tmat);

                SenoneSequence ss = lastSenoneSequence;
                if (ss == null || !sameSenoneSequence(stid, lastStid)) {
                    ss = getSenoneSequence(stid);
                }
                lastSenoneSequence = ss;
                lastStid = stid;
        
                HMM hmm = new SenoneHMM(unit,
                                  ss, 
                                  transitionMatrix,
                                  HMMPosition.lookup(position));
                hmmManager.put(hmm);
            }
	}

	est.close();
	return pool;
    }

    /**
     * Loads the transition matrices
     *
     * @param path the path to the transitions matrices
     *
     * @return a pool of transition matrices
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while loading the data
     */
    protected Pool loadTransitionMatricesAscii(String path)
        throws FileNotFoundException, IOException {

	String location = super.getLocation();
        InputStream inputStream = StreamFactory.getInputStream(location, path);

	SphinxProperties acousticProperties = super.getAcousticProperties();
	LogMath logMath = super.getLogMath();
        boolean sparseForm = acousticProperties.getBoolean
	    (TiedStateAcousticModel.PROP_SPARSE_FORM, 
	     TiedStateAcousticModel.PROP_SPARSE_FORM_DEFAULT);
	logger.info("Loading transition matrices from: ");
	logger.info( path);
	int numMatrices;
	int numStates;

    	Pool pool = new Pool(path);
	ExtendedStreamTokenizer est = new ExtendedStreamTokenizer
            (inputStream, '#', false);

	est.expectString("tmat");
	numMatrices = est.getInt("numMatrices");
	est.expectString("X");
	// numStates = est.getInt("numStates");

	for (int i = 0; i < numMatrices; i++) {
	    est.expectString("tmat");
	    est.expectString("[" + i + "]");
	    est.expectString("nstate");
	    // Number of emitting states + 1, final non-emitting state
	    numStates = est.getInt("numStates") + 1;
	    float[][] tmat = new float[numStates][numStates];

	    for (int j = 0; j < numStates ; j++) {
		for (int k = 0; k < numStates ; k++) {

		    // the last row is just zeros, so we just do
		    // the first (numStates - 1) rows

		    if (j < numStates - 1) {
			if (sparseForm) {
			    if (k == j  || k  == j + 1) {
				tmat[j][k] = est.getFloat("tmat value");
			    }
			} else {
			    tmat[j][k] = est.getFloat("tmat value");
			}
		    }

                    tmat[j][k] = logMath.linearToLog(tmat[j][k]);

		    if (logger.isLoggable(Level.FINE)) {
			logger.fine("tmat j " + j  + " k " 
			    + k + " tm "+ tmat[j][k]);
		    }
		}
	    }
	    pool.put(i, tmat);
	}
	est.close();
	return pool;
    }



    /**
     * Loads the transition matrices (Binary)
     *
     * @param path the path to the transitions matrices
     *
     * @return a pool of transition matrices
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while loading the data
     */
    protected Pool loadTransitionMatricesBinary(String path)
        throws FileNotFoundException, IOException {

	SphinxProperties acousticProperties = super.getAcousticProperties();
	LogMath logMath = super.getLogMath();
        boolean sparseForm = acousticProperties.getBoolean
	    (TiedStateAcousticModel.PROP_SPARSE_FORM, 
	     TiedStateAcousticModel.PROP_SPARSE_FORM_DEFAULT);
	logger.info("Loading transition matrices from: ");
	logger.info( path);
	int numMatrices;
	int numStates;
        int numRows;
        int numValues;


        Properties props = new Properties();
	String location = super.getLocation();
        DataInputStream dis = readS3BinaryHeader(location, path, props);

        String version = props.getProperty("version");
        boolean doCheckSum;

        if (version == null || !version.equals(TMAT_FILE_VERSION)) {
            throw new IOException("Unsupported version in " + path);
        }

        String checksum = props.getProperty("chksum0");
        doCheckSum =  (checksum != null && checksum.equals("yes"));

    	Pool pool = new Pool(path);

	numMatrices = readInt(dis);
	// numRows = readInt(dis);
	// numStates = readInt(dis);
	numValues = readInt(dis);

        // assert numValues == numStates * numRows * numMatrices;

	int count = 0;
	for (int i = 0; i < numMatrices; i++) {
	    numStates = readInt(dis);
	    float[][] tmat = new float[numStates][];
            // last row should be zeros
            tmat[numStates - 1] = new float[numStates];
            convertToLogMath(tmat[numStates-1]);

	    for (int j = 0; j < numStates - 1; j++) {
                tmat[j] = readFloatArray(dis, numStates);
		count += numStates;
                nonZeroFloor(tmat[j], 0f);
                normalize(tmat[j]);
                convertToLogMath(tmat[j]);
	    }
	    pool.put(i, tmat);
	}
	dis.close();
	assert numValues == count;
	return pool;
    }

}

