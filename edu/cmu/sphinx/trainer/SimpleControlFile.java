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
import edu.cmu.sphinx.knowledge.dictionary.*;

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
import java.util.LinkedList;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides mechanisms for accessing a next utterance's file name
 * and transcription.
 */
public class SimpleControlFile implements ControlFile {

    private SphinxProperties props;	// the sphinx properties
    private String audioFile;           // the audio file
    private String transcriptFile;      // the transcript file
    private Dictionary dictionary;          // the dictionary
    private String wordSeparator;       // the word separator
    private int currentPartition;       // the current partition
    private int numberOfPartitions;     // total number of partitions
    private Iterator audioFileIterator; // iterator for the control file
    private Iterator transcriptFileIterator; // iterator for the transcriptions
    private List audioFileList;         // list containing the audio files
    private List transcriptFileList;    // list containing the transcriptions

    /*
     * The logger for this class
     */
    private static Logger logger =
        Logger.getLogger("edu.cmu.sphinx.trainer.SimpleControlFile");

    /**
     * Constructor for SimpleControlFile.
     */
    public SimpleControlFile() {
	initialize("nada");
    }

    /**
     * Initializes the SimpleControlFile with the proper context.
     *
     * @param context the context to use
     * @param thisPartition the current partition of the transcript file
     * @param numberOfPartitions the total number of partitions
     */
    public void initialize(String context, int thisPartition, 
			   int numberOfPartitions) {
	this.props = SphinxProperties.getSphinxProperties(context);
	this.audioFile = props.getString(PROP_AUDIO_FILE, 
					   PROP_AUDIO_FILE_DEFAULT);
	this.transcriptFile = props.getString(PROP_TRANSCRIPT_FILE, 
					   PROP_TRANSCRIPT_FILE_DEFAULT);
	logger.info("Audio control file: " + this.audioFile);
	logger.info("Transcript file: " + this.transcriptFile);
	this.dictionary = new TrainerDictionary();
	this.wordSeparator = " \t\n\r\f"; // the white spaces
	this.currentPartition = thisPartition;
	this.numberOfPartitions = numberOfPartitions;
	logger.info("Processing part " + this.currentPartition +
		    " of " + this.numberOfPartitions);
	try {
	    this.audioFileList = getLines(audioFile);
	} catch (IOException ioe) {
	    throw new Error("IOE: Can't open file " + audioFile, ioe);
	}
	try {
	    this.transcriptFileList = getLines(transcriptFile);
	} catch (IOException ioe) {
	    throw new Error("IOE: Can't open file " + transcriptFile, ioe);
	}
    }

    /**
     * Initializes the SimpleControlFile with the proper context.
     *
     * @param context the context to use
     */
    public void initialize(String context) {
	initialize(context, 1, 1);
    }

    /**
     * Gets an iterator for utterances.
     */
    public void startUtteranceIterator() {
	audioFileIterator = audioFileList.iterator();
	transcriptFileIterator = transcriptFileList.iterator();
    }

    /**
     * Returns whether there is another utterance.
     *
     * @return true if there is another utterance.
     */
    public boolean hasMoreUtterances() {
	// Should throw exception or break if one has next and the
	// other doesn't.
	return (audioFileIterator.hasNext() 
		&& transcriptFileIterator.hasNext());
    }

    /**
     * Gets the next utterance.
     *
     * @return the next utterance.
     */
    public Utterance nextUtterance() {
	Utterance utterance = new SimpleUtterance((String) audioFileIterator.next());
	utterance.add((String) transcriptFileIterator.next(), dictionary,
		      false, wordSeparator);
	return utterance;
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
