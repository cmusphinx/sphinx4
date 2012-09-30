/**
 * 
 */
package edu.cmu.sphinx.fst.operations;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.cmu.sphinx.fst.Fst;
import edu.cmu.sphinx.fst.openfst.Convert;
import edu.cmu.sphinx.fst.semiring.TropicalSemiring;

/**
 * @author John Salatas <jsalatas@users.sourceforge.net>
 * 
 */
public class ReverseTest {
    @Test
    public void testReverse() {
        System.out.println("Testing Reverse...");
        // Input label sort test

        Fst fst = Convert.importFst("src/test/edu/cmu/sphinx/fst/data/tests/algorithms/reverse/A",
                new TropicalSemiring());
        Fst fstB = Fst
                .loadModel("src/test/edu/cmu/sphinx/fst/data/tests/algorithms/reverse/fstreverse.fst.ser");

        Fst fstReversed = Reverse.get(fst);

        assertTrue(fstB.equals(fstReversed));

        System.out.println("Testing Reverse Completed!\n");
    }

}
