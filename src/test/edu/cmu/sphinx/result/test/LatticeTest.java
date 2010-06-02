package edu.cmu.sphinx.result.test;

import org.junit.Test;

import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.LatticeOptimizer;
import edu.cmu.sphinx.result.Node;
import edu.cmu.sphinx.util.LogMath;

public class LatticeTest {

	/**
	 * Self test for Lattices. Test loading, saving, dynamically creating and
	 * optimizing Lattices
	 * 
	 * @param args
	 */
	@Test
	public void testLattice() {

		Lattice lattice = null;

		System.err.println("Building test Lattice");

		lattice = new Lattice(new LogMath(1.0001f, true));

		/*
		 * 1 --> 2 - / \ 0 --> 1 --> 4 \ \ / 2 --> 3 -
		 */

		Node n0 = lattice.addNode("0", "0", 0, 0);
		Node n1 = lattice.addNode("1", "1", 0, 0);
		Node n1a = lattice.addNode("1a", "1", 0, 0);
		Node n2 = lattice.addNode("2", "2", 0, 0);
		Node n2a = lattice.addNode("2a", "2", 0, 0);
		Node n3 = lattice.addNode("3", "3", 0, 0);
		Node n4 = lattice.addNode("4", "4", 0, 0);

		lattice.addEdge(n0, n1, -1, 0);
		lattice.addEdge(n0, n1a, -1, 0);
		lattice.addEdge(n1, n4, -1, 0);
		lattice.addEdge(n1a, n2a, -1, 0);
		lattice.addEdge(n2a, n4, -1, 0);
		lattice.addEdge(n0, n2, -1, 0);
		lattice.addEdge(n2, n3, -1, 0);
		lattice.addEdge(n1, n3, -1, 0);
		lattice.addEdge(n3, n4, -1, 0);

		lattice.setInitialNode(n0);
		lattice.setTerminalNode(n4);
		System.err.println("Lattice has " + lattice.getNodes().size()
				+ " nodes and " + lattice.getEdges().size() + " edges");

		System.err.println("Testing Save/Load .LAT file");
		lattice.dump("logs/test.lat");

		lattice.dumpAllPaths();

		LatticeOptimizer lo = new LatticeOptimizer(lattice);
		lo.optimize();

		/*
		 * 2 / \ 0 --> 1 --> 4 \ \ / 2 --> 3
		 */

		lattice.dumpAllPaths();
	}
}
