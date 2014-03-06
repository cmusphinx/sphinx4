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

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.sphinx.fst.Fst;
import edu.cmu.sphinx.fst.openfst.Convert;
import edu.cmu.sphinx.fst.semiring.TropicalSemiring;

/**
 * @author John Salatas <jsalatas@users.sourceforge.net>
 * 
 */
public class ConnectTest {
    @Test
    public void testConnect() {
        System.out.println("Testing Connect...");
        Fst fst = Convert.importFst("src/test/edu/cmu/sphinx/fst/data/tests/algorithms/connect/A",
                new TropicalSemiring());
        Fst connectSaved = Fst
                .loadModel("src/test/edu/cmu/sphinx/fst/data/tests/algorithms/connect/fstconnect.fst.ser");
        Connect.apply(fst);

        assertTrue(connectSaved.equals(fst));

        System.out.println("Testing Connect Completed!\n");
    }

    public static void main(String[] args) {
        ConnectTest test = new ConnectTest();
        test.testConnect();
    }

}
