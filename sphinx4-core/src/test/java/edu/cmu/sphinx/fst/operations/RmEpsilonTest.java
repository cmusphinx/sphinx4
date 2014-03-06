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
import edu.cmu.sphinx.fst.semiring.ProbabilitySemiring;

/**
 * @author John Salatas <jsalatas@users.sourceforge.net>
 * 
 */
public class RmEpsilonTest {

    @Test
    public void testRmEpsilon() {
        System.out.println("Testing RmEpsilon...");

        Fst fst = Convert.importFst("src/test/edu/cmu/sphinx/fst/data/tests/algorithms/rmepsilon/A",
                new ProbabilitySemiring());
        Fst fstRmEps = Fst
                .loadModel("src/test/edu/cmu/sphinx/fst/data/tests/algorithms/rmepsilon/fstrmepsilon.fst.ser");
        Fst rmEpsilon = RmEpsilon.get(fst);

        assertTrue(fstRmEps.equals(rmEpsilon));

        System.out.println("Testing RmEpsilon Completed!\n");
    }
}
