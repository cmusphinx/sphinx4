package edu.cmu.sphinx.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

/**
 * Keeps references to a list of timers which can be referenced by a key-pair consisting of an owner and a timer name.
 *
 * @author Holger Brandl
 */
public class TimerPool {

    private static final Map<Object, List<Timer>> weakRefTimerPool = new WeakHashMap<Object, List<Timer>>();


    // disable the constructor
    private TimerPool() {

    }


    /**
     * Retrieves (or creates) a timer with the given name
     *
     * @param owner owner of the timer
     * @param timerName the name of the particular timer to retrieve. If the timer does not already exist, it will be
     *                  created  
     * @return the timer
     */
    public static synchronized Timer getTimer(Object owner, String timerName) {
        if (!weakRefTimerPool.containsKey(owner))
            weakRefTimerPool.put(owner, new ArrayList<Timer>());

        List<Timer> ownerTimers = weakRefTimerPool.get(owner);

        for (Timer timer : ownerTimers) {
            if (timer.getName().equals(timerName))
                return timer;
        }

        // there is no timer named 'timerName' yet, so create it
        Timer requestedTimer = new Timer(timerName);
        ownerTimers.add(requestedTimer);

        return requestedTimer;
    }


    /** @return the number of currently caches {@code Timer} instances. */
    public static int getNumCachedTimers() {
        int counter = 0;
        for (List<Timer> timers : weakRefTimerPool.values()) {
            counter += timers.size();
        }

        return counter;
    }


    /** 
     * Dump all timers 
     * @param logger the logger to use for dump
     */
    public static void dumpAll(Logger logger) {
        showTimesShortTitle(logger);

        for (List<Timer> timers : weakRefTimerPool.values()) {
            for (Timer timer : timers) {
                timer.dump(logger);
            }
        }
    }


    /** Shows the timing stats title. 
     * @param logger */
    private static void showTimesShortTitle(Logger logger) {
        String title = "Timers";
        String titleBar =
                "# ----------------------------- " + title +
                        "----------------------------------------------------------- ";
        logger.info(Utilities.pad(titleBar, 78));
        logger.info(Utilities.pad("# Name", 20) + ' '
         + Utilities.pad("Count", 8)
         + Utilities.pad("CurTime", 10)
         + Utilities.pad("MinTime", 10)
         + Utilities.pad("MaxTime", 10)
         + Utilities.pad("AvgTime", 10)
         + Utilities.pad("TotTime", 10));
    }


    /** Resets all timers */
    public static void resetAll() {
        for (List<Timer> timers : weakRefTimerPool.values()) {
            for (Timer timer : timers) {
                timer.reset();
            }
        }
    }
}
