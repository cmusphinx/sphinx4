package edu.cmu.sphinx.demo.raw;

import edu.cmu.sphinx.decoder.Decoder;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.decoder.pruner.Pruner;
import edu.cmu.sphinx.decoder.pruner.SimplePruner;
import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer;
import edu.cmu.sphinx.decoder.search.ActiveListFactory;
import edu.cmu.sphinx.decoder.search.PartitionActiveListFactory;
import edu.cmu.sphinx.decoder.search.SearchManager;
import edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifier;
import edu.cmu.sphinx.frontend.endpoint.SpeechMarker;
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.feature.LiveCMN;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.instrumentation.BestPathAccuracyTracker;
import edu.cmu.sphinx.instrumentation.MemoryTracker;
import edu.cmu.sphinx.instrumentation.Monitor;
import edu.cmu.sphinx.instrumentation.SpeedTracker;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.language.grammar.Grammar;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.util.LogMath;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: peter
 * Date: Nov 2, 2009
 * Time: 2:20:06 PM
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
abstract public class CommonConfiguration {

    protected int absoluteBeamWidth;
    protected double relativeBeamWidth;
    protected double wordInsertionProbability;
    protected int absoluteWordBeamWidth;
    protected double relativeWordBeamWidth;
    protected double silenceInsertionProbability;
    protected float languageWeight;

    protected DataProcessor audioDataSource;
    protected DataProcessor speechMarker;
    protected DataProcessor dataBlocker;
    protected DataProcessor speechClassifier;
    protected DataProcessor nonSpeechDataFilter;
    protected DataProcessor premphasizer;
    protected DataProcessor windower;
    protected DataProcessor fft;
    protected DataProcessor melFilterBank;
    protected DataProcessor dct;
    protected DataProcessor dither;
    protected DataProcessor cmn;
    protected DataProcessor featureExtraction;
    protected FrontEnd frontend;

    protected String dictURL;
    protected String fillerDictURL;
    protected String modelLocation;

    protected UnitManager unitManager;
    protected Loader modelLoader;
    protected AcousticModel model;

    protected Dictionary dictionary;
    protected Grammar grammar;
    protected LanguageModel languageModel;
    protected Linguist linguist;

    protected ActiveListFactory activeList;
    protected AcousticScorer scorer;
    protected Pruner pruner;
    protected ActiveListFactory activeListFactory;
    protected ActiveListFactory wordActiveListFactory;
    protected SearchManager searchManager;
    protected List<Monitor> monitors;
    protected Decoder decoder;
    protected Recognizer recognizer;

    protected LogMath logMath;

    protected void initCommon() {
        Logger.getLogger("").setLevel(Level.WARNING);
        this.logMath = new LogMath(1.0001f, true);
    }

    protected void initAudioDataSource() {
        this.audioDataSource = new AudioFileDataSource(3200, null);
    }

    public AudioFileDataSource getAudioFileDataSource() {
        return (AudioFileDataSource) audioDataSource;
    }

    abstract protected void initModels() throws MalformedURLException, URISyntaxException, ClassNotFoundException;

    abstract protected void initLinguist() throws MalformedURLException, ClassNotFoundException;


    protected void initFrontEnd() {

        this.dataBlocker = new DataBlocker(
                10 // blockSizeMs
        );
        this.speechClassifier = new SpeechClassifier(
                10,     // frameLengthMs,
                0.003, // adjustment,
                10,     // threshold,
                0       // minSignal
        );

        this.speechMarker = new SpeechMarker(
                200, // startSpeechTime,
                500, // endSilenceTime,
                100, // speechLeader,
                50,  // speechLeaderFrames
                100, // speechTrailer
                15.0 // endSilenceDecay
        );

        this.nonSpeechDataFilter = new NonSpeechDataFilter();

        this.premphasizer = new Preemphasizer(
                0.97 // preemphasisFactor
        );
        this.windower = new RaisedCosineWindower(
                0.46, // double alpha
                25.625f, // windowSizeInMs
                10.0f // windowShiftInMs
        );
        this.fft = new DiscreteFourierTransform(
                -1, // numberFftPoints
                false // invert
        );
        this.melFilterBank = new MelFrequencyFilterBank(
                130.0, // minFreq,
                6800.0, // maxFreq,
                40 // numberFilters
        );
        this.dct = new DiscreteCosineTransform(
                40, // numberMelFilters,
                13  // cepstrumSize
        );
        this.cmn = new LiveCMN(
                12.0, // initialMean,
                100,  // cmnWindow,
                160   // cmnShiftWindow
        );
        this.featureExtraction = new DeltasFeatureExtractor(
                3 // window
        );

        ArrayList<DataProcessor> pipeline = new ArrayList<DataProcessor>();
        pipeline.add(audioDataSource);
        pipeline.add(dataBlocker);
        pipeline.add(speechClassifier);
        pipeline.add(speechMarker);
        pipeline.add(nonSpeechDataFilter);
        pipeline.add(premphasizer);
        pipeline.add(windower);
        pipeline.add(fft);
        pipeline.add(melFilterBank);
        pipeline.add(dct);
        pipeline.add(cmn);
        pipeline.add(featureExtraction);

        this.frontend = new FrontEnd(pipeline);

    }

    protected void initRecognizer() {

        this.scorer = new ThreadedAcousticScorer(frontend, null, 10, true, 0, Thread.NORM_PRIORITY);

        this.pruner = new SimplePruner();

        this.activeListFactory = new PartitionActiveListFactory(absoluteBeamWidth, relativeBeamWidth, logMath);

        this.searchManager = new SimpleBreadthFirstSearchManager(
                logMath, linguist, pruner,
                scorer, activeListFactory,
                false, 0.0, 0, false);

        this.decoder = new Decoder(searchManager,
                false, false,
                new ArrayList<ResultListener>(),
                100000);

        this.recognizer = new Recognizer(decoder, monitors);

        this.monitors = new ArrayList<Monitor>();
        this.monitors.add(new BestPathAccuracyTracker(recognizer, false, false, false, false, false, false));
        this.monitors.add(new MemoryTracker(recognizer, false, false));
        this.monitors.add(new SpeedTracker(recognizer, frontend, true, false, false, false));
    }


    protected void init() throws MalformedURLException, URISyntaxException, ClassNotFoundException {
        initCommon();
        initAudioDataSource();
        initFrontEnd();
        initModels();
        initLinguist();
        initRecognizer();
    }

    public Recognizer getRecognizer() {
        return recognizer;
    }

    public CommonConfiguration() throws MalformedURLException, URISyntaxException, ClassNotFoundException {
        init();
    }


}
