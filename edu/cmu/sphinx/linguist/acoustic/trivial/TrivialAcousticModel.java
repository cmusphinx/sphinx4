/*
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.linguist.acoustic.trivial;

import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.HMMStateArc;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.util.SphinxProperties;

/**
 * Represents the generic interface to the Acoustic 
 * Model for sphinx4
 */
public class TrivialAcousticModel implements AcousticModel {

    private static String PREFIX = 
        "edu.cmu.sphinx.linguist.acoustic.trivial.TrivialAcousticModel";

    /**
     * Sphinx property that defines the left context size
     */
    public static String PROP_LEFT_CONTEXT_SIZE = PREFIX + ".leftContextSize";
    
    /**
     * The default value for PROP_LEFT_CONTEXT_SIZE
     */
    public static int PROP_LEFT_CONTEXT_SIZE_DEFAULT = 1;


    /**
     * Sphinx property that defines the right context size
     */
    public static String PROP_RIGHT_CONTEXT_SIZE = PREFIX + ".rightContextSize";
    
    /**
     * The default value for PROP_RIGHT_CONTEXT_SIZE
     */
    public static int PROP_RIGHT_CONTEXT_SIZE_DEFAULT = 1;


    private String name;
    private Map hmmMap = new HashMap();
    private int leftContextSize;
    private int rightContextSize;

    /**
     * Initializes this acoustic model
     *
     * @param name the name of this acoustic model
     * @param context the context for this acoustic model
     *
     * @throws IOException if the model could not be loaded
     *
     */
    public void initialize (String name, String context) throws IOException {
        this.name = name;
        
        // get acoustic model configuration data from the sphinx
        // properties

        SphinxProperties props = SphinxProperties.getSphinxProperties(context);
        this.leftContextSize = props.getInt(name,
                PROP_LEFT_CONTEXT_SIZE,
                PROP_LEFT_CONTEXT_SIZE_DEFAULT);
        this.rightContextSize = props.getInt(name,
                PROP_RIGHT_CONTEXT_SIZE,
                PROP_RIGHT_CONTEXT_SIZE_DEFAULT);


        // create HMMs for all of the units

        String[] unitNames = {
            "AX_one", "AY_five", "AY_nine", "EH_seven", "EY_eight", "E_seven",
            "F_five", "F_four", "II_three", "II_zero", "I_six", "K_six",
            "N_nine", "N_nine_2", "N_one", "N_seven", "OO_two", "OW_four",
            "OW_oh", "OW_zero", "R_four", "R_three", "R_zero", "S_seven",
            "S_six", "S_six_2", "TH_three", "T_eight", "T_two", "V_five",
            "V_seven", "W_one", "Z_zero", "AX_one", "SIL"
        };

        for (int i = 0; i < unitNames.length; i++) {
            createTrivialHMM(unitNames[i]);
        }
    }

    /**
     * Returns the name of this AcousticModel, or null if it has no name.
     *
     * @return the name of this AcousticModel, or null if it has no name
     */
    public String getName()  {
        return name;
    }

    /**
     * Returns the properties of this acoustic model.
     *
     * @return the properties of this acoustic model
     */
    public Properties getProperties() {
        return new Properties();
    }
    
    /**
     * Given a unit, returns the HMM that best matches the given unit.
     * If exactMatch is false and an exact match is not found, 
     * then different word positions
     * are used. If any of the contexts are non-silence filler units.
     * a silence filler unit is tried instead.
     *
     * @param unit 		the unit of interest
     * @param position 	the position of the unit of interest
     * @param exactMatch 	if true, only an exact match is
     * 			acceptable.
     *
     * @return 	the HMM that best matches, or null if no match
     * 		could be found.
     */
     public HMM lookupNearestHMM(Unit unit, HMMPosition position,
	     boolean exactMatch)  {
         HMM hmm = null;
         if (!exactMatch || position == HMMPosition.UNDEFINED) {
             unit = unit.getBaseUnit();
             hmm = (HMM) hmmMap.get(unit);
         }
         return hmm;
     }

     /**
      * Returns an iterator that can be used to iterate through all
      * the HMMs of the acoustic model
      *
      * @return an iterator that can be used to iterate through all
      * HMMs in the model. The iterator returns objects of type
      * <code>HMM</code>.
      */
     public Iterator getHMMIterator() {
         return hmmMap.values().iterator();
     }
     /**
      * Returns an iterator that can be used to iterate through all
      * the CI units in the acoustic model
      *
      * @return an iterator that can be used to iterate through all
      * CI units. The iterator returns objects of type
      * <code>Unit</code>
      */
     public Iterator getContextIndependentUnitIterator() {
         return hmmMap.keySet().iterator();
     }

