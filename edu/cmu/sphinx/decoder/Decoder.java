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

package edu.cmu.sphinx.decoder;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.SignalListener;

import edu.cmu.sphinx.frontend.util.DataUtil;
import edu.cmu.sphinx.frontend.util.Utterance;

import edu.cmu.sphinx.decoder.search.Token;

import edu.cmu.sphinx.util.NISTAlign;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.BeamFinder;

import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.ResultListener;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Iterator;
import java.text.DecimalFormat;


/**
 * A Decoder.
 */
public class Decoder {


    /**
     * Base property name for the decoder properties
     */
    private  final static String PROP_PREFIX =
	"edu.cmu.sphinx.decoder.Decoder.";

    /**
     * The SphinxProperty name for the input data type.
     */
    public final static String PROP_SHOW_PROPS_AT_START = 
        PROP_PREFIX + "showPropertiesAtStart";


    /**
     * The default value for the property PROP_SHOW_PROPS_AT_START.
     */
    public final static boolean PROP_SHOW_PROPS_AT_START_DEFAULT = false;


    /**
     * A SphinxProperty name for a boolean property that when set will
     * cause the decoder to show all the tokens associated with the
     * best result
     */
    public final static String PROP_SHOW_BEST_TOKEN =
	PROP_PREFIX + "showBestToken";


    /**
     * The default value for the property PROP_SHOW_BEST_TOKEN.
     */
    public final static boolean PROP_SHOW_BEST_TOKEN_DEFAULT = false;


    /**
     * A SphinxProperty name for a boolean property that when set will
     * cause the decoder to show all the tokens associated with the
     * actual result (if present)
     */
    public final static String PROP_SHOW_ACTUAL_TOKEN =
	PROP_PREFIX + "showActualToken";


    /**
     * The default value for the property PROP_SHOW_ACTUAL_TOKEN.
     */
    public final static boolean PROP_SHOW_ACTUAL_TOKEN_DEFAULT = false;


    /**
     * A SphinxProperty name for a boolean property that when set will
     * cause the decoder to show all the tokens associated with an
     * error result
     */
    public final static String PROP_SHOW_ERROR_TOKEN =
	PROP_PREFIX + "showErrorToken";


    /**
     * The default value for the property PROP_SHOW_ERROR_TOKEN.
     */
    public final static boolean PROP_SHOW_ERROR_TOKEN_DEFAULT = false;


    /**
     * A SphinxProperty name for a boolean property that when set will
     * cause the decoder to show all intermediate best token results
     */
    public final static String PROP_SHOW_PARTIAL_RESULTS =
	PROP_PREFIX + "showPartialResults";


    /**
     * The default value for the property PROP_SHOW_PARTIAL_RESULTS.
     */
    public final static boolean PROP_SHOW_PARTIAL_RESULTS_DEFAULT = false;


    /**
     * A SphinxProperty name for a boolean property, that when set to
     * true will cause the decoder to show intermediate token results
     * for the reference text
     */
    public final static String PROP_SHOW_PARTIAL_ACTUAL_RESULTS =
	PROP_PREFIX + "showPartialActualResults";


    /**
     * The default value for the property PROP_SHOW_PARTIAL_ACTUAL_RESULTS.
     */
    public final static boolean PROP_SHOW_PARTIAL_ACTUAL_RESULTS_DEFAULT = 
        false;

    /**
     * A SphinxProperty name for a boolean property, that when set to
     * true will cause the decoder to show detailed statistics
     */
    public final static String PROP_SHOW_DETAILED_STATISTICS  =
	PROP_PREFIX + "showDetailedStatistics";


    /**
     * The default value for the property PROP_SHOW_PARTIAL_ACTUAL_RESULTS.
     */
    public final static boolean PROP_SHOW_DETAILED_STATISTICS_DEFAULT = 
        false;

    /**
     * A SphinxProperty name for a boolean property, that when set to
     * true will cause the decoder to show the score of the hypothesis
     */
    public final static String PROP_SHOW_HYPOTHESIS_SCORE  =
	PROP_PREFIX + "showHypothesisScore";


    /**
     * The default value for the property PROP_SHOW_HYPOTHESIS_SCORE
     */
    public final static boolean PROP_SHOW_HYPOTHESIS_SCORE_DEFAULT =
        false;


    private static DecimalFormat memFormat = new DecimalFormat("0.00 Mb");
    private static DecimalFormat timeFormat = new DecimalFormat("0.00");

    private Recognizer recognizer;

    private NISTAlign aligner;// analyzes results

    private String context;
    private SphinxProperties props;	  // sphinx properties

    private float audioTime = 0.0f;
    private float processingTime = 0.0f;
    private float cumulativeAudioTime = 0.0f;
    private float cumulativeProcessingTime = 0.0f;

