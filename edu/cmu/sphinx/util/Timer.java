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
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Keeps track of execution times. This class provides methods that
 * can be used for timing processes. The process to be timed should be
 * bracketed by calls to timer.start() and timer.stop().  Repeated
 * operations can be timed more than once. The timer will report the
 * minimum, maximum, average and last time executed for all start/stop
 * pairs when the timer.dump is called.
 *
 */
public class Timer {
    private final static DecimalFormat timeFormatter 
	= new DecimalFormat("###0.0000");
    private final static DecimalFormat percentFormatter 
	= new DecimalFormat("###0.00%");
    private static Map timerPool = new LinkedHashMap();

    private String name;
    private double sum;
    private long count = 0L;
    private long startTime = 0L;
    private long curTime = 0L;
    private long minTime = Long.MAX_VALUE;
    private long maxTime = 0L;
    private boolean notReliable; // if true, timing is not reliable

  /**
   * Retrieves (or creates) a timer with the given name 
   *
   * @param timerName the name of the particular timer to retrieve. If 
   *  the timer does not already exist, it will be created
   *
   * @return the timer.
   */
    public static Timer getTimer(String timerName) {
	Timer timer = null;
	timer = (Timer) timerPool.get(timerName);
	if (timer == null) {
	    timer = new Timer(timerName);
	    timerPool.put(timerName, timer);
	}
	return timer;
    }

    /**
     * Dump all timers 
     *
     */
    public static void dumpAll() {
    	showTimesShortTitle();
        for (Iterator i = timerPool.values().iterator(); i.hasNext(); ) {
            Timer timer = (Timer) i.next();
            timer.dump();
        }
    }

    /**
     * Resets all timers 
     *
     */
    public static void resetAll() {
        for (Iterator i = timerPool.values().iterator(); i.hasNext(); ) {
            Timer timer = (Timer) i.next();
            timer.reset();
	}
    }

    /**
     * Creates a timer.
     *
     * @param name the name of the timer
     */
    private Timer(String name) {
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

    /**
     * Resets the timer as if it has never run before.
     */
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

    /**
     * Starts the timer running.
     */
    public void start() {
	if (startTime != 0L) {
	    notReliable = true; // start called while timer already running
            System.out.println
                (getName() + " timer.start() called without a stop()");
            // throw new IllegalStateException("timer stutter start " + name);
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
            // throw new IllegalStateException("timer stutter start " + name);
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
     * @param verbose if <code>true</code>, print out details from
     * 	this run; otherwise, don't print the details
     *
     * @return the duration since start in milliseconds
     */
    public long stop(boolean verbose) {
	if (startTime == 0L) {
	    notReliable = true;		// stop called, but start never called
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
	if (verbose) {
	    dump();
	}
	return curTime;
    }

    /**
     * Stops the timer.
     */
    public void stop() {
	stop(false);
    }

    /**
     * Starts a timer by name
     *
     * @param name the name of the timer to start
     */
    public static void start(String name) {
        Timer.getTimer(name).start();
    }

    /**
     * Stops a timer by name
     *
     * @param name the name of the timer to stop
     */
    public static void stop(String name) {
        Timer.getTimer(name).stop();
    }

    /**
     * Dump the timer. Shows the timer details.
     */
    public void dump() {
	showTimesShort();
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
	return sum/count;
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
     *
     * @return a string representation of the time.
     */
    private String fmtTime(long time) {
	return fmtTime(time/1000.0);
    }

    /**
     * Formats times into a standard format.
     *
     * @param time the time (in seconds) to be formatted
     *
     * @return a string representation of the time.
     */
    private String fmtTime(double time) {
	return Utilities.pad(timeFormatter.format(time) + "s", 10);
    }

    /**
     * Shows the timing stats title.
     *
     */
    private static void showTimesShortTitle() {
        String title = "Timers";
	String titleBar =
             "# ----------------------------- " + title +
             "----------------------------------------------------------- ";
        System.out.println(Utilities.pad(titleBar, 78));
	System.out.print(Utilities.pad("# Name", 15) + " ");
	System.out.print(Utilities.pad("Count", 8));
	System.out.print(Utilities.pad("CurTime", 10));
	System.out.print(Utilities.pad("MinTime", 10));
	System.out.print(Utilities.pad("MaxTime", 10));
	System.out.print(Utilities.pad("AvgTime", 10));
	System.out.print(Utilities.pad("TotTime", 10));
	System.out.println();
    }
    /**
     * Shows brief timing stats. 
     *
     */
    private void showTimesShort() {
	double avgTime  = 0.0;
	double totTime = sum / 1000.0;

	if (count == 0) {
	    return;
	}
	
	if (count > 0) {
	    avgTime = sum / count / 1000.0;
	}

	if (notReliable) {
	    System.out.print(Utilities.pad(name, 15) + " ");
	    System.out.println("Not reliable.");
	} else {
	    System.out.print(Utilities.pad(name, 15) + " ");
	    System.out.print(Utilities.pad("" + count, 8));
	    System.out.print(fmtTime(curTime));
	    System.out.print(fmtTime(minTime));
	    System.out.print(fmtTime(maxTime));
	    System.out.print(fmtTime(avgTime));
	    System.out.print(fmtTime(sum / 1000.0));
	    System.out.println();
	}
    }
}

