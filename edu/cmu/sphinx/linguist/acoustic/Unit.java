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

package edu.cmu.sphinx.linguist.acoustic;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a unit of speech. Units may represent phones, words or
 * any other suitable unit
 */
public class  Unit {
    private String name;
    private boolean filler = false;
    private boolean silence = false;
    private Context context = null;
    private int baseID;
    private Unit baseUnit;

    private volatile String key = null;


   /**
    * Constructs a context dependent  unit.  Constructors are package
    * private, use the UnitManager to create and access units.
    *
    * @param name the name of the unit
    * @param filler <code>true</code> if the unit is a filler unit
    * @param id the base id for the unit
    */
    Unit(String name, boolean filler, int id) {
	this.name = name;
	this.filler = filler;
	this.context = Context.EMPTY_CONTEXT;
        this.baseID = id;
        this.baseUnit = this;
	if (name.equals(UnitManager.SILENCE_NAME)) {
	    silence = true;
	}
    }

   /**
    * Constructs a context dependent  unit.  Constructors are package
    * private, use the UnitManager to create and access units.
    *
    * @param baseUnit the base id for the unit
    * @param filler <code>true</code> if the unit is a filler unit
    * @param context the context for this unit
    */
    Unit(Unit baseUnit, boolean filler, Context context) {
        this.baseUnit = baseUnit;
	this.filler = filler;
	this.context = context;
	this.name = baseUnit.getName();
        this.baseID = baseUnit.getBaseID();
	if (name.equals(UnitManager.SILENCE_NAME)) {
	    silence = true;
	}
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
            if (context == Context.EMPTY_CONTEXT) {
                key =  (filler ? "*" :"") + name;
            } else {
                key =  (filler ? "*" :"") + name + "[" + context + "]";
            }
	}
	return key;
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
     * Gets the base ID for this unit
     *
     * @return the id
     */
    public int getBaseID() {
        return baseID;
    }

    /**
     * gets the key for this unit
     */
    private String getKey() {
	return toString();
    }

    /**
     * Gets the  base unit associated with this HMM
     *
     * @return the unit associated with this HMM
     */
    public Unit getBaseUnit() {
        return baseUnit;
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
     public boolean isPartialMatch(String name, Context context) {
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
	     context[i] = UnitManager.SILENCE;
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


