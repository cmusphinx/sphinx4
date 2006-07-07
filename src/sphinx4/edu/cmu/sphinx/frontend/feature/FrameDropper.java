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

package edu.cmu.sphinx.frontend.feature;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;


/**
 * Drops certain feature frames, usually to speed up decoding.
 * For example, if you 'dropEveryNthFrame' is set to 2, it will drop
 * every other feature frame. If you set 'replaceNthWithPrevious' to 3,
 * then you replace with 3rd frame with the 2nd frame, the 6th frame
 * with the 5th frame, etc..
 */
public class FrameDropper extends BaseDataProcessor {

    /**
     * The SphinxProperty that specifies dropping one in every
     * Nth frame. If N=2, we drop every other frame. If N=3, we
     * drop every third frame, etc..
     */
    public static final String PROP_DROP_EVERY_NTH_FRAME
        = "dropEveryNthFrame";
    
    /**
     * The default value of PROP_DROP_EVERY_NTH_FRAME.
     */
    public static final int PROP_DROP_EVERY_NTH_FRAME_DEFAULT = -1;

    /**
     * The SphinxProperty that specifies whether to replace the
     * Nth frame with the previous frame.
     */
    public static final String PROP_REPLACE_NTH_WITH_PREVIOUS
        = "replaceNthWithPrevious";

    /**
     * The default value of PROP_REPLACE_NTH_WITH_PREVIOUS.
     */
    public static final boolean PROP_REPLACE_NTH_WITH_PREVIOUS_DEFAULT
        = false;


    private Data lastFeature;
    private boolean replaceNthWithPrevious;
    private int dropEveryNthFrame;
    private int id;   // first frame has ID "0", second "1", etc.

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_DROP_EVERY_NTH_FRAME, PropertyType.INT);
        registry.register(PROP_REPLACE_NTH_WITH_PREVIOUS, PropertyType.BOOLEAN);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        dropEveryNthFrame = ps.getInt
            ( PROP_DROP_EVERY_NTH_FRAME,
             PROP_DROP_EVERY_NTH_FRAME_DEFAULT);

        if (dropEveryNthFrame <= 1) {
            throw new PropertyException(this, PROP_DROP_EVERY_NTH_FRAME, 
                "must be greater than one");
        }
        
        replaceNthWithPrevious = ps.getBoolean
            ( PROP_REPLACE_NTH_WITH_PREVIOUS, 
             PROP_REPLACE_NTH_WITH_PREVIOUS_DEFAULT);
    }

    /**
     * Initializes this FrameDropper.
     *
     */
    public void initialize() {
        super.initialize();
        this.id = -1;
    }

    /**
     * Returns the next Data object from this FrameDropper.
     * The Data objects belonging to a single Utterance should be
     * preceded by a DataStartSignal and ended by a DataEndSignal.
     *
     * @return the next available Data object, returns null if no
     *         Data object is available
     *
     * @throws DataProcessingException if a data processing error occurs
     */
    public Data getData() throws DataProcessingException {
        Data feature = readData();
        if (feature != null) {
            if (! (feature instanceof Signal)) {
                if ((id % dropEveryNthFrame) == (dropEveryNthFrame - 1)) {
                    // should drop the feature
                    if (replaceNthWithPrevious) {
                        // replace the feature
                        if (feature instanceof FloatData) {
                            FloatData floatLastFeature = (FloatData)
                                lastFeature;
                            feature = new FloatData
                                (floatLastFeature.getValues(),
                                 floatLastFeature.getSampleRate(),
                                 floatLastFeature.getCollectTime(),
                                 floatLastFeature.getFirstSampleNumber());
                        } else {
                            DoubleData doubleLastFeature  = (DoubleData)
                                lastFeature;
                            feature = new DoubleData
                                (doubleLastFeature.getValues(),
                                 doubleLastFeature.getSampleRate(),
                                 doubleLastFeature.getCollectTime(),
                                 doubleLastFeature.getFirstSampleNumber());
                        }
                    } else {
                        // read the next feature
                        feature = readData();
                    }
                }
            }
            if (feature != null) {
                if (feature instanceof DataEndSignal) {
                    id = -1;
                }                
                if (feature instanceof FloatData) {
                    lastFeature = (FloatData) feature;
                } else {
                    lastFeature = null;
                }
            } else {
                lastFeature = null;
            }
        }

        return feature;
    }

    /**
     * Read a Data object from the predecessor DataProcessor,
     * and increment the ID count appropriately.
     *
     * @return the read Data object
     */
    private Data readData() throws DataProcessingException {
        Data frame = getPredecessor().getData();
        if (frame != null) {
            id++;
        }
        return frame;
    }
}
