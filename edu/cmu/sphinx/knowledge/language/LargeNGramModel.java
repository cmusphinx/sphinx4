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

package edu.cmu.sphinx.knowledge.language;

import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Utilities;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.StringTokenizer;

// for testing
import java.io.*;
import edu.cmu.sphinx.util.Timer;


/**
 * A simple ARPA language model loader. This loader makes no attempt
 * to optimize storage, so it can only load very small language models
 *
 * Note that all probabilites in the grammar are stored in LogMath log
 * base format. Language Probabilties in the language model file are
 * stored in log 10  base.
 */
public class LargeNGramModel implements LanguageModel {

    private static final String DARPA_LM_HEADER = "Darpa Trigram LM";

    private static final int LOG2_BIGRAM_SEGMENT_SIZE_DEFAULT = 9;


    private SphinxProperties props;
    private LogMath logMath;
    private Map map;
    private Set vocabulary;
    private int lineNumber;
    private String fileName;
    private BufferedReader reader;
    private int maxNGram = 0;
    
    /**
     * Creates a simple ngram model from the data at the URL. The
     * data should be an ARPA format
     *
     * @param context the context for this model
     *
     * @throws IOException if there is trouble loading the data
     */
    public LargeNGramModel(String context) 
        throws IOException, FileNotFoundException {
	initialize(context);
    }
    
    /**
     * Raw constructor
     */
    public LargeNGramModel() {
    }
    
    /**
     * Initializes this LanguageModel
     *
     * @param context the context to associate this linguist with
     */
    public void initialize(String context) throws IOException {
        this.props = SphinxProperties.getSphinxProperties(context);
        
        String format = props.getString
            (LanguageModel.PROP_FORMAT, LanguageModel.PROP_FORMAT_DEFAULT);
        String location = props.getString
            (LanguageModel.PROP_LOCATION, LanguageModel.PROP_LOCATION_DEFAULT);
        float unigramWeight = props.getFloat
            (LanguageModel.PROP_UNIGRAM_WEIGHT, 
	     LanguageModel.PROP_UNIGRAM_WEIGHT_DEFAULT);
        
        map = new HashMap();
        vocabulary = new HashSet();
        logMath = LogMath.getLogMath(context);
        loadBinary(location); //, unigramWeight);
    }
    
    
    /**
     * Called before a recognitino
     */
    public void start() {
    }
    
    /**
     * Called after a recognition
     */
    public void stop() {
    }

    /**
     * apply the unigram weight to the set of unigrams
     */
    private void applyUnigramWeight(float weight) {
    }
    
    /**
     * Gets the ngram probability of the word sequence represented by
     * the word list 
     *
     * @param wordSequence the word sequence
     *
     * @return the probability of the word sequence.
     * Probability is in logMath log base
     *
     */
    public float getProbability(WordSequence wordSequence) {
        float logProbability = 0.0f;

        Probability prob = getProb(wordSequence);
        if (prob == null) {
            if (wordSequence.size() > 1 ) {
                 logProbability = 
		     getBackoff(wordSequence.getOldest())
		     + getProbability(wordSequence.getNewest());
                       
            } else {    // if the single word is not in the model at all
                // then its zero likelihood that we'll use it   
                logProbability = logMath.getLogZero();
            }
        } else {
            logProbability = prob.logProbability;
        }
        if (false) {
            System.out.println(wordSequence + " : " +
                    logProbability
                    + " " + logMath.logToLinear(logProbability));
        }

        return logProbability;
    }


    /**
     * Returns the backoff probability for the give sequence of words
     *
     * @param the sequence of words
     *
     * @return the backoff probability in LogMath log base
     */
    public float getBackoff(WordSequence wordSequence) {
        float logBackoff = 0.0f;           // log of 1.0
        Probability prob = getProb(wordSequence);
        if (prob != null) {
            logBackoff = prob.logBackoff;
        }
        return logBackoff;
    }
    
    /**
     * Returns the maximum depth of the language model
     *
     * @return the maximum depth of the language mdoel
     */
    public int getMaxDepth() {
        return maxNGram;
    }

