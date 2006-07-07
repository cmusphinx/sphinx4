
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;

import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.GaussianMixture;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.HMMManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Pool;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Senone;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneHMM;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneSequence;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.util.ExtendedStreamTokenizer;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StreamFactory;


/**
 * an acoustic model loader that initializes models
 *
 * Mixture weights and transition probabilities are maintained in logMath 
 * log base, 
 */
class ModelInitializerLoader implements Loader {

    /**
     * The logger for this class
     */
    private static Logger logger = 
	    Logger.getLogger(AcousticModel.PROP_PREFIX + "AcousticModel");

    private final static String NUM_SENONES = "num_senones";
    private final static String NUM_GAUSSIANS_PER_STATE = "num_gaussians";
    private final static String NUM_STREAMS = "num_streams";

    private final static String FILLER = "filler";
    private final static String SILENCE_CIPHONE  = "SIL";

    private final static int BYTE_ORDER_MAGIC = 0x11223344;

    public final static String MODEL_VERSION = "0.3";

    private final static int CONTEXT_SIZE = 1;


    private Pool meansPool;
    private Pool variancePool;
    private Pool matrixPool;
    private Pool meanTransformationMatrixPool;
    private Pool meanTransformationVectorPool;
    private Pool varianceTransformationMatrixPool;
    private Pool varianceTransformationVectorPool;
    private Pool mixtureWeightsPool;

    private Pool senonePool;
    private int vectorLength;

    private Map contextIndependentUnits;
    private Map phoneList;
    private HMMManager hmmManager;
    private LogMath logMath;
    private SphinxProperties acousticProperties;
    private boolean binary = false;
    private String location;
    private boolean swap;

    private final static String DENSITY_FILE_VERSION = "1.0";
    private final static String MIXW_FILE_VERSION = "1.0";
    private final static String TMAT_FILE_VERSION = "1.0";


    /**
     * Creates a dummy model.
     *
     * @param modelName  the name of the model as specified in the
     *    props file.
     * @param props  the SphinxProperties object
     */
    public ModelInitializerLoader(String modelName, SphinxProperties props) 
	throws FileNotFoundException, IOException, ZipException {

	logMath = LogMath.getLogMath(props.getContext());

	// extract the feature vector length
	String vectorLengthProp = TiedStateAcousticModel.PROP_VECTOR_LENGTH;
	if (modelName != null) {
	    vectorLengthProp = TiedStateAcousticModel.PROP_PREFIX + modelName +
		".FeatureVectorLength";
	}
	vectorLength = props.getInt
	    (vectorLengthProp, TiedStateAcousticModel.PROP_VECTOR_LENGTH_DEFAULT);
	
        hmmManager = new HMMManager();
        contextIndependentUnits = new LinkedHashMap();
	phoneList = new LinkedHashMap();

        // dummy pools for these elements
        meanTransformationMatrixPool
            = createDummyMatrixPool("meanTransformationMatrix");
        meanTransformationVectorPool
            = createDummyVectorPool("meanTransformationMatrix");
        varianceTransformationMatrixPool
            = createDummyMatrixPool("varianceTransformationMatrix");
        varianceTransformationVectorPool
            = createDummyVectorPool("varianceTransformationMatrix");

        // do the actual acoustic model loading
        createModels(modelName, props);
    }


