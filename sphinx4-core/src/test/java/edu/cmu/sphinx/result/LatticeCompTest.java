/*
 * Copyright 1999-2004 Carnegie Mellon University. Portions Copyright 2004 Sun
 * Microsystems, Inc. Portions Copyright 2004 Mitsubishi Electric Research
 * Laboratories. All Rights Reserved. Use is subject to license terms. See the
 * file "license.terms" for information on usage and redistribution of this
 * file, and for a DISCLAIMER OF ALL WARRANTIES.
 */

package edu.cmu.sphinx.result;

import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.testng.annotations.Test;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;

/**
 * Compares the lattices generated when the LexTreeLinguist flag 'keepAllTokens'
 * is turned on/off.
 */
public class LatticeCompTest {

    /**
     * Main method for running the LatticeCompTest demo.
     * 
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    @Test
    public void testLatticeComp() throws UnsupportedAudioFileException, IOException {

        Configuration configuration = new Configuration();

        // Load model from the jar
        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/result/hellongram.trigram.lm");

        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
        InputStream stream = LatticeCompTest.class.getResourceAsStream("green.wav");
        stream.skip(44);

        // Simple recognition with generic model
        recognizer.startRecognition(stream);
        SpeechResult result = recognizer.getResult();
        Lattice lattice = result.getLattice();

        Lattice otherLattice = Lattice.readSlf(getClass().getResourceAsStream("correct.slf"));

        Collection<Node> latNodes = lattice.getNodes();
        Collection<Node> otherLatNodes = otherLattice.getNodes();
        Iterator<Node> it = latNodes.iterator();
        boolean latticesAreEquivalent = true;
        while (it.hasNext()) {
            Node node = it.next();
            Iterator<Node> otherIt = otherLatNodes.iterator();
            boolean hasEquivalentNode = false;
            while (otherIt.hasNext()) {
                Node otherNode = otherIt.next();
                boolean nodesAreEquivalent = node.getWord().getSpelling().equals(otherNode.getWord().getSpelling())
                        && node.getEnteringEdges().size() == otherNode.getEnteringEdges().size()
                        && node.getLeavingEdges().size() == otherNode.getLeavingEdges().size();
                if (nodesAreEquivalent) {
                    hasEquivalentNode = true;
                    break;
                }
            }

            if (!hasEquivalentNode) {
                latticesAreEquivalent = false;
                break;
            }
        }
        assertTrue(latticesAreEquivalent);

    }
}
