/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Indicates that the next audio frame is the start or end of the
 * audio segment.
 */
public class SegmentEndPointSignal implements Signal {

    private boolean isStart;
    private boolean isEnd;
    private Data dataFrame;


    private SegmentEndPointSignal(boolean isStart, boolean isEnd, Data dataFrame) {
	this.isStart = isStart;
	this.isEnd = isEnd;
	this.dataFrame = dataFrame;
    }


    /**
     * Constructs a SegmentEndPointSignal indicating segment start with
     * the given audio frame.
     *
     * @param frameData the first audio frame in the segment
     */
    public static SegmentEndPointSignal getStartSignal(Data dataFrame) {
	return (new SegmentEndPointSignal(true, false, dataFrame));
    }


    /**
     * Constructs a SegmentEndPointSignal indicating segment end with
     * the given audio frame.
     *
     * @param frameData the last audio frame in the segment
     */
    public static SegmentEndPointSignal getEndSignal(Data dataFrame) {
	return (new SegmentEndPointSignal(false, true, dataFrame));
    }


    /**
     * Constructs a SegmentEndPointSignal indicating both segment start
     * and end with the given audio frame.
     *
     * @param frameData the first, which is also the last, audio frame
     *    in the segment
     */
    public static SegmentEndPointSignal getStartEndSignal(Data dataFrame) {
	return (new SegmentEndPointSignal(true, true, dataFrame));
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
	return isEnd;
    }


    /**
     * Returns the Data object associated with this SegmentEndPointSignal.
     *
     * @return a Data object
     */
    public Data getData() {
	return dataFrame;
    }


    /**
     * Sets the Data object associated with this SegmentEndPointSignal
     *
     * @param data the Data object
     */
    public void setData(Data data) {
	this.dataFrame = data;
    }
}