    /**
     * Creates the AcousticModel containing zeroes or floor values.
     *
     * @param modelName the name of the acoustic model; if null we just
     *    load from the default location
     * @param props the SphinxProperties object to use
     */
    private void createModels(String modelName, SphinxProperties props) 
	throws FileNotFoundException, IOException, ZipException {

	String prefix, phone, dataDir, propsFile;

	if (modelName == null) {
	    prefix = TiedStateAcousticModel.PROP_PREFIX;
	} else {
	    prefix = TiedStateAcousticModel.PROP_PREFIX + modelName + ".";
	}
	// System.out.println("Using prefix: " + prefix);

	location = props.getString
	    (prefix + "location", TiedStateAcousticModel.PROP_LOCATION_DEFAULT);
	phone = props.getString
	    (prefix + "phone_list", 
	     TrainerAcousticModel.PROP_PHONE_LIST_DEFAULT);
	dataDir = props.getString
	    (prefix + "data_location", 
	     TiedStateAcousticModel.PROP_DATA_LOCATION_DEFAULT) + "/";
	propsFile = props.getString
	    (prefix + "properties_file", 
	     TiedStateAcousticModel.PROP_PROPERTIES_FILE_DEFAULT);

	logger.info("Creating Sphinx3 acoustic model: " + modelName);
	logger.info("    Path      : " + location);
	logger.info("    phonelist : " + phone);
	logger.info("    dataDir   : " + dataDir);

        // load the acoustic properties file (am.props), 
	// create a different URL depending on the data format

        String url = null;
        String format = StreamFactory.resolve(location);

        if (format.equals(StreamFactory.ZIP_FILE)) {
            url = "jar:" + location + "!/" + propsFile;
        } else {
            url = "file:" + location + "/" + propsFile;
        }

	if (modelName == null) {
	    prefix = props.getContext() + ".acoustic";
	} else {
	    prefix = props.getContext() + ".acoustic." + modelName;
	}
	acousticProperties = loadAcousticPropertiesFile(prefix, url);

        // load the HMM model file
	boolean useCDUnits = props.getBoolean
	    (TiedStateAcousticModel.PROP_USE_CD_UNITS, false);
	assert useCDUnits == false;
	try {
	    loadPhoneList(useCDUnits,
			  StreamFactory.getInputStream(location, phone),
			  location + File.separator + phone,
			  props); 
	} catch (StreamCorruptedException sce) {
	    printPhoneListHelp();
	}
    }

    /**
     * Prints out a help message with format of phone list.
     */
    private void printPhoneListHelp() {
	System.out.println("The format for the phone list file is:");
	System.out.println("\tversion 0.1");
	System.out.println("\tsame_sized_models yes");
	System.out.println("\tn_state 3");
	System.out.println("\ttmat_skip (no|yes)");
	System.out.println("\tAA");
	System.out.println("\tAE");
	System.out.println("\tAH");
	System.out.println("\t...");
	System.out.println("Or:");
	System.out.println("\tversion 0.1");
	System.out.println("\tsame_sized_models no");
	System.out.println("\ttmat_skip (no|yes)");
	System.out.println("\tAA 5");
	System.out.println("\tAE 3");
	System.out.println("\tAH 4");
	System.out.println("\t...");
    }

    /**
     * Returns the map of context indepent units. The map can be
     * accessed by unit name.
     *
     * @return the map of context independent units.
     */
    public Map getContextIndependentUnits() {
	return contextIndependentUnits;
    }


    /**
     * Adds a model to the senone pool.
     *
     * @param pool the senone pool
     * @param stateID vector with senone ID for an HMM
     * @param distFloor the lowest allowed score 
     * @param varianceFloor the lowest allowed variance
     *
     * @return the senone pool
     */
    private void addModelToSenonePool(Pool pool, int[] stateID, 
				      float distFloor, float varianceFloor) {
	assert pool != null;

	int numMixtureWeights = mixtureWeightsPool.size();

	/*
	int numMeans = meansPool.size();
	int numVariances = variancePool.size();
	int numSenones = mixtureWeightsPool.getFeature(NUM_SENONES, 0);
	int whichGaussian = 0;

	logger.fine("NG " + numGaussiansPerSenone);
	logger.fine("NS " + numSenones);
	logger.fine("NMIX " + numMixtureWeights);
	logger.fine("NMNS " + numMeans);
	logger.fine("NMNS " + numVariances);

	assert numMixtureWeights == numSenones;
	assert numVariances == numSenones * numGaussiansPerSenone;
	assert numMeans == numSenones * numGaussiansPerSenone;
	*/
	int numGaussiansPerSenone = 
            mixtureWeightsPool.getFeature(NUM_GAUSSIANS_PER_STATE, 0);
	assert numGaussiansPerSenone > 0;
	for (int i = 0; i < stateID.length; i++) {
	    int state = stateID[i];
	    MixtureComponent[] mixtureComponents = new
		MixtureComponent[numGaussiansPerSenone];
	    for (int j = 0; j <  numGaussiansPerSenone; j++) {
		int whichGaussian = state * numGaussiansPerSenone + j;
		mixtureComponents[j] = new MixtureComponent(
		    logMath,
		    (float[]) meansPool.get(whichGaussian),
		    (float[][]) meanTransformationMatrixPool.get(0),
		    (float[]) meanTransformationVectorPool.get(0),
		    (float[]) variancePool.get(whichGaussian),
		    (float[][]) varianceTransformationMatrixPool.get(0),
		    (float[]) varianceTransformationVectorPool.get(0),
		    distFloor,
		    varianceFloor);
	    }

	    Senone senone = new GaussianMixture( 
	      logMath, (float[]) mixtureWeightsPool.get(state), 
	      mixtureComponents, state);

	    pool.put(state, senone);
	}
    }


