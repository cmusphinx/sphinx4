
/**
 * [[[copyright]]]
 */
package edu.cmu.sphinx.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.StreamTokenizer;
import java.io.FileReader;
import java.io.Reader;
import java.io.BufferedReader;


public class ExtendedStreamTokenizer {
    private String path;
    private StreamTokenizer st;
    private Reader reader;

    /**
     * Creates and returns a stream tokenizer that has
     * been properly configured to parse sphinx3 data
     *
     * @param path the source of the data
     *
     * @throws FileNotFoundException if a file cannot be found
     */
    public ExtendedStreamTokenizer(String path) 
			throws FileNotFoundException {
	FileReader fileReader = new FileReader(path);
	reader = new BufferedReader(fileReader);
	this.path = path;
	st = new StreamTokenizer(reader);
	st.resetSyntax();
	st.whitespaceChars(0, 32);
	st.wordChars(33, 127);
	st.eolIsSignificant(false);
	st.commentChar('#');
    }


    /**
     * Closes the tokenizer
     */
    public void close() throws IOException {
	reader.close();
    }

    /**
     * Gets the next word from the tokenizer
     *
     * @throws StreamCorruptedException if the word does not match
     * @throws IOException if an error occurs while loading the data
     */
    public String getString() throws StreamCorruptedException, IOException  {
	if (st.nextToken() != StreamTokenizer.TT_WORD) {
	    corrupt("word expected but not found");
	}
	return st.sval;
    }


    /**
     * Throws an error with the line and path added
     *
     * @param msg the annotation message
     */
    private void corrupt(String msg) throws StreamCorruptedException {
	throw new StreamCorruptedException(
	    msg + " at line " + st.lineno() + " in file " + path);
    }


    /**
     * Loads a word from the tokenizer and ensures that it
     * matches 'expecting'
     *
     * @param expecting	the word read must match this
     *
     * @throws StreamCorruptedException if the word does not match
     * @throws IOException if an error occurs while loading the data
     */
    public void expectString(String expecting) 
    		throws StreamCorruptedException, IOException  {
	if (!getString().equals(expecting)) {
	    corrupt("matching " + expecting);
	}
    }

    /**
     * Loads an integer  from the tokenizer and ensures that it
     * matches 'expecting'
     *
     * @param name	the name of the value
     * @param expecting	the word read must match this
     *
     * @throws StreamCorruptedException if the word does not match
     * @throws IOException if an error occurs while loading the data
     */
    public void expectInt(String name, int expecting) 
    		throws StreamCorruptedException, IOException  {
	int val = getInt( name);
	if (val != expecting)  {
	    corrupt("Expecting integer " + expecting);
	}
    }

    /**
     * gets an integer from the tokenizer stream
     *
     * @param name	the name of the parameter (for error reporting)
     *
     * @return the next word in the stream as an integer
     *
     * @throws StreamCorruptedException if the next value is not a
     * @throws IOException if an error occurs while loading the data
     * number
     */
    public int getInt(String name) 
		throws StreamCorruptedException, IOException  {
	int iVal = 0;
	try {
	    String val = getString();
	    iVal = Integer.parseInt(val);
	} catch (NumberFormatException nfe) {
	    corrupt("while parsing int " + name);
	}
	return iVal;
    }

    /**
     * gets a double from the tokenizer stream
     *
     * @param name	the name of the parameter (for error reporting)
     *
     * @return the next word in the stream as a double
     *
     * @throws StreamCorruptedException if the next value is not a
     * @throws IOException if an error occurs while loading the data
     * number
     */
    public double getDouble(String name) 
    		throws StreamCorruptedException, IOException  {
	double dVal = 0.0;
	try {
	    String val = getString();
	    dVal =  Double.parseDouble(val);
	} catch (NumberFormatException nfe) {
	    corrupt("while parsing double " + name);
	}
	return dVal;
    }

    /**
     * gets a float from the tokenizer stream
     *
     * @param name	the name of the parameter (for error reporting)
     *
     * @return the next word in the stream as a float
     *
     * @throws StreamCorruptedException if the next value is not a
     * @throws IOException if an error occurs while loading the data
     * number
     */
    public float getFloat(String name) 
	throws StreamCorruptedException, IOException  {
	float fVal = 0.0F;
	try {
	    String val = getString();
	    fVal =  Float.parseFloat(val);
	} catch (NumberFormatException nfe) {
	    corrupt("while parsing float " + name);
	}
	return fVal;
    }
}

