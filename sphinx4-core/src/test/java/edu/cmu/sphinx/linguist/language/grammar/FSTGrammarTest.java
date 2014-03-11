package edu.cmu.sphinx.linguist.language.grammar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.io.IOException;
import java.net.URL;

import org.testng.annotations.Test;

import edu.cmu.sphinx.Sphinx4TestCase;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FastDictionary;


public class FSTGrammarTest extends Sphinx4TestCase {

    @Test
    public void testForcedAlignerGrammar() throws IOException {
        URL dictionaryUrl = getResourceUrl("FSTGrammarTest.dic");
        URL noisedictUrl = getResourceUrl("noisedict");

        Dictionary dictionary = new FastDictionary(dictionaryUrl,
                                                   noisedictUrl,
                                                   null,
                                                   false,
                                                   null,
                                                   false,
                                                   false,
                                                   new UnitManager());

        String path = getResourceFile("FSTGrammarTest.gram").getPath();
        FSTGrammar grammar = new FSTGrammar(path,
                                            true,
                                            true,
                                            true,
                                            true,
                                            dictionary);
        grammar.allocate();
        assertThat(grammar.getGrammarNodes(), hasSize(14));
    }
}
