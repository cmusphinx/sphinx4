/**
 * 
 * Copyright 1999-2012 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.fst.operations;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.cmu.sphinx.fst.Fst;
import edu.cmu.sphinx.fst.openfst.Convert;
import edu.cmu.sphinx.fst.semiring.TropicalSemiring;

/**
 * Compose Testing for
 * 
 * Examples provided by M. Mohri, "Weighted Automata Algorithms", Handbook of
 * Weighted Automata, Springer-Verlag, 2009, pp. 213–254.
 * 
 * 
 * 
 * @author John Salatas <jsalatas@users.sourceforge.net>
 * 
 */
public class ComposeTest {
    @Test
    public void testCompose() {
        System.out.println("Testing Composition...");
        Fst fstA = Convert.importFst("src/test/edu/cmu/sphinx/fst/data/tests/algorithms/compose/A",
                new TropicalSemiring());
        Fst fstB = Convert.importFst("src/test/edu/cmu/sphinx/fst/data/tests/algorithms/compose/B",
                new TropicalSemiring());
        Fst composed = Fst
                .loadModel("src/test/edu/cmu/sphinx/fst/data/tests/algorithms/compose/fstcompose.fst.ser");

        Fst fstComposed = Compose.get(fstA, fstB, new TropicalSemiring());

        assertTrue(composed.equals(fstComposed));

        System.out.println("Testing Composition Completed!\n");
    }
}