     /**
      * Returns the set of words in the lanaguage model. The set is
      * unmodifiable.
      *
      * @return the unmodifiable set of words
      */
     public Set getVocabulary() {
         return Collections.unmodifiableSet(vocabulary);
     }
    
    /**
     * Gets the probability entry for the given word sequence or null if
     * there is no entry
     *
     * @param wordSequence a word sequence
     *
     * @return the probability entry for the wordlist or null
     */
    private Probability getProb(WordSequence wordSequence) {
        return (Probability) map.get(wordSequence);
    }
    
    /**
     * Gets the probability entry for the single word
     *
     * @param string the word of interest
     *
     * @return the probability entry or null
     */
    private Probability getProb(String string) {
        return (Probability) map.get(string);
    }
    

    /**
     * Converts a wordList to a string
     *
     * @param wordLIst the wordList
     *
     * @return the string
     */
    private String listToString(List wordList) {
        StringBuffer sb = new StringBuffer();
        for (Iterator i = wordList.iterator(); i.hasNext(); ) {
            sb.append(i.next().toString());
            sb.append(" ");
        }
        return sb.toString();
    }
    
    /**
     * Provides the log base that controls the range of probabilities
     * returned by this N-Gram
     */
    public void setLogMath(LogMath logMath) {
        this.logMath = logMath;
    }


