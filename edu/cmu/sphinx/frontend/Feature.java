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

import edu.cmu.sphinx.frontend.util.Util;

/**
 * Represents a single Feature. A Feature is simply an array of numbers,
 * usually of length 39 and of type float.
 */
public class Feature extends Data {

    private float[] featureData = null;  // the feature data
    private String type;                 // acoustic model name
    private int ID;                      // which feature in the utterance


    /**
     * Constructs a Feature with the given feature data.
     *
     * @param featureData the feature data points
     * @param ID the ID of this Feature with respect to the current
     *    speech segment.
     */
    public Feature(float[] featureData, int ID) {
	this(featureData, ID, null);
    }


    /**
     * Constructs a Feature with the given featureData, ID, and utterance
     *
     * @param featureData the feature data
     * @param ID the Id of this Feature with respect to the current
     *    speech segment
     * @param utterance the Utterance associated with this Feature
     */
    public Feature(float[] featureData, int ID, Utterance utterance) {
        super(utterance);
        this.featureData = featureData;
	this.type = null;
        this.ID = ID;
    }


    /**
     * Constructs a Feature with the given Signal.
     *
     * @param signal the Signal this Feature carries
     * @param ID the ID of this Feature with respect to the current
     *    speech segment.
     */
    public Feature(Signal signal, int ID) {
        super(signal);
        this.ID = ID;
    }


    /**
     * Returns the feature data.
     *
     * @return the feature data
     */
    public float[] getFeatureData() {
	return featureData;
    }


    /**
     * Returns the ID of this Feature, telling you which Feature it is
     * with respect to the utterance.
     *
     * @return the ID
     */
    public int getID() {
        return ID;
    }


    /**
     * Returns the type of this Feature. It should normally be the name
     * of the acoustic model used, because it is used to identify which
     * acoustic model this Feature should be aligned with in the decoder.
     * It can also return null, meaning that it has no type name. This
     * would mean that there is only one acoustic model in the decoder.
     *
     * @return the type name of this Feature, or null if it does not
     *    have a type name.
     */
    public String getType() {
	return type;
    }


    /**
     * Returns the audio data that corresponds to this Feature.
     * Note that this method only returns that particular window of
     * audio data that this Feature corresponds to, not the audio data
     * of the entire utterance.
     *
     * <p>The audio data might not be available, because the
     * <code>edu.cmu.sphinx.frontend.keepAudioReference</code>
     * SphinxProperty is set to false. In that case, this method
     * return null.
     *
     * @return the audio data that corresponds to this Feature, or null
     *    if the audio data is not available.
     */
    public byte[] getAudio() {
        if (getUtterance() == null) {
            return null;
        } else {
            return getUtterance().getAudio(getID());
        }
    }


    /**
     * Sets the type name of this Feature. It should be the name
     * of the acoustic model used. This is the only mutable field in
     * this class. It can only be set once, subsequent calls to this
     * method have no effect.
     *
     * @param type the type name of this Feature
     */
    public void setType(String type) {
	if (this.type == null) {
	    this.type = type;
	}
    }


    /**
     * Returns a String representation of this Feature.
     * The format of the string is:
     * <pre>featureLength data0 data1 ...</pre>
     *
     * @return the String representation
     */
    public String toString() {
        if (featureData != null) {
            return ("Feature: " + ID + ", data: " + getType());
	    // + Util.floatArrayToString(featureData));
        } else {
            return ("Feature: " + getSignal().toString());
        }
    }
}
