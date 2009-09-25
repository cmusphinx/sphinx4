package edu.cmu.sphinx.decoder.scorer;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.frontend.util.DataUtil;
import edu.cmu.sphinx.util.props.ConfigurableAdapter;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Implements some basic scorer functionality but keeps specific scoring open for sub-classes.
 *
 * @author Holger Brandl
 */
public abstract class AbstractScorer extends ConfigurableAdapter implements AcousticScorer {

    /** Property the defines the frontend to retrieve features from for scoring */
    @S4Component(type = BaseDataProcessor.class)
    public final static String FEATURE_FRONTEND = "frontend";
    protected BaseDataProcessor frontEnd;

    /**
     * An opotional post-processor for computed scores that will normalize scores. If not set, no normalization will
     * applied and the token scores will be returned unchanged.
     */
    @S4Component(type = ScoreNormalizer.class, mandatory = false)
    public final static String SCORE_NORMALIZER = "scoreNormalizer";
    private ScoreNormalizer scoreNormalizer;

    private Boolean useSpeechSignals;

    public AbstractScorer() {
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        this.frontEnd = (BaseDataProcessor) ps.getComponent(FEATURE_FRONTEND);
        this.scoreNormalizer = (ScoreNormalizer) ps.getComponent(SCORE_NORMALIZER);
    }

    /**
     * @param frontEnd the frontend to retrieve features from for scoring
     * @param scoreNormalizer optional post-processor for computed scores that will normalize scores. If not set, no normalization will
     * applied and the token scores will be returned unchanged.
     */
    public AbstractScorer(BaseDataProcessor frontEnd, ScoreNormalizer scoreNormalizer) {

        this.frontEnd = frontEnd;
        this.scoreNormalizer = scoreNormalizer;
    }


    /**
     * Scores the given set of states.
     *
     * @param scoreableList A list containing scoreable objects to be scored
     * @return The best scoring scoreable, or <code>null</code> if there are no more features to score
     */
    public Data calculateScores(List<? extends Scoreable> scoreableList) {

    	try {
            Data data = getNextData();
            while (data instanceof Signal) {
                if (data instanceof SpeechEndSignal || data instanceof DataEndSignal) {
                    return data;
                }

                data = getNextData();
            }

            if (data == null)
            	return null;

            if (scoreableList.size() <= 0)
            	return null;
            
            // convert the data to FloatData if not yet done
            if (data instanceof DoubleData)
                data = DataUtil.DoubleData2FloatData((DoubleData) data);

            Data bestToken = doScoring(scoreableList, data);

            // apply optional score normalization
            if (scoreNormalizer != null && bestToken instanceof Token)
                bestToken = scoreNormalizer.normalize(scoreableList, (Token)bestToken);

            return bestToken;
        } catch (DataProcessingException dpe) {
            dpe.printStackTrace();
            return null;
        }
    }


    private Data getNextData() {
        Data data = frontEnd.getData();

        // reconfigure the scorer for the coming data stream
        if (data instanceof DataStartSignal) {
            handleDataStartSignal((DataStartSignal) data);
        }

        if (data instanceof DataEndSignal)
            handleDataEndSignal((DataEndSignal) data);

        return data;
    }


    /** Handles the first element in a feature-stream. */
    protected void handleDataStartSignal(DataStartSignal dataStartSignal) {
        Map<String, Object> dataProps = dataStartSignal.getProps();

        if (dataProps.containsKey(DataStartSignal.SPEECH_TAGGED_FEATURE_STREAM))
            useSpeechSignals = (Boolean) dataProps.get(DataStartSignal.SPEECH_TAGGED_FEATURE_STREAM);
        else
            useSpeechSignals = false;
    }


    /** Handles the last element in a feature-stream. */
    protected void handleDataEndSignal(DataEndSignal dataEndSignal) {
        // we don't treat the end-signal here, but extending classes might do
    }


    public void startRecognition() {
        if (useSpeechSignals == null) {
            Data firstData = getNextData();
            
            if (firstData == null)
        	    return;
            
            assert firstData instanceof DataStartSignal :
                    "The first element in an sphinx4-feature stream must be a DataStartSignal but was a " + firstData.getClass().getSimpleName();
        }

        if (!useSpeechSignals)
            return;

        Data data = getNextData();
        while (!(data instanceof SpeechStartSignal)) {
            if (data == null) {
                break;
            }

            data = getNextData();
        }
    }


    public void stopRecognition() {
        // nothing needs to be done here
    }


    /**
     * Scores a a list of <code>Token</code>s given a <code>Data</code>-object.
     *
     * @param scoreableList The list of Tokens to be scored
     * @param data          The <code>Data</code>-object to be used for scoring.
     * @return the best scoring <code>Token</code> or <code>null</code> if the list of tokens was empty.
     */
    protected abstract Data doScoring(List<? extends Scoreable> scoreableList, Data data);


    // Even if we don't do any meaningful allocation here, we implement the methods because
    // most extending scorers do need them either.
    
    public void allocate() {
    }


    public void deallocate() {
    }
}
