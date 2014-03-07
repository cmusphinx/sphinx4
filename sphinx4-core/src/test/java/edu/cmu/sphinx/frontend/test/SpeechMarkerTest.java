package edu.cmu.sphinx.frontend.test;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechMarker;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

/**
 * DOCUMENT ME!
 *
 * @author Holger Brandl
 */
public class SpeechMarkerTest extends RandomDataProcessor {


    public BaseDataProcessor createDataFilter(boolean mergeSpeechSegments) {
        try {


            SpeechMarker speechMarker = ConfigurationManager.getInstance(SpeechMarker.class);
            speechMarker.initialize();

//            Map<String, Object> props = new HashMap<String, Object>();
//            props.put(NonSpeechDataFilter.PROP_MERGE_SPEECH_SEGMENTS, mergeSpeechSegments);
//            NonSpeechDataFilter nonSpeechDataFilter = (NonSpeechDataFilter) ConfigurationManager.getInstance(NonSpeechDataFilter.class, props);
            return speechMarker;


        } catch (PropertyException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Test whether the speech marker is able to handle cases in which an DataEndSignal occurs somewhere after a
     * SpeechStartSignal. This is might occur if the microphone is stopped while someone is speaking.
     */
    @Test
    public void testEndWithoutSilence() throws DataProcessingException {
        int sampleRate = 1000;

        input.add(new DataStartSignal(sampleRate));

//        input.addAll(createFeatVectors(1, sampleRate, 0, 10, 10)); // create one second of data sampled with 1kHz
//        input.addAll(createClassifiedSpeech(sampleRate, 1.0, false));
//        input.add(new SpeechStartSignal(-1));
        input.addAll(createClassifiedSpeech(sampleRate, 2.0, true));
        input.add(new DataEndSignal(-2));

        List<Data> results = collectOutput(createDataFilter(false));

        Assert.assertTrue(results.size() == 104);
        Assert.assertTrue(results.get(0) instanceof DataStartSignal);
        Assert.assertTrue(results.get(1) instanceof SpeechStartSignal);
        Assert.assertTrue(results.get(102) instanceof SpeechEndSignal);
        Assert.assertTrue(results.get(103) instanceof DataEndSignal);
    }


    private List<SpeechClassifiedData> createClassifiedSpeech(int sampleRate, double lengthSec, boolean isSpeech) {
        List<SpeechClassifiedData> datas = new ArrayList<SpeechClassifiedData>();

        List<DoubleData> featVecs = createFeatVectors(1, sampleRate, 0, 10, 10);
        for (DoubleData featVec : featVecs) {
            datas.add(new SpeechClassifiedData(featVec, isSpeech));
        }

        return datas;
    }
}
