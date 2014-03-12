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

package edu.cmu.sphinx.fst;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.testng.annotations.Test;

import edu.cmu.sphinx.fst.Fst;
import edu.cmu.sphinx.fst.operations.Compose;
import edu.cmu.sphinx.fst.semiring.TropicalSemiring;

/**
 * @author John Salatas <jsalatas@users.sourceforge.net>
 * 
 */
public class ComposeEpsilonTest  {
    
    @Test
    public void testCompose() {
        Fst fstA = Convert.importFst("src/test/edu/cmu/sphinx/fst/data/tests/algorithms/composeeps/A",
                new TropicalSemiring());
        Fst fstB = Convert.importFst("src/test/edu/cmu/sphinx/fst/data/tests/algorithms/composeeps/B",
                new TropicalSemiring());
        Fst fstC = Convert.importFst(
                "src/test/edu/cmu/sphinx/fst/data/tests/algorithms/composeeps/fstcomposeeps",
                new TropicalSemiring());

        Fst fstComposed = Compose.get(fstA, fstB, new TropicalSemiring());
        assertThat(fstC, equalTo(fstComposed));
    }

}
