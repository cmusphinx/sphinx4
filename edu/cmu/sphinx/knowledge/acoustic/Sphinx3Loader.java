
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

package edu.cmu.sphinx.knowledge.acoustic;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StreamFactory;
import edu.cmu.sphinx.util.ExtendedStreamTokenizer;
import edu.cmu.sphinx.util.LogMath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipException;



/**
 * an acoustic model loader that loads sphinx3 ascii data
 *
 * Mixture weights are maintained in logMath log base, transition
 * probabilities are in linear base.
 */
class Sphinx3Loader implements Loader {

    /**
     * The logger for this class
     */
    private static Logger logger = 
	    Logger.getLogger(AcousticModel.PROP_PREFIX + "AcousticModel");

    private final static String DEFAULT_MODEL_FORMAT = "sphinx4.v1";
    private final static String DEFAULT_MODEL_LOCATION = ".";
    private final static String DEFAULT_MODEL_FILE = "model.mdef";
    private final static String DEFAULT_DATA_DIR = "data";
    private final static String DEFAULT_AM_PROPS_FILE = "am.props";

    private final static String NUM_SENONES = "num_senones";
    private final static String NUM_GAUSSIANS_PER_STATE = "num_gaussians";
    private final static String NUM_STREAMS = "num_streams";

    private final static String FILLER = "filler";
    private final static String SILENCE_CIPHONE  = "SIL";

    private final static String PROPS_MC_FLOOR =
    		AcousticModel.PROP_PREFIX + "MixtureComponentScoreFloor";
    private final static String PROPS_VARIANCE_FLOOR =
	    	AcousticModel.PROP_PREFIX + "varianceFloor";
    private final static String PROPS_MW_FLOOR = 
		AcousticModel.PROP_PREFIX + "mixtureWeightFloor";
    private final static String PROPS_VECTOR_LENGTH = 
                AcousticModel.PROP_PREFIX + "FeatureVectorLength";
    private final static String PROPS_AM_PROPERTIES_FILE = 
                AcousticModel.PROP_PREFIX + "properties_file";
    private final static String PROPS_SPARSE_FORM = 
                AcousticModel.PROP_PREFIX + "sparseForm";

    private final static String FILE_SEPARATOR = "/";

    /**
     * If true (default), instructs the loader to load context
     * dependent units.  Otherwise, the loader will ignore context
     * dependent units.
     */
    private final static String PROPS_USE_CD_UNITS =
	    	AcousticModel.PROP_PREFIX + "useCDUnits";
    
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
    private HMMManager hmmManager;
    private LogMath logMath;
    private SphinxProperties acousticProperties;


    /**
     * Loads the sphinx3 ascii model.
     *
     * @param modelName  the name of the model as specified in the
     *    props file.
     * @param props  the SphinxProperties object
     */
    public Sphinx3Loader(String modelName, SphinxProperties props) throws 
	FileNotFoundException, IOException, ZipException {
	logMath = LogMath.getLogMath(props.getContext());

	// extract the feature vector length
	String vectorLengthProp = PROPS_VECTOR_LENGTH;
	if (modelName != null) {
	    vectorLengthProp = AcousticModel.PROP_PREFIX + modelName +
		".FeatureVectorLength";
	}
	vectorLength = props.getInt(vectorLengthProp, 39);
	
        hmmManager = new HMMManager();
        contextIndependentUnits = new LinkedHashMap();

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
        loadModelFiles(modelName, props);
    }


    /**
     * Loads the sphinx3 ascii model.
     *
     * @param props	the configuration property 
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while loading the data
     */
    public Sphinx3Loader(SphinxProperties props) throws
	FileNotFoundException, IOException, ZipException {
	this(null, props);
    }


