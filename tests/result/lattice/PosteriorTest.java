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

package tests.result.lattice;

import edu.cmu.sphinx.result.Edge;
import edu.cmu.sphinx.result.Node;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.LatticeOptimizer;

import edu.cmu.sphinx.util.LogMath;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.IOException;
import java.net.URL;


/**
 * Tests the posterior score computation code.
 * Sets up a simple lattice, and dumps out the posterior probabilities
 * of each node. 
 */
public class PosteriorTest {
    
    public static void main(String[] argv) {
	try {
	    URL configURL = new File("./logMath.xml").toURI().toURL();
	    ConfigurationManager cm = new ConfigurationManager(configURL);
	    
	    LogMath logMath = (LogMath) cm.lookup("logMath");
	    
	    Lattice lattice = new Lattice(logMath);
	    
	    Node a = lattice.addNode("A", 0, 0);
	    Node b = lattice.addNode("B", 0, 0);
	    Node c = lattice.addNode("C", 0, 0);
	    Node d = lattice.addNode("D", 0, 0);
	    
	    lattice.setInitialNode(a);
	    lattice.setTerminalNode(d);
	    
	    lattice.addEdge(a, b, logMath.linearToLog(4), 0);
	    lattice.addEdge(a, c, logMath.linearToLog(6), 0);
	    lattice.addEdge(c, b, logMath.linearToLog(1), 0);
	    lattice.addEdge(b, d, logMath.linearToLog(5), 0);
	    lattice.addEdge(c, d, logMath.linearToLog(2), 0);
	    
	    lattice.computeNodePosteriors(1.0f);
	    double pathABD = 4 * 5;
	    double pathACBD = 6 * 1 * 5;
	    double pathACD = 6 * 2;
	    double allPaths = pathABD + pathACBD + pathACD;

	    double bPosterior = (pathABD + pathACBD)/allPaths;
	    double cPosterior = (pathACBD + pathACD)/allPaths;

	    System.out.println
		("A: " + logMath.logToLinear((float) a.getPosterior()) +
		 "  (manual: 1.0)");
	    System.out.println
		("B: " + logMath.logToLinear((float) b.getPosterior()) +
		 "  (manual: " + bPosterior + ")");
	    System.out.println
		("C: " + logMath.logToLinear((float) c.getPosterior()) +
		 "  (manual: " + cPosterior + ")");
	    System.out.println
		("D: " + logMath.logToLinear((float) d.getPosterior()) +
		 "  (manual: 1.0)");
	} catch (IOException e) {
            System.err.println("Problem when loading LatticeDumpTest: " + e);
            e.printStackTrace();
        } catch (PropertyException e) {
            System.err.println("Problem configuring LatticeDumpTest: " + e);
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Problem creating LatticeDumpTest: " + e);
            e.printStackTrace();
	}
    }
}
