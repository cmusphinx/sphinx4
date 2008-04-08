package edu.cmu.sphinx.decoder.scorer;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.frontend.util.DataUtil;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Component;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Implements some common scorer functionality but keeps specific scoring open for subclasses.
 *
 * @author Holger Brandl
 */
public abstract class AbstractScorer implements AcousticScorer {


    /** Property the defines the frontend to retrieve features from for scoring */
    @S4Component(type = BaseDataProcessor.class)
    public final static String PROP_FRONTEND = "frontend";

    /**
     * If set only the feature frames which appear between a <code>SpeechStartSignal</code> and a
     * <code>SpeechEndSignal</code> will be used for decoding. If not set all feature frames will be used for decoding.
     */
    @S4Boolean(defaultValue = true)
    public static final String USE_STREAM_SIGNALS = "useStreamSignals";
    private boolean useStreamSignals;

    protected BaseDataProcessor frontEnd;

    protected Logger logger;
    protected String name;


    public void newProperties(PropertySheet ps) throws PropertyException {
        frontEnd = (BaseDataProcessor) ps.getComponent(PROP_FRONTEND);
        useStreamSignals = ps.getBoolean(USE_STREAM_SIGNALS);

        logger = ps.getLogger();
        name = ps.getInstanceName();
    }


    /**
     * Scores the given set of states
     *
     * @param scoreableList a list containing scoreable objects to be scored
     * @return the best scorign scoreable, or null if there are no more features to score
     */
    public Scoreable calculateScores(List<Token> scoreableList) {

        if (scoreableList.size() <= 0) {
            return null;
        }

        try {
            Data data = frontEnd.getData();

            while (data instanceof Signal && useStreamSignals) {
                if (data instanceof SpeechEndSignal)
                    return null;

                data = frontEnd.getData();
            }

            if (data == null || data instanceof DataEndSignal)
                return null;

            if (data instanceof DoubleData)
                data = DataUtil.DoubleData2FloatData((DoubleData) data);

            return doScoring(scoreableList, data);
        } catch (DataProcessingException dpe) {
            dpe.printStackTrace();
            return null;
        }
    }


    /**
     * Scores a a list of <code>Token</code>s given a <code>Data</code>-object.
     *
     * @param scoreableList The list of Tokens to be scored
     * @param data          The <code>Data</code>-object to be used for scoring.
     * @return the best scoring <code>Token</code> or <code>null</code> if the list of tokens was empty.
     */
    protected abstract Scoreable doScoring(List<Token> scoreableList, Data data);


    public void startRecognition() {
        // iterate through the feature stream until a SpeechStartSignal is being found
        int debugCounter = 0;

        while (!(frontEnd.getData() instanceof SpeechStartSignal) && useStreamSignals) {
            debugCounter++;

            if (debugCounter > 100) { // print a warning every second
                debugCounter = 0;
                logger.finer("Waiting for speech segment...");
            }
        }

        // (if this line becomes reached && useStreamSignals) all following feature frames will be part of a speech segment
    }


    public void stopRecognition() {
    }


    public void allocate() throws IOException {
    }


    public void deallocate() {
    }
}
