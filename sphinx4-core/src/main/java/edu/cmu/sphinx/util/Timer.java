/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.util;

import java.text.DecimalFormat;
import java.util.logging.Logger;

/**
 * Keeps track of execution times. This class provides methods that can be used for timing processes. The process to be
 * timed should be bracketed by calls to timer.start() and timer.stop().  Repeated operations can be timed more than
 * once. The timer will report the minimum, maximum, average and last time executed for all start/stop pairs when the
 * timer.dump is called.
 * <p>
 * Timer instances can be obtained from a global cache implemented in {@code TimerPool}.
 *
 * @see TimerPool
 */
public class Timer {

    private final static DecimalFormat timeFormatter = new DecimalFormat("###0.0000");

    private final String name;

    private double sum;
    private long count;
    private long startTime;
    private long curTime;
    private long minTime = Long.MAX_VALUE;
    private long maxTime;
    private boolean notReliable; // if true, timing is not reliable


    /**
     * Creates a timer.
     *
     * @param name the name of the timer
     */
    Timer(String name) {
        assert name != null : "timers must have a name!";
        this.name = name;
        reset();
    }


    /**
     * Retrieves the name of the timer
     *
     * @return the name of the timer
     */
    public String getName() {
        return name;
    }


    /** Resets the timer as if it has never run before. */
    public void reset() {
        startTime = 0L;
        count = 0L;
        sum = 0L;
        minTime = Long.MAX_VALUE;
        maxTime = 0L;
        notReliable = false;
    }


    /**
     * Returns true if the timer has started.
     *
     * @return true if the timer has started; false otherwise
     */
    public boolean isStarted() {
        return (startTime > 0L);
    }


    /** Starts the timer running. */
    public void start() {
        if (startTime != 0L) {
            notReliable = true; // start called while timer already running
            System.out.println
                    (getName() + " timer.start() called without a stop()");
        }
        startTime = System.currentTimeMillis();
    }


    /**
     * Starts the timer at the given time.
     *
     * @param time the starting time
     */
    public void start(long time) {
        if (startTime != 0L) {
            notReliable = true; // start called while timer already running
            System.out.println
                    (getName() + " timer.start() called without a stop()");
        }
        if (time > System.currentTimeMillis()) {
            throw new IllegalStateException
                    ("Start time is later than current time");
        }
        startTime = time;
    }


    /**
     * Stops the timer.
     *
     * @return the duration since start in milliseconds
     */
    public long stop() {
        if (startTime == 0L) {
            notReliable = true;        // stop called, but start never called
            System.out.println
                    (getName() + " timer.stop() called without a start()");
        }
        curTime = System.currentTimeMillis() - startTime;
        startTime = 0L;
        if (curTime > maxTime) {
            maxTime = curTime;
        }
        if (curTime < minTime) {
            minTime = curTime;
        }
        count++;
        sum += curTime;
        return curTime;
    }


    /** 
     * Dump the timer. Shows the timer details. 
     * @param logger to use for dump
     */
    public void dump(Logger logger) {
        showTimesShort(logger);
    }


    /**
     * Gets the count of starts for this timer
     *
     * @return the count
     */
    public long getCount() {
        return count;
    }


    /**
     * Returns the latest time gathered
     *
     * @return the time in milliseconds
     */
    public long getCurTime() {
        return curTime;
    }


    /**
     * Gets the average time for this timer in milliseconds
     *
     * @return the average time
     */
    public double getAverageTime() {
        if (count == 0) {
            return 0.0;
        }
        return sum / count;
    }


    /**
     * Gets the min time for this timer in milliseconds
     *
     * @return the min time
     */
    public long getMinTime() {
        return minTime;
    }


    /**
     * Gets the max time for this timer in milliseconds
     *
     * @return the max time in milliseconds
     */
    public long getMaxTime() {
        return maxTime;
    }


    /**
     * Formats times into a standard format.
     *
     * @param time the time (in milliseconds) to be formatted
     * @return a string representation of the time.
     */
    private String fmtTime(long time) {
        return fmtTime(time / 1000.0);
    }


    /**
     * Formats times into a standard format.
     *
     * @param time the time (in seconds) to be formatted
     * @return a string representation of the time.
     */
    private String fmtTime(double time) {
        return Utilities.pad(timeFormatter.format(time) + 's', 10);
    }


    /** Shows brief timing statistics . 
     * @param logger */
    private void showTimesShort(Logger logger) {
        double avgTime = 0.0;

        if (count == 0) {
            return;
        }

        if (count > 0) {
            avgTime = sum / count / 1000.0;
        }

        if (notReliable) {
            logger.info(Utilities.pad(name, 20) + ' ' + "Not reliable.");
        } else {
            logger.info(Utilities.pad(name, 20) + ' ' 
                    + Utilities.pad(String.valueOf(count), 8)
                    + fmtTime(curTime)
                    + fmtTime(minTime)
                    + fmtTime(maxTime)
                    + fmtTime(avgTime)
                    + fmtTime(sum / 1000.0));
        }
    }
}