    private float maxMemoryUsed = 0.0f;
    private float avgMemoryUsed = 0.0f;

    private int numMemoryStats = 0; // # of times memory stats are collected

    private boolean showPropertiesAtStart = false;
    private boolean showPartialResults = false;
    private boolean showBestToken = false;
    private boolean showErrorToken = false;
    private boolean showActualToken = false;
    private boolean showPartialActualResults = false;
    private boolean showDetailedStatistics = false;
    private boolean showHypothesisScore = false;

    private BeamFinder beamFinder;

    private boolean findResult = true;

    private String currentReferenceText;


    /**
     * Constructs a live mode Decoder without fully initializing
     * all the components. The components can be initialized
     * later by the <code>initialize()</code> method.
     * This constructor can be used to avoid having several Decoders
     * fully loaded, taking up memory.
     *
     * @param context the context of this Decoder
     */
    public Decoder(String context) throws IOException {
        this(context, false);
    }


    /**
     * Constructs a Decoder with the given context,
     * specifying whether to initialize all the components
     * at construction time.
     * This is to avoid having several Decoders fully loaded but
     * only one is actually being used. "Fully loading" a Decoder
     * means creating the Recognizer object for it, thus creating
     * the FrontEnd, AcousticModel, SentenceHMM, etc..
     *
     * @param context the context of this Decoder
     * @param initialize indicate whether to fully load this Decoder
     */
    private Decoder(String context,
                    boolean initialize) throws IOException {

        this.context = context;
	props = SphinxProperties.getSphinxProperties(context);

	showPropertiesAtStart =
            props.getBoolean(PROP_SHOW_PROPS_AT_START,
                             PROP_SHOW_PROPS_AT_START_DEFAULT);
	showPartialResults =
	    props.getBoolean(PROP_SHOW_PARTIAL_RESULTS, 
                             PROP_SHOW_PARTIAL_RESULTS_DEFAULT);
	showPartialActualResults =
	    props.getBoolean(PROP_SHOW_PARTIAL_ACTUAL_RESULTS,
                             PROP_SHOW_PARTIAL_ACTUAL_RESULTS_DEFAULT);
	showErrorToken =
	    props.getBoolean(PROP_SHOW_ERROR_TOKEN,
                             PROP_SHOW_ERROR_TOKEN_DEFAULT);
	showBestToken =
	    props.getBoolean(PROP_SHOW_BEST_TOKEN,
                             PROP_SHOW_BEST_TOKEN_DEFAULT);
	showActualToken =
	    props.getBoolean(PROP_SHOW_ACTUAL_TOKEN,
                             PROP_SHOW_ACTUAL_TOKEN_DEFAULT);
	showDetailedStatistics =
	    props.getBoolean(PROP_SHOW_DETAILED_STATISTICS,
                             PROP_SHOW_DETAILED_STATISTICS_DEFAULT);
	showHypothesisScore =
	    props.getBoolean(PROP_SHOW_HYPOTHESIS_SCORE,
                             PROP_SHOW_HYPOTHESIS_SCORE_DEFAULT);

        recognizer = null;        // first initialize it to null
        aligner = null;

        if (initialize) {
            initialize();
        }
    }


    /**
     * Fully loads this Decoder, effectively creating the
     * Recognizer and NISTAlign. This would mean loading
     * all the components (e.g., Frontend, AcousticModel, SentenceHMM, etc.)
     * of this Decoder. This method does nothing if this Decoder has
     * already been initialized.
     */
    public void initialize() throws IOException {
        if (recognizer == null) {
            beamFinder = new BeamFinder(context);
            recognizer = new Recognizer(context);

            recognizer.addResultListener(new ResultListener() {
                    public void newResult(Result result) {
                        if (result != null) {
                            if (result.isFinal()) {
                                getDecoderTimer().stop();
                            }
                            beamFinder.process(result);
                            if (showPartialResults) {
                                showPartialResult(result);
                            }
                            
                            if (showPartialActualResults) {
                                showPartialActualResults(result);
                            }
                        }
                    }
                });

            recognizer.getFrontEnd().addSignalListener(new SignalListener() {
                    public void signalOccurred(Signal signal) {
                        if (signal instanceof DataStartSignal) {
                            getDecoderTimer().start(signal.getTime());
                        }
                    }
                });

            if (beamFinder.isEnabled()) {
                recognizer.setFeatureBlockSize(1);
            }
        }
        if (aligner == null) {
            aligner = new NISTAlign();
        }
    }


    /**
     * Returns true if this Decoder has been loaded, that is,
     * if the components of this Decoder has been initialized.
     *
     * @return true if this Decoder has been initialized, false otherwise
     */
    public boolean isInitialized() {
        return (recognizer != null);
    }


