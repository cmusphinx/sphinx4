/**
 * [[[copyright]]]
 */
package edu.cmu.sphinx.util;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.net.URL;
import java.io.IOException;

/**
 * Represents a named value. A StatisticsVariable may be used to 
 * track data in a fashion that will allow the data to be viewed 
 * or dumped at any
 * time.  Statistics are kept in a pool and are grouped in contexts.
 * Statistics can be dumped as a whole or by context.
 */
 public class StatisticsVariable {
     private static Map contextPool = new HashMap();

     /**
      * the value of this StatisticsVariable. It can be manipulated
      * directly by the application.
      */
     public  double value;		

     private String name;		// the name of this value
     private String contextName;	// the context for this statistic
     private boolean enabled;		// if true this var is enabled

     /**
      * Gets the StatisticsVariable with the given name from the given
      * context. If the statistic does not currently exist, it is
      * created. If the context does not currently exist, it is
      * created.
      *
      * @param contextName the name of the context
      * @param statName  the name of the StatisticsVariable
      *
      * @return the StatisticsVariable with the given name and context
      */
     static public StatisticsVariable getStatisticsVariable(
     				String contextName, String statName) {
         Map context = (Map) contextPool.get(contextName);
	 if (context == null) {
	    context = new LinkedHashMap();
	    contextPool.put(contextName, context);
	 }

	 StatisticsVariable stat = (StatisticsVariable) context.get(statName);
	 if (stat == null) {
	     stat = new StatisticsVariable(contextName, statName);
	     context.put(statName, stat);
	 }
	 return stat;
     }


     /**
      * Gets the StatisticsVariable with the given name for the given
      * instance and context. This is a convenience function.
      *
      * @param contextName the name of the context
      * @param instanceName the instance name of creator
      * @param statName  the name of the StatisticsVariable
      */
     static public StatisticsVariable getStatisticsVariable(
	     String contextName, String instanceName, String statName) {
	 return getStatisticsVariable(contextName, 
		 instanceName + "." + statName);
     }

     /**
      * Dump all of the StatisticsVariable in the given context
      *
      * @param contextName the context of interest
      */
     static public void dumpAll(String contextName) {
	Map context = (Map) contextPool.get(contextName);
	if (context != null) {
	     System.out.println(" ========= statistics for " +
		     contextName + "=======");
	    for (Iterator i = context.values().iterator(); i.hasNext(); ) {
		StatisticsVariable stats = (StatisticsVariable) i.next();
		stats.dump();
	    }
	}
     }

     /**
      * Dumps all of the known StatisticsVariables
      */
     static public void dumpAll() {
	for (Iterator i = contextPool.keySet().iterator(); i.hasNext(); ) {
	    String context = (String) i.next();
	    dumpAll(context);
	}
     }

     /**
      * Resets all of the StatisticsVariables in the given context
      *
      * @param contextName the context of interest
      */
     static public void resetAll(String contextName) {
	Map context = (Map) contextPool.get(contextName);
	if (context != null) {
	    for (Iterator i = context.values().iterator(); i.hasNext(); ) {
		StatisticsVariable stats = (StatisticsVariable) i.next();
		stats.reset();
	    }
	}
     }

     /**
      * Resets all of the known StatisticsVariables
      */
     static public void resetAll() {
	for (Iterator i = contextPool.keySet().iterator(); i.hasNext(); ) {
	    String context = (String) i.next();
	    resetAll(context);
	}
     }

     /**
      * Contructs a StatisticsVariable with the given name and context
      *
      * @param contextName the name of the context for this
      * 	StatisticsVariable
      * @param statName the name of this StatisticsVariable
      */
     private StatisticsVariable(String contextName, String statName) {
	 SphinxProperties props = 
	     SphinxProperties.getSphinxProperties(contextName);
	 this.enabled = props.getBoolean("statistics", statName, true);
         this.contextName = contextName;
	 this.name = statName;
	 this.value = 0.0;
     }

     /**
      * Retrieves the name of the context for this StatisticsVariable
      *
      * @return the name of the context
      */
     public String getContextName() {
     	return contextName;
     }

     /**
      * Retrieves the name of this StatisticsVariable
      *
      * @return the name of this StatisticsVariable
      */
     public String getName() {
         return name;
     }

     /**
      * Retrieves the value for this StatisticsVariable
      *
      * @return the current value for this StatisticsVariable
      */
     public double getValue() {
         return value;
     }

     /**
      * Sets the value for this StatisticsVariable
      * 
      * @param value the new value
      */
     public void setValue(double value) {
         this.value = value;
    }

     /**
      * Resets this StatisticsVariable. The value is set to zero.
      */
     public void reset() {
	 setValue(0.0);
     }

     /**
      * Dumps this StatisticsVariable. 
      */
     public void dump() {
	 if (isEnabled()) {
	     System.out.println(name + " " + value);
	 }
     }

     /**
      * Determines if this StatisticsVariable is enabled
      *
      * @return true if enabled
      */
     public boolean isEnabled() {
	 return enabled;
     }

     /**
      * Sets the enabled state of this StatisticsVariable
      *
      * @param enabled the new enabled state
      */
     public void setEnabled(boolean enabled) {
	 this.enabled = enabled;
     }

     /**
      * Some simple tests for the StatisticsVariable
      */
     public static void main(String[] args) {
	try {
	    SphinxProperties.initContext("main", new URL("file:./test.props"));
	 } catch (IOException ioe) {
	     System.out.println("Can't load main moon props");
	 }
	 StatisticsVariable loops =
	     StatisticsVariable.getStatisticsVariable("main", "loops");
	 StatisticsVariable sum =
	     StatisticsVariable.getStatisticsVariable("main", "sum");

	 StatisticsVariable foot =
	     StatisticsVariable.getStatisticsVariable("body", "foot");
	 StatisticsVariable leg =
	     StatisticsVariable.getStatisticsVariable("body", "leg");
	 StatisticsVariable finger =
	     StatisticsVariable.getStatisticsVariable("body", "finger");

	 foot.setValue(2);
	 leg.setValue(2);
	 finger.setValue(10);

	StatisticsVariable.dumpAll("main");
	StatisticsVariable.dumpAll("body");

	for (int i = 0; i < 1000; i++) {
	    loops.value ++;
	    sum.value += i;
	}

	StatisticsVariable.dumpAll("main");


	 StatisticsVariable loopsAlias =
	     StatisticsVariable.getStatisticsVariable("main", "loops");
	 StatisticsVariable sumAlias =
	     StatisticsVariable.getStatisticsVariable("main", "sum");

	for (int i = 0; i < 1000; i++) {
	    loopsAlias.value ++;
	    sumAlias.value += i;
	}

	StatisticsVariable.dumpAll();
	StatisticsVariable.resetAll();
	StatisticsVariable.dumpAll();
     }
}
