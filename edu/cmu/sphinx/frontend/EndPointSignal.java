/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * A data-less Signal to indicate the start or the end of a sequence of frames.
 */
public class EndPointSignal implements Signal {

    /**
     * A signal to indicates the start of segment.
     */
    public static final EndPointSignal SEGMENT_START = new EndPointSignal();


    /**
     * A signal to indicate the end of segment.
     */
    public static final EndPointSignal SEGMENT_END = new EndPointSignal();


    /**
     * A signal to indicate the start of frame sequence.
     */
    public static final EndPointSignal FRAME_START = new EndPointSignal();


    /**
     * A signal to indicate the end of frame sequence.
     */
    public static final EndPointSignal FRAME_END = new EndPointSignal();


    private EndPointSignal() {}
}
