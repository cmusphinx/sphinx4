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
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;


/**
 * A simple HelloDigits demo showing a simple speech application 
 * built using Sphinx-4. This application uses the Sphinx-4 endpointer,
 * which automatically segments incoming audio into utterances and silences.
 */
public class ZipRecognizer implements Runnable {
    private Microphone microphone;
    private Recognizer recognizer; 
    private List zipListeners = new ArrayList();
    private boolean done;
    private Object lock = new Object();
    private boolean recognizing = false;

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

    public void microphoneOn() {
        new Thread(this).start();
    }

    public void microphoneOff() {
        microphone.stopRecording();
    }

    public void startup() throws IOException {
        done = false;
        recognizer.allocate();
    }

    public void shutdown() {
        done = true;
        microphoneOff();
        recognizer.deallocate();
    }

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

    /**
     * looks up the digit for a word
     * 
     * @param word the digit word
     * @return digit the digit form of the word (or null)
     */
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

    private String lookupDigit(String word) {
        return (String) digitMap.get(word);
    }

   public synchronized void addZipListener(ZipListener zipListener) {
       zipListeners.add(zipListener);
   }

   public synchronized void removeZipListener(ZipListener zipListener) {
       zipListeners.remove(zipListener);
   }

   private synchronized void fireListeners(String zipcode) {
       for (Iterator i = zipListeners.iterator(); i.hasNext(); ) {
           ZipListener zl = (ZipListener) i.next();
           zl.notify(zipcode);
       }
   }
    /**
     * Main method for running the HelloDigits demo.
     */
    public static void main(String[] args) throws IOException {
        final ZipDatabase zipDB = new ZipDatabase();
        BufferedReader br = 
            new BufferedReader(new InputStreamReader(System.in));

        ZipRecognizer zipRecognizer = new ZipRecognizer();
        zipRecognizer.addZipListener(new ZipListener() {
                public void notify(String zip) {
                    System.out.println("Zipcode is " + zip);
                    ZipInfo zipInfo = zipDB.lookup(zip);
                    if (zipInfo == null) {
                        System.out.println("Can't find city at zipcode " + zip);
                    } else {
                        System.out.println("Zip  : " + zip);
                        System.out.println("City : " + zipInfo.getCity());
                        System.out.println("State: " + zipInfo.getState());
                    }
                }
            });
        System.out.println(" Starting ...");
        zipRecognizer.startup();
        System.out.println(" Ready ...");
        while (true) {
            System.out.print("hit return to speak (q to quit): ");

            if (br.readLine().equals("q")) {
                break;
            }
            zipRecognizer.microphoneOn();
        }
        System.out.println(" Stopping ...");
        zipRecognizer.shutdown();
        System.out.println(" Done");
    }
}

interface ZipListener {
    void notify(String zipcode);
}

