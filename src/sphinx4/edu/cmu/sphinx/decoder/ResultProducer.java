package edu.cmu.sphinx.decoder;

import edu.cmu.sphinx.result.ResultListener;

/**
 * Some API-elements shared by components which are able to produce <code>Result</code>s.
 *
 * @see edu.cmu.sphinx.result.Result
 */
public interface ResultProducer {

    /** Registers a new listener for <code>Result</code>-notifications */
    void addResultListener(ResultListener resultListener);


    /** Removs a listener from this <code>ResultProducer</code>-instance. */
    void removeResultListener(ResultListener resultListener);
}
