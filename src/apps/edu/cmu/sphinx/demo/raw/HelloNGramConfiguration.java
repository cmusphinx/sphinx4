package edu.cmu.sphinx.demo.raw;

import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.linguist.dictionary.FastDictionary;
import edu.cmu.sphinx.linguist.language.ngram.BackoffLanguageModel;
import edu.cmu.sphinx.linguist.language.ngram.SimpleNGramModel;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

/**
 * User: peter
 * Date: Nov 5, 2009
 * Time: 9:19:18 PM
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
public class HelloNGramConfiguration extends CommonConfiguration {

    public HelloNGramConfiguration() throws MalformedURLException, URISyntaxException, ClassNotFoundException {
    }

    protected void initCommon() {
        super.initCommon();

        this.absoluteBeamWidth = 5000;
        this.relativeBeamWidth = 1E-120;
        this.absoluteWordBeamWidth = 200;
        this.relativeWordBeamWidth = 1E-80;

        this.wordInsertionProbability = 0.7;
        this.languageWeight = 10.5f;
        this.silenceInsertionProbability = 0.1;
    }

    protected void initModels() throws MalformedURLException, URISyntaxException, ClassNotFoundException {

        this.unitManager = new UnitManager();

        this.modelLoader = new Sphinx3Loader(
                "resource:/WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz",
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
                "resource:/WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz/dict/cmudict.0.6d",
                "resource:/WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz/noisedict",
                new ArrayList<URL>(),
                false,
                "<sil>",
                false,
                false,
                unitManager);
    }

    protected void initLinguist() throws MalformedURLException, ClassNotFoundException {

        this.languageModel = new SimpleNGramModel(
                // urlLocation,
                "resource:/edu/cmu/sphinx/demo/hellongram/hellongram.trigram.lm",
                dictionary, // dictionary
                0.7f, // unigramWeight,
                logMath, // logMath,
                3 // desiredMaxDepth,
        );

        this.linguist = new LexTreeLinguist(
                model, // acousticModel
                logMath, // logMath
                unitManager, //  unitManager,
                (BackoffLanguageModel)languageModel, // languageModel,
                dictionary, // dictionary,
                true, //boolean fullWordHistories,
                true, // wantUnigramSmear,
                wordInsertionProbability, // wordInsertionProbability,
                silenceInsertionProbability, // double silenceInsertionProbability,
                1E-10, // fillerInsertionProbability,
                1.0, // unitInsertionProbability,
                languageWeight, // languageWeight,
                false, // addFillerWords,
                false, // generateUnitStates,
                1.0f, // unigramSmearWeight,
                0 // arcCacheSize
        );
    }


}
