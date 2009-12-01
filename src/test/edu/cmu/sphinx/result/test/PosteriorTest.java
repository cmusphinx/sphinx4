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

import org.junit.Test;

import edu.cmu.sphinx.result.Node;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.util.LogMath;

/**
 * Tests the posterior score computation code. Sets up a simple lattice, and
 * dumps out the posterior probabilities of each node.
 */
public class PosteriorTest {

	@Test
	public void testPosterior() {

		LogMath logMath = new LogMath(1.0001f, true);

		Lattice lattice = new Lattice(logMath);

		Node a = lattice.addNode("A", "A", 0, 0);
		Node b = lattice.addNode("B", "B", 0, 0);
		Node c = lattice.addNode("C", "C", 0, 0);
		Node d = lattice.addNode("D", "D", 0, 0);

		double acousticAB = 4;
		double acousticAC = 6;
		double acousticCB = 1;
		double acousticBD = 5;
		double acousticCD = 2;

		lattice.setInitialNode(a);
		lattice.setTerminalNode(d);

		lattice.addEdge(a, b, logMath.linearToLog(acousticAB), 0);
		lattice.addEdge(a, c, logMath.linearToLog(acousticAC), 0);
		lattice.addEdge(c, b, logMath.linearToLog(acousticCB), 0);
		lattice.addEdge(b, d, logMath.linearToLog(acousticBD), 0);
		lattice.addEdge(c, d, logMath.linearToLog(acousticCD), 0);

		lattice.computeNodePosteriors(1.0f);
		double pathABD = acousticAB * acousticBD;
		double pathACBD = acousticAC * acousticCB * acousticBD;
		double pathACD = acousticAC * acousticCD;
		double allPaths = pathABD + pathACBD + pathACD;

		double bPosterior = (pathABD + pathACBD) / allPaths;
		double cPosterior = (pathACBD + pathACD) / allPaths;

		double delta = 1e-4;
		assertEquals (logMath.logToLinear((float) a.getPosterior()), 1.0, delta);
		assertEquals (logMath.logToLinear((float) b.getPosterior()), bPosterior, delta);
		assertEquals (logMath.logToLinear((float) c.getPosterior()), cPosterior, delta);
		assertEquals (logMath.logToLinear((float) d.getPosterior()), 1.0, delta);
	}
}
