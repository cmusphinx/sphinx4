package edu.cmu.sphinx.util;

import java.util.WeakHashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;

/**
 *  Keeps reference to a list of timers.
 *
 * @author Holger Brandl
 */
public class TimerPool {
    
    private static WeakHashMap<String, Timer> weakRefTimerPool = new WeakHashMap<String, Timer>();

    private final static Map<String, Timer> timerPool = new LinkedHashMap<String, Timer>();


    /**
     * Retrieves (or creates) a timer with the given name
     *
     * @param timerName the name of the particular timer to retrieve. If the timer does not already exist, it will be
     *                  created
     * @return the timer.
     */
    public static Timer getTimer(String timerName) {
        Timer timer = null;
        
        timer = timerPool.get(timerName);
        if (timer == null) {
            timer = new Timer(timerName);
            timerPool.put(timerName, timer);
        }
        return timer;
    }


    /** Dump all timers */
    public static void dumpAll() {
        showTimesShortTitle();
        for (Iterator<Timer> i = timerPool.values().iterator(); i.hasNext();) {
            Timer timer = i.next();
            timer.dump();
        }
    }


    /** Shows the timing stats title. */
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


    /** Resets all timers */
    public static void resetAll() {
        for (Iterator<Timer> i = timerPool.values().iterator(); i.hasNext();) {
            Timer timer = i.next();
            timer.reset();
        }
    }
}
