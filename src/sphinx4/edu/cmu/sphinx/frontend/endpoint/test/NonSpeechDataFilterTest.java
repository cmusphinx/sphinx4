package edu.cmu.sphinx.frontend.endpoint.test;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.frontend.test.AbstractTestProcessor;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import static junit.framework.Assert.assertTrue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Some tests to ensure that the NonSpeechDataFilter filters non-speech in the specified manner.
 *
 * @author Holger Brandl
 */
public class NonSpeechDataFilterTest extends AbstractTestProcessor {


    public NonSpeechDataFilter createDataFilter(boolean mergeSpeechSegments) {
        try {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(NonSpeechDataFilter.PROP_MERGE_SPEECH_SEGMENTS, mergeSpeechSegments);

            return (NonSpeechDataFilter) ConfigurationManager.getInstance(NonSpeechDataFilter.class, props);
        } catch (PropertyException e) {
            e.printStackTrace();
        }

        return null;
    }


    @Test
    public void testOneSpeechRegion() throws DataProcessingException {
        int sampleRate = 1000;

        input.add(new DataStartSignal(sampleRate));

        input.addAll(createFeatVectors(1, sampleRate, 0, 10, 10)); // create one second of data sampled with 1kHz
        input.add(new SpeechStartSignal(-1));
        input.addAll(createFeatVectors(1, sampleRate, 0, 10, 10));
        input.add(new SpeechEndSignal(-1));
        input.addAll(createFeatVectors(1, sampleRate, 0, 10, 10));

        input.add(new DataEndSignal(0));

        List<Data> result = collectOutput(createDataFilter(false));

        assertTrue(result.size() == 104);
        assertTrue(result.get(0) instanceof DataStartSignal);
        assertTrue(result.get(1) instanceof SpeechStartSignal);
        assertTrue(result.get(102) instanceof SpeechEndSignal);
        assertTrue(result.get(103) instanceof DataEndSignal);
    }


    @Test
    public void testMultipleSpeechRegionWithoutMerging() throws DataProcessingException {
        int sampleRate = 1000;

        input.add(new DataStartSignal(sampleRate));

        input.addAll(createFeatVectors(1, sampleRate, 0, 10, 10)); // create one second of data sampled with 1kHz
        input.add(new SpeechStartSignal(-1));
        input.addAll(createFeatVectors(1, sampleRate, 0, 10, 10));
        input.add(new SpeechEndSignal(-1));
        input.addAll(createFeatVectors(1, sampleRate, 0, 10, 10));
        input.add(new SpeechStartSignal(-1));
        input.addAll(createFeatVectors(1, sampleRate, 0, 10, 10));
        input.add(new SpeechEndSignal(-1));
        input.addAll(createFeatVectors(1, sampleRate, 0, 10, 10));


        input.add(new DataEndSignal(0));

        // false should make the processor to merge
        List<Data> result = new ArrayList<Data>();

        NonSpeechDataFilter nonSpeechDataFilter = createDataFilter(false);
        result.addAll(collectOutput(nonSpeechDataFilter));
        result.addAll(collectOutput(nonSpeechDataFilter));

        assertTrue(result.size() == 208);

        assertTrue(result.get(0) instanceof DataStartSignal);
        assertTrue(result.get(1) instanceof SpeechStartSignal);

        assertTrue(result.get(102) instanceof SpeechEndSignal);
        assertTrue(result.get(103) instanceof DataEndSignal);

        assertTrue(result.get(104) instanceof DataStartSignal);
        assertTrue(result.get(105) instanceof SpeechStartSignal);

        assertTrue(result.get(206) instanceof SpeechEndSignal);
        assertTrue(result.get(207) instanceof DataEndSignal);
    }


    @Test
    public void testMultipleSpeechRegionWithMerging() throws DataProcessingException {
        int sampleRate = 1000;

        input.add(new DataStartSignal(sampleRate));

        input.addAll(createFeatVectors(1, sampleRate, 0, 10, 10)); // create one second of data sampled with 1kHz
        input.add(new SpeechStartSignal(-1));
        input.addAll(createFeatVectors(1, sampleRate, 0, 10, 10));
        input.add(new SpeechEndSignal(-1));
        input.addAll(createFeatVectors(1, sampleRate, 0, 10, 10));
        input.add(new SpeechStartSignal(-1));
        input.addAll(createFeatVectors(1, sampleRate, 0, 10, 10));
        input.add(new SpeechEndSignal(-1));
        input.addAll(createFeatVectors(1, sampleRate, 0, 10, 10));

        input.add(new DataEndSignal(0));

        List<Data> result = collectOutput(createDataFilter(true));
        assertTrue(result.size() == 304);

        assertTrue(result.get(0) instanceof DataStartSignal);
        assertTrue(result.get(1) instanceof SpeechStartSignal);

        assertTrue(result.get(302) instanceof SpeechEndSignal);
        assertTrue(result.get(303) instanceof DataEndSignal);
    }
}
