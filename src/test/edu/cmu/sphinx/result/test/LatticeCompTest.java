/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.result.test;

import static org.junit.Assert.*;
import edu.cmu.sphinx.frontend.util.StreamDataSource;

import edu.cmu.sphinx.recognizer.Recognizer;

import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.Lattice;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.ConfigurationManagerUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

/**
 * Compares the lattices generated when the LexTreeLinguist flag 'keepAllTokens'
 * is turned on/off.
 */
public class LatticeCompTest {

	/**
	 * Main method for running the LatticeCompTest demo.
	 * @throws IOException 
	 * @throws UnsupportedAudioFileException 
	 */
	@Test
	public void testLatticeComp() throws UnsupportedAudioFileException, IOException {

		URL audioFileURL = new File("src/test/edu/cmu/sphinx/result/test/green.wav").toURI().toURL();
		URL configURL = new File("src/test/edu/cmu/sphinx/result/test/config.xml").toURI().toURL();

		ConfigurationManager cm = new ConfigurationManager(configURL);

		Recognizer recognizer = (Recognizer) cm.lookup("recognizer");

		StreamDataSource reader = (StreamDataSource) cm
				.lookup("streamDataSource");

		AudioInputStream ais = AudioSystem.getAudioInputStream(audioFileURL);

		/* set the stream data source to read from the audio file */
		reader.setInputStream(ais, audioFileURL.getFile());
		
		/* allocate the resource necessary for the recognizer */
		recognizer.allocate();

		/* decode the audio file */
		Result result = recognizer.recognize();

		/* print out the results */
		Lattice lattice = new Lattice(result);
		lattice.dumpAISee("logs/lattice.gdl", "lattice");

		recognizer.deallocate();
		
		cm = new ConfigurationManager(configURL);
		
		ConfigurationManagerUtils.setProperty(cm, "keepAllTokens", "true");
		
		recognizer = (Recognizer) cm.lookup("recognizer");
		recognizer.allocate();
		
		reader = (StreamDataSource) cm.lookup("streamDataSource");
		reader.setInputStream(AudioSystem.getAudioInputStream(audioFileURL),
				audioFileURL.getFile());

		Result allResult = recognizer.recognize();

		Lattice allLattice = new Lattice(allResult);
		allLattice.dumpAISee("logs/allLattice.gdl", "All Lattice");

		assertTrue(lattice.isEquivalent(allLattice));
	}
}
