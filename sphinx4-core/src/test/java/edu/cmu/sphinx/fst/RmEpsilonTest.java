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

import java.io.File;
import java.net.URL;

import org.testng.annotations.Test;

import com.google.common.io.Resources;

import edu.cmu.sphinx.fst.operations.RmEpsilon;
import edu.cmu.sphinx.fst.semiring.ProbabilitySemiring;

/**
 * @author John Salatas <jsalatas@users.sourceforge.net>
 * 
 */
public class RmEpsilonTest {

    @Test
    public void testRmEpsilon() {
        String path = "algorithms/rmepsilon/A.fst.txt";
        URL url = Resources.getResource(getClass(), path);
        File parent = new File(url.getPath()).getParentFile();
        
        path = new File(parent, "A").getPath();
        Fst fst = Convert.importFst(path,new ProbabilitySemiring());
        path = new File(parent, "fstrmepsilon.fst.ser").getPath();
        Fst fstRmEps = Fst.loadModel(path);
        
        Fst rmEpsilon = RmEpsilon.get(fst);
        assertThat(fstRmEps, equalTo(rmEpsilon));
    }
}
