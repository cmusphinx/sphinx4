package edu.cmu.sphinx.demo.jsapi.icecream;

import javax.speech.AudioException;
import javax.speech.Central;
import javax.speech.EngineException;
import javax.speech.EngineStateError;
import javax.speech.recognition.FinalRuleResult;
import javax.speech.recognition.Recognizer;
import javax.speech.recognition.RecognizerModeDesc;
import javax.speech.recognition.Result;
import javax.speech.recognition.ResultEvent;
import javax.speech.recognition.ResultListener;
import javax.speech.recognition.ResultToken;

import edu.cmu.sphinx.jsapi.SphinxEngineCentral;
import edu.cmu.sphinx.jsapi.SphinxRecognizerModeDesc;

/**
 * A MIDlet demonstrating the use of speech synthesis and recognition together.
 */
public class IceCreamDemo implements ResultListener {
    private Object lock = new Object();

    public void run() throws EngineException, AudioException, EngineStateError {
        Central.registerEngineCentral(
                SphinxEngineCentral.class.getCanonicalName());
        RecognizerModeDesc mode = new SphinxRecognizerModeDesc(
            "/edu/cmu/sphinx/demo/jsapi/icecream/sphinx4.config.xml");
        Recognizer recognizer = (Recognizer) Central.createRecognizer(mode);

        // Start up the recognizer.
        System.out.println("Allocating recognizer...");
        recognizer.allocate();

        // Create the grammar. It is enabled by default.
        // Add a listener for recognition results.
        recognizer.addResultListener(this);

        // Request focus and start listening.
        recognizer.requestFocus();
        recognizer.resume();
        System.out.println("What is your favorite icecream?");
    }

    public void waitForResult() throws InterruptedException {
        synchronized (lock) {
            lock.wait();
        }
    }

    /**
     * Called when the MIDlet is started or resumed.
     * <p>
     * Note that <code>startApp()</code> can be called several times if
     * <code>pauseApp()</code> has been called in between.
     */
    public static void main(String[] args) {
        try {
            IceCreamDemo demo = new IceCreamDemo();
            demo.run();
            demo.waitForResult();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays a finalized result.
     * 
     * @param result
     *                A result which was accepted by {@link #resultUpdate}.
     * @returns the spoken text
     */
    private String displayResult(Result result) {
        StringBuffer spoken = new StringBuffer();

        // Get the array of tokens that the recognizer thinks
        // the user most likely spoke.
        ResultToken[] best = result.getBestTokens();

        // Concatenate all of the result's tokens.
        for (int i = 0; i < best.length; i++) {
            if (i > 0)
                spoken.append(' ');
            spoken.append(best[i].getSpokenText());
        }

        // Append the spoken tokens to the user interface.
        return spoken.toString();
    }
    
    @Override
    public void audioReleased(ResultEvent event) {
        System.out.println(event);
    }
    @Override
    public void grammarFinalized(ResultEvent event) {
        System.out.println(event);
    }
    @Override
    public void resultAccepted(ResultEvent event) {
        System.out.println(event);
        synchronized (lock) {
            lock.notifyAll();
        }
        final Result result = (Result) event.getSource();
        displayResult(result);
    }
    
    @Override
    public void resultCreated(ResultEvent event) {
        System.out.println(event);
    }
    @Override
    public void resultRejected(ResultEvent event) {
        System.out.println(event);
        synchronized (lock) {
            lock.notifyAll();
        }
    }
    @Override
    public void resultUpdated(ResultEvent event) {
        switch (event.getId()) {
        case ResultEvent.RESULT_ACCEPTED:
            FinalRuleResult result = (FinalRuleResult) event.getSource();

            // Display the result.
            String spoken = this.displayResult(result);

            // Speak whatever the user said.
            System.out.println(spoken);
            break;
        }
    }

    @Override
    public void trainingInfoReleased(ResultEvent event) {
        System.out.println(event);
    }
}