    /**
     * Loads the AcousticModel from a directory in the file system.
     *
     * @param modelName the name of the acoustic model; if null we just
     *    load from the default location
     * @param props the SphinxProperties object to use
     */
    private void loadModelFiles(String modelName, SphinxProperties props) 
	throws FileNotFoundException, IOException, ZipException {

	String prefix, location, model, dataDir, propsFile;

	if (modelName == null) {
	    prefix = AcousticModel.PROP_PREFIX;
	} else {
	    prefix = AcousticModel.PROP_PREFIX + modelName + ".";
	}
	// System.out.println("Using prefix: " + prefix);

	location = props.getString(prefix + "location",
				   DEFAULT_MODEL_LOCATION);
	model = props.getString(prefix + "definition_file",
				DEFAULT_MODEL_FILE);
	dataDir = props.getString(prefix + "data_location", 
				  DEFAULT_DATA_DIR) + FILE_SEPARATOR;
	propsFile = props.getString(prefix + "properties_file",
				    DEFAULT_AM_PROPS_FILE);

	float distFloor = props.getFloat(PROPS_MC_FLOOR, 0.0F);
        float varianceFloor = props.getFloat(PROPS_VARIANCE_FLOOR, .0001f);
        float mixtureWeightFloor = props.getFloat(PROPS_MW_FLOOR, 1E-7f);

	logger.info("Loading Sphinx3 ascii acoustic model: " + modelName);
	logger.info("    Path      : " + location);
	logger.info("    modellName: " + model);
	logger.info("    dataDir   : " + dataDir);

        // load the acoustic properties file (am.props), 
	// create a different URL depending on the data format

        String url = null;
        String format = StreamFactory.resolve(location);

        if (format.equals(StreamFactory.ZIP_FILE)) {
            url = "jar:" + location + "!/" + propsFile;
        } else {
            url = "file:" + location + FILE_SEPARATOR + propsFile;
        }

	if (modelName == null) {
	    prefix = props.getContext() + ".acoustic";
	} else {
	    prefix = props.getContext() + ".acoustic." + modelName;
	}
	acousticProperties = loadAcousticPropertiesFile(prefix, url);

        // load the means, variance, mixture weights, and matrix files
        String file = dataDir + "means.ascii";

	meansPool = loadDensityFile
            (StreamFactory.getInputStream(location, file),
             file, -Float.MAX_VALUE);

        file = dataDir + "variances.ascii";
	variancePool = loadDensityFile
            (StreamFactory.getInputStream(location, file),
             file, varianceFloor);

        file = dataDir + "mixture_weights.ascii";
	mixtureWeightsPool = loadMixtureWeights
            (StreamFactory.getInputStream(location, file),
             file, mixtureWeightFloor);

        file = dataDir + "transition_matrices.ascii";
        matrixPool = loadTransitionMatrices
            (StreamFactory.getInputStream(location, file), file);

	senonePool = createSenonePool(distFloor);

        // load the HMM model file
	boolean useCDUnits = props.getBoolean(PROPS_USE_CD_UNITS, true);
	loadHMMPool(useCDUnits,
                    StreamFactory.getInputStream(location, model),
                    location + File.separator + model);
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
     * Creates the senone pool from the rest of the pools.
     *
     * @param distFloor the lowest allowed score 
     *
     * @return the senone pool
     */
    private Pool createSenonePool(float distFloor) {
    	Pool pool = new Pool("senones");
	int numMixtureWeights = mixtureWeightsPool.size();

	int numMeans = meansPool.size();
	int numVariances = variancePool.size();
	int numGaussiansPerSenone = 
            mixtureWeightsPool.getFeature(NUM_GAUSSIANS_PER_STATE, 0);
	int numSenones = mixtureWeightsPool.getFeature(NUM_SENONES, 0);
	int whichGaussian = 0;

	logger.fine("NG " + numGaussiansPerSenone);
	logger.fine("NS " + numSenones);
	logger.fine("NMIX " + numMixtureWeights);
	logger.fine("NMNS " + numMeans);
	logger.fine("NMNS " + numVariances);

	assert numGaussiansPerSenone > 0;
	assert numMixtureWeights == numSenones;
	assert numVariances == numSenones * numGaussiansPerSenone;
	assert numMeans == numSenones * numGaussiansPerSenone;

	for (int i = 0; i < numSenones; i++) {
	    MixtureComponent[] mixtureComponents = new
		MixtureComponent[numGaussiansPerSenone];
	    for (int j = 0; j <  numGaussiansPerSenone; j++) {
		mixtureComponents[j] = new MixtureComponent(
		    logMath,
		    (float[]) meansPool.get(whichGaussian),
		    (float[][]) meanTransformationMatrixPool.get(0),
		    (float[]) meanTransformationVectorPool.get(0),
		    (float[]) variancePool.get(whichGaussian),
		    (float[][]) varianceTransformationMatrixPool.get(0),
		    (float[]) varianceTransformationVectorPool.get(0),
		    distFloor);

		whichGaussian++;
	    }

	    Senone senone = new GaussianMixture( 
	      logMath, (float[]) mixtureWeightsPool.get(i), mixtureComponents);

	    pool.put(i, senone);
	}
	return pool;
    }


    /**
     * Loads the Sphinx 3 acoustic model properties file, which
     * is basically a normal system properties file.
     *
     * @param props the SphinxProperties object 
     * @param path the path to the acoustic properties file
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
     * Loads the sphinx3 densityfile, a set of density arrays are
     * created and placed in the given pool.
     *
     * @param inputStream the inputStream of the density file
     * @param name the name of the data
     * @param floor the minimum density allowed
     *
     * @return a pool of loaded densities
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while loading the data
     */
    private Pool loadDensityFile(InputStream inputStream, String path,
                                 float floor) 
        throws FileNotFoundException, IOException {
	int token_type;
	int numStates;
	int numStreams;
	int numGaussiansPerState;

	if (inputStream == null) {
	    throw new FileNotFoundException("Error trying to read file "
					    + path);
	}
        // 'false' argument refers to EOL is insignificant
	ExtendedStreamTokenizer est = new ExtendedStreamTokenizer
            (inputStream, '#', false);

    	Pool pool = new Pool(path);

	logger.info("Loading density file from: ");
	logger.info(path);

	est.expectString("param");
	numStates = est.getInt("numStates");
	numStreams = est.getInt("numStreams");
	numGaussiansPerState = est.getInt("numGaussiansPerState");

	pool.setFeature(NUM_SENONES, numStates);
	pool.setFeature(NUM_STREAMS, numStreams);
	pool.setFeature(NUM_GAUSSIANS_PER_STATE, numGaussiansPerState);

	for (int i = 0; i < numStates; i++) {
	    est.expectString("mgau");
	    est.expectInt("mgau index", i);
	    est.expectString("feat");
	    est.expectInt("feat index", 0);
	    for (int j = 0; j < numGaussiansPerState; j++) {
		est.expectString("density");
		est.expectInt("densityValue", j);

		float[] density = new float[vectorLength];
		for (int k = 0; k < vectorLength; k++) {
		    density[k] = est.getFloat("val");
		    if (density[k] < floor) {
			density[k] = floor;
		    }
		}
		int id = i * numGaussiansPerState + j;
		pool.put(id, density);
	    }
	}
	est.close();
	return pool;
    }

    /**
     * Loads the sphinx3 densityfile, a set of density arrays are
     * created and placed in the given pool.
     *
     * @param path the path to a density file
     * @param name the name of the data
     *
     * @return a pool of loaded densities
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while loading the data
     */
    private Pool loadHMMPool(boolean useCDUnits,
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

	numStatePerHMM = numStateMap/(numTri+numBase);

	assert numTiedState == mixtureWeightsPool.getFeature(NUM_SENONES, 0);
	assert numTiedTransitionMatrices == matrixPool.size();

	int[] stid = new int[numStatePerHMM-1];

	// Load the base phones
	for (int i = 0; i < numBase; i++) {
	    String name = est.getString();
	    String left = est.getString();
	    String right = est.getString();
	    String position = est.getString();
	    String attribute  = est.getString();
	    int  tmat  = est.getInt("tmat");

	    for (int j=0; j<numStatePerHMM-1; j++) {
                stid[j] = est.getInt("j");
		assert stid[j] >= 0 && stid[j] < numContextIndependentTiedState;
	    }
	    est.expectString("N");

	    assert left.equals("-");
	    assert right.equals("-");
	    assert position.equals("-");
	    assert tmat < numTiedTransitionMatrices;

	    Unit unit = new Unit(name, attribute.equals(FILLER));
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

	    HMM hmm = new HMM(unit,  ss, 
		    	transitionMatrix, HMMPosition.lookup(position));
	    hmmManager.put(hmm);
	}

	// Load the context dependent phones.  If the useCDUnits
	// property is false, the CD phones will not be created, but
        // the values still need to be read in from the file.

        //boolean useCDUnits = props.getBoolean(PROPS_USE_CD_UNITS, true);
        //System.out.println(PROPS_USE_CD_UNITS + "=" + useCDUnits);
        
	for (int i = 0; i < numTri; i++) {
	    String name = est.getString();
	    String left = est.getString();
	    String right = est.getString();
	    String position = est.getString();
	    String attribute  = est.getString();
	    int  tmat  = est.getInt("tmat");

	    for (int j = 0; j < numStatePerHMM-1; j++) {
                stid[j] = est.getInt("j");
		assert stid[j] >= numContextIndependentTiedState && 
                       stid[j] < numTiedState;
	    }
	    est.expectString("N");

	    assert !left.equals("-");
	    assert !right.equals("-");
	    assert !position.equals("-");
	    assert attribute.equals("n/a");
	    assert tmat < numTiedTransitionMatrices;

            if (useCDUnits) {
                Unit[] leftContext = new Unit[1];
                leftContext[0] = (Unit) contextIndependentUnits.get(left);
                
                Unit[] rightContext = new Unit[1];
                rightContext[0] = (Unit) contextIndependentUnits.get(right);
                
                Context context = LeftRightContext.get(leftContext,
                                                       rightContext);

                Unit unit = new Unit(name, context);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Loaded " + unit);
                }

                float[][] transitionMatrix = (float[][]) matrixPool.get(tmat);
                SenoneSequence ss = getSenoneSequence(stid);

                HMM hmm = new HMM(unit,
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
     * Loads the mixture weights
     *
     * @param inputStream the inputStream of the mixture weight file
     * @param floor the minimum mixture weight allowed
     *
     * @return a pool of mixture weights
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while loading the data
     */
    private Pool loadMixtureWeights(InputStream inputStream, String path,
                                    float floor) 
        throws FileNotFoundException, IOException {
	logger.info("Loading mixture weights from: " );
	logger.info(path);

	int numStates;
	int numStreams;
	int numGaussiansPerState;

    	Pool pool = new Pool(path);
	ExtendedStreamTokenizer est = new ExtendedStreamTokenizer
            (inputStream, '#', false);

	est.expectString("mixw");
	numStates = est.getInt("numStates");
	numStreams = est.getInt("numStreams");
	numGaussiansPerState = est.getInt("numGaussiansPerState");

	pool.setFeature(NUM_SENONES, numStates);
	pool.setFeature(NUM_STREAMS, numStreams);
	pool.setFeature(NUM_GAUSSIANS_PER_STATE, numGaussiansPerState);

	for (int i = 0; i < numStates; i++) {
	    est.expectString("mixw");
	    est.expectString("[" + i);
	    est.expectString("0]");
	    float total = est.getFloat("total");
	    float[] logMixtureWeight = new float[numGaussiansPerState];

	    for (int j = 0; j < numGaussiansPerState; j++) {
		// mixtureWeight[j] = est.getFloat("mixwVal");
		float val = est.getFloat("mixwVal");
		if (val < floor) {
		    val = floor;
		}
		logMixtureWeight[j] = (float) logMath.linearToLog(val);
	    }
	    pool.put(i, logMixtureWeight);
	}
	est.close();
	return pool;
    }


    /**
     * Loads the transition matrices
     *
     * @param inputStream the InputStream to the transitions matrices
     *
     * @return a pool of transition matrices
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while loading the data
     */
    private Pool loadTransitionMatrices(InputStream inputStream, String path)
        throws FileNotFoundException, IOException {
        boolean sparseForm 
	    = acousticProperties.getBoolean(PROPS_SPARSE_FORM, true);
	logger.info("Loading transition matrices from: ");
	logger.info( path);
	int numMatrices;
	int numStates;

    	Pool pool = new Pool(path);
	ExtendedStreamTokenizer est = new ExtendedStreamTokenizer
            (inputStream, '#', false);

	est.expectString("tmat");
	numMatrices = est.getInt("numMatrices");
	numStates = est.getInt("numStates");

	for (int i = 0; i < numMatrices; i++) {
	    est.expectString("tmat");
	    est.expectString("[" + i + "]");

	    float[][] tmat = new float[numStates][numStates];

	    for (int j = 0; j < numStates - 1; j++) {
		for (int k = 0; k < numStates ; k++) {
		    if (sparseForm) {
			if (k == j  || k  == j + 1) {
			    tmat[j][k] = est.getFloat("tmat value");
			}
		    } else {
			tmat[j][k] = est.getFloat("tmat value");
		    }

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

