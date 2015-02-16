package edu.cmu.sphinx.linguist.language.ngram;


import static edu.cmu.sphinx.util.LogMath.getLogMath;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.TextDictionary;


public class DynamicTrigramModelTest {

    private Dictionary dictionary;

    @BeforeClass
    public void setUp() throws IOException {
        URL dictUrl =
                getClass()
                        .getResource(
                                "/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        URL noiseDictUrl =
                getClass().getResource(
                        "/edu/cmu/sphinx/models/en-us/en-us/noisedict");

        dictionary =
                new TextDictionary(dictUrl, noiseDictUrl, null, null,
                                   new UnitManager());
        dictionary.allocate();
    }

    @Test
    public void unigramModel() throws IOException {
        DynamicTrigramModel model = new DynamicTrigramModel(dictionary);
        model.setText(asList("one"));
        model.allocate();
        assertThat(model.getVocabulary(), contains("one"));
        assertThat(model.getProbability(new WordSequence(dictionary
                .getWord("one"))), equalTo(getLogMath().linearToLog(1.0/3)));
    }

    @Test
    public void bigramModel() throws IOException {
        DynamicTrigramModel model = new DynamicTrigramModel(dictionary);
        model.setText(asList("one", "two"));
        model.allocate();
        assertThat(model.getVocabulary(), containsInAnyOrder("one", "two"));
    }

    @Test
    public void trigramModel() throws IOException {
        DynamicTrigramModel model = new DynamicTrigramModel(dictionary);
        model.setText(asList("one", "two", "three"));
        model.allocate();
        assertThat(model.getVocabulary(),
                containsInAnyOrder("one", "two", "three"));
    }

    @Test(enabled = false)
    public void compareWithPrecomputed() throws ClassNotFoundException,
            IOException {
        DynamicTrigramModel model = new DynamicTrigramModel(dictionary);
        URL url = getClass().getResource("npr.transcript");
        
        Scanner scanner = new Scanner(new File("../words"));
        List<String> words = new ArrayList<String>();
        while (scanner.hasNext()) {
            words.add(scanner.next());
        }
        scanner.close();
        model.setText(words);
        model.allocate();
        url = getClass().getResource("npr.lm");
        SimpleNGramModel simpleModel =
                new SimpleNGramModel(url.getPath(), dictionary, 1.f, -1);
        model.allocate();
        simpleModel.allocate();
        assertThat(model.getVocabulary(), equalTo(simpleModel.getVocabulary()));
        for (WordSequence wordSequence : simpleModel.getNGrams()) {
            if (wordSequence.size() < 3)
                continue;
            System.err.println(wordSequence);
            assertThat(model.getProbability(wordSequence),
                    equalTo(simpleModel.getProbability(wordSequence)));
        }
    }
}