    /**
     * Decodes an utterance.
     *
     * @return the decoded Result
     */
    public Result decode() {
	Result result = recognizer.recognize();
        return result;
    }


    /**
     * Decodes an utterance.
     *
     * @param ref the reference input
     *
     * @return the decoded Result
     */
    public Result decode(String ref) {
        currentReferenceText = ref;
        Result result = recognizer.recognize();
        if (result != null) {
            showFinalResult(result);
        }
        return result;
    }


    /**
     * Returns the set of batch results
     *
     * @return a batch result representing the set of runs for this
     * batch decoder.
     */
    public BatchResults getBatchResults() {
        return new BatchResults(aligner.getTotalWords(),
                                aligner.getTotalSentences(),
                                aligner.getTotalSubstitutions(),
                                aligner.getTotalInsertions(),
                                aligner.getTotalDeletions(),
                                aligner.getTotalSentencesWithErrors());
    }


    /**
     * Returns the Recognizer.
     *
     * @return the Recognizer
     */
    public Recognizer getRecognizer() {
        return recognizer;
    }


    /**
     * Returns the context.
     *
     * @return the context
     */
    public String getContext() {
        return context;
    }


    /**
     * Returns the SphinxProperties of this Decoder.
     *
     * @return the SphinxProperties
     */
    public SphinxProperties getSphinxProperties() {
	return SphinxProperties.getSphinxProperties(getContext());
    }


    /**
     * Forced alignment of an utterance.
     *
     * @param ref the reference input
     *
     * @return the decoded Result
     * @throws IOException if recognizer has not been initialized
     */
    public Result align(String ref) throws IOException {
	Result result;
        currentReferenceText = ref;
	recognizer.forcedAligner(context, ref);
	result = recognizer.recognize();
	showFinalResult(result);
        return result;
    }

    /**
     * Returns the decoding timer.
     *
     * @return the decoding timer
     */
    public Timer getDecoderTimer() {
        return Timer.getTimer(context, "Decode");
    }

    /**
     * Sets the reference text, i.e., the string that is the correct
     * recognition answer.
     *
     * @param reference the reference text
     */
    public void setReferenceText(String reference) {
        this.currentReferenceText = reference;
    }

    /**
     * Shows the given partial Result.
     *
     * @param result the partial Result to show
     */
    protected void showPartialResult(Result result) {
        String resultLine = null;

        if (result.isFinal()) {
            resultLine = "FINAL   ";
        } else {
            resultLine = "partial ";
        }

        System.out.println
            (resultLine + "Result: " + result.toString() + "      " +
             "Active branches: " + result.getActiveTokens().size());
    }

    /**
     * Shows the given partial Result.
     *
     * @param result the partial Result to show
     */
    protected void showPartialActualResults(Result result) {
        System.out.println("========= Partial Actual Results "
                    + " for frame " + result.getFrameNumber() + 
                    " ===========================");
        System.out.println("  Reference string: ["
                    + currentReferenceText + "]");
        Token bestToken = result.getBestActiveToken();
        Token bestMatching =
            result.getBestActiveParitalMatchingToken(currentReferenceText);

        if (bestToken != null && bestMatching == bestToken) {
            System.out.println("  --- Best active/matching: " +
                        bestToken.getWordPath());
            bestToken.dumpTokenPath(true);
        } else {
            if (bestToken != null) {
                System.out.println("  --- Best active: " +
                        bestToken.getWordPath());
                bestToken.dumpTokenPath(true);
            }
            if (bestMatching != null) {
                System.out.println(" --- Best matching: " +
                        bestMatching.getWordPath());
                bestMatching.dumpTokenPath(true);
            } else {
                System.out.println(" --- NO MATCH");
            }
        }
    }


    /**
     * Shows the final result
     *
     * @param result the recognition result
     */
    public void showFinalResult(Result result) {
	boolean match = true;
	Token bestToken = result.getBestToken();
        
	if (currentReferenceText != null) {
            System.out.println();
            match = aligner.align(currentReferenceText,
                                  result.getBestResultNoFiller());
            aligner.printSentenceSummary();
            System.out.println("RAW:       " + result.toString());
            System.out.println();
            aligner.printTotalSummary();
	} else {
            System.out.println("FINAL Result: " 
                    + result.getBestResultNoFiller());
	}

	if (showBestToken || (!match && showErrorToken)) {
	    if (bestToken != null) {
		System.out.println("\n\n Token path for best result");
		bestToken.dumpTokenPath();
	    }
        }

        if (showHypothesisScore) {
            if (bestToken == null) {
                System.out.print("   HypScore: NONE");
            } else {
                System.out.print("   HypScore: "
                            + result.getBestToken().getScore());
            }

            if (!match) {
                Token matchingToken = result.findToken(currentReferenceText);
                if (matchingToken != null) {
                    System.out.print(
                            "  ActScore: " + matchingToken.getScore());
                    if (showActualToken) {
                        System.out.println("\n\n Token path for actual result");
                        matchingToken.dumpTokenPath();
                    }
                } else {
                    System.out.print("  ActScore: NONE");
                }
            }

            System.out.println();
        }

        calculateTimes(result);
	showAudioUsage();
	showMemoryUsage();
        beamFinder.showLatestResult();
        if (showDetailedStatistics) {
            StatisticsVariable.dumpAll(context);
        }
        System.out.println();
    }

