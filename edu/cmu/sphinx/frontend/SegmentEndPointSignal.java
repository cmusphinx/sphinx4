/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Indicates that the next audio frame is the start or end of the
 * audio segment.
 */
public class SegmentEndPointSignal extends EndPointSignal {


    /**
     * Constructs a SegmentEndPointSignal.
     *
     * @param isStart true if this Signal indicates start of segment
     *                false if this Signal indicates end of segment
     */
    private SegmentEndPointSignal(boolean isStart) {
        super(isStart);
    }


    /**
     * Constructs a SegmentEndPointSignal indicating segment start.
     *
     * @return a Signal indicating start of segment
     */
    public static SegmentEndPointSignal getStartSignal() {
        return (new SegmentEndPointSignal(true));
    }


    /**
     * Constructs a SegmentEndPointSignal indicating segment end.
     *
     * @return a Signal indicating end of segment
     */
    public static SegmentEndPointSignal getEndSignal() {
	return (new SegmentEndPointSignal(false));
    }
}
