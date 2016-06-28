package edu.cmu.sphinx.api;

import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.testng.annotations.Test;

import edu.cmu.sphinx.result.WordResult;

public class LiveRecognizerTest {
    @Test
    public void testLm() throws IOException {
        Configuration configuration = new Configuration();

        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/linguist/language/ngram/arpa/test.dic");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/linguist/language/ngram/arpa/test.lm");

        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
        InputStream stream = LiveRecognizerTest.class
                .getResourceAsStream("/edu/cmu/sphinx/tools/bandwidth/10001-90210-01803.wav");
        stream.skip(44);

        // Simple recognition with generic model
        recognizer.startRecognition(stream);
        SpeechResult result = recognizer.getResult();

        assertEquals("one zero zero zero one", result.getHypothesis());

        WordResult word = result.getWords().get(0);
        assertEquals("{one, 0.999, [820:1060]}", word.toString());
    }


    @Test
    public void testGram() throws IOException {
        Configuration configuration = new Configuration();

        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setGrammarPath("resource:/edu/cmu/sphinx/jsgf/test/");
        configuration.setGrammarName("digits.grxml");
        configuration.setUseGrammar(true);

        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
        InputStream stream = LiveRecognizerTest.class
                .getResourceAsStream("/edu/cmu/sphinx/tools/bandwidth/10001-90210-01803.wav");
        stream.skip(44);

        // Simple recognition with generic model
        recognizer.startRecognition(stream);
        SpeechResult result = recognizer.getResult();

        assertEquals("one zero zero zero one", result.getHypothesis());

        WordResult word = result.getWords().get(0);
        assertEquals("{one, 1.000, [840:1060]}", word.toString());
    }

    @Test
    public void testGramWeights() throws IOException {
        Configuration configuration = new Configuration();

        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setGrammarPath("resource:/edu/cmu/sphinx/jsgf/test");
        configuration.setGrammarName("weights");
        configuration.setUseGrammar(true);

        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
        InputStream stream = LiveRecognizerTest.class
                .getResourceAsStream("/edu/cmu/sphinx/tools/bandwidth/10001-90210-01803.wav");
        stream.skip(44);

        // Simple recognition with generic model
        recognizer.startRecognition(stream);
        SpeechResult result = recognizer.getResult();

        assertEquals("one zero zero zero one", result.getHypothesis());

        WordResult word = result.getWords().get(0);
        assertEquals("{one, 1.000, [840:1060]}", word.toString());
    }
}