    /**
     * Calculate the processing and audio time of the current result.
     *
     * @param result the Result to calculate times on
     */
    protected void calculateTimes(Result result) {
        processingTime = getDecoderTimer().getCurTime() / 1000.f;
        audioTime = getAudioTime(result);
	cumulativeProcessingTime += processingTime;
	cumulativeAudioTime += audioTime;
    }

    /**
     * Sets whether this decoder should print the partial Results
     * on System.out. The default is FALSE.
     *
     * @param show true to print partial Result, false to not print
     */
    public void setShowPartialResults(boolean show) {
        showPartialResults = show;
    }


    /**
     * Returns the NISTAlign of this Decoder.
     *
     * @return the NISTAlign
     */
    public NISTAlign getNISTAlign() {
        return aligner;
    }


    /**
     * Shows decoder summary information
     */
    public void showSummary() {
	System.out.println("# ------------- Summary statistics ----------- ");
	aligner.printTotalSummary();
	showAudioSummary();
	showMemoryUsage();
        beamFinder.showSummary();
	StatisticsVariable.dumpAll(context);

	System.out.println("# ------------- Properties ----------- ");
	props.list(System.out);
    }

    /**
     * Shows the audio usage data
     */
    protected void showAudioUsage() {
        System.out.print("   This  Time Audio: " +
                         timeFormat.format(audioTime) + "s");
        System.out.print("  Proc: " +
                         timeFormat.format(processingTime) + "s");
        System.out.println("  Speed: " +
                           timeFormat.format(getSpeed()) +
                           " X real time");
	showAudioSummary();
    }

    /**
     * Shows the audio summary data
     */
    protected void showAudioSummary() {
        System.out.print("   Total Time Audio: " +
                         timeFormat.format(cumulativeAudioTime) + "s");
        System.out.print("  Proc: " +
		timeFormat.format(cumulativeProcessingTime) + "s");
        System.out.println("  Speed: " +
                           timeFormat.format(getCumulativeSpeed())
                           + " X real time");
    }

    /**
     * Shows the size of the heap used
     */
    protected void showMemoryUsage() {
	numMemoryStats++;
        float totalMem = Runtime.getRuntime().totalMemory() / (1024.0f
		* 1024.0f);
	float freeMem = Runtime.getRuntime().freeMemory() / (1024.0f
		* 1024.0f);
	float usedMem = totalMem - freeMem;
	
	if (usedMem > maxMemoryUsed) {
	    maxMemoryUsed = usedMem;
	}

	avgMemoryUsed = 
	    ((avgMemoryUsed * (numMemoryStats - 1)) + usedMem)/numMemoryStats;

	System.out.println
	    ("   Mem  Total: " + memFormat.format(totalMem) + "  " +
	     "Free: " + memFormat.format(freeMem));
	System.out.println
	    ("   Used: This: " + memFormat.format(usedMem) + "  " +
	     "Avg: " + memFormat.format(avgMemoryUsed) + "  " +
	     "Max: " + memFormat.format(maxMemoryUsed));
    }

    /**
     * Returns the audio time for the result
     *
     * @param result the result
     */
    public float getAudioTime(Result result) {
        return DataUtil.getAudioTime
            (result.getFrameNumber(),
             SphinxProperties.getSphinxProperties(context));
    }

    /**
     * Resets the speed statistics of this decoder.
     */
    public void resetSpeed() {
        audioTime = 0.0f;
        processingTime = 0.0f;
        cumulativeAudioTime = 0.0f;
        cumulativeProcessingTime = 0.0f;
    }

    /**
     * Returns the speed of the last decoding as a fraction of real time.
     *
     * @return the speed of the last decoding
     */
    public float getSpeed() {
        if (processingTime == 0 || audioTime == 0) {
            return 0;
        } else {
            return (processingTime / audioTime);
        }
    }

    /**
     * Returns the cumulative speed of this decoder as a
     * fraction of real time.
     *
     * @return the cumulative speed of this decoder
     */
    public float getCumulativeSpeed() {
        if (cumulativeProcessingTime == 0 || cumulativeAudioTime == 0) {
            return 0;
        } else {
            return (cumulativeProcessingTime / cumulativeAudioTime);
        }
    }
}



