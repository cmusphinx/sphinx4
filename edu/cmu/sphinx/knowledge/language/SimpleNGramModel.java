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

import java.io.BufferedReader;
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
public class SimpleNGramModel implements LanguageModel {

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
    public SimpleNGramModel(String context) 
        throws IOException, FileNotFoundException {
            initialize(context);
    }
    
    /**
     * Raw constructor
     */
    public SimpleNGramModel() {
    }
    
    /**
     * Initializes this LanguageModel
     *
     * @param context the context to associate this linguist with
     */
    public void initialize(String context)  throws IOException {
        this.props = SphinxProperties.getSphinxProperties(context);
        
        String format = props.getString
            (LanguageModel.PROP_FORMAT, LanguageModel.PROP_FORMAT_DEFAULT);
        String location = props.getString
            (LanguageModel.PROP_LOCATION, LanguageModel.PROP_LOCATION_DEFAULT);
        float unigramWeight = props.getFloat
            (LanguageModel.PROP_UNIGRAM_WEIGHT, LanguageModel.PROP_UNIGRAM_WEIGHT_DEFAULT);
        
        map = new HashMap();
        vocabulary = new HashSet();
        logMath = LogMath.getLogMath(context);
        load(format, location, unigramWeight);
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
     * Retrieves a string representation of the wordlist, suitable
     * for map access
     *
     * @param wordList the list of words
     *
     * @return a string representation of the word list
     */
    private String getRepresentation(List wordList) {
        StringBuffer sb = new StringBuffer();
        
        for (Iterator i = wordList.iterator(); i.hasNext(); ) {
            String s = (String) i.next();
            sb.append(s);
            if (i.hasNext()) {
                sb.append("+");
            }
        }
        return sb.toString();
    }
    
    
    /**
     * Loads the language model from the given location. 
     *
     * @param format the format of the model
     * @param location the location of the model
     * @param unigramWeight the unigram weight
     * 
     * @throws IOException if an error occurs while loading
     */
    private void load(String format, String location, float unigramWeight) 
        throws FileNotFoundException, IOException {
        
        String line;
        float logUnigramWeight = logMath.linearToLog(unigramWeight);
        float inverseLogUnigramWeight = 
            logMath.linearToLog(1.0 - unigramWeight);
        
        if (!format.equals("arpa")) {
            throw new IOException("Loading of " + format + 
                                  " language models not supported");
        }
        
        open(location);
        
        // look for beginning of data
        readUntil("\\data\\");
         
        // look for ngram statements
        
        List ngramList = new ArrayList();
        while ((line = readLine()) != null) {
            if (line.startsWith("ngram")) {
                StringTokenizer st = new StringTokenizer(line, " \t\n\r\f=");
                if (st.countTokens() != 3) {
                    corrupt("corrupt ngram field " + line + " " +
                            st.countTokens() );
                }
                st.nextToken();
                int index = Integer.parseInt(st.nextToken());
                int count = Integer.parseInt(st.nextToken());
                ngramList.add(index - 1, new Integer(count));
                if (index > maxNGram) {
                    maxNGram = index;
                }
            } else if (line.equals("\\1-grams:")) {
                break;
            }
        }

        int numUnigrams = ((Integer) ngramList.get(0)).intValue() - 1;
        // -log(x) = log(1/x)
        float logUniformProbability = -logMath.linearToLog(numUnigrams);

        
        for (int index = 0; index < ngramList.size(); index++) { 
            int ngram = index + 1;
            int ngramCount = ((Integer) ngramList.get(index)).intValue();
            for (int i = 0; i < ngramCount; i++) {
                StringTokenizer tok = new StringTokenizer(readLine());
                int tokenCount = tok.countTokens();
                if (tokenCount != ngram + 1 && tokenCount != ngram + 2) {
                    corrupt("Bad format");
                }
                float log10Prob = Float.parseFloat(tok.nextToken());
                float log10Backoff = 0.0f;

                List wordList = new ArrayList(3);
                for (int j = 0; j < ngram; j++) {
                    String word = tok.nextToken().toLowerCase();
                    vocabulary.add(word);
                    wordList.add(word);
                }
                WordSequence wordSequence = new WordSequence(wordList);
                if (tok.hasMoreTokens()) {
                    log10Backoff = Float.parseFloat(tok.nextToken());
                }

                float logProb = logMath.log10ToLog(log10Prob);
                float logBackoff = logMath.log10ToLog(log10Backoff);

                // Apply unigram weights if this is a unigram probability
                if (ngram == 1) {
                    float p1 = logProb + logUnigramWeight;
                    float p2 = logUniformProbability + inverseLogUnigramWeight;
                    logProb = logMath.addAsLinear(p1, p2);
                }
                put(wordSequence, logProb, logBackoff);
            }
            
            if (index < ngramList.size() - 1) {
                String next = "\\" + (ngram + 1) + "-grams:";
                readUntil(next);
            } 
        }
        readUntil("\\end\\");
        close();
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
        SimpleNGramModel sm = new SimpleNGramModel("test");
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

