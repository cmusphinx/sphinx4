
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
import edu.cmu.sphinx.util.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipException;



/**
 * an acoustic model loader that loads sphinx3 ascii data
 *
 * Mixture weights and transition probabilities are maintained in logMath 
 * log base, 
 */
class Sphinx3Loader implements Loader {

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
     * Loads the sphinx3 ascii model.
     *
     * @param modelName  the name of the model as specified in the
     *    props file.
     * @param props  the SphinxProperties object
     * @param binary  if <code>true</code> the file is in binary
     * format
     */
    public Sphinx3Loader(String modelName, SphinxProperties props, 
            boolean binary) throws 
	FileNotFoundException, IOException, ZipException {

        this.binary = binary;
	logMath = LogMath.getLogMath(props.getContext());

	// extract the feature vector length
	String vectorLengthProp = AcousticModel.PROP_VECTOR_LENGTH;
	if (modelName != null) {
	    vectorLengthProp = AcousticModel.PROP_PREFIX + modelName +
		".FeatureVectorLength";
	}
	vectorLength = props.getInt
	    (vectorLengthProp, AcousticModel.PROP_VECTOR_LENGTH_DEFAULT);
	
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
     * Loads the AcousticModel from a directory in the file system.
     *
     * @param modelName the name of the acoustic model; if null we just
     *    load from the default location
     * @param props the SphinxProperties object to use
     */
    private void loadModelFiles(String modelName, SphinxProperties props) 
	throws FileNotFoundException, IOException, ZipException {

	String prefix, model, dataDir, propsFile;

	if (modelName == null) {
	    prefix = AcousticModel.PROP_PREFIX;
	} else {
	    prefix = AcousticModel.PROP_PREFIX + modelName + ".";
	}
	// System.out.println("Using prefix: " + prefix);

	location = props.getString
	    (prefix + "location", AcousticModel.PROP_LOCATION_DEFAULT);
	model = props.getString
	    (prefix + "definition_file", AcousticModel.PROP_MODEL_DEFAULT);
	dataDir = props.getString
	    (prefix + "data_location", 
	     AcousticModel.PROP_DATA_LOCATION_DEFAULT) + "/";
	propsFile = props.getString
	    (prefix + "properties_file", 
	     AcousticModel.PROP_PROPERTIES_FILE_DEFAULT);

	float distFloor = props.getFloat(AcousticModel.PROP_MC_FLOOR, 
					 AcousticModel.PROP_MC_FLOOR_DEFAULT);
        float mixtureWeightFloor = 
	    props.getFloat(AcousticModel.PROP_MW_FLOOR, 
			   AcousticModel.PROP_MW_FLOOR_DEFAULT);
        float varianceFloor = 
	    props.getFloat(AcousticModel.PROP_VARIANCE_FLOOR, 
			   AcousticModel.PROP_VARIANCE_FLOOR_DEFAULT);

	logger.info("Loading Sphinx3 acoustic model: " + modelName);
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
            url = "file:" + location + "/" + propsFile;
        }

	if (modelName == null) {
	    prefix = props.getContext() + ".acoustic";
	} else {
	    prefix = props.getContext() + ".acoustic." + modelName;
	}
	acousticProperties = loadAcousticPropertiesFile(prefix, url);


        if (binary) {
            meansPool = loadDensityFileBinary(dataDir + "means",
                    -Float.MAX_VALUE);
            variancePool = loadDensityFileBinary(dataDir + "variances",
                    varianceFloor);
            mixtureWeightsPool = loadMixtureWeightsBinary(
                dataDir + "mixture_weights", mixtureWeightFloor);
           
            matrixPool = loadTransitionMatricesBinary(
                    dataDir + "transition_matrices");

        } else  {
            meansPool = loadDensityFileAscii(dataDir + "means.ascii",
                    -Float.MAX_VALUE);
            variancePool = loadDensityFileAscii(dataDir + "variances.ascii",
                    varianceFloor);
            mixtureWeightsPool = loadMixtureWeightsAscii(
                    dataDir + "mixture_weights.ascii", mixtureWeightFloor);
            matrixPool = loadTransitionMatricesAscii(
                    dataDir + "transition_matrices.ascii");
        }

	senonePool = createSenonePool(distFloor, varianceFloor);

        // load the HMM model file
	boolean useCDUnits = props.getBoolean
	    (AcousticModel.PROP_USE_CD_UNITS, 
	     AcousticModel.PROP_USE_CD_UNITS_DEFAULT);
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
     * @param varianceFloor the lowest allowed variance
     *
     * @return the senone pool
     */
    private Pool createSenonePool(float distFloor, float varianceFloor) {
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
		    distFloor,
		    varianceFloor);

		whichGaussian++;
	    }

