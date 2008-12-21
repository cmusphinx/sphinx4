package edu.cmu.sphinx.demo;

import edu.cmu.sphinx.demo.wavfile.WavFile;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import org.junit.Assert;
import org.junit.Test;

/**
 * Some unit test that ensure that the demos are working correctly. Basically each demo is tested with a known audio
 * input.
 *
 * @author Holger Brandl
 */
public class DemoUnitTests {

    @Test
    public void testWavFileDemo() {
        ConfigurationManager cm = new ConfigurationManager(WavFile.class.getResource("config.xml"));

        // look up the recognizer (which will also lookup all its dependencies
        Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
        recognizer.allocate();

        // configure the audio input for the recognizer
        AudioFileDataSource dataSource = (AudioFileDataSource) cm.lookup("audioFileDataSource");
        dataSource.setAudioFile(WavFile.class.getResource("12345.wav"), null);

        // recognize and make sure that the result is correct
        Result result = recognizer.recognize();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getBestResultNoFiller().equals("one two three four five"));
    }
}
