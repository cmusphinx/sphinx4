/**
 * [[[copyright]]]
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
 * Timers can be organized into groups called contexts. Timers in the
 * same context can be dumped together.
 */
public class Timer {
    private final static DecimalFormat timeFormatter 
	= new DecimalFormat("###0.0000");
    private final static DecimalFormat percentFormatter 
	= new DecimalFormat("###0.00%");
    private static Map contextPool = new LinkedHashMap();

    private String name;
    private String context;
    private long startTime;
    private long curTime;
    private long count;
    private double sum;
    private long minTime = Long.MAX_VALUE;
    private long maxTime = 0L;
    private boolean notReliable; // if true, timing is not reliable

  /**
   * Retrieves (or creates) a timer with the given name in the given context.
   *
   * @param context the context for this timer. Timers are grouped by a
   * context. Timers can be manipulated as  group within these contexts. 
   *
   * @param timerName the name of the particular timer to retrieve. If 
   *  the timer does not already exist, it will be created
   *
   * @return the timer.
   */
    public static Timer getTimer(String context, String timerName) {
	Map timerPool = (Map) contextPool.get(context);
	Timer timer = null;
	if (timerPool == null) {
	    timerPool = new LinkedHashMap();
	    contextPool.put(context, timerPool);
	}
	timer = (Timer) timerPool.get(timerName);
	if (timer == null) {
	    timer = new Timer(timerName, context);
	    timerPool.put(timerName, timer);
	}
	return timer;
    }

    /**
     * Dump all timers of a particular context.
     *
     * @param context all timers of this context will be dumped
     */
    public static void dumpAll(String context) {
    	showTimesShortTitle(context);
	Map timerPool = (Map) contextPool.get(context);
	if (timerPool != null) {
	    for (Iterator i = timerPool.values().iterator(); i.hasNext(); ) {
		Timer timer = (Timer) i.next();
		timer.dump();
	    }
	}
    }

    /**
     * Resets all timers of a particular context.
     *
     * @param context all timers of this context will be reset
     */
    public static void resetAll(String context) {
	Map timerPool = (Map) contextPool.get(context);
	if (timerPool != null) {
	    for (Iterator i = timerPool.values().iterator(); i.hasNext(); ) {
		Timer timer = (Timer) i.next();
		timer.reset();
	    }
	}
    }

    /**
     * Dump all of the timers in all of the contexts
     */
    public static void dumpAll() {
	for (Iterator i = contextPool.keySet().iterator(); i.hasNext(); ) {
	    String context = (String) i.next();
	    dumpAll(context);
	}
    }

    /**
     * Resets all of the timers in all of the contexts
     */
    public static void resetAll() {
	for (Iterator i = contextPool.keySet().iterator(); i.hasNext(); ) {
	    String context = (String) i.next();
	    resetAll(context);
	}
    }

    /**
     * Creates a timer.
     *
     * @param name the name of the timer
     * @param context the context that the timer is in.
     */
    private Timer(String name, String context) {
         this.name = name;
	 this.context = context;
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
     * Retrieves the context for the timer
     *
     * @return the context of the timer
     */
    public String getContext() {
	return context;
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
     * Starts the timer running.
     */
    public void start() {
	if (startTime != 0L) {
	    notReliable = true; // start called while timer already running
	// throw new IllegalStateException("timer stutter start " + name);
	}
	startTime = System.currentTimeMillis();
    }


    /**
     * Stops the timer.
     *
     * @param verbose if <code>true</code>, print out details from
     * 	this run; otherwise, don't print the details
     */
    public void stop(boolean verbose) {
	if (startTime == 0L) {
	    notReliable = true;		// stop called, but start never called
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
    }

    /**
     * Stops the timer.
     */
    public void stop() {
	stop(false);
    }

    /**
     * Dump the timer. Shows the timer details.
     */
    public void dump() {
	showTimesShort();
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
     * @param title shows the title and column headings for the time
     * 	display
     */
    private static void showTimesShortTitle(String title) {
	String titleBar =
             "# ----------------------------- " + title +
             "----------------------------------------------------------- ";
        System.out.println(Utilities.pad(titleBar, 78));
	System.out.print(Utilities.pad("# Name", 15) + " ");
	System.out.print(Utilities.pad("Count", 6));
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
	    System.out.print(Utilities.pad("" + count, 6));
	    System.out.print(fmtTime(curTime));
	    System.out.print(fmtTime(minTime));
	    System.out.print(fmtTime(maxTime));
	    System.out.print(fmtTime(avgTime));
	    System.out.print(fmtTime(sum / 1000.0));
	    System.out.println();
	}
    }


    /**
     * a simple set of tests for the Timer clas
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
	System.out.println("Dump empty set");
	Timer.dumpAll();

	System.out.println("Create timers");
	Timer launchTime = Timer.getTimer("navy", "launch");
	Timer cruiseTime = Timer.getTimer("navy", "cruise");
	Timer sinkTime = Timer.getTimer("navy", "sink");

	Timer markTime = Timer.getTimer("army", "mark");
	Timer marchTime = Timer.getTimer("army", "march");
	Timer restTime = Timer.getTimer("army", "rest");

	Timer aflaunchTime = Timer.getTimer("airforce", "launch");
	Timer flightTime = Timer.getTimer("airforce", "flight");
	Timer landTime = Timer.getTimer("airforce", "land");

	System.out.println("Dump navy");
	Timer.dumpAll("navy");
	System.out.println("Dump all");
	Timer.dumpAll();

	System.out.println("Navy time");

	launchTime.start();
	sleep(300L);
	launchTime.stop();

	for (int i = 0; i < 4; i++) {
	    cruiseTime.start();
	    sleep(500L);
	    cruiseTime.stop();
	}

	sinkTime.start();
	sleep(500L);
	sinkTime.stop();

	dumpAll("navy");
	System.out.println("airforce time");

	aflaunchTime.start();
	sleep(800L);
	aflaunchTime.stop();

	flightTime.start();
	sleep(700L);
	flightTime.stop();

	for (int i = 0; i < 4; i++) {
	    landTime.start();
	    sleep(400L);
	    landTime.stop();
	}

	System.out.println("dump airforce time");
	dumpAll("airforce");
	System.out.println("Dump all");
	Timer.dumpAll();

	System.out.println("Dump garbage");
	Timer.dumpAll("garbage");
    }

    /**
     * Sleep for a while, while silently ignoring interrupts
     *
     * @param delay the time to sleep in milliseconds
     */
    static void sleep(long delay) {
	try {
	    Thread.sleep(delay);
	} catch (InterruptedException ie) {
	}
    }
}

