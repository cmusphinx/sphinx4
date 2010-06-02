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

import edu.cmu.sphinx.linguist.dictionary.Dictionary;

import edu.cmu.sphinx.result.GDLLatticeFactory;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.SausageMaker;
import edu.cmu.sphinx.result.Sausage;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Test;

public class GDLLatticeTest {

	@Test
	public void testGDLLattice() throws IOException {

		String latticeGDL;
		latticeGDL = "src/test/edu/cmu/sphinx/result/test/testLattice.gdl";

		URL configURL = new File("src/test/edu/cmu/sphinx/result/test/config.xml")
				.toURI().toURL();
		ConfigurationManager cm = new ConfigurationManager(configURL);
		LogMath logMath = (LogMath) cm.lookup("logMath");
		Dictionary dictionary = (Dictionary) cm.lookup("dictionary");
		dictionary.allocate();

		Lattice lattice = GDLLatticeFactory.getLattice(latticeGDL, dictionary);
		lattice.setLogMath(logMath);
		lattice.dumpAISee("logs/newLattice.gdl", "New Lattice");

		SausageMaker sm = new SausageMaker(lattice);
		Sausage s = sm.makeSausage();

		s.dumpAISee("logs/newSausage.gdl", "New Sausage");
	}
}
