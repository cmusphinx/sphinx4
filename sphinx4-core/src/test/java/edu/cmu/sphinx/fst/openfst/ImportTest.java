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

package edu.cmu.sphinx.fst.openfst;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.sphinx.fst.Fst;
import edu.cmu.sphinx.fst.semiring.TropicalSemiring;

/**
 * 
 * @author "John Salatas <jsalatas@users.sourceforge.net>"
 * 
 */
public class ImportTest {

    @Test
    public void testConvert() {
        System.out.println("Testing Import...");

        Fst fst1 = Convert.importFst("src/test/edu/cmu/sphinx/fst/data/openfst/basic",
                new TropicalSemiring());

        Fst fst2 = Fst.loadModel("src/test/edu/cmu/sphinx/fst/data/openfst/basic.fst.ser");

        assertTrue(fst1.equals(fst2));

        System.out.println("Testing Import Completed!\n");
    }

}