    /**
     * Loads the Sphinx 3 acoustic model properties file, which
     * is basically a normal system properties file.
     *
     * @param context this models' context
     * @param url the path to the acoustic properties file
     *
     * @return a SphinxProperty object containing the acoustic properties,
     *    or null if there are no acoustic model properties
     *
     * @throws FileNotFoundException if the file cannot be found
     * @throws IOException if an error occurs while loading the data
     */
    private SphinxProperties loadAcousticPropertiesFile(String context,
                                                        String url)
        throws FileNotFoundException, IOException {
        SphinxProperties.initContext(context, new URL(url));
        return (SphinxProperties.getSphinxProperties(context));
    }


    /**
     * Adds a set of density arrays to a given pool.
     *
     * @param pool the pool to add densities to
     * @param stateID a vector with the senone id of the states in a model
     * @param numStreams the number of streams
     * @param numGaussiansPerState the number of Gaussians per state
     *
     * @throws IOException if an error occurs while loading the data
     */
    private void addModelToDensityPool(Pool pool, int[] stateID, 
				    int numStreams, int numGaussiansPerState) 
        throws IOException {
	int token_type;
	int numStates;
	int numInPool;

	assert pool != null;
	assert stateID != null;

	numStates = stateID.length;

	numInPool = pool.getFeature(NUM_SENONES, 0);
	pool.setFeature(NUM_SENONES, numStates + numInPool);
	numInPool = pool.getFeature(NUM_STREAMS, -1);
	if (numInPool == -1) {
	    pool.setFeature(NUM_STREAMS, numStreams);
	} else {
	    assert numInPool == numStreams;
	}
	numInPool = pool.getFeature(NUM_GAUSSIANS_PER_STATE, -1);
	if (numInPool == -1) {
	    pool.setFeature(NUM_GAUSSIANS_PER_STATE, numGaussiansPerState);
	} else {
	    assert numInPool == numGaussiansPerState;
	}

	// TODO: numStreams should be any number > 0, but for now....
	assert numStreams == 1;
	for (int i = 0; i < numStates; i++) {
	    int state = stateID[i];
	    for (int j = 0; j < numGaussiansPerState; j++) {
		// We're creating densities here, so it's ok if values
		// are all zero.
		float[] density = new float[vectorLength];
		int id = state * numGaussiansPerState + j;
		pool.put(id, density);
	    }
	}
    }


