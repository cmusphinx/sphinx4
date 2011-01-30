package edu.cmu.sphinx.demo.raw;

import edu.cmu.sphinx.demo.raw.CommonConfiguration;
import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.linguist.dictionary.FastDictionary;
import edu.cmu.sphinx.linguist.flat.FlatLinguist;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

/**
 * User: peter
 * Date: Nov 2, 2009
 * Time: 4:52:55 PM
 * <p/>
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * <p/>
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */
class TranscriberConfiguration extends CommonConfiguration {

    public TranscriberConfiguration() throws MalformedURLException, URISyntaxException, ClassNotFoundException {
        super();
    }

    protected void initCommon() {
        super.initCommon();

        this.absoluteBeamWidth = -1;
        this.relativeBeamWidth = 1E-80;
        this.wordInsertionProbability = 1E-36;
        this.languageWeight = 8.0f;
    }

    protected void initModels() throws MalformedURLException, URISyntaxException, ClassNotFoundException {

        this.unitManager = new UnitManager();

        this.modelLoader = new Sphinx3Loader(
                "resource:/TIDIGITS_8gau_13dCep_16k_40mel_130Hz_6800Hz",
                "mdef",
                "",
                logMath,
                unitManager,
                0.0f,
                1e-7f,
                0.0001f,
                true);

        this.model = new TiedStateAcousticModel(modelLoader, unitManager, true);

        this.dictionary = new FastDictionary(
                "resource:/TIDIGITS_8gau_13dCep_16k_40mel_130Hz_6800Hz/dict/dictionary",
                "resource:/TIDIGITS_8gau_13dCep_16k_40mel_130Hz_6800Hz/noisedict",
                new ArrayList<URL>(),
                false,
                "<sil>",
                false,
                false,
                unitManager);
    }

    protected void initLinguist() throws MalformedURLException, ClassNotFoundException {

        this.grammar = new JSGFGrammar(
                // URL baseURL,
                "resource:/edu/cmu/sphinx/demo/transcriber",
                logMath, // LogMath logMath,
                "digits", // String grammarName,
                false, // boolean showGrammar,
                false, // boolean optimizeGrammar,
                false, // boolean addSilenceWords,
                false, // boolean addFillerWords,
                dictionary // Dictionary dictionary
        );

        this.linguist = new FlatLinguist(
                model, // AcousticModel acousticModel,
                logMath, // LogMath logMath,
                grammar, // Grammar grammar,
                unitManager, // UnitManager unitManager,
                wordInsertionProbability, // double wordInsertionProbability,
                1.0, // double silenceInsertionProbability,
                1.0, // double fillerInsertionProbability,
                1.0, // double unitInsertionProbability,
                languageWeight, // float languageWeight,
                false, // boolean dumpGStates,
                false, // boolean showCompilationProgress,
                false, // boolean spreadWordProbabilitiesAcrossPronunciations,
                false, // boolean addOutOfGrammarBranch,
                1.0, // double outOfGrammarBranchProbability,
                1.0, // double phoneInsertionProbability,
                null // AcousticModel phoneLoopAcousticModel
        );
    }

}
