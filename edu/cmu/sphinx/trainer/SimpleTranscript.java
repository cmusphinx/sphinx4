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

package edu.cmu.sphinx.trainer;


import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.knowledge.acoustic.*;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * Provides mechanisms for accessing a next utterance's file name
 * and transcription.
 */
public class SimpleTranscript implements Transcript {

    private SphinxProperties props;	// the sphinx properties
    private String controlFile;         // the control file
    private String transcriptFile;      // the transcript file
    private String dictionary;          // the dictionary
    private String wordSeparator;       // the word separator
    private int currentPartition;       // the current partition
    private int numberOfPartitions;    // total number of partitions
    private Iterator controlFileIterator;
    private Iterator transcriptFileIterator;

    /**
     * Initializes the SimpleTranscript with the proper context.
     *
     * @param context the context to use
     * @param thisPartition the current partition of the transcript file
     * @param numberOfPartitions the total number of partitions
     */
    public void initialize(String context, int thisPartition, 
			   int numberOfPartitions) {
	this.props = SphinxProperties.getSphinxProperties(context);
	this.controlFile = props.getString(PROP_CONTROL_FILE, 
					   PROP_CONTROL_FILE_DEFAULT);
	this.transcriptFile = props.getString(PROP_TRANSCRIPT_FILE, 
					   PROP_TRANSCRIPT_FILE_DEFAULT);
	this.dictionary = null; // here, we don't care
	this.wordSeparator = " \t\n\r\f"; // the white spaces
	this.currentPartition = thisPartition;
	this.numberOfPartitions = numberOfPartitions;
	try {
	    this.controlFileIterator = getLines(controlFile).iterator();
	} catch (IOException ioe) {
	    throw new Error("IOE: Can't open file " + controlFile, ioe);
	}
	try {
	    this.transcriptFileIterator = getLines(transcriptFile).iterator();
	} catch (IOException ioe) {
	    throw new Error("IOE: Can't open file " + transcriptFile, ioe);
	}
    }

    /**
     * Initializes the SimpleTranscript with the proper context.
     *
     * @param context the context to use
     */
    public void initialize(String context) {
	initialize(context, 1, 1);
    }

    /**
     * Starts the SimpleTranscript.
     */
    public void start() {
    }

    /**
     * Stops the SimpleTranscript.
     */
    public void stop() {
    }

    /**
     * Gets the next utterance's full path file name.
     */
    public String getNextUttId() {
	if (controlFileIterator.hasNext()) {
	    return (String) controlFileIterator.next();
	} else {
	    return null;
	}
    }

    /**
     * Gets the next utterance's transcription.
     */
    public String getNextTranscription() {
	if (transcriptFileIterator.hasNext()) {
	    return (String) transcriptFileIterator.next();
	} else {
	    return null;
	}
    }

    /**
     * Gets the next utterance's dictionary.
     */
    public String getNextDictionary() {
	return dictionary;
    }

    /**
     * Gets the word separator for the utterance.
     */
    public String wordSeparator() {
	return wordSeparator;
    }

    // Next method copied from decoder.BatchDecoder

    /**
     * Gets the set of lines from the file. 
     *
     * @param file the name of the file 
     * @throws IOException if error occurs while reading file
     */
    private List getLines(String file) throws IOException {
	List list = new ArrayList();
	BufferedReader reader 
	    = new BufferedReader(new FileReader(file));

	String line = null;

	while ((line = reader.readLine()) != null) {
	    list.add(line);
	}
	reader.close();

	if (numberOfPartitions > 1) {
	    int linesPerBatch = list.size() / numberOfPartitions;
	    if (linesPerBatch < 1) {
		linesPerBatch = 1;
	    }
	    if (currentPartition >= numberOfPartitions) {
		currentPartition = numberOfPartitions - 1;
	    }
	    int startLine = currentPartition * linesPerBatch;
	    // last batch needs to get all remaining lines
	    if (currentPartition == (numberOfPartitions - 1)) {
		list = list.subList(startLine, list.size());
	    } else {
		list = list.subList(startLine, startLine +
			linesPerBatch);
	    }
	}
	return list;
    }

}