     /**
      * Returns the size of the left context for context dependent
      * units
      *
      * @return the left context size
      */
     public int getLeftContextSize() {
         return leftContextSize;
     }

     /**
      * Returns the size of the right context for context dependent
      * units
      *
      * @return the left context size
      */
     public int getRightContextSize() {
         return rightContextSize;
     }

     /**
      * Creates a trivial HMM
      *
      * @param unitName the name of the unit
      */
    private void createTrivialHMM(String unitName) {
        // FIXME
        //Unit unit = Unit.getUnit(unitName);
        //HMM hmm = new TrivialHMM(unit, HMMPosition.UNDEFINED);
        //hmmMap.put(unit, hmm);
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.linguist.acoustic.AcousticModel#allocate()
     */
    public void allocate() throws IOException {
        
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.linguist.acoustic.AcousticModel#deallocate()
     */
    public void deallocate() {
        
    }
}


/**
 * Trivial HMM
 */
class TrivialHMM implements HMM {
    private final static int NUM_STATES = 4;
    Unit unit;
    HMMPosition position;
    HMMState[] hmmStates;
    private Unit baseUnit;

    /**
     * Creates a trivial hmm
     *
     * @param unit the unit for the hmm
     * @param position the position of the hmm
     */
    TrivialHMM(Unit unit, HMMPosition position) {
        this.unit = unit;
        this.position = position;
        hmmStates = new HMMState[NUM_STATES];
        // baseUnit = Unit.getUnit(unit.getName());
        baseUnit = unit.getBaseUnit();

        for (int i = 0; i < hmmStates.length; i++) {
            boolean finalState = i == hmmStates.length - 1;
            hmmStates[i] = new TrivialHMMState(this, i, finalState);
        }
    }

    /**
     * Gets the  unit associated with this HMM
     *
     * @return the unit associated with this HMM
     */
    public Unit getUnit() {
        return unit;
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
     * Retrieves the hmm state 
     *
     * @param which the state of interest
     */
    public HMMState getState(int which) {
        return hmmStates[which];
    }


    /**
     * Returns the order of the HMM
     *
     * @return the order of the HMM
     */
    public int getOrder() {
        return hmmStates.length;
    }


    /**
     * Retrieves the position of this HMM. 
     *
     * @return the position for this HMM
     */
    public HMMPosition getPosition() {
        return position;
    }

    /**
     * Gets the initial states (with probabilities) for this HMM
     *
     * @return the set of arcs that transition to the initial states
     * for this HMM
     */
    public HMMState getInitialState() {
        return hmmStates[0];
    }
}


/**
 * A trivial implementation of an HMMState
 */
class TrivialHMMState implements HMMState {
    private static HMMStateArc[] EMPTY_ARC = new HMMStateArc[0];
    private HMM hmm;
    private int which;
    private boolean isFinal;

    TrivialHMMState(HMM hmm, int which, boolean isFinal) {
        this.hmm = hmm;
        this.which = which;
        this.isFinal = isFinal;
    }

    /**
     * Gets the HMM associated with this state
     *
     * @return the HMM
     */
    public HMM getHMM() {
        return hmm;
    }

    /**
     * Gets the state 
     *
     * @return the state
     */
    public int getState() {
        return which;
    }


    /**
     * Gets the score for this HMM state
     *
     * @param feature the feature to be scored
     *
     * @return the acoustic score for this state.
     */
    public float getScore(Data feature) {
        return 0.0f;
    }

    /**
     * Determines if this HMMState is an emittting state
     *
     * @return true if the state is an emitting state
     */
    public boolean isEmitting() {
        return !isFinal;
    }


    /**
     * Retrieves the state of successor states for this state
     *
     * @return the set of successor state arcs
     */
    public HMMStateArc[] getSuccessors() {
        if (isFinal) {
            return EMPTY_ARC;
        } else {
            HMMStateArc[] arcs = new HMMStateArc[2];
            arcs[0] = new HMMStateArc(this, 0f);
            arcs[1] = new HMMStateArc(hmm.getState(which + 1), 0f);
            return arcs;
        }
    }


    /**
     * Determines if this state is an exit state of the HMM
     *
     * @return true if the state is an exit state
     */
    public boolean isExitState() {
        return isFinal;
    }
}
