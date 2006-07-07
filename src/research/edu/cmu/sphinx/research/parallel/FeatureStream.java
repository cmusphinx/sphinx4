
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

package edu.cmu.sphinx.research.parallel;

import edu.cmu.sphinx.decoder.search.ActiveList;

import edu.cmu.sphinx.frontend.FrontEnd;

import edu.cmu.sphinx.linguist.acoustic.AcousticModel;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implements a feature stream used in parallel decoding.
 * A feature stream describes the type of features that are generated,
 * so it is represented by the {@link #PROP_FRONT_END front end}
 * that generates the features, and the {@link #PROP_ACOUSTIC_MODEL 
 * acoustic model} that should be used to decode those features.
 * <p>
 * Since the parallel decoder uses information from multiple feature
 * streams for decoding, the {@link #PROP_ETA eta value} of the 
 * feature stream represents the relative weight that is applied 
 * for the scores from each feature stream.
 * <p>
 * To allow stream-specific pruning, each feature stream maintains
 * its own {@link edu.cmu.sphinx.decoder.search.ActiveList active list},
 * which is where tokens of that particular stream should be stored.
 */
public class FeatureStream implements Configurable {

    /**
     * Property for the acoustic model of this feature stream.
     */
    public static final String PROP_ACOUSTIC_MODEL = "acousticModel";

    /**
     * Property for the front end of this feature stream.
     */
    public static final String PROP_FRONT_END = "frontEnd";

    /**
     * Property for the eta value of this feature stream.
     */
    public static final String PROP_ETA = "eta";

    /**
     * Default value of PROP_ETA.
     */
    public static final float PROP_ETA_DEFAULT = 1.0f;


    private String name;
    private AcousticModel model;
    private FrontEnd frontEnd;
    private float eta;
    private ActiveList activeList;


    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
        throws PropertyException {
        this.name = name;
        registry.register(PROP_ACOUSTIC_MODEL, PropertyType.COMPONENT);
        registry.register(PROP_FRONT_END, PropertyType.COMPONENT);
        registry.register(PROP_ETA, PropertyType.FLOAT);
    }


    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        model = (AcousticModel) ps.getComponent
            (PROP_ACOUSTIC_MODEL, AcousticModel.class);
        name = model.getName();
        frontEnd = (FrontEnd) ps.getComponent(PROP_FRONT_END, FrontEnd.class);
        eta = ps.getFloat(PROP_ETA, PROP_ETA_DEFAULT);
    }


    /**
     * Returns the name of this FeatureStream.
     *
     * @return the name of this FeatureStream
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the acoustic model of this feature stream.
     *
     * @return the acoustic model of this feature stream
     */
    public AcousticModel getAcousticModel() {
        return model;
    }

    /**
     * Returns the front end of this feature stream.
     *
     * @return the front end of this feature stream
     */
    public FrontEnd getFrontEnd() {
        return frontEnd;
    }

    /**
     * Returns the eta value of this FeatureStream.
     *
     * @return the eta value of this FeatureStream
     */
    public float getEta() {
        return eta;
    }

    /**
     * Returns the active list
     *
     * @return the active list
     */
    public ActiveList getActiveList() {
        return activeList;
    }

    /**
     * Frees the acoustic model.
     */
    public void freeAcousticModel() {
        model = null;
    }

    /**
     * Sets the eta value of this FeatureStream
     *
     * @param eta the eta value
     */
    public void setEta(float eta) {
        this.eta = eta;
    }

    /**
     * Sets the active list
     *
     * @param list the active list
     */
    public void setActiveList(ActiveList list) {
        this.activeList = list;
    }
}        
