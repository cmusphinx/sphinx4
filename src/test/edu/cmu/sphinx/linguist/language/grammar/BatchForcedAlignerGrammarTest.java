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
                "src/test/edu/cmu/sphinx/linguist/language/grammar/utts.transcription", true, true, true, true, dictionary);
        grammar.allocate();
        String sentence = grammar.getRandomSentence();
        assertTrue(sentence.equals("one") || sentence.equals("two") || sentence.equals("three"));
    }
}
