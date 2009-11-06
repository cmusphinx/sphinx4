package edu.cmu.sphinx.demo.raw;

import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.linguist.dictionary.FastDictionary;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.flat.FlatLinguist;
import edu.cmu.sphinx.linguist.language.ngram.SimpleNGramModel;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist;
import edu.cmu.sphinx.jsapi.JSGFGrammar;

import java.net.MalformedURLException;
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

    public HelloNGramConfiguration(String root) throws MalformedURLException {
            super(root);
        }

        protected void initCommon() {
            super.initCommon();

            this.absoluteBeamWidth = 5000;
            this.relativeBeamWidth = 1E-120;
            this.absoluteWordBeamWidth = 200;
            this.relativeWordBeamWidth =1E-80;

            this.wordInsertionProbability = 0.7;
            this.languageWeight = 10.5f;
            this.silenceInsertionProbability = 0.1;
        }

        protected void initModels() throws MalformedURLException {

            this.unitManager = new UnitManager();

            this.modelLoader = new Sphinx3Loader(
                    "file:"+root+"/models/acoustic/wsj/model.props",
                    logMath,
                    unitManager,
                    true,
                    true,
                    39,
                    "file:"+root+"/models/acoustic/wsj/etc/WSJ_clean_13dCep_16k_40mel_130Hz_6800Hz.4000.mdef",
                    "file:"+root+"/models/acoustic/wsj/cd_continuous_8gau/",
                    0.0f,
                    1e-7f,
                    0.0001f,
                    true);

            this.model = new TiedStateAcousticModel(modelLoader, unitManager, true);

            this.dictionary = new FastDictionary(
                    new URL("file:"+root+"/models/acoustic/wsj/dict/cmudict.0.6d"),
                    new URL("file:"+root+"/models/acoustic/wsj/dict/fillerdict"),
                    new ArrayList<URL>(),
                    false,
                    "<sil>",
                    false,
                    false,
                    unitManager);
        }

        protected void initLinguist() throws MalformedURLException {

            this.languageModel = new SimpleNGramModel(
                    "arpa", // format,
                            // urlLocation,
                    new URL("file:"+root+"/src/apps/edu/cmu/sphinx/demo/hellongram/hellongram.trigram.lm"),
                    dictionary, // dictionary
                    0.7f, // unigramWeight,
                    logMath, // logMath,
                    3 // desiredMaxDepth,
                    );

            this.linguist = new LexTreeLinguist(
                    model, // acousticModel
                    logMath, // logMath
                    unitManager, //  unitManager,
                    languageModel, // languageModel,
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
