/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Indicates an endpoint (start/end) of an audio segment. This
 * SegmentEndPointSignal should also carry to the corresponding start/end
 * audio frame. 
 */
public class SegmentEndPointSignal implements Signal {

    private boolean isStart;

    private SegmentEndPointSignal(boolean isStart) {
	this.isStart = isStart;
    }


    /**
     * Constructs a SegmentEndPointSignal indicating segment start with
     * the given audio frame.
     *
     * @param frameData the first audio frame in the segment
     */
    public static SegmentEndPointSignal createSegmentStartSignal() {
	return (new SegmentEndPointSignal(true));
    }


    /**
     * Constructs a SegmentEndPointSignal indicating segment end with
     * the given audio frame.
     *
     * @param frameData the last audio frame in the segment
     */
    public static SegmentEndPointSignal createSegmentEndSignal() {
	return (new SegmentEndPointSignal(false));
    }


    /**
     * Returns true if this SegmentEndPointSignal indicates a segment start,
     *    false otherwise
     *
     * @return true or false
     */
    public boolean isStart() {
	return isStart;
    }


    /**
     * Returns true if this SegmentEndPointSignal indicates a segment end,
     *    false otherwise
     *
     * @return true of false
     */
    public boolean isEnd() {
	return !isStart;
    }
}
