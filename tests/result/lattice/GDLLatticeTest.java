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

import edu.cmu.sphinx.frontend.util.StreamDataSource;

import edu.cmu.sphinx.linguist.dictionary.Dictionary;

import edu.cmu.sphinx.recognizer.Recognizer;

import edu.cmu.sphinx.result.ConfidenceResult;
import edu.cmu.sphinx.result.ConfidenceScorer;
import edu.cmu.sphinx.result.GDLLatticeFactory;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.LatticeOptimizer;
import edu.cmu.sphinx.result.MAPConfidenceScorer;
import edu.cmu.sphinx.result.Path;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.SausageMaker;
import edu.cmu.sphinx.result.Sausage;
import edu.cmu.sphinx.result.WordResult;

import edu.cmu.sphinx.util.LogMath;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

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
        } catch (InstantiationException e) {
            System.err.println("Problem creating MAPConfidenceTest: " + e);
            e.printStackTrace();
        }
    }
}
