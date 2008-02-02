/*
 *
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.decoder;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.result.Result;


/**
 * A decoder which does not use the common pull-principle of S4 but recognizes frame-wise (aka push-principle). When
 * using this decoder, make sure that the scorer used by the <code>searchManager</code> can access some buffered
 * <code>Data</code>s. This can be achieved e.g. by inserting the a data-buffer right before this component in the
 * feature-frontend.
 */
public class PushDecoder extends AbstractDecoder implements DataProcessor {

    private DataProcessor predecessor;


    private boolean isRecognizing;
    public Result result;


    public Data getData() throws DataProcessingException {
        Data d = getPredecessor().getData();

        if (isRecognizing && (d instanceof FloatData || d instanceof DoubleData)) {
            result = decode(null);

            if (result != null && result.isFinal()) {
                fireResultListeners(result);
                result = null;
            }
        }

        if (d instanceof SpeechStartSignal) {
            searchManager.startRecognition();
            isRecognizing = true;
            result = null;
        }

        if (d instanceof SpeechEndSignal) {
            searchManager.stopRecognition();

            //fire results which were not yet final
            if (result != null)
                fireResultListeners(result);

            isRecognizing = false;
        }

        return d;
    }


    public String getName() {
        return PushDecoder.class.getName();
    }


    public DataProcessor getPredecessor() {
        return predecessor;
    }


    public void setPredecessor(DataProcessor predecessor) {
        this.predecessor = predecessor;
    }


    public void initialize() {
    }


    /**
     * Decode frames until recognition is complete
     *
     * @param referenceText the reference text (or null)
     * @return a result
     */
    public Result decode(String referenceText) {
        return searchManager.recognize(1);
    }
}
