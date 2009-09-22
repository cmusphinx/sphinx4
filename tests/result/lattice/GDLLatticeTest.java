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

import edu.cmu.sphinx.linguist.dictionary.Dictionary;

import edu.cmu.sphinx.result.GDLLatticeFactory;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.SausageMaker;
import edu.cmu.sphinx.result.Sausage;

import edu.cmu.sphinx.util.LogMath;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class GDLLatticeTest {

    /**
     * Main method for running the MAPConfidenceTest demo.
     */
    public static void main(String[] args) {
        try {       
            String latticeGDL;
	    if (args.length > 0) {
                latticeGDL = args[0];
            } else {
                latticeGDL = "testLattice.gdl";
            }

            URL configURL = new File("./config.xml").toURI().toURL();
            ConfigurationManager cm = new ConfigurationManager(configURL);
            LogMath logMath = (LogMath) cm.lookup("logMath");
            Dictionary dictionary = (Dictionary) cm.lookup("dictionary");
            dictionary.allocate();

            Lattice lattice = 
                GDLLatticeFactory.getLattice(latticeGDL, dictionary);
            lattice.setLogMath(logMath);
            lattice.dumpAISee("newLattice.gdl", "New Lattice");

            SausageMaker sm = new SausageMaker(lattice);
            Sausage s = sm.makeSausage();

            s.dumpAISee("newSausage.gdl", "New Sausage");
        } catch (IOException e) {
            System.err.println("Problem when loading MAPConfidenceTest: " + e);
            e.printStackTrace();
        } catch (PropertyException e) {
            System.err.println("Problem configuring MAPConfidenceTest: " + e);
            e.printStackTrace();
        }
    }
}
