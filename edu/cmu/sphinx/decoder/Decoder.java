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

import edu.cmu.sphinx.frontend.AudioSource;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Util;
import edu.cmu.sphinx.frontend.Utterance;
import edu.cmu.sphinx.search.Recognizer;
import edu.cmu.sphinx.search.ResultListener;
import edu.cmu.sphinx.search.Result;
import edu.cmu.sphinx.search.Token;
import edu.cmu.sphinx.util.ResultAnalyzer;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.StatisticsVariable;
import edu.cmu.sphinx.util.Timer;

import java.io.IOException;
import java.text.DecimalFormat;


/**
 * A Decoder.
 */
public class Decoder {

    /**
     * Base property name for the decoder properties
     */

    private  final static String PROP_PREFIX = 
	"edu.cmu.sphinx.decoder.search.Decoder.";

    /**
     * A SphinxProperty name for a boolean property that when set will
     * cause the decoder to show all the tokens associated with the
     * best result
     */
    public final static String PROP_SHOW_BEST_TOKEN =
	PROP_PREFIX + "showBestToken";


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

    private static DecimalFormat memFormat = new DecimalFormat("0.00 Mb");
    private static DecimalFormat timeFormat = new DecimalFormat("0.00");

    private Recognizer recognizer;

    private ResultAnalyzer resultAnalyzer;// analyzes results

    private String context;
    private SphinxProperties props;	  // sphinx properties

    private float audioTime = 0.0f;
    private float processingTime = 0.0f;
    private float cumulativeAudioTime = 0.0f;
    private float cumulativeProcessingTime = 0.0f;

    private boolean showPartialResults = false;
    private boolean showBestToken = false;	
    private boolean showErrorToken = false;

    private boolean findResult = true;


    /**
     * Constructs a live mode Decoder.
     *
     * @param context the context of this Decoder
     * @param audioSource the source of audio of this Decoder
     */
    public Decoder(String context, AudioSource audioSource) throws
    IOException {
        this(context, audioSource, true);
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
     * Constructs a Decoder with the given context and AudioSource,
     * specifying whether to initialize all the components 
     * at construction time.
     * This is to avoid having several Decoders fully loaded but
     * only one is actually being used. "Fully loading" a Decoder
     * means creating the Recognizer object for it, thus creating
     * the FrontEnd, AcousticModel, SentenceHMM, etc..
     *
     * @param context the context of this Decoder
     * @param audioSource the source of audio of this Decoder
     * @param initialize indicate whether to fully load this Decoder 
     */
    private Decoder(String context,
                    AudioSource audioSource,
                    boolean initialize) throws IOException {
                        
        this.context = context;
	props = SphinxProperties.getSphinxProperties(context);

	showPartialResults =
	    props.getBoolean(PROP_SHOW_PARTIAL_RESULTS, false);
	showErrorToken =
	    props.getBoolean(PROP_SHOW_ERROR_TOKEN, false);
	showBestToken =
	    props.getBoolean(PROP_SHOW_BEST_TOKEN, false);
        
        recognizer = null;        // first initialize it to null
        resultAnalyzer = null;

        if (initialize) {
            initialize(audioSource);
        }
    }


    /**
     * Fully loads this Decoder, effectively creating the
     * Recognizer and ResultAnalyzer. This would mean loading
     * all the components (e.g., Frontend, AcousticModel, SentenceHMM, etc.)
     * of this Decoder. This method does nothing if this Decoder has
     * already been initialized.
     *
     * @param audioSource the AudioSource this Decoder should use
     */
    public void initialize(AudioSource audioSource) throws IOException {
        if (recognizer == null) {
            recognizer = new Recognizer(context, audioSource);
            recognizer.addResultListener(new ResultListener() {
		public void newResult(Result result) {
		    if (showPartialResults) {
			showPartialResult(result);
		    }
		}
	    });
        }
        if (resultAnalyzer == null) {
            resultAnalyzer = new ResultAnalyzer(true);
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

	Timer timer = Timer.getTimer(context, "Decode");
        timer.start();  // start the timer

	result = recognizer.recognize();

        timer.stop();  // stop the timer

	showFinalResult(ref, result, timer);
        
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

        timer.start();  // start the timer

	recognizer.forcedAligner(context, ref);
	result = recognizer.recognize();

        timer.stop();  // stop the timer

	showFinalResult(ref, result, timer);


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
     * Shows the final result
     *
     * @param ref the expected result or null
     * @param result the recognition result
     */
    protected void showFinalResult(String ref, Result result, Timer timer) {
	boolean match = true;

	if (ref != null) {
	    match = resultAnalyzer.analyze(ref, result.toString());
	} else {
            System.out.println("FINAL Result: " + result.toString());
	}

	if (showBestToken || (!match && showErrorToken)) {
	    if (result.getBestToken() != null) {
		result.getBestToken().dumpTokenPath();
	    }
    }

	System.out.print("   HypScore: " 
		    + result.getBestToken().getScore());
	if (!match) {
	    Token  matchingToken = result.findToken(ref);
	    if (matchingToken != null) {
		System.out.print(
			"  ActScore: " + matchingToken.getScore());
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
     * Returns the ResultAnalyzer of this Decoder.
     *
     * @return the ResultAnalyzer
     */
    public ResultAnalyzer getResultAnalyzer() {
        return resultAnalyzer;
    }


    /**
     * Shows decoder summary information
     */
    public void showSummary() {
	System.out.println("# ------------- Summary statistics ----------- ");
	resultAnalyzer.showResults();
	showAudioSummary();
	showMemoryUsage();
	StatisticsVariable.dumpAll(context);

        if (Boolean.getBoolean("showMisrecognitions")) {
            resultAnalyzer.showMisrecognitions();
        }
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



