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

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;


/**
 * A FeatureSource produces Features.
 */
public class FrameDropper extends DataProcessor implements FeatureSource {

    /**
     * The SphinxProperty that specifies dropping one in every
     * Nth frame. If N=2, we drop every other frame. If N=3, we
     * drop every third frame, etc..
     */
    public static final String PROP_DROP_EVERY_NTH_FRAME
        = FrontEnd.PROP_PREFIX + "FrameDropper.dropEveryNthFrame";
    
    /**
     * The default value of PROP_DROP_EVERY_NTH_FRAME.
     */
    public static final int PROP_DROP_EVERY_NTH_FRAME_DEFAULT = -1;

    /**
     * The SphinxProperty that specifies whether to replace the
     * Nth frame with the previous frame.
     */
    public static final String PROP_REPLACE_NTH_WITH_PREVIOUS
        = FrontEnd.PROP_PREFIX + "FrameDropper.replaceNthWithPrevious";

    /**
     * The default value of PROP_REPLACE_NTH_WITH_PREVIOUS.
     */
    public static final boolean PROP_REPLACE_NTH_WITH_PREVIOUS_DEFAULT
        = false;


    private FeatureSource predecessor;
    private Feature lastFeature;
    private boolean replaceNthWithPrevious;
    private int dropEveryNthFrame;
    private int id;


    /**
     * Creates a default FrameDropper.
     *
     * @param name the name of this FrameDropper
     * @param context the context to use
     * @param props the SphinxProperties to use
     * @param predecessor the FeatureSource from which to read Features
     */
    public FrameDropper(String name, String context, 
                        SphinxProperties props,
                        FeatureSource predecessor) {
        super(name, context, props);
        this.predecessor = predecessor;
        this.id = 0;

        dropEveryNthFrame = props.getInt
            (PROP_DROP_EVERY_NTH_FRAME,
             PROP_DROP_EVERY_NTH_FRAME_DEFAULT);

        assert (dropEveryNthFrame > 1);
        
        replaceNthWithPrevious = props.getBoolean
            (PROP_REPLACE_NTH_WITH_PREVIOUS, 
             PROP_REPLACE_NTH_WITH_PREVIOUS_DEFAULT);
    }

    /**
     * Returns the next Feature object from this FrameDropper.
     * The Feature objects belonging to a single Utterance should be
     * preceded by a Feature object with the Signal.UTTERANCE_START, and
     * ended by a Feature object wtih the Signal.UTTERANCE_END.
     *
     * @return the next available Feature object, returns null if no
     *     Feature object is available
     *
     * @throws java.io.IOException
     */
    public Feature getFeature() throws IOException {
        Feature feature = predecessor.getFeature();
        if (feature != null) {
            if (feature.hasContent()) {
                if ((feature.getID() % dropEveryNthFrame) == 
                    (dropEveryNthFrame - 1)) {
                    if (replaceNthWithPrevious) {
                        feature = new Feature
                            (lastFeature.getFeatureData(), id++,
                             lastFeature.getCollectTime());
                    } else {
                        feature = predecessor.getFeature();
                        if (feature != null && feature.hasContent()) {
                            feature.setID(id++);
                        }
                    }
                } else {
                    feature.setID(id++);
                }
            }
            if (feature != null) {
                if (feature.hasSignal(Signal.UTTERANCE_START)) {
                    id = 0;
                }
                
                if (feature.hasContent()) {
                    lastFeature = feature;
                } else {
                    lastFeature = null;
                }
            } else {
                lastFeature = null;
            }
        }

        return feature;
    }
}
