/*
 * 
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
package edu.cmu.sphinx.instrumentation;

import java.text.DecimalFormat;

import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.SignalListener;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.RecognizerState;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.ResultListener;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.Resetable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * Monitors a recognizer for speed
 */
public class SpeedTracker
        implements
            Configurable,
            ResultListener,
            Resetable,
            StateListener,
            SignalListener {
    /**
     * A Sphinx property that defines which recognizer to monitor
     */
    public final static String PROP_RECOGNIZER = "recognizer";
    /**
     * A Sphinx property that defines which frontend to monitor
     */
    public final static String PROP_FRONTEND = "frontend";
    /**
     * A sphinx property that define whether summary accuracy information is
     * displayed
     */
    public final static String PROP_SHOW_SUMMARY = "showSummary";
    /**
     * The default setting of PROP_SHOW_SUMMARY
     */
    public final static boolean PROP_SHOW_SUMMARY_DEFAULT = true;
    /**
     * A sphinx property that define whether detailed accuracy information is
     * displayed
     */
    public final static String PROP_SHOW_DETAILS = "showDetails";
    /**
     * The default setting of PROP_SHOW_DETAILS
     */
    public final static boolean PROP_SHOW_DETAILS_DEFAULT = true;
    
    /**
     * A sphinx property that define whether detailed response information is
     * displayed
     */
    public final static String PROP_SHOW_RESPONSE_TIME = "showResponseTime";
    /**
     * The default setting of PROP_SHOW_RESPONSE
     */
    public final static boolean PROP_SHOW_RESPONSE_TIME_DEFAULT = false;
    
    /**
     * A sphinx property that define whether detailed timer information is
     * displayed
     */
    public final static String PROP_SHOW_TIMERS = "showTimers";
    /**
     * The default setting of PROP_SHOW_DETAILS
     */
    public final static boolean PROP_SHOW_TIMERS_DEFAULT = false;
    
    private static DecimalFormat timeFormat = new DecimalFormat("0.00");
    
    
    // ------------------------------
    // Configuration data
    // ------------------------------
    private String name;
    private Recognizer recognizer;
    private FrontEnd frontEnd;
    private boolean showSummary;
    private boolean showDetails;
    private boolean showTimers;
    private long startTime;
    private float audioTime;
    private float processingTime;
    private float totalAudioTime;
    private float totalProcessingTime;
    
    private boolean showResponseTime;
    private int numUtteranceStart;
    private long maxResponseTime = Long.MIN_VALUE;
    private long minResponseTime = Long.MAX_VALUE;
    private long totalResponseTime = 0L;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        this.name = name;
        registry.register(PROP_RECOGNIZER, PropertyType.COMPONENT);
        registry.register(PROP_FRONTEND, PropertyType.COMPONENT);
        registry.register(PROP_SHOW_SUMMARY, PropertyType.BOOLEAN);
        registry.register(PROP_SHOW_DETAILS, PropertyType.BOOLEAN);
        registry.register(PROP_SHOW_TIMERS, PropertyType.BOOLEAN);
        registry.register(PROP_SHOW_RESPONSE_TIME, PropertyType.BOOLEAN);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        Recognizer newRecognizer = (Recognizer) ps.getComponent(
                PROP_RECOGNIZER, Recognizer.class);
        if (recognizer == null) {
            recognizer = newRecognizer;
            recognizer.addResultListener(this);
            recognizer.addStateListener(this);
        } else if (recognizer != newRecognizer) {
            recognizer.removeResultListener(this);
            recognizer.removeStateListener(this);
            recognizer = newRecognizer;
            recognizer.addResultListener(this);
            recognizer.addStateListener(this);
        }


        FrontEnd newFrontEnd = (FrontEnd) ps.getComponent(PROP_FRONTEND,
                FrontEnd.class);
        if (frontEnd == null) {
            frontEnd = newFrontEnd;
            frontEnd.addSignalListener(this);
        } else if (frontEnd != newFrontEnd) {
            frontEnd.removeSignalListener(this);
            frontEnd = newFrontEnd;
            frontEnd.addSignalListener(this);
        }
        showSummary = ps.getBoolean(PROP_SHOW_SUMMARY,
                PROP_SHOW_SUMMARY_DEFAULT);
        showDetails = ps.getBoolean(PROP_SHOW_DETAILS,
                PROP_SHOW_DETAILS_DEFAULT);
        showResponseTime = ps.getBoolean(PROP_SHOW_RESPONSE_TIME,
                PROP_SHOW_RESPONSE_TIME_DEFAULT);
        showTimers = ps.getBoolean(PROP_SHOW_TIMERS, PROP_SHOW_TIMERS_DEFAULT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.result.ResultListener#newResult(edu.cmu.sphinx.result.Result)
     */
    public void newResult(Result result) {
        if (result.isFinal()) {
            processingTime = (getTime() - startTime) / 1000.0f;
            totalAudioTime += audioTime;
            totalProcessingTime += processingTime;
            if (showDetails) {
                showAudioUsage();
            }
        }
    }


    /**
     * Shows the audio usage data
     */
    protected void showAudioUsage() {
        System.out.print("   This  Time Audio: " + timeFormat.format(audioTime)
                + "s");
        System.out.print("  Proc: " + timeFormat.format(processingTime) + "s");
        System.out.println("  Speed: " + timeFormat.format(getSpeed())
                + " X real time");
        showAudioSummary();
    }

    /**
     * Shows the audio summary data
     */
    protected void showAudioSummary() {
        System.out.print("   Total Time Audio: "
                + timeFormat.format(totalAudioTime) + "s");
        System.out.print("  Proc: " + timeFormat.format(totalProcessingTime)
                + "s");
        System.out.println("  Speed: "
                + timeFormat.format(getCumulativeSpeed()) + " X real time");
        
        if (showResponseTime) {
            float avgResponseTime =
                (float) totalResponseTime / (numUtteranceStart * 1000);
            System.out.println
            ("   Response Time:  Avg: " + avgResponseTime + "s" +
             "  Max: " + ((float) maxResponseTime/1000) + 
             "s  Min: " + ((float) minResponseTime/1000) + "s");
            }
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
     * Resets the speed statistics
     */
    public void reset() {
        totalProcessingTime = 0;
        totalAudioTime = 0;
        numUtteranceStart = 0;
    }

    /**
     * Returns the cumulative speed of this decoder as a fraction of real time.
     * 
     * @return the cumulative speed of this decoder
     */
    public float getCumulativeSpeed() {
        if (totalProcessingTime == 0 || totalAudioTime == 0) {
            return 0;
        } else {
            return (totalProcessingTime / totalAudioTime);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.frontend.SignalListener#signalOccurred(edu.cmu.sphinx.frontend.Signal)
     */
    public void signalOccurred(Signal signal) {
        if (signal instanceof DataStartSignal) {
            startTime = getTime();
            long responseTime = (System.currentTimeMillis() - signal.getTime());
            totalResponseTime += responseTime;
            if (responseTime > maxResponseTime) {
                maxResponseTime = responseTime;
            }
            if (responseTime < minResponseTime) {
                minResponseTime = responseTime;
            }
            numUtteranceStart++;
        } else if (signal instanceof DataEndSignal) {
            DataEndSignal endSignal = (DataEndSignal) signal;
            audioTime = (float)(endSignal.getDuration()/1000f);
        }
    }

    /**
     * Returns the current time in milliseconds
     * 
     * @return the time in milliseconds.
     */
    private long getTime() {
        return System.currentTimeMillis();
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.recognizer.StateListener#statusChanged(edu.cmu.sphinx.recognizer.RecognizerState)
     */
    public void statusChanged(RecognizerState status) {
        if (status == RecognizerState.ALLOCATED) {
            if (showTimers) {
                Timer.dumpAll();
            }
        }
        
        if (status == RecognizerState.DEALLOCATING) {
            if (showTimers) {
                Timer.dumpAll();
            } 
        }
        
        if (status == RecognizerState.DEALLOCATED) {
            if (showSummary) {
                showAudioSummary();
            }

        }
    }
}
