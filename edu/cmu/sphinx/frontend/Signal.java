/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import java.io.Serializable;

/**
 * Indicates events like beginning/end of audio segment, data dropped,
 * quality changed, etc..
 */
public class Signal implements Serializable {

    private String name = null;

    /**
     * Signal to indicate data present.
     */
    public static final Signal CONTENT = new Signal("content");

    /**
     * Signal to indicate the end of a speech segment.
     */
    public static final Signal SEGMENT_END = new Signal("segmentEnd");

    /**
     * Signal to indicate the start of a speech segment.
     */
    public static final Signal SEGMENT_START = new Signal("segmentStart");

    /**
     * Constructs a Signal with the given name.
     */
    protected Signal(String name) {
        this.name = name;
    }


    /**
     * Returns true if the given Signal is equal to this Signal.
     *
     * @param signal the Signal to compare
     *
     * @return true if they are the same, false otherwise
     */
    public boolean equals(Signal signal) {
        return toString().equals(signal.toString());
    }


    /**
     * Returns a hash code value for this Signal.
     *
     * @return a hash code value for this Signal.
     */
    public int hashCode() {
        return toString().hashCode();
    }


    /**
     * Returns the name of this Signal.
     *
     * @return the name of this Signal.
     */
    public String toString() {
        return name;
    }
}
