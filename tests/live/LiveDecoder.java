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

import edu.cmu.sphinx.frontend.BatchCMN;
import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumSource;
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
            if (hasEnergyEndpointer()) {
                //TODO: this doesn't work anymore
                insertEnergyEndpointViewer();
            }
        }
    }


    /**
     * Inserts an EnergyEndpoint viewer, called CepstraGroupProducer.
     * This is a FrontEnd processor that is positioned after the
     * EnergyEndpointer, and before the NonSpeechFilter. It "watches" the
     * endpointed Cepstra coming out of the EnergyEndpointer, and
     * plots the speech energy levels and speech endpoints.
     * It is so called because this processor groups the watched
     * Cepstra together to form a CepstraGroup, and feeds the CepstraGroup
     * to the CepstraPanel of the LiveFrame for plotting. 
     */
    private void insertEnergyEndpointViewer() {

        // BUG: TODO: This code assumes that the frontend is a
        // SimpleFrontEnd which is not a valid assumption. This code
        // should be refactored to either not depend on the FrontEnd
        // implementation, or if necessary, define the FrontEnd and
        // set the front end explicitly. 
        // Until then, it is disabled.

        if (true) {             // disable this code
            return;
        }
        SimpleFrontEnd frontend = (SimpleFrontEnd) 
	    getRecognizer().getFrontEnd();

        CepstrumSource predecessor = (CepstrumSource) frontend.getProcessor
            ("Endpointer");

        if (Boolean.getBoolean("showEnergyPanel")) {
            // create the viewer
            CepstrumSource monitor = new CepstrumMonitor
                ("CepstrumMonitor", getContext(), predecessor) {
                    
                    // Implements the abstract cepstrumMonitor()
                    // of CepstrumMonitor. Here, it updates the 
                    // CepstraPanel of the LiveFrame with the cepstrum.
                    public void cepstrumMonitored(Cepstrum cepstrum) {
                        CepstraPanel panel =
                            live.getLiveFrame().getCepstraPanel();
                        if (cepstrum.hasSignal(Signal.UTTERANCE_START)) {
                            panel.clearCepstra();
                        }
                        panel.addCepstrum(cepstrum);
                    }
                };
            
            predecessor = monitor;
        }
        
        // are we filtering out the non-speech regions?
        boolean filterNonSpeech = SphinxProperties.getSphinxProperties
            (getContext()).getBoolean(FrontEnd.PROP_FILTER_NON_SPEECH, true);

        if (filterNonSpeech) {
            NonSpeechFilter filter = (NonSpeechFilter) frontend.getProcessor
                ("NonSpeechFilter");
            filter.setPredecessor(predecessor);
        }
    }


    /**
     * Returns true if this LiveDecoder has some sort of endpointer.
     *
     * @return true if this LiveDecoder has some sort of endpointer
     */
    public boolean hasEndpointer() {
        return hasEnergyEndpointer();
    }


    /**
     * Returns whether this LiveDecoder has an endpointer.
     *
     * @return true if this LiveDecoder has an endpointer, false otherwise
     */
    private boolean hasEnergyEndpointer() {
        String endpointer = 
            SphinxProperties.getSphinxProperties(getContext()).getString
            (FrontEnd.PROP_ENDPOINTER, null);
        return (endpointer != null &&
                endpointer.equals
                ("edu.cmu.sphinx.frontend.endpoint.EnergyEndpointer"));
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
        Utterance utterance = microphone.getUtterance();
        if (utterance != null) {
            return utterance.getAudioTime();
        } else {
            return Util.getAudioTime
                (result.getFrameNumber(),
                 SphinxProperties.getSphinxProperties(getContext()));
        }
    }
}
