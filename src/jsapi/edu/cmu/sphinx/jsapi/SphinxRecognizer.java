/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.jsapi;


import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import javax.speech.EngineException;
import javax.speech.EngineStateError;
import javax.speech.recognition.GrammarException;
import javax.speech.recognition.Rule;
import javax.speech.recognition.RuleGrammar;

import com.sun.speech.engine.recognition.BaseRecognizer;
import com.sun.speech.engine.recognition.BaseRuleGrammar;

import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * A SphinxRecognizer provides access to Sphinx speech recognition capabilities.
 */
public class SphinxRecognizer extends BaseRecognizer {
    /** Msecs to sleep before the status of the recognizer is checked again. */
    private static final long SLEEP_MSEC = 50;

    /** The encapsulated recognizer. */
    private Recognizer recognizer;

    /** The input device. */
    private DataProcessor dataProcessor;

    /** The grammar manager. */
    private JSGFGrammar grammar;

    /** The result listener. */
    private final Sphinx4ResultListener resultListener;

    /**
     * The decoding thread. It points either to the single decoding thread
     * or is <code>null</code> if no recognition thread is started.
     */
    private RecognitionThread recognitionThread;

    /**
     * Creates the default sphinx recognizer.
     * @exception EngineException
     *            if the recognizer could not be created
     */
    public SphinxRecognizer() throws EngineException {
        this(new SphinxRecognizerModeDesc());
    }


    /**
     * Creates a sphinx 4 recognizer that matches the given mode descriptor
     *
     * @param modeDesc the mode descriptor describing the type of recognizer to create
     * @exception EngineException
     *            if the recognizer could not be created
     */
    public SphinxRecognizer(SphinxRecognizerModeDesc modeDesc)
        throws EngineException {
        String config = modeDesc.getConfigFile();
        URL url = SphinxRecognizer.class.getResource(config);
        if (url == null) {
            throw new EngineException(
                    "Unable to load configuration from resource '" + config
                    + "'!");
        }

        try {
            final ConfigurationManager configuration =
                new ConfigurationManager(url);

            recognizer = (Recognizer) configuration.lookup("recognizer");
            dataProcessor = (DataProcessor) configuration.lookup("microphone");
            grammar = (JSGFGrammar) configuration.lookup("jsgfGrammar");
        } catch (Exception ex) {
            throw new EngineException(ex.getMessage());
        }

        resultListener = new Sphinx4ResultListener(this);
    }

    /**
     * Called from the {@link #resume()} method.
     */
    @Override
    protected void handleResume() {
        if (recognizer == null) {
            return;
        }

        if (recognitionThread != null) {
            return;
        }

        recognitionThread = new RecognitionThread(this);
        recognitionThread.start();
    }

    /**
     * Called from the {@link #pause()} method.
     */
    @Override
    protected void handlePause() {
        if (recognitionThread == null) {
            return;
        }

        stopRecognitionThread();
        if (dataProcessor instanceof Microphone) {
            final Microphone microphone = (Microphone) dataProcessor;
            microphone.stopRecording();
        }
    }


    /**
     * Called from the {@link #allocate()} method.
     *
     * @throws EngineException if problems are encountered
     */
    @Override
    protected void handleAllocate()
            throws EngineException {
        if (recognizer == null) {
            throw new EngineException(
                    "cannot allocate: no recognizer created!");
        }

        recognizer.allocate();

        final RuleGrammar[] grammars = listRuleGrammars();
        for (int i = 0; i < grammars.length; i++) {
            deleteRuleGrammar(grammars[i]);
        }

        recognizer.addResultListener(resultListener);

        setEngineState(CLEAR_ALL_STATE, ALLOCATED);
    }

    /**
     * {@inheritDoc}
     * Sphinx4 has different concepts for loading grammars. Since the JSAPI 1.0
     * way does not work properly, this workaround is needed. It simply adds
     * the rules of the loaded grammar to the existing rules.
     */
    @Override
    public RuleGrammar loadJSGF(Reader reader) throws GrammarException,
            IOException, EngineStateError {
        final RuleGrammar loadedGrammar = super.loadJSGF(reader);
        try {
        final RuleGrammar ruleGrammar = new BaseRuleGrammar (this, grammar.getRuleGrammar());
        final String[] loadedRuleNames = loadedGrammar.listRuleNames();
        for (String name : loadedRuleNames) {
            final Rule rule = loadedGrammar.getRule(name);
            ruleGrammar.setRule(name, rule, true);
            ruleGrammar.setEnabled(name, true);
        }
        grammar.commitChanges();
        } catch (Exception e) {
            throw new IOException(e);
        }
        return loadedGrammar;
    }

    /**
     * Called from the {@link #deallocate()} method.
     *
     * @throws EngineException if this <code>Engine</code> cannot be
     *   deallocated.
     */
    @Override
    protected void handleDeallocate()
            throws EngineException {
        if (recognizer == null) {
            throw new EngineException(
                    "cannot deallocate: no recognizer created!");
        }
        // Stop the decoder thread.
        stopRecognitionThread();

        // Deallocate the recognizer and wait until it stops recognizing.
        recognizer.deallocate();
        while (recognizer.getState() == Recognizer.State.RECOGNIZING) {
            try {
                Thread.sleep(SLEEP_MSEC);
            } catch (InterruptedException e) {
                throw new EngineException(e.getMessage());
            }
        }
        recognizer.resetMonitors();
        recognizer.removeResultListener(resultListener);
    }

    /**
     * Load a RuleGrammar and its imported grammars from a URL containing
     * JSGF text.
     * From javax.speech.recognition.Recognizer.
     * @param url the base URL containing the JSGF grammar file.
     * @param name the name of the JSGF grammar to load.
     * @return Loaded grammar.
     *
     * @exception GrammarException
     *            Error in the grammar.
     * @exception IOException
     *            Error reading the grammar.
     */
    @Override
    public RuleGrammar loadJSGF(final URL url, final String name)
            throws GrammarException, IOException {
        try {
            grammar.loadJSGF(name);
        } catch (JSGFGrammarException e) {
            throw new GrammarException(e.toString());
        } catch (JSGFGrammarParseException e) {
            throw new GrammarException(e.toString());
        }
        return super.loadJSGF(url, name);
    }

    /**
     * Selector for the data processor (the microphone).
     * @return The used data processor.
     */
    DataProcessor getDataProcessor() {
        return dataProcessor;
    }

    /**
     * Selector for the wrapped sphinx4 recognizer.
     * @return Recognizer
     */
    Recognizer getRecognizer() {
        return recognizer;
    }

    /**
     * Stop the recognition thread and wait until it is terminated.
     */
    private void stopRecognitionThread() {
        if (recognitionThread == null) {
            return;
        }

        recognitionThread.stopRecognition();

        final long maxSleepTime = 5000;
        long sleepTime = 0;

        while (recognitionThread.isAlive() && (sleepTime < maxSleepTime)) {
            try {
                Thread.sleep(SLEEP_MSEC);
                sleepTime += SLEEP_MSEC;
            } catch (InterruptedException e) {
                return;
            }
        }

        recognitionThread = null;
    }

    /**
     * Get the current rule grammar.
     * @return Active grammar.
     */
    RuleGrammar getRuleGrammar() {        
        return new BaseRuleGrammar (this, grammar.getRuleGrammar());
    }
}


