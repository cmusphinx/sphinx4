/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Indicates that the next audio frame is the start or end of the
 * a sequence of frames.
 */
public class EndPointSignal implements Signal {

    private boolean isStart;
    

    /**
     * Constructs an EndPointSignal
     * indicating whether it is the start and/or the end.
     *
     * @param isStart true if this is the start of the sequence
     *                false if this is the end of the sequence
     */
    protected EndPointSignal(boolean isStart) {
	this.isStart = isStart;
    }



    /**
     * Returns true if this EndPointSignal indicates a segment start,
     *    false otherwise
     *
     * @return true or false
     */
    public boolean isStart() {
	return isStart;
    }


    /**
     * Returns true if this EndPointSignal indicates a segment end,
     *    false otherwise
     *
     * @return true of false
     */
    public boolean isEnd() {
	return (!isStart);
    }
}
