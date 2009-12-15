
package edu.cmu.sphinx.jsapi;

import javax.speech.recognition.ResultEvent;
import javax.speech.recognition.RuleGrammar;

import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

/**
 * Result listener for results from the sphinx recognizer.
 *
 * @author Dirk Schnelle-Walka
 * @version $Revision: 1864 $
 */
class Sphinx4ResultListener
        implements ResultListener {
    /** The recognizer which is notified when a result is obtained. */
    private final SphinxRecognizer recognizer;

    /**
     * Construct a new result listener.
     * @param rec The recognizer.
     */
    public Sphinx4ResultListener(final SphinxRecognizer rec) {
        recognizer = rec;
    }

    /**
     * Method called when a result is generated.
     * @param result The new result.
     */
    public void newResult(final Result result) {
        if (!result.isFinal()) {
            return;
        }

        final RuleGrammar grammar = recognizer.getRuleGrammar();
        final Sphinx4Result res = new Sphinx4Result(grammar, result);

        if (res.numTokens() == 0) {
            res.setResultState(javax.speech.recognition.Result.REJECTED);

            final ResultEvent event =
                    new ResultEvent(res, ResultEvent.RESULT_REJECTED);
            recognizer.fireResultRejected(event);
        } else {
            res.setResultState(javax.speech.recognition.Result.ACCEPTED);

            final ResultEvent event =
                    new ResultEvent(res, ResultEvent.RESULT_ACCEPTED);
            recognizer.fireResultAccepted(event);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void newProperties(final PropertySheet sheet)
        throws PropertyException {
    }
}
