/*
 * Copyright 2015 Carnegie Mellon University. 
 * All Rights Reserved. Use is subject to license terms. See the
 * file "license.terms" for information on usage and redistribution of this
 * file, and for a DISCLAIMER OF ALL WARRANTIES.
 */

package edu.cmu.sphinx.result;

import static edu.cmu.sphinx.util.props.ConfigurationManagerUtils.setProperty;
import static javax.sound.sampled.AudioSystem.getAudioInputStream;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;
import org.testng.Assert;

import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.util.props.ConfigurationManager;


/**
 * Compares the lattices after recognition and loaded from file
 * for LAT and HTK format
 */
public class LatticeIOTest {

    private File latFile = new File("tmp.lat");
    private File slfFile = new File("tmp.slf");

    /**
     * Method for cleaning tmp files if any was created
     */
    @AfterTest
    public void removeTmpFiles() {
        if (latFile.exists())
            latFile.delete();
        if (slfFile.exists())
            slfFile.delete();
    }

    /**
     * Main method for running the LatticeIOTest demo.
     * 
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    @Test
    public void testLatticeIO() throws UnsupportedAudioFileException,
            IOException {
        // TODO: make an integration test, too heavy to be a unit test
        URL audioFileURL = getClass().getResource("green.wav");
        URL configURL = getClass().getResource("config.xml");
        URL lm = getClass().getResource("hellongram.trigram.lm");

        ConfigurationManager cm = new ConfigurationManager(configURL);
        setProperty(cm, "trigramModel", "location", lm.toString());

        Recognizer recognizer = cm.lookup("recognizer");
        StreamDataSource dataSource = cm.lookup(StreamDataSource.class);

        AudioInputStream ais = getAudioInputStream(audioFileURL);
        dataSource.setInputStream(ais);

        recognizer.allocate();
        Lattice lattice = new Lattice(recognizer.recognize());
        new LatticeOptimizer(lattice).optimize();
        lattice.computeNodePosteriors(1.0f);
        lattice.dump(latFile.getAbsolutePath());
        lattice.dumpSlf(new FileWriter(slfFile));
        Lattice latLattice = new Lattice(latFile.getAbsolutePath());
        latLattice.computeNodePosteriors(1.0f);
        Lattice slfLattice = Lattice.readSlf(slfFile.getAbsolutePath());
        slfLattice.computeNodePosteriors(1.0f);
        Iterator<WordResult> latIt = lattice.getWordResultPath().iterator();
        Iterator<WordResult> latLatIt = latLattice.getWordResultPath().iterator();
        Iterator<WordResult> slfLatIt = slfLattice.getWordResultPath().iterator();
        while (latIt.hasNext()) {
            WordResult latWord = latIt.next();
            WordResult latLatWord = latLatIt.next();
            WordResult slfLatWord = slfLatIt.next();
            Assert.assertEquals(latWord.getWord().toString(), latLatWord.getWord().toString());
            Assert.assertEquals(latWord.getWord().toString(), slfLatWord.getWord().toString());
            Assert.assertEquals(latWord.getTimeFrame().getStart(), latLatWord.getTimeFrame().getStart());
        }
        Assert.assertEquals(lattice.getTerminalNode().getViterbiScore(), latLattice.getTerminalNode().getViterbiScore(), 0.001);
        Assert.assertEquals(lattice.getTerminalNode().getViterbiScore(), slfLattice.getTerminalNode().getViterbiScore(), 0.001);
    }
}
