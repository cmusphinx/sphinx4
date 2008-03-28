package edu.cmu.sphinx.decoder.scorer;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.databranch.DataBufferProcessor;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.frontend.test.AbstractTestProcessor;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unit tests for the processing logic of the scorer implementations
 *
 * @author Holger Brandl
 */
public class ScorerTests {

    Token testToken =  new Token(1.f, 1.f, null){

        @Override
        public float calculateScore(Data feature, boolean keepData, float gain) {
            return -1;
        }
    };

    @BeforeClass
    public static void configureLogger() {
        Logger.getLogger(ScorerTests.class.getSimpleName()).setLevel(Level.FINER);
    }


    @Test
    public void waitUntilSpeechStart() throws IOException {
        List<Class<? extends AbstractScorer>> scorerClasses = Arrays.asList(SimpleAcousticScorer.class, ThreadedAcousticScorer.class);

        for (Class<? extends AbstractScorer> scorerClass : scorerClasses) {
            System.err.println("testing: " + scorerClass.getSimpleName());
            DataBufferProcessor dummyFrontEnd = createDummyFrontEnd();

            Map<String, Object> props = new HashMap<String, Object>();
            props.put(AbstractScorer.PROP_FRONTEND, dummyFrontEnd);
            AcousticScorer scorer = ConfigurationManager.getInstance(scorerClass, props);

            int startBufferSize = dummyFrontEnd.getBufferSize();

            scorer.allocate();
            scorer.startRecognition();

            Assert.assertTrue(dummyFrontEnd.getBufferSize() < (startBufferSize - 100));

            List<Token> dummyTokens = Arrays.asList(testToken);
            
            scorer.calculateScores(dummyTokens);

            scorer.stopRecognition();
            scorer.deallocate();
        }
    }


    private DataBufferProcessor createDummyFrontEnd() {
        DataBufferProcessor bufferProc = ConfigurationManager.getInstance(DataBufferProcessor.class);
        bufferProc.processDataFrame(new DataStartSignal(16000));

        for (DoubleData doubleData : AbstractTestProcessor.createFeatVectors(5, 16000, 0, 39, 10))
            bufferProc.processDataFrame(doubleData);

        bufferProc.processDataFrame(new SpeechStartSignal());
        for (DoubleData doubleData : AbstractTestProcessor.createFeatVectors(3, 16000, 1000, 39, 10))
            bufferProc.processDataFrame(doubleData);

        bufferProc.processDataFrame(new SpeechEndSignal());
        for (DoubleData doubleData : AbstractTestProcessor.createFeatVectors(5, 16000, 2000, 39, 10))
            bufferProc.processDataFrame(doubleData);

        bufferProc.processDataFrame(new DataEndSignal(123));

        return bufferProc;
    }
}
