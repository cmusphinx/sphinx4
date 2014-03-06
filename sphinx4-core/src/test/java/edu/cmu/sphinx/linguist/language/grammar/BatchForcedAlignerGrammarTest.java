/*
 * Copyright 1999-2012 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2012 Nexiwave
 * 
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.linguist.language.grammar;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FastDictionary;
import edu.cmu.sphinx.linguist.language.grammar.BatchForcedAlignerGrammar;

public class BatchForcedAlignerGrammarTest {

    @Test
    public void testForcedAlignerGrammar() throws IOException {
        Dictionary dictionary = new FastDictionary(new URL("file:models/acoustic/wsj/dict/digits.dict"), new URL(
                "file:models/acoustic/wsj/noisedict"), null, false, null, false, false, new UnitManager());
        BatchForcedAlignerGrammar grammar = new BatchForcedAlignerGrammar(
                "src/test/edu/cmu/sphinx/linguist/language/grammar/BatchForcedAlignerGrammarTest.utts", true, true, true, true, dictionary);
        grammar.allocate();
        String sentence = grammar.getRandomSentence();
        assertTrue(sentence.equals("one") || sentence.equals("two") || sentence.equals("three"));
    }
}
