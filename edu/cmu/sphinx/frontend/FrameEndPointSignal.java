/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Indicates that the next audio window is the start or end of the
 * audio frame.
 */
public class FrameEndPointSignal extends EndPointSignal {


    private FrameEndPointSignal(boolean isStart) {
        super(isStart);
    }


    /**
     * Constructs a FrameEndPointSignal indicating frame start.
     *
     * @return a Signal indicating start of frame
     */
    public static FrameEndPointSignal getStartSignal() {
	return (new FrameEndPointSignal(true));
    }


    /**
     * Constructs a FrameEndPointSignal indicating frame end.
     *
     * @return a Signal indicating end of frame
     */
    public static FrameEndPointSignal getEndSignal() {
	return (new FrameEndPointSignal(false));
    }
}
