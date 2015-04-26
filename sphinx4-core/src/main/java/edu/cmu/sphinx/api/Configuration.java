/*
 * Copyright 2013 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */

package edu.cmu.sphinx.api;


/**
 * Represents common configuration options.
 *
 * This configuration is used by high-level recognition classes.
 *
 * @see SpeechAligner
 * @see LiveSpeechRecognizer
 * @see StreamSpeechRecognizer
 */
public class Configuration {

    private String acousticModelPath;
    private String dictionaryPath;
    private String languageModelPath;
    private String grammarPath;
    private String grammarName;

    private int sampleRate = 16000;
    private boolean useGrammar = false;

    /**
     * @return path to acoustic model
     */
    public String getAcousticModelPath() {
        return acousticModelPath;
    }

    /**
     * Sets path to acoustic model.
     * @param acousticModelPath URL of the acoustic model
     */
    public void setAcousticModelPath(String acousticModelPath) {
        this.acousticModelPath = acousticModelPath;
    }

    /**
     * @return path to dictionary.
     */
    public String getDictionaryPath() {
        return dictionaryPath;
    }

    /**
     * Sets path to dictionary.
     * @param dictionaryPath URL of the dictionary
     */
    public void setDictionaryPath(String dictionaryPath) {
        this.dictionaryPath = dictionaryPath;
    }

    /**
     * @return path to the language model
     */
    public String getLanguageModelPath() {
        return languageModelPath;
    }

    /**
     * Sets paths to language model resource.
     * @param languageModelPath URL of the language model
     */
    public void setLanguageModelPath(String languageModelPath) {
        this.languageModelPath = languageModelPath;
    }

    /**
     * @return grammar path
     */
    public String getGrammarPath() {
        return grammarPath;
    }

    /**
     * Sets path to grammar resources.
     * @param grammarPath URL of the grammar
     */
    public void setGrammarPath(String grammarPath) {
        this.grammarPath = grammarPath;
    }

    /**
     * @return grammar name
     */
    public String getGrammarName() {
        return grammarName;
    }

    /**
     * Sets grammar name if fixed grammar is used.
     * @param grammarName of the grammar
     */
    public void setGrammarName(String grammarName) {
        this.grammarName = grammarName;
    }

    /**
     * @return whether fixed grammar should be used instead of language model.
     */
    public boolean getUseGrammar() {
        return useGrammar;
    }

    /**
     * Sets whether fixed grammar should be used instead of language model.
     * @param useGrammar to use grammar or language model
     */
    public void setUseGrammar(boolean useGrammar) {
        this.useGrammar = useGrammar;
    }

    /**
     * @return the configured sample rate.
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Sets sample rate for the input stream.
     * @param sampleRate sample rate in Hertz
     */
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }
}