    /**
     * Returns the log math the controls the log base for the range of
     * probabilities used by this n-gram
     */
    public LogMath getLogMath() {
        return this.logMath;
    }

    
    /**
     * Dumps the language model
     */
    public void dump() {
        for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            Probability prob = (Probability) map.get(key);
            System.out.println(key + " " + prob);
        }
    }
    

    /**
     * Loads the language model from the given location. 
     *
     * @param location the location of the language model
     */
    private void loadBinary(String location) throws IOException {
	boolean bigEndian = true;

	DataInputStream stream = new DataInputStream
	    (new BufferedInputStream(new FileInputStream(location)));
	
	// read standard header string-size; set bigEndian flag
	
	int headerLength = stream.readInt();
	if (headerLength != (DARPA_LM_HEADER.length() + 1)) { // not big-endian
	    headerLength = Utilities.swapInteger(headerLength);
	    if (headerLength == (DARPA_LM_HEADER.length() + 1)) {
		bigEndian = false;
	    } else {
		throw new Error
		    ("Bad binary LM file magic number: " + headerLength +
		     ", not an LM dumpfile?");
	    }
	}

	// read and verify standard header string

	StringBuffer header = new StringBuffer();
	for (int i = 0; i < headerLength - 1; i++) {
	    header.append((char)stream.readByte());
	}
	stream.readByte(); // read the '\0'

	if (!header.toString().equals(DARPA_LM_HEADER)) {
	    throw new Error("Bad binary LM file header: " + header);
	}

	// read LM filename string size and string
	
	int fileNameLength = readInt(stream, bigEndian);
	for (int i = 0; i < fileNameLength; i++) {
	    stream.readByte();
	}

	int numberUnigrams = 0;
	int logBigramSegmentSize = LOG2_BIGRAM_SEGMENT_SIZE_DEFAULT;
	
	// read version number, if present. it must be <= 0.

	int version = readInt(stream, bigEndian);
	System.out.println("Version: " + version);
	if (version <= 0) { // yes, its the version number
	    readInt(stream, bigEndian); // read and skip timestamp
	    
	    // read and skip format description
	    int formatLength;
	    for (;;) {
		if ((formatLength = readInt(stream, bigEndian)) == 0) {
		    break;
		}
		for (int i = 0; i < formatLength; i++) {
		    stream.readByte();
		}
	    }

	    // read log bigram segment size if present
	    if (version <= -2) {
		logBigramSegmentSize = readInt(stream, bigEndian);
		if (logBigramSegmentSize < 1 || logBigramSegmentSize > 15) {
		    throw new Error("log2(bg_seg_sz) outside range 1..15");
		}
	    }

	    numberUnigrams = readInt(stream, bigEndian);
	} else {
	    numberUnigrams = version;
	}

	if (numberUnigrams <= 0) {
	    throw new Error("Bad number of unigrams: " + numberUnigrams +
			    ", must be > 0.");
	}
	System.out.println("# of unigrams: " + numberUnigrams);

	int numberBigrams = readInt(stream, bigEndian);
	if (numberBigrams < 0) {
	    throw new Error("Bad number of bigrams: " + numberBigrams);
	}
	int numberTrigrams = readInt(stream, bigEndian);
	if (numberTrigrams < 0) {
	    throw new Error("Bad number of trigrams: " + numberTrigrams);
	}

	readUnigrams(stream, numberUnigrams, bigEndian);

	System.out.println("# of bigrams: " + numberBigrams);

	// skip all the bigram entries, the +1 is the sentinel at the end
	if (numberBigrams > 0) {
	    stream.skipBytes((numberBigrams + 1) * 10);
	}

	System.out.println("# of trigrams: " + numberTrigrams);

	// skip all the trigram entries
	if (numberTrigrams > 0) {
	    stream.skipBytes(numberTrigrams * 6);
	}

	// read the bigram probabilities table
	if (numberBigrams > 0) {
	    float[] bigramProbTable = 
		readProbabilitiesTable(stream, bigEndian);
	}

	// read the trigram backoff weight table and trigram prob table
	if (numberTrigrams > 0) {
	    float[] trigramBackoffTable =
		readProbabilitiesTable(stream, bigEndian);
	    float[] trigramProbTable =
		readProbabilitiesTable(stream, bigEndian);
	    /*
	    float[] trigramSegmentTable =
		readTrigramSegmentTable(stream, bigEndian);
	    */
	}

	// to be continued
    }


    /**
     * Reads the probability table from the given DataInputStream.
     *
     * @param stream the DataInputStream from which to read the table
     * @param bigEndian true if the given stream is bigEndian, false otherwise
     */
    private float[] readProbabilitiesTable(DataInputStream stream, 
					   boolean bigEndian) 
	throws IOException {
	int numProbs = readInt(stream, bigEndian);
	if (numProbs <= 0 || numProbs > 65536) {
	    throw new Error("Bad probabilities table size: "+ numProbs);
	}
	float[] probTable = new float[numProbs];
	for (int i = 0; i < numProbs; i++) {
	    probTable[i] = readFloat(stream, bigEndian);
	}
	return probTable;
    }


    /**
     * Read in the unigrams in the given DataInputStream.
     *
     * @param stream the DataInputStream to read from
     * @param numberUnigrams the number of unigrams to read
     * @param bigEndian true if the DataInputStream is big-endian,
     *                  false otherwise
     */
    private void readUnigrams(DataInputStream stream, int numberUnigrams,
			      boolean bigEndian) throws IOException {

	for (int i = 0; i < numberUnigrams; i++) {

	    // read unigram ID, unigram probability, unigram backoff weight
	    int unigramID = readInt(stream, bigEndian);
	    float unigramProbability = readFloat(stream, bigEndian);
	    float unigramBackoffWeight = readFloat(stream, bigEndian);
	    int firstBigramEntry = readInt(stream, bigEndian);

	    if (false) {
		System.out.println("Unigram: ID: " + unigramID +
				   ", Prob: " + unigramProbability +
				   ", BackoffWeight: " + unigramBackoffWeight +
				   ", FirstBigramEntry: " + firstBigramEntry);
	    }
	}
    }


    /**
     * Reads an integer from the given DataInputStream.
     *
     * @param stream the DataInputStream to read from
     * @param bigEndian true if the DataInputStream is in bigEndian,
     *                  false otherwise
     */
    private int readInt(DataInputStream stream, boolean bigEndian) 
	throws IOException {
	if (bigEndian) {
	    return stream.readInt();
	} else {
	    return Utilities.readLittleEndianInt(stream);
	}
    }


    /**
     * Reads a float from the given DataInputStream.
     *
     * @param stream the DataInputStream to read from
     * @param bigEndian true if the DataInputStream is in bigEndian,
     *                  false otherwise
     */
    private float readFloat(DataInputStream stream, boolean bigEndian)
	throws IOException {
	if (bigEndian) {
	    return stream.readFloat();
	} else {
	    return Utilities.readLittleEndianFloat(stream);
	}
    }

    
    /**
     * Puts the probability into the map
     * 
     * @param wordSequence the tag for the prob.
     * @param logProb the probability in log math base 
     * @param logBackoff the backoff probability in log math base 
     */
    private void put(WordSequence wordSequence, 
            float logProb, float logBackoff) {

        if (false) {
            System.out.println("Putting " + wordSequence + " p " +
                    logProb + " b " + logBackoff);
        }
        map.put(wordSequence, new Probability(logProb, logBackoff));
    }
    
    /**
     * Reads the next line from the LM file. Keeps track of line
     * number.
     *
     * @throws IOException if an error occurs while reading the input
     * or an EOF is encountered.
     *
     */
    private String readLine() throws IOException {
        String line;
        lineNumber++;
        line = reader.readLine();
        if (line == null) {
            corrupt("Premature EOF");
        }
        return line;
    }
    
    /**
     * Opens the language model at the given location
     *
     * @param location the path to the language model
     *
     * @throws IOException if an error occurs while opening the file
     */
    private void open(String location) throws
    FileNotFoundException, IOException {
        lineNumber = 0;
        fileName = location;
        reader = new BufferedReader(new FileReader(location));
    }
    
    /**
     * Reads from the input stream until the input matches the given
     * string
     *
     * @param match the string to match on
     *
     * @throws IOException if an error occurs while reading the input
     * or an EOF is encountered before finding the match
     */
    private void readUntil(String match) throws IOException {
        try {
            while (!readLine().equals(match)) {
            }
        } catch (IOException ioe) {
            corrupt("Premature EOF while waiting for " + match);
        }
    }
    
    /**
     * Closes the language model file
     *
     * @throws IOException if an error occurs
     */
    private void close() throws IOException {
        reader.close();
        reader = null;
    }
    
    /**
     * Generates a 'corrupt' IO exception
     *
     * @throws an IOException with the given string
     */
    private void corrupt(String why) throws IOException {
        throw new IOException("Corrupt Language Model " + fileName +
                              " at line " +
                              lineNumber + ":" + why);
    }
    
        
    /**
     * A test routine
     *
     */
    public static void main(String[] args) throws Exception {
        String propsPath; 
        if (args.length == 0) {
            propsPath = "file:./test.props";
        } else {
            propsPath = args[0];
        }

        Timer.start("LM Load");
        SphinxProperties.initContext("test", new URL(propsPath));
        LargeNGramModel sm = new LargeNGramModel("test");
        Timer.stop("LM Load");

        Timer.dumpAll();

        LogMath logMath = LogMath.getLogMath("test");
        
        BufferedReader reader = new BufferedReader
            (new InputStreamReader(System.in));
        
        String input;
        
        System.out.println("Max depth is " + sm.getMaxDepth());
        System.out.print("Enter words: ");
        while ((input = reader.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(input);
            List list = new ArrayList();
            while (st.hasMoreTokens()) {
                String tok = (String) st.nextToken();
                list.add(tok);
            }
            WordSequence wordSequence = new WordSequence(list);
            System.out.println("Probability of " + wordSequence + " is: " +
               sm.getProbability(wordSequence) + "(" 
               + logMath.logToLn(sm.getProbability(wordSequence)) + ")");
            System.out.print("Enter words: ");
        }
        
        
        Timer timer = Timer.getTimer("test", "lookup trigram");
        
        List list1 = new ArrayList();
        WordSequence ws1 = new WordSequence("t", "h", "e");
        WordSequence ws2 = new WordSequence("a", "l", "q");
        
        for (int i = 0; i < 1000000; i++) {
            timer.start();
            sm.getProbability(ws1);
            timer.stop();
            timer.start();
            sm.getProbability(ws2);
            timer.stop();
        }
        
        Timer.dumpAll("test");
    }
}

/**
 * Represents a probability and a backoff probability
 */
class Probability {
    float logProbability;
    float logBackoff;

    /**
     * Constructs a probability
     *
     * @param probability the probability
     * @param backoff the backoff probability
     */
    Probability(float logProbability, float logBackoff) {
        this.logProbability = logProbability;
        this.logBackoff = logBackoff;
    }
    
    /**
     * Returns a string representation of this object
     *
     * @return the string form of this object
     */
    public String toString() {
        return "Prob: " + logProbability + " " + logBackoff;
    }
};

