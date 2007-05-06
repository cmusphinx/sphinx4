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

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Manages the set of units for a recognizer */
@SuppressWarnings({"UnnecessaryLocalVariable"})
public class UnitManager implements Configurable {

    /** The name for the silence unit */
    public final static String SILENCE_NAME = "SIL";
    private final static int SILENCE_ID = 1;

    /** The silence unit */
    public final static Unit SILENCE =
            new Unit(SILENCE_NAME, true, SILENCE_ID);

    private String name;
    private Map<String, Unit> ciMap;


    {
        ciMap = new HashMap<String, Unit>();
        ciMap.put(SILENCE_NAME, SILENCE);
    }


    private int nextID = SILENCE_ID + 1;
    private Logger logger;


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    public void newProperties(PropertySheet ps) throws PropertyException {
        logger = ps.getLogger();
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#getName()
    */
    public String getName() {
        return name;
    }


    /**
     * Gets or creates a unit from the unit pool
     *
     * @param name    the name of the unit
     * @param filler  <code>true</code> if the unit is a filler unit
     * @param context the context for this unit
     * @return the unit
     */
    public Unit getUnit(String name, boolean filler, Context context) {
        Unit unit = null;
        if (Context.EMPTY_CONTEXT == context) {
            unit = ciMap.get(name);
            if (unit == null) {
                unit = createCIUnit(name, filler);
            }
        } else {
            unit = createCDUnit(name, filler, context);
        }
        return unit;
    }


    /**
     * Gets or creates a unit from the unit pool
     *
     * @param name   the name of the unit
     * @param filler <code>true</code> if the unit is a filler unit
     * @return the unit
     */
    public Unit getUnit(String name, boolean filler) {
        return getUnit(name, filler, Context.EMPTY_CONTEXT);
    }


    /**
     * Gets or creates a unit from the unit pool
     *
     * @param name the name of the unit
     * @return the unit
     */
    public Unit getUnit(String name) {
        return getUnit(name, false, Context.EMPTY_CONTEXT);
    }


    /**
     * creates a unit ci unit
     *
     * @param name   the name of the unit
     * @param filler <code>true</code> if the unit is a filler unit
     * @return the unit
     */
    private Unit createCIUnit(String name, boolean filler) {
        Unit unit = ciMap.get(name);
        if (unit == null) {
            Unit u = new Unit(name, filler, nextID++);
            unit = u;
            ciMap.put(name, unit);
            if (logger.isLoggable(Level.INFO)) {
                logger.info("CI Unit: " + unit);
            }
        }
        return unit;
    }


    /**
     * creates a cd unit
     *
     * @param name    the name of the unit
     * @param filler  <code>true</code> if the unit is a filler unit
     * @param context the context for this unit
     * @return the unit
     */
    private Unit createCDUnit(String name,
                              boolean filler, Context context) {
        Unit baseUnit = lookupCIUnit(name);
        return new Unit(baseUnit, filler, context);
    }


    /**
     * Looks up the CI unit with the given name
     *
     * @param name the name of the CI unit
     * @return the CI unit
     */
    private Unit lookupCIUnit(String name) {
        return ciMap.get(name);
    }
}
