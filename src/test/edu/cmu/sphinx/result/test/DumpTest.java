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

import edu.cmu.sphinx.frontend.util.StreamDataSource;

import edu.cmu.sphinx.recognizer.Recognizer;

import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.LatticeOptimizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.Sausage;
import edu.cmu.sphinx.result.SausageMaker;
import edu.cmu.sphinx.result.TokenGraphDumper;

import edu.cmu.sphinx.util.props.ConfigurationManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

public class DumpTest {

	@Test
	public void testDump () 
			throws UnsupportedAudioFileException, IOException {

		URL audioFileURL = new File("src/test/edu/cmu/sphinx/result/test/green.wav")
				.toURI().toURL();
		URL configURL = new File("src/test/edu/cmu/sphinx/result/test/config.xml")
				.toURI().toURL();

		ConfigurationManager cm = new ConfigurationManager(configURL);
		Recognizer recognizer = (Recognizer) cm.lookup("recognizer");

		recognizer.allocate();

		StreamDataSource reader = (StreamDataSource) cm
				.lookup("streamDataSource");

		/* set the stream data source to read from the audio file */
		reader.setInputStream(AudioSystem.getAudioInputStream(audioFileURL),
				audioFileURL.getFile());

		Result result = recognizer.recognize();

		/* Dump token graph */
		TokenGraphDumper dumper = new TokenGraphDumper(result);
		dumper.dumpGDL("Full Token Graph", "logs/tokengraph.gdl");
		
		/* Dump Lattice */
		Lattice lattice = new Lattice(result);
		LatticeOptimizer lo = new LatticeOptimizer(lattice);
		lo.optimize();
		lattice.dumpAISee("logs/lattice.gdl", "lattice");

		/* Dump sausage */
		float lmw = (float) 7.0;
		lmw = cm.getPropertySheet("lexTreeLinguist").getFloat("languageWeight");
		lattice.computeNodePosteriors(lmw);
		SausageMaker sm = new SausageMaker(lattice);
		Sausage sausage = sm.makeSausage();

		System.out.println("best sausage hypothesis: \""
				+ sausage.getBestHypothesisString() + '\"');
		
		sausage.dumpAISee("sausage.gdl", "sausage decode");
	}
}
