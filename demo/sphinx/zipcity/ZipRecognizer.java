/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package demo.sphinx.zipcity;



import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.RecognizerState;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Manages the speech recognition for zip city
 */
public class ZipRecognizer implements Runnable {
    private Microphone microphone;
    private Recognizer recognizer; 
    private List zipListeners = new ArrayList();

    /**
     * Creates the ZipRecognizer.
     *
     * @throws IOException if an error occurs while loading resources
     */
    public ZipRecognizer() throws IOException {
        try  {
            URL url = this.getClass().getResource("zipcity.config.xml");
            if (url == null) {
                throw new IOException("Can't find zipcity.config.xml");
            } 
            ConfigurationManager cm = new ConfigurationManager(url);
            recognizer = (Recognizer) cm.lookup("recognizer");
            microphone = (Microphone) cm.lookup("microphone");
        } catch (PropertyException e) {
            throw new IOException("Problem configuring ZipRecognizer " + e);
        } catch (InstantiationException e) {
            throw new IOException("Problem creating ZipRecognizer " + e);
        }
    }

    /**
     * Turns on the microphone and starts recognition
     */
    public void microphoneOn() {
        new Thread(this).start();
    }

    /**
     * Turns off the microphone, ending the current recognition in
     * progress
     */
    public void microphoneOff() {
        microphone.stopRecording();
    }

    /**
     * Allocates resources necessary for recognition.
     */
    public void startup() throws IOException {
        recognizer.allocate();
    }

    /**
     * Releases recognition resources
     */
    public void shutdown() {
        microphoneOff();
        if (recognizer.getState() == RecognizerState.ALLOCATED) {
            recognizer.deallocate();
        }
    }

    /**
     * Performs a single recognition
     */
    public void run() {
        microphone.clear();
        microphone.startRecording();
        Result result = recognizer.recognize();
        microphone.stopRecording();
        if (result != null) {
            String resultText = result.getBestFinalResultNoFiller();
            if (resultText.length() > 0) {
                String zip = convertResultToZip(resultText);
                fireListeners(zip);
            } else {
                fireListeners(null);
            }
        }
    }

    /**
     * Converts a string of the form "one two three four five" to
     * a string of the form "12345"
     *
     *  @param zipstring the zip string
     *  @return the zip string in a digits form
     */
    private String convertResultToZip(String zipstring) {
        StringBuffer sb = new StringBuffer();
        StringTokenizer st = new StringTokenizer(zipstring);
        while (st.hasMoreTokens()) {
            String word = st.nextToken();
            String digit = lookupDigit(word);
            if (digit == null) {
                throw new Error("Can't map " + word + " to a digit.");
            }
            sb.append(digit);
        }
        return sb.toString();
    }

    private static Map digitMap = new HashMap();

    static {
        digitMap.put("oh",          "0");
        digitMap.put("zero",        "0");
        digitMap.put("one",         "1");
        digitMap.put("two",         "2");
        digitMap.put("three",       "3");
        digitMap.put("four",        "4");
        digitMap.put("five",        "5");
        digitMap.put("six",         "6");
        digitMap.put("seven",       "7");
        digitMap.put("eight",       "8");
        digitMap.put("nine",        "9");
    }

    /**
     * looks up the digit for a word
     * 
     * @param word the digit word
     * @return digit the digit form of the word (or null)
     */
    private String lookupDigit(String word) {
        return (String) digitMap.get(word);
    }

    /**
     * Adds a listener that is called whenever a new zip code
     * is recognized
     *
     * @param zipListener the zip code listener
     */
   public synchronized void addZipListener(ZipListener zipListener) {
       zipListeners.add(zipListener);
   }

    /**
     * Removes a previously added zip listener
     *
     * @param zipListener the zip code listener
     */
   public synchronized void removeZipListener(ZipListener zipListener) {
       zipListeners.remove(zipListener);
   }

   /**
    * Invoke all added zip listeners
    *
    * @param zipcode the recognized zip code
    */
   private synchronized void fireListeners(String zipcode) {
       for (Iterator i = zipListeners.iterator(); i.hasNext(); ) {
           ZipListener zl = (ZipListener) i.next();
           zl.notify(zipcode);
       }
   }
}


/**
 * An interface for zip listeners
 */
interface ZipListener {
    /**
     * Invoked when a new zip code is recognized
     */
    void notify(String zipcode);
}