    /**
     * If a data point is below 'floor' make
     * it equal to floor.
     *
     * @param data the data to floor
     * @param floor the floored value
     */
    private void floorData(float[] data, float floor) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] < floor) {
                data[i] = floor;
            }
        }
    }

    /**
     * Normalize the given data
     *
     * @param data the data to normalize
     */
    private void normalize(float[] data) {
        float sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }

        if (sum != 0.0f) {
	    // Invert, so we multiply instead of dividing inside the loop
	    sum = 1.0f / sum;
            for (int i = 0; i < data.length; i++) {
                 data[i] = data[i] * sum ;
            }
        }
    }

    /**
     * Dump the data
     *
     * @param name the name of the data
     * @param data the data itself
     *
     */
    private void dumpData(String name, float[] data) {
        System.out.println(" ----- " + name + " -----------");
        for (int i = 0; i < data.length; i++) {
            System.out.println(name + " " + i + ": " + data[i]);
        }
    }

    /**
     * Convert to log math
     *
     * @param data the data to normalize
     */
    // linearToLog returns a float, so zero values in linear scale
    // should return -Float.MAX_VALUE.
    private void convertToLogMath(float[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] = logMath.linearToLog(data[i]);
        }
    }



    /**
     * Loads the phone list, which possibly contains the sizes (number
     * of states) of models.
     *
     * @param useCDUnits if true, uses context dependent units
     * @param inputStream the open input stream to use
     * @param path the path to a density file
     * @param props the properties for this set of HMMs
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while loading the data
     */
    private void loadPhoneList(boolean useCDUnits,
                             InputStream inputStream,
                             String path, SphinxProperties props) 
            throws FileNotFoundException, IOException {
	int token_type;
	int numState = 0;
	int numStatePerHMM;
	int numContextIndependentTiedState;
	// TODO: this should be flexible, but we're hardwiring for now
	int numStreams = 1;
	// Since we're initializing, we start simple.
	int numGaussiansPerState = 1;
	String version;
	boolean sameSizedModels;
	boolean tmatSkip;

	ExtendedStreamTokenizer est = new ExtendedStreamTokenizer
            (inputStream, '#', false);
	//    	Pool pool = new Pool(path);

	// Initialize the pools we'll need.
	meansPool = new Pool("means");
	variancePool = new Pool("variances");
	mixtureWeightsPool = new Pool("mixtureweights");
	matrixPool = new Pool("transitionmatrices");
	senonePool = new Pool("senones");

	float distFloor = props.getFloat(TiedStateAcousticModel.PROP_MC_FLOOR, 
					 TiedStateAcousticModel.PROP_MC_FLOOR_DEFAULT);
        float mixtureWeightFloor = 
	    props.getFloat(TiedStateAcousticModel.PROP_MW_FLOOR, 
			   TiedStateAcousticModel.PROP_MW_FLOOR_DEFAULT);
        float transitionProbabilityFloor = 
	    props.getFloat(TiedStateAcousticModel.PROP_TP_FLOOR, 
			   TiedStateAcousticModel.PROP_TP_FLOOR_DEFAULT);
        float varianceFloor = 
	    props.getFloat(TiedStateAcousticModel.PROP_VARIANCE_FLOOR, 
			   TiedStateAcousticModel.PROP_VARIANCE_FLOOR_DEFAULT);


	logger.info("Loading phone list file from: ");
	logger.info(path);

	// At this point, we only accept version 0.1
	version = "0.1";
	est.expectString("version");
	est.expectString(version);

	est.expectString("same_sized_models");
	sameSizedModels = est.getString().equals("yes");

	if (sameSizedModels) {
	    est.expectString("n_state");
	    numState = est.getInt("numBase");
	}

	// for this phone list version, let's assume left-to-right
	// models, with optional state skip.
	est.expectString("tmat_skip");
	tmatSkip = est.getString().equals("yes");


	// Load the phones with sizes

	// stateIndex contains the absolute state index, that is, a
	// unique index in the senone pool.
	int stateIndex;
	int unitCount;
	String attribute;
	for (stateIndex = 0, unitCount = 0;;) {
	    String phone = est.getString();
	    if (est.isEOF()) {
		break;
	    }
	    int size = numState;
	    if (!sameSizedModels) {
		size = est.getInt("ModelSize");
	    }
	    phoneList.put(phone, new Integer(size));
	    logger.fine("Phone: " + phone + " size: " + size);
	    int[] stid = new int[size];
	    String position = "-";

	    for (int j = 0; j < size; j++, stateIndex++) {
                stid[j] = stateIndex;
	    }

	    // The first filler
	    if (phone.equals(SILENCE_CIPHONE)) {
		attribute = FILLER;
	    } else {
		attribute = "-";
	    }

	    Unit unit = Unit.getUnit(phone, attribute.equals(FILLER));
	    contextIndependentUnits.put(unit.getName(), unit);

	    if (logger.isLoggable(Level.FINE)) {
		logger.fine("Loaded " + unit + " with " + size + " states");
	    }

	    // The first filler
	    if (unit.isFiller() && unit.getName().equals(SILENCE_CIPHONE)) {
		unit = Unit.SILENCE;
	    }

	    // Means
	    addModelToDensityPool(meansPool, stid, numStreams, 
				  numGaussiansPerState);

	    // Variances
	    addModelToDensityPool(variancePool, stid, numStreams, 
				  numGaussiansPerState);

	    // Mixture weights
	    addModelToMixtureWeightPool(mixtureWeightsPool, stid, numStreams,
				    numGaussiansPerState, mixtureWeightFloor);

	    // Transition matrix
	    addModelToTransitionMatricesPool(matrixPool, 
					     unitCount, 
					     stid.length,
					     transitionProbabilityFloor, 
					     tmatSkip);

	    // After creating all pools, we create the senone pool.
	    addModelToSenonePool(senonePool, stid, distFloor, varianceFloor);

	    // With the senone pool in place, we go through all units, and
	    // create the HMMs.

	    // Create tmat
	    float[][] transitionMatrix = (float[][]) 
		matrixPool.get(unitCount);
	    SenoneSequence ss = getSenoneSequence(stid);

	    HMM hmm = new SenoneHMM(unit,  ss, 
		    	transitionMatrix, HMMPosition.lookup(position));
	    hmmManager.put(hmm);
	    unitCount++;
	}

	// If we want to use this code to load sizes/create models for
	// CD units, we need to find another way of establishing the
	// number of CI models, instead of just reading until the end
	// of file.

	est.close();
    }


    /**
     * Gets the senone sequence representing the given senones
     *
     * @param stateid is the array of senone state ids
     *
     * @return the senone sequence associated with the states
     */

    private SenoneSequence getSenoneSequence(int[] stateid) {
	Senone[] senones = new Senone[stateid.length];

	for (int i=0; i<stateid.length; i++){
            senones[i] = (Senone) senonePool.get(stateid[i]);
        }

	// TODO: Is there any advantage in trying to pool these?
	return new SenoneSequence(senones);
    }

    /**
     * Adds model to the mixture weights
     *
     * @param pool the pool to add models to
     * @param stateID vector containing state ids for hmm
     * @param numStreams the number of streams
     * @param numGaussiansPerState the number of Gaussians per state
     * @param floor the minimum mixture weight allowed
     *
     * @throws IOException if an error occurs while loading the data
     */
    private void addModelToMixtureWeightPool(Pool pool, int[] stateID, 
	     int numStreams, int numGaussiansPerState, float floor) 
        throws IOException {

	int numStates = stateID.length;
	int numInPool;

	assert pool != null;

	numInPool = pool.getFeature(NUM_SENONES, 0);
	pool.setFeature(NUM_SENONES, numStates + numInPool);
	numInPool = pool.getFeature(NUM_STREAMS, -1);
	if (numInPool == -1) {
	    pool.setFeature(NUM_STREAMS, numStreams);
	} else {
	    assert numInPool == numStreams;
	}
	numInPool = pool.getFeature(NUM_GAUSSIANS_PER_STATE, -1);
	if (numInPool == -1) {
	    pool.setFeature(NUM_GAUSSIANS_PER_STATE, numGaussiansPerState);
	} else {
	    assert numInPool == numGaussiansPerState;
	}

	// TODO: allow any number for numStreams
	assert numStreams == 1;
	for (int i = 0; i < numStates; i++) {
	    int state = stateID[i];
	    float[] logMixtureWeight = new float[numGaussiansPerState];
	    // Initialize the weights with the same value, e.g. floor
	    floorData(logMixtureWeight, floor);
	    // Normalize, so the numbers are not all too low
	    normalize(logMixtureWeight);
            convertToLogMath(logMixtureWeight);
	    pool.put(state, logMixtureWeight);
	}
    }

    /**
     * Adds transition matrix to the transition matrices pool
     *
     * @param pool the pool to add matrix to
     * @param hmmId current HMM's id
     * @param numEmittingStates number of states in current HMM
     * @param floor the transition probability floor
     * @param skip if true, states can be skipped
     *
     * @throws IOException if an error occurs while loading the data
     */
    private void addModelToTransitionMatricesPool(Pool pool, int hmmId, 
				  int numEmittingStates, float floor, 
				  boolean skip)
        throws IOException {

	assert pool != null;

	// Add one to account for the last, non-emitting, state
	int numStates = numEmittingStates + 1;

	float[][] tmat = new float[numStates][numStates];

	for (int j = 0; j < numStates ; j++) {
	    for (int k = 0; k < numStates ; k++) {
		// Just to be sure...
		tmat[j][k] = 0.0f;

		// the last row is just zeros, so we just do
		// the first (numStates - 1) rows

		// The value assigned could be anything, provided
		// we normalize it.
		if (j < numStates - 1) {
		    // Usual case: state can transition to itself
		    // or the next state.
		    if (k == j  || k  == j + 1) {
			tmat[j][k] = floor;
		    }
		    // If we can skip, we can also transition to
		    // the next state
		    if (skip) {
			if (k == j + 2) {
			    tmat[j][k] = floor;
			}
		    }
		}
	    }
	    normalize(tmat[j]);
	    convertToLogMath(tmat[j]);
	}
	pool.put(hmmId, tmat);
    }


    /**
     * Creates a pool with a single identity matrix in it.
     *
     * @param name the name of the pool
     *
     * @return the pool with the matrix
     */
    private Pool createDummyMatrixPool(String name) {
    	Pool pool = new Pool(name);
	float[][] matrix = new float[vectorLength][vectorLength];
	logger.info("creating dummy matrix pool " + name);

	for (int i = 0; i < vectorLength; i++) {
	    for (int j = 0; j < vectorLength; j++) {
		if (i == j) {
		    matrix[i][j] = 1.0F;
		} else {
		    matrix[i][j] = 0.0F;
		}
	    }
	}

	pool.put(0, matrix);
	return pool;
    }

    /**
     * Creates a pool with a single zero vector in it.
     *
     * @param name the name of the pool
     *
     * @return the pool with the vector
     */
    private Pool createDummyVectorPool(String name) {
	logger.info("creating dummy vector pool " + name);
    	Pool pool = new Pool(name);
	float[] vector = new float[vectorLength];

	for (int i = 0; i < vectorLength; i++) {
	    vector[i] = 0.0f;
	}
	pool.put(0, vector);
	return pool;
    }


    /**
     * Returns the properties of the loaded AcousticModel.
     *
     * @return the properties of the loaded AcousticModel, or null if
     *   it has no properties
     */
    public SphinxProperties getModelProperties() {
        return acousticProperties;
    }


    /**
     * Gets the pool of means for this loader
     *
     * @return the pool
     */
    public Pool getMeansPool() {
	return meansPool;
    }


    /**
     * Gets the pool of means transformation matrices for this loader
     *
     * @return the pool
     */
    public Pool getMeansTransformationMatrixPool() {
	return meanTransformationMatrixPool;
    }


    /**
     * Gets the pool of means transformation vectors for this loader
     *
     * @return the pool
     */
    public Pool getMeansTransformationVectorPool() {
	return meanTransformationVectorPool;
    }


    /*
     * Gets the variance pool
     *
     * @return the pool
     */
    public Pool getVariancePool() {
	return variancePool;
    }

    /**
     * Gets the variance transformation matrix pool
     *
     * @return the pool
     */
    public Pool getVarianceTransformationMatrixPool() {
	return varianceTransformationMatrixPool;
    }

    /**
     * Gets the pool of variance transformation vectors for this loader
     *
     * @return the pool
     */
    public Pool getVarianceTransformationVectorPool() {
	return varianceTransformationVectorPool;
    }


    /*
     * Gets the mixture weight pool
     *
     * @return the pool
     */
    public Pool getMixtureWeightPool() {
	return mixtureWeightsPool;
    }

    /*
     * Gets the transition matrix pool
     *
     * @return the pool
     */
    public Pool getTransitionMatrixPool() {
	return matrixPool;
    }

    /*
     * Gets the senone pool for this loader
     *
     * @return the pool
     */
    public Pool getSenonePool() {
	return senonePool;
    }

     /**
      * Returns the size of the left context for context dependent
      * units
      *
      * @return the left context size
      */
     public int getLeftContextSize() {
	 return CONTEXT_SIZE;
     }

     /**
      * Returns the size of the right context for context dependent
      * units
      *
      * @return the left context size
      */
     public int getRightContextSize() {
	 return CONTEXT_SIZE;
     }


    /**
     * Returns the hmm manager associated with this loader
     *
     * @return the hmm Manager
     */
    public HMMManager getHMMManager() {
	return hmmManager;
    }

    /**
     * Log info about this loader
     */
    public void logInfo() {
	logger.info("Sphinx3Loader");
	meansPool.logInfo(logger);
	variancePool.logInfo(logger);
	matrixPool.logInfo(logger);
	senonePool.logInfo(logger);
	meanTransformationMatrixPool.logInfo(logger);
	meanTransformationVectorPool.logInfo(logger);
	varianceTransformationMatrixPool.logInfo(logger);
	varianceTransformationVectorPool.logInfo(logger);
	mixtureWeightsPool.logInfo(logger);
	senonePool.logInfo(logger);
	logger.info("Context Independent Unit Entries: " 
		+ contextIndependentUnits.size());
	hmmManager.logInfo(logger);
    }
}

