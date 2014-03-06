package edu.cmu.sphinx.linguist.language.grammar;

import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FastDictionary;

public class FSTGrammarTest {

    @Test
    public void testForcedAlignerGrammar() throws IOException {
        Dictionary dictionary = new FastDictionary(new URL("file:src/test/edu/cmu/sphinx/linguist/language/grammar/FSTGrammarTest.dic"), new URL(
                "file:models/acoustic/wsj/noisedict"), null, false, null, false, false, new UnitManager());
        FSTGrammar grammar = new FSTGrammar
        		("src/test/edu/cmu/sphinx/linguist/language/grammar/FSTGrammarTest.gram", true, true, true, true, dictionary);
        grammar.allocate();
        Assert.assertEquals(14, grammar.getGrammarNodes().size());
    }
}
