/**
 * 
 */
package edu.cmu.sphinx.fst.operations;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.sphinx.fst.Fst;
import edu.cmu.sphinx.fst.openfst.Convert;
import edu.cmu.sphinx.fst.semiring.TropicalSemiring;

/**
 * @author John Salatas <jsalatas@users.sourceforge.net>
 * 
 */
public class NShortestPathsTest {

    @Test
    public void testNShortestPaths() {
        System.out.println("Testing NShortestPaths...");

        Fst fst = Convert.importFst("src/test/edu/cmu/sphinx/fst/data/tests/algorithms/shortestpath/A",
                new TropicalSemiring());
        Fst nsp = Convert.importFst("src/test/edu/cmu/sphinx/fst/data/tests/algorithms/shortestpath/nsp",
                new TropicalSemiring());

        Fst fstNsp = NShortestPaths.get(fst, 6, true);

        assertTrue(nsp.equals(fstNsp));

        System.out.println("Testing NShortestPaths Completed!\n");
    }
}
