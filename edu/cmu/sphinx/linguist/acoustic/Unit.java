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

package edu.cmu.sphinx.linguist.acoustic;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a unit of speech. Units may represent phones, words or
 * any other suitable unit
 */
public class  Unit {
    /**
     * The name for the silence unit
     */
    public static String SILENCE_NAME = "SIL";

    /**
     * The silence unit
     */
    public static Unit SILENCE;

    private static Map ciMap;
    private static int nextID ;

    static {
    }

    private String name;
    private boolean filler = false;
    private boolean silence = false;
    private Context context = null;
    private int baseID;

    private volatile String key = null;

    /**
     * Clears out this unit.
     */
    public static void reset() {
        nextID = 1;
        ciMap = new HashMap();
        SILENCE = createCIUnit(SILENCE_NAME, true);
    }

    /**
     * Gets or creates a unit from the unit pool
     *
     * @param name the name of the unit
     * @param filler <code>true</code> if the unit is a filler unit
     * @param context the context for this unit
     *
     * @return the unit
     */
    public static Unit getUnit(String name, boolean filler, Context context) {
        Unit unit = null;
        if (Context.EMPTY_CONTEXT == context) {
            unit = (Unit) ciMap.get(name);
            if (unit == null) {
                unit = createCIUnit(name, filler);
            }
        } else {
            unit =  new Unit(name, filler, context);
        }
        return unit;
    }

    /**
     * Gets or creates a unit from the unit pool
     *
     * @param name the name of the unit
     * @param filler <code>true</code> if the unit is a filler unit
     *
     * @return the unit
     */
    public static Unit getUnit(String name, boolean filler) {
        return getUnit(name, filler, Context.EMPTY_CONTEXT);
    }

    /**
     * Gets a CI unit by name
     *
     * @param name the name of the unit
     *
     * @return the unit
     */
    /*
    public static Unit getCIUnit(String name) {
        return (Unit) ciMap.get(name);
    }
    */

    /**
     * Gets or creates a unit from the unit pool
     *
     * @param name the name of the unit
     *
     * @return the unit
     */
    public static Unit getUnit(String name) {
        return getUnit(name, false, Context.EMPTY_CONTEXT);
    }


    /**
     * creates a unit ci unit 
     *
     * @param name the name of the unit
     * @param filler <code>true</code> if the unit is a filler unit
     *
     * @return the unit
     */
    private static Unit createCIUnit(String name, boolean filler) {
        Unit unit = (Unit) ciMap.get(name);
        if (unit == null) {
            Unit u = new Unit(name, filler, Context.EMPTY_CONTEXT, nextID++);
            unit = u;
            ciMap.put(name, unit);
        }
        return unit;
    }

    /**
     * creates a cd unit 
     *
     * @param name the name of the unit
     * @param filler <code>true</code> if the unit is a filler unit
     * @param context the context for this unit
     *
     * @return the unit
     */
    private static Unit createCDUnit(String name, 
            boolean filler, Context context) {
        Unit u = new Unit(name, filler, context, getIDFromName(name));
        return u;
    }

    /**
     * Gets the CI id for a unit based on its name
     *
     * @param name the name of the unit
     * @return the id
     */
    private static int getIDFromName(String name) {
        return ((Unit) ciMap.get(name)).getBaseID();
    }




   /**
    * Constructs a context dependent  unit
    *
    * @param name the name of the unit
    * @param filler <code>true</code> if the unit is a filler unit
    * @param context the context for this unit
    * @param id the base id for the unit
    */
    private Unit(String name, boolean filler, Context context, int id) {
	this.name = name;
	this.filler = filler;
	this.context = context;
        this.baseID = id;
	if (name.equals(SILENCE_NAME)) {
	    silence = true;
	}
    }

   /**
    * Constructs a context dependent  unit
    *
    * @param name the name of the unit
    * @param filler <code>true</code> if the unit is a filler unit
    * @param context the context for this unit
    */
    private Unit(String name, boolean filler, Context context) {
        this(name, filler, context, -1);
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
        if (baseID == -1) {
            baseID = getBaseUnit().getBaseID();
            assert baseID != -1;
        }
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
        if (context == Context.EMPTY_CONTEXT) {
            return this;
        } else {        // BUG: this maybe too slow
            return getUnit(this.getName());
        }
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
