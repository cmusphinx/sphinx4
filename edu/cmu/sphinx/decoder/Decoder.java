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

import edu.cmu.sphinx.frontend.DataSource;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.Util;
import edu.cmu.sphinx.frontend.Utterance;
import edu.cmu.sphinx.search.Recognizer;
import edu.cmu.sphinx.search.Token;
import edu.cmu.sphinx.util.NISTAlign;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.ResultListener;

import java.io.IOException;
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
     * A SphinxProperty name for a boolean property that when set will
     * cause the decoder to show all the tokens associated with the
     * best result
     */
    public final static String PROP_SHOW_BEST_TOKEN =
	PROP_PREFIX + "showBestToken";

    /**
     * A SphinxProperty name for a boolean property that when set will
     * cause the decoder to show all the tokens associated with the
     * actual result (if present)
     */
    public final static String PROP_SHOW_ACTUAL_TOKEN =
	PROP_PREFIX + "showActualToken";


    /**
     * A SphinxProperty name for a boolean property that when set will
     * cause the decoder to show all the tokens associated with an
     * error result
     */
    public final static String PROP_SHOW_ERROR_TOKEN =
	PROP_PREFIX + "showErrorToken";
    /**
     * A SphinxProperty name for a boolean property that when set will
     * cause the decoder to show all intermediate best token results
     */
    public final static String PROP_SHOW_PARTIAL_RESULTS =
	PROP_PREFIX + "showPartialResults";

    /**
     * A SphinxProperty name for a boolean property, that when set to
     * true will cause the decoder to show intermediate token results
     * for the reference text
     */
    public final static String PROP_SHOW_PARTIAL_ACTUAL_RESULTS =
	PROP_PREFIX + "showPartialActualResults";

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

    private boolean showPartialResults = false;
    private boolean showBestToken = false;
    private boolean showErrorToken = false;
    private boolean showActualToken = false;
    private boolean showPartialActualResults;;

    private boolean findResult = true;

    private String currentReferenceText;


    /**
     * Constructs a live mode Decoder.
     *
     * @param context the context of this Decoder
     * @param dataSource the source of audio of this Decoder
     */
    public Decoder(String context, DataSource dataSource) throws
    IOException {
        this(context, dataSource, true);
    }


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
        this(context, null, false);
    }


    /**
     * Constructs a Decoder with the given context and DataSource,
     * specifying whether to initialize all the components
     * at construction time.
     * This is to avoid having several Decoders fully loaded but
     * only one is actually being used. "Fully loading" a Decoder
     * means creating the Recognizer object for it, thus creating
     * the FrontEnd, AcousticModel, SentenceHMM, etc..
     *
     * @param context the context of this Decoder
     * @param dataSource the source of audio of this Decoder
     * @param initialize indicate whether to fully load this Decoder
     */
    private Decoder(String context,
                    DataSource dataSource,
                    boolean initialize) throws IOException {

        this.context = context;
	props = SphinxProperties.getSphinxProperties(context);

	showPartialResults =
	    props.getBoolean(PROP_SHOW_PARTIAL_RESULTS, false);
	showPartialActualResults =
	    props.getBoolean(PROP_SHOW_PARTIAL_ACTUAL_RESULTS, false);
	showErrorToken =
	    props.getBoolean(PROP_SHOW_ERROR_TOKEN, false);
	showBestToken =
	    props.getBoolean(PROP_SHOW_BEST_TOKEN, false);
	showActualToken =
	    props.getBoolean(PROP_SHOW_ACTUAL_TOKEN, false);

        recognizer = null;        // first initialize it to null
        aligner = null;

        if (initialize) {
            initialize(dataSource);
        }
    }


    /**
     * Fully loads this Decoder, effectively creating the
     * Recognizer and NISTAlign. This would mean loading
     * all the components (e.g., Frontend, AcousticModel, SentenceHMM, etc.)
     * of this Decoder. This method does nothing if this Decoder has
     * already been initialized.
     *
     * @param dataSource the DataSource this Decoder should use
     */
    public void initialize(DataSource dataSource) throws IOException {
        if (recognizer == null) {
            recognizer = new Recognizer(context, dataSource);
            recognizer.addResultListener(new ResultListener() {
		public void newResult(Result result) {
		    if (showPartialResults) {
			showPartialResult(result);
		    }

                    if (showPartialActualResults) {
                        showPartialActualResults(result);
                    }
		}
	    });
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
     * @param timer the Timer to use
     * @param ref the reference input
     *
     * @return the decoded Result
     */
    public Result decode(String ref) {
        Result result;
        currentReferenceText = ref;

	Timer timer = Timer.getTimer(context, "Decode");
        timer.start();  // start the timer

	result = recognizer.recognize();

        timer.stop();  // stop the timer

	showFinalResult(result, timer);

        return result;
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
     * @param timer the Timer to use
     * @param ref the reference input
     *
     * @return the decoded Result
     * @throws IOException if recognizer has not been initialized
     */
    public Result align(String ref) throws IOException {
	Result result;
	Timer timer = Timer.getTimer(context, "Decode");
        currentReferenceText = ref;

        timer.start();  // start the timer

	recognizer.forcedAligner(context, ref);
	result = recognizer.recognize();

        timer.stop();  // stop the timer

	showFinalResult(result, timer);


        return result;
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
    protected void showFinalResult(Result result, Timer timer) {
	boolean match = true;
	Token bestToken = result.getBestToken();

	if (currentReferenceText != null) {
	    match = aligner.align(currentReferenceText, result.toString());
            aligner.printSentenceSummary();
            aligner.printTotalSummary();
	} else {
            System.out.println("FINAL Result: " + result.toString());
	}

	if (showBestToken || (!match && showErrorToken)) {
	    if (bestToken != null) {
		System.out.println("\n\n Token path for best result");
		bestToken.dumpTokenPath();
	    }
        }


	if (bestToken == null) {
	    System.out.print("   HypScore: NONE");
	} else {
	    System.out.print("   HypScore: "
			+ result.getBestToken().getScore());
	}
	if (!match) {
	    Token  matchingToken = result.findToken(currentReferenceText);
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

        processingTime = timer.getCurTime() / 1000.f;
        audioTime = getAudioTime(result);
	cumulativeProcessingTime += processingTime;
	cumulativeAudioTime += audioTime;

	showAudioUsage();
	showMemoryUsage();
	StatisticsVariable.dumpAll(context);
        System.out.println("--------------");
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
	StatisticsVariable.dumpAll(context);

	System.out.println("# ------------- Properties ----------- ");
	props.list(System.out);
    }

    /**
     * Shows the audio usage data
     *
     * @param audioTime current audio time
     * @param processsingTime current processing time
     */
    private void showAudioUsage() {
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
    private void showAudioSummary() {
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
    private void showMemoryUsage() {
	float totalMem = Runtime.getRuntime().totalMemory() / (1024.0f
		* 1024.0f);
	float freeMem = Runtime.getRuntime().freeMemory() / (1024.0f
		* 1024.0f);

	float usedMem = totalMem - freeMem;

	System.out.println(
	    "   Mem Total: " + memFormat.format(totalMem) + " " +
	    "Free: " + memFormat.format(freeMem) + " " +
	    "Used: " + memFormat.format(usedMem));
    }

    /**
     * Returns the audio time for the result
     *
     * @param result the result
     */
    public float getAudioTime(Result result) {
        Utterance utterance = result.getUtterance();
        if (utterance != null) {
            return utterance.getAudioTime();
        } else {
            return Util.getAudioTime
                (result.getFrameNumber(),
                 SphinxProperties.getSphinxProperties(context));
        }
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



