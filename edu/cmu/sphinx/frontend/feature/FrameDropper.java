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

package edu.cmu.sphinx.frontend.feature;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.frontend.Signal;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;


/**
 * Drops certain feature frames, usually to speed up decoding.
 * For example, if you 'dropEveryNthFrame' is set to 2, it will drop
 * every other feature frame. If you set 'replaceNthWithPrevious' to 3,
 * then you replace with 3rd frame with the 2nd frame, the 6th frame
 * with the 5th frame, etc..
 */
public class FrameDropper extends BaseDataProcessor {
    
    private static final String PROP_PREFIX
        = "edu.cmu.sphinx.frontend.feature.FrameDropper.";

    /**
     * The SphinxProperty that specifies dropping one in every
     * Nth frame. If N=2, we drop every other frame. If N=3, we
     * drop every third frame, etc..
     */
    public static final String PROP_DROP_EVERY_NTH_FRAME
        = PROP_PREFIX + "dropEveryNthFrame";
    
    /**
     * The default value of PROP_DROP_EVERY_NTH_FRAME.
     */
    public static final int PROP_DROP_EVERY_NTH_FRAME_DEFAULT = -1;

    /**
     * The SphinxProperty that specifies whether to replace the
     * Nth frame with the previous frame.
     */
    public static final String PROP_REPLACE_NTH_WITH_PREVIOUS
        = PROP_PREFIX + "replaceNthWithPrevious";

    /**
     * The default value of PROP_REPLACE_NTH_WITH_PREVIOUS.
     */
    public static final boolean PROP_REPLACE_NTH_WITH_PREVIOUS_DEFAULT
        = false;


    private FloatData lastFeature;
    private boolean replaceNthWithPrevious;
    private int dropEveryNthFrame;
    private int id;   // first frame has ID "0", second "1", etc.


    /**
     * Initializes this FrameDropper.
     *
     * @param name        the name of this FrameDropper
     * @param frontEnd    the front end this FrameDropper belongs
     * @param props       the SphinxProperties to use
     * @param predecessor the DataProcessor from which to read features
     */
    public void initialize(String name, String frontEnd, 
                           SphinxProperties props, DataProcessor predecessor) {
        super.initialize(name, frontEnd, props, predecessor);
        this.id = -1;

        dropEveryNthFrame = props.getInt
            (getFullPropertyName(PROP_DROP_EVERY_NTH_FRAME),
             PROP_DROP_EVERY_NTH_FRAME_DEFAULT);

        assert (dropEveryNthFrame > 1);
        
        replaceNthWithPrevious = props.getBoolean
            (getFullPropertyName(PROP_REPLACE_NTH_WITH_PREVIOUS), 
             PROP_REPLACE_NTH_WITH_PREVIOUS_DEFAULT);
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
        Data feature = getPredecessor().getData();
        if (feature != null) {
            if (feature instanceof FloatData) {
                if ((id % dropEveryNthFrame) == (dropEveryNthFrame - 1)) {
                    // should drop the feature
                    if (replaceNthWithPrevious) {
                        // replace the feature
                        feature = new FloatData
                            (lastFeature.getValues(),
                             lastFeature.getCollectTime(),
                             lastFeature.getFirstSampleNumber());
                    } else {
                        // read the next feature
                        feature = getPredecessor().getData();
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
