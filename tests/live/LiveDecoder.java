/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package tests.live;

import edu.cmu.sphinx.decoder.Decoder;
import edu.cmu.sphinx.decoder.FeatureListener;

import edu.cmu.sphinx.frontend.BatchCMN;
import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumSource;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.LiveCMN;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.SimpleFrontEnd;
import edu.cmu.sphinx.frontend.Utterance;
import edu.cmu.sphinx.frontend.util.Util;

import edu.cmu.sphinx.frontend.endpoint.NonSpeechFilter;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.result.Result;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import tests.frontend.CepstraPanel;
import tests.frontend.CepstrumMonitor;


/**
 * A live-mode Decoder. A LiveDecoder might contain a transcript file
 * that specifies a list of words to test.
 */
public class LiveDecoder extends Decoder {

    private final Live live;
    private Microphone microphone;
    private String testFile;
    private List referenceList;
    private ListIterator iterator;
    private long audioStart;       // in milliseconds
    private long audioLength;      // in milliseconds


    /**
     * Constructs a live mode Decoder.
     *
     * @param context the context of this LiveDecoder
     * @param microphone the Microphone used by this LiveDecoder
     */
    public LiveDecoder(String context, Live live) throws
        IOException, LineUnavailableException {
        super(context);
        this.live = live;
        referenceList = new LinkedList();
    }
    
    /**
     * Initializes this LiveDecoder.
     */
    public void initialize(Microphone microphone) throws IOException {
        if (!isInitialized()) {
            this.microphone = microphone;
            super.initialize(microphone);
            getRecognizer().addSignalFeatureListener(new FeatureListener() {
                    public void featureOccurred(Feature feature) {
                        if (feature.getSignal() == Signal.UTTERANCE_START) {
                            audioStart = feature.getCollectTime();
                        }
                        else if (feature.getSignal() == Signal.UTTERANCE_END) {
                            audioLength = 
                                feature.getCollectTime() - audioStart;
                        }
                    }
                });
        }
    }

    /**
     * Returns true if this LiveDecoder has some sort of endpointer.
     *
     * @return true if this LiveDecoder has some sort of endpointer
     */
    public boolean hasEndpointer() {
        String endpointer = 
            SphinxProperties.getSphinxProperties(getContext()).getString
            (FrontEnd.PROP_ENDPOINTER, null);
        return (endpointer != null);
    }
    
    /**
     * Return the Live instance that spawned this decoder.
     *
     * @return the Live instance that spawned this decoder.
     */
    public Live getLive() {
        return live;
    } 
    
    /**
     * Returns the microphone used by this LiveDecoder.
     *
     * @return the microphone used by this LiveDecoder
     */
    public Microphone getMicrophone() {
        return microphone;
    }

    /**
     * Returns the name of the test file.
     *
     * @return the name of the test file
     */
    public String getTestFile() {
        return testFile;
    }

    /**
     * Sets the test file.
     *
     * @param testFile the test file     
     */
    public void setTestFile(String testFile) throws IOException {
        referenceList.clear();
        this.testFile = testFile;
        BufferedReader reader = new BufferedReader(new FileReader(testFile));
        String line = null;
        while ((line = reader.readLine()) != null) {
            referenceList.add(line);
        }
        iterator = referenceList.listIterator();
    }

    /**
     * Returns the next utterance in the test file. If its at the last
     * utterance already, it will cycle back to the first utterance.
     * If there is no utterance in the file at all, it will return 
     * an empty string.
     *
     * @param the next utterance in the test file; if no utterance,
     *    it will return an empty string
     */
    public String getNextReference() {
        if (iterator == null || !iterator.hasNext()) {
            iterator = referenceList.listIterator();
        }
        String next = "";
        if (iterator.hasNext()) {
            next = (String) iterator.next();
            if (next == null) {
                next = "";
            }
        }
        return next;
    }

    /**
     * Shows the given partial Result.
     *
     * @param result the partial Result to show
     */
    protected void showPartialResult(Result result) {
        live.getLiveFrame().setRecognitionLabel(result.toString());
    }

    /**
     * Returns the audio time for the result.
     */
    public float getAudioTime(Result result) {
        return (audioLength / 1000f);
    }
}
