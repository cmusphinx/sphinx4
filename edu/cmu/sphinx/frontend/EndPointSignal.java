/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;


/**
 * A data-less Signal to indicate the start or the end of a sequence of frames.
 */
public class EndPointSignal implements Signal {

    private String signalName = null;

    /**
     * A signal to indicate the start of segment.
     */
    public static final EndPointSignal SEGMENT_START = new EndPointSignal
    ("SegmentStartSignal");
    
    
    /**
     * A signal to indicate the end of segment.
     */
    public static final EndPointSignal SEGMENT_END = new EndPointSignal
    ("SegmentEndSignal");


    /**
     * A signal to indicate the start of frame sequence.
     */
    public static final EndPointSignal FRAME_START = new EndPointSignal
    ("FrameStartSignal");

    
    /**
     * A signal to indicate the end of frame sequence.
     */
    public static final EndPointSignal FRAME_END = new EndPointSignal
    ("FrameEndSignal");
    
    
    private EndPointSignal(String signalName) {
        this.signalName = signalName;
    }
    
    
    /**
     * Returns true if the given EndPointSignal is the same as this
     * EndPointSignal, false otherwise. Two EndPointSignals are the
     * same if their <code>toString()</code> method returns
     * strings that return true on <code>String.equal()</code>.
     *
     * @return true or false
     */
    public boolean equals(EndPointSignal endPointSignal) {
        return toString().equals(endPointSignal.toString());
    }


    /**
     * Returns the name of this EndPointSignal.
     *
     * @return the name of this EndPointSignal
     */
    public String toString() {
        return signalName;
    }
}
