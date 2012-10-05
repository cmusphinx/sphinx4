package edu.cmu.sphinx.linguist.language.grammar;

import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FastDictionary;
import edu.cmu.sphinx.util.LogMath;

public class FSTGrammarTest {

    @Test
    public void testForcedAlignerGrammar() throws IOException {
    	LogMath logMath = new LogMath(1.0001f, true);
        Dictionary dictionary = new FastDictionary(new URL("file:models/acoustic/wsj/dict/digits.dict"), new URL(
                "file:models/acoustic/wsj/noisedict"), null, false, null, false, false, new UnitManager());
        FSTGrammar grammar = new FSTGrammar
        		("src/test/edu/cmu/sphinx/linguist/language/grammar/FSTGrammarTest.gram", logMath, true, true, true, true, dictionary);
        grammar.allocate();
        Assert.assertEquals(grammar.getGrammarNodes().size(), 9);
    }
}
