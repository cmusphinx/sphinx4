/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.knowledge.acoustic;
import edu.cmu.sphinx.util.Utilities;
import java.io.Serializable;

/**
 * Represents a unit of speech. Units may represent phones, words or
 * any other suitable unit
 */
public class  Unit implements Serializable {
    /**
     * The silence unit
     */
    public static String SILENCE_NAME = "SIL";
    public static Unit SILENCE = new Unit(SILENCE_NAME, true);


    private String name;
    private boolean filler = false;
    private boolean silence = false;
    private Context context = null;

    private volatile String key = null;
    private static int objectCount; 	 // for tracking object counts

   /**
    * Constructs a unit with an empty context
    *
    * @param name the name of the unit
    * @param filler <code>true</code> if the unit is a filler unit
    */
    public Unit(String name, boolean filler) {
	this(name, filler, Context.EMPTY_CONTEXT);
    }

   /**
    * Constructs a unit with a context. Unit is assumed to not be a
    * filler unit
    *
    * @param name the name of the unit
    * @param context the context for this unit
    */
    public Unit(String name, Context context) {
	this(name, false, context);
    }

   /**
    * Constructs a context dependent  unit
    *
    * @param name the name of the unit
    * @param filler <code>true</code> if the unit is a filler unit
    * @param context the context for this unit
    */
    public Unit(String name, boolean filler, Context context) {
	this.name = name;
	this.filler = filler;
	this.context = context;
	if (name.equals(SILENCE_NAME)) {
	    silence = true;
	}
	Utilities.objectTracker("Unit", objectCount++);
    }

    /**
     * Gets the name for this unit
     *
     * @return the name for this unit
     */
    public String getName() {
	return name;
    }

    /**
     * Determines if this unit is context dependent
     *
     * @return true if the unit is context dependent
     */
    public boolean isContextDependent() {
	return getContext() != Context.EMPTY_CONTEXT;
    }


    /**
     * Returns the context for this unit
     *
     * @return the context for this unit (or null if context
     * 	independent
     */
    public Context getContext() {
	return context;
    }

    /**
     * Determines if this unit is a filler unit
     *
     * @return <code>true</code> if the unit is a filler unit
     */
    public boolean isFiller() {
	return filler;
    }


    /**
     * Determines if this unit is the silence unit
     *
     * @return true if the unit is the silence unit
     */
    public boolean isSilence() {
	return silence;
    }

    /**
     * Checks to see of an object is equal to this unit
     * 
     * @param o the object to check
     *
     * @return true if the objects are equal
     */
    public boolean equals(Object o) {
	if (this == o) {
	    return true;
	} else if (o instanceof Unit) {
	    Unit otherUnit = (Unit) o;
	    return getKey().equals(otherUnit.getKey());
	} else {
	    return false;
	}
    }

    /**
     * calculates a hashCode for this unit. Since we defined an equals
     * for Unit, we must define a hashCode as well
     *
     * @return the hashcode for this object
     */
    public int hashCode() {
	return getKey().hashCode();
    }


    /**
     * Converts to a string
     *
     * @return string version 
     */
    public String toString() {
	if (key == null) {
	    key =  (filler ? "*" :"") + name + "[" + context + "]";
	}
	return key;
    }

    /**
     * gets the key for this unit
     */
    private String getKey() {
	return toString();
    }

     /**
      * Checks to see if the given unit with associated contexts
      * is a partial match for this unit.   Zero, One or both contexts
      * can be null. A null context matches any context
      *
      * @param name the name of the unit
      * @param context the  context to match against
      *
      * @return true if this unit matches the name and non-null context
      */
     boolean isPartialMatch(String name, Context context) {
	 return (getName().equals(name) && 
		 context.isPartialMatch(this.context));
     }


     /**
      * Creates and returns an empty context with the given
      * size. The context is padded with SIL filler
      *
      * @param size the size of the context
      *
      * @return the context
      */

     public static Unit[] getEmptyContext(int size) {
	 Unit[] context = new Unit[size];

	 for (int i = 0; i < context.length; i++) {
	     context[i] = Unit.SILENCE;
	 }
	 return context;
     }

    /**
     * Checks to see that there is 100% overlap in the given contexts
     *
     * @param a context to check for a match
     * @param b context to check for a match
     *
     * @return <code>true</code> if the contexts match
     */
    public static  boolean isContextMatch(Unit[] a, Unit[] b) {
	 if (a == null || b == null) {
	     return a == b;
	} else if (a.length != b.length) {
	     return false;
	 } else  {
	     for (int i = 0; i < a.length; i++) {
		 if (!a[i].getName().equals(b[i].getName())) {
		     return false;
		 }
	     }
	     return true;
	}
    }
}