	    Senone senone = new GaussianMixture( 
	      logMath, (float[]) mixtureWeightsPool.get(i), 
              mixtureComponents, i);

	    pool.put(i, senone);
	}
	return pool;
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
     * Loads the sphinx3 densityfile, a set of density arrays are
     * created and placed in the given pool.
     *
     * @param path the name of the data
     * @param floor the minimum density allowed
     *
     * @return a pool of loaded densities
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while loading the data
     */
    private Pool loadDensityFileAscii(String path, float floor) 
        throws FileNotFoundException, IOException {
	int token_type;
	int numStates;
	int numStreams;
	int numGaussiansPerState;

        InputStream inputStream = StreamFactory.getInputStream(location, path);

	if (inputStream == null) {
	    throw new FileNotFoundException("Error trying to read file "
                                        + location + path);
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
                 //   System.out.println(" " + i + " " + j + " " + k +
                  //          " " + density[k]);
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
     * @param path the name of the data
     * @param floor the minimum density allowed
     *
     * @return a pool of loaded densities
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while loading the data
     */
    private Pool loadDensityFileBinary(String path, float floor) 
        throws FileNotFoundException, IOException {
	int token_type;
	int numStates;
	int numStreams;
	int numGaussiansPerState;
        Properties props = new Properties();
        int blockSize = 0;

        DataInputStream dis = readS3BinaryHeader(location, path, props);

        String version = props.getProperty("version");
        boolean doCheckSum;

        if (version == null || !version.equals(DENSITY_FILE_VERSION)) {
            throw new IOException("Unsupported version in " + path);
        }

        String checksum = props.getProperty("chksum0");
        doCheckSum =  (checksum != null && checksum.equals("yes"));

        numStates = readInt(dis);
        numStreams = readInt(dis);
        numGaussiansPerState = readInt(dis);


        int[] vectorLength = new int[numStreams];
        for (int i = 0; i < numStreams; i++) {
            vectorLength[i] = readInt(dis);
        }

        int rawLength = readInt(dis);

        //System.out.println("Nstates " + numStates);
        //System.out.println("Nstreams " + numStreams);
        //System.out.println("NgaussiansPerState " + numGaussiansPerState);
        //System.out.println("vectorLength " + vectorLength.length);
        //System.out.println("rawLength " + rawLength);

        for (int i = 0;  i < numStreams; i++) {
            blockSize += vectorLength[i];
        }


        assert rawLength == numGaussiansPerState * blockSize * numStates;
        assert numStreams == 1;

    	Pool pool = new Pool(path);
	pool.setFeature(NUM_SENONES, numStates);
	pool.setFeature(NUM_STREAMS, numStreams);
	pool.setFeature(NUM_GAUSSIANS_PER_STATE, numGaussiansPerState);

        int r = 0;
	for (int i = 0; i < numStates; i++) {
            for (int j = 0; j < numStreams; j++) {
                for (int k = 0; k < numGaussiansPerState; k++) {
                    float[] density = readFloatArray(dis, vectorLength[j]);
                    floorData(density, floor);
                    pool.put(i * numGaussiansPerState + k, density);
		}
	    }
	}

        int checkSum = readInt(dis);
        // BUG: not checking the check sum yet.
        dis.close();
        return pool;
    }


    /**
     * Reads the S3 binary hearder from the given location+path. Adds
     * header information to the given set of properties.
     *
     * @param location the location of the file
     * @param path the name of the file
     * @param props the properties
     *
     * @return the input stream positioned after the header
     *
     * @throws IOException on error
     */

    private DataInputStream readS3BinaryHeader(String location, String
            path, Properties props) throws IOException {

        InputStream inputStream = StreamFactory.getInputStream(location, path);
        DataInputStream dis = new DataInputStream(new
                BufferedInputStream(inputStream));

        String id = readWord(dis);
        if (!id.equals("s3")) {
            throw new IOException("Not proper s3 binary file " +
                    location + path);
        }

        String name;
        while ((name = readWord(dis)) != null) {
            if (!name.equals("endhdr")) {
                String value = readWord(dis);
                props.setProperty(name, value);
            } else {
                break;
            }
        }

        int byteOrderMagic = dis.readInt();

        if (byteOrderMagic == BYTE_ORDER_MAGIC) {
            // System.out.println("Not swapping " + path);
            swap = false;
        } else if (byteSwap(byteOrderMagic) == BYTE_ORDER_MAGIC) {
            // System.out.println("SWAPPING " + path);
            swap = true;
        } else {
            throw new IOException("Corrupt S3 file " + location + path);
        }

        return dis;
    }

    /**
     * Reads the next word (text separated by whitespace) from the
     * given stream
     *
     * @param dis the input stream
     *
     * @return the next word
     *
     * @throws IOException on error
     */
    String readWord(DataInputStream dis) throws IOException {
        StringBuffer sb = new StringBuffer();
        char c;

        // skip leading whitespace
        do {
            c = readChar(dis);
        } while(Character.isWhitespace(c));

        // read the word

        do {
            sb.append(c);
            c = readChar(dis);
        } while (!Character.isWhitespace(c));
        return sb.toString();
    }


    /**
     * Reads a single char from the stream
     *
     * @param dis the stream to read
     * @return the next character on the stream
     *
     * @throws IOException if an error occurs
     */
    private char readChar(DataInputStream dis) throws IOException {
        return (char) dis.readByte();
    }

    /**
     * swap a 32 bit word
     *
     * @param val the value to swap
     *
     * @return the swapped value
     */
    private int byteSwap(int val) {
        return ((0xff & (val >>24)) | (0xff00 & (val >>8)) |
         (0xff0000 & (val <<8)) | (0xff000000 & (val <<24)));
    }

    /**
     * Read an integer from the input stream, byte-swapping as
     * necessary
     *
     * @param dis the inputstream
     *
     * @return an integer value
     *
     * @throws IOException on error
     */
    private int readInt(DataInputStream dis) throws IOException {
        if (swap) {
            return Utilities.readLittleEndianInt(dis);
        } else {
            return dis.readInt();
        }
    }

    /**
     * Read a float from the input stream, byte-swapping as
     * necessary
     *
     * @param dis the inputstream
     *
     * @return a floating pint value
     *
     * @throws IOException on error
     */

    private float readFloat(DataInputStream dis) throws IOException {
        float val;
        if (swap) {
            val =  Utilities.readLittleEndianFloat(dis);
        } else {
            val =  dis.readFloat();
        }
        return val;
    }

    // Do we need the method nonZeroFloor??
    /**
     * If a data point is non-zero and below 'floor' make
     * it equal to floor (don't floor zero values though).
     *
     * @param data the data to floor
     * @param floor the floored value
     */
    private void nonZeroFloor(float[] data, float floor) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] != 0.0 && data[i] < floor) {
                data[i] = floor;
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
            for (int i = 0; i < data.length; i++) {
                 data[i] = data[i] / sum ;
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
     * Reads the given number of floats from the stream and returns
     * them in an array of floats
     *
     * @param dis the stream to read data from
     * @param size the number of floats to read
     *
     * @return an array of size float elements
     *
     * @throws IOException if an exception occurs
     */
    private float[] readFloatArray(DataInputStream dis, int size)
        throws IOException{
        float[] data = new float[size];

        for (int i = 0; i < size; i++) {
            data[i] = readFloat(dis);
        }
        return data;
    }

    /**
     * Loads the sphinx3 densityfile, a set of density arrays are
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

	    for (int j=0; j < numStatePerHMM-1; j++) {
                stid[j] = est.getInt("j");
		assert stid[j] >= 0 && stid[j] < numContextIndependentTiedState;
	    }
	    est.expectString("N");

	    assert left.equals("-");
	    assert right.equals("-");
	    assert position.equals("-");
	    assert tmat < numTiedTransitionMatrices;

	    Unit unit = Unit.createCIUnit(name, attribute.equals(FILLER));
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

        String lastUnitName = "";
        Unit lastUnit = null;

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
                    unit = Unit.createCDUnit(name, false, context);
                    lastUnit = unit;
                }

                lastUnitName = unitName;

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
     * @param path the path to the mixture weight file
     * @param floor the minimum mixture weight allowed
     *
     * @return a pool of mixture weights
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while loading the data
     */
    private Pool loadMixtureWeightsAscii(String path,
                                    float floor) 
        throws FileNotFoundException, IOException {
	logger.info("Loading mixture weights from: " );
	logger.info(path);

	int numStates;
	int numStreams;
	int numGaussiansPerState;

        InputStream inputStream = StreamFactory.getInputStream(location, path);
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
		float val = est.getFloat("mixwVal");
		if (val < floor) {
		    val = floor;
		}
		logMixtureWeight[j] =  val;

	    }
            convertToLogMath(logMixtureWeight);
	    pool.put(i, logMixtureWeight);
	}
	est.close();
	return pool;
    }

    /**
     * Loads the mixture weights (Binary)
     *
     * @param path the path to the mixture weight file
     * @param floor the minimum mixture weight allowed
     *
     * @return a pool of mixture weights
     *
     * @throws FileNotFoundException if a file cannot be found
     * @throws IOException if an error occurs while loading the data
     */
    private Pool loadMixtureWeightsBinary(String path,
                                    float floor) 
        throws FileNotFoundException, IOException {
	logger.info("Loading mixture weights from: " );
	logger.info(path);

	int numStates;
	int numStreams;
	int numGaussiansPerState;
        int numValues;
        Properties props = new Properties();

        DataInputStream dis = readS3BinaryHeader(location, path, props);

        String version = props.getProperty("version");
        boolean doCheckSum;

        if (version == null || !version.equals(MIXW_FILE_VERSION)) {
            throw new IOException("Unsupported version in " + path);
        }

        String checksum = props.getProperty("chksum0");
        doCheckSum =  (checksum != null && checksum.equals("yes"));

    	Pool pool = new Pool(path);

	numStates = readInt(dis);
	numStreams = readInt(dis);
	numGaussiansPerState = readInt(dis);
	numValues = readInt(dis);

        assert numValues == numStates * numStreams * numGaussiansPerState;
        assert numStreams == 1;


	pool.setFeature(NUM_SENONES, numStates);
	pool.setFeature(NUM_STREAMS, numStreams);
	pool.setFeature(NUM_GAUSSIANS_PER_STATE, numGaussiansPerState);

	for (int i = 0; i < numStates; i++) {
	    float[] logMixtureWeight = readFloatArray(dis,numGaussiansPerState);
            normalize(logMixtureWeight);
            floorData(logMixtureWeight, floor);
            convertToLogMath(logMixtureWeight);
	    pool.put(i, logMixtureWeight);
	}
        dis.close();
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
    private Pool loadTransitionMatricesAscii(String path)
        throws FileNotFoundException, IOException {
        InputStream inputStream = StreamFactory.getInputStream(location, path);
        boolean sparseForm = acousticProperties.getBoolean
	    (AcousticModel.PROP_SPARSE_FORM, 
	     AcousticModel.PROP_SPARSE_FORM_DEFAULT);
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
    private Pool loadTransitionMatricesBinary(String path)
        throws FileNotFoundException, IOException {
        boolean sparseForm = acousticProperties.getBoolean
	    (AcousticModel.PROP_SPARSE_FORM, 
	     AcousticModel.PROP_SPARSE_FORM_DEFAULT);
	logger.info("Loading transition matrices from: ");
	logger.info( path);
	int numMatrices;
	int numStates;
        int numRows;
        int numValues;


        Properties props = new Properties();
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
	numRows = readInt(dis);
	numStates = readInt(dis);
	numValues = readInt(dis);

        assert numValues == numStates * numRows * numMatrices;

	for (int i = 0; i < numMatrices; i++) {
	    float[][] tmat = new float[numStates][];
            // last row should be zeros
            tmat[numStates -1] = new float[numStates];
            convertToLogMath(tmat[numStates-1]);

	    for (int j = 0; j < numRows; j++) {
                tmat[j] = readFloatArray(dis, numStates);
                nonZeroFloor(tmat[j], 0f);
                normalize(tmat[j]);
                convertToLogMath(tmat[j]);
	    }
	    pool.put(i, tmat);
	}
	dis.close();
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

