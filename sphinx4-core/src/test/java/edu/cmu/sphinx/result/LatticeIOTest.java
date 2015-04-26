/*
 * Copyright 2015 Carnegie Mellon University. 
 * All Rights Reserved. Use is subject to license terms. See the
 * file "license.terms" for information on usage and redistribution of this
 * file, and for a DISCLAIMER OF ALL WARRANTIES.
 */

package edu.cmu.sphinx.result;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;
import org.testng.Assert;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.WordResult;

/**
 * Compares the lattices after recognition and loaded from file for LAT and HTK
 * format
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
    public void testLatticeIO() throws UnsupportedAudioFileException, IOException {
        Configuration configuration = new Configuration();

        // Load model from the jar
        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/result/hellongram.trigram.lm");

        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
        InputStream stream = getClass().getResourceAsStream("green.wav");
        stream.skip(44);

        // Simple recognition with generic model
        recognizer.startRecognition(stream);
        SpeechResult result = recognizer.getResult();
        Lattice lattice = result.getLattice();

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
