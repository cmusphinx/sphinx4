/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.Timer;


/**
 * A PullingProcessor reads a Data object from a DataSource, and processes
 * it. This way, it produces another Data object that can be read
 * by another object. Therefore, it is a <code>DataSource</code>.
 *
 * <p>PullingProcessor implements the methods that allow you
 * to get/set the source to "pull" from, but leaves it to the subclass
 * to implement the <code>read()</code> method of the <code>DataSource</code>
 * interface.
 */
public abstract class PullingProcessor implements DataSource {


    /**
     * the predecessor DataSource to pull Data objects from
     */
    private DataSource predecessorDataSource;


    /**
     * A Timer for timing processing.
     */
    private Timer timer;

    
    /**
     * Indicates whether to dump the processed Data
     */
    private boolean dump;


    /**
     * Returns the DataSource to pull Data objects from
     *
     * @return the DataSource to pull Data objects from, or null if no source
     */
    public final DataSource getSource() {
	return predecessorDataSource;
    }


    /**
     * Sets the DataSource to pull Data objects from.
     *
     * @param whereToPullFrom the DataSource to pull Data objects from
     */
    public void setSource(DataSource whereToPullFrom) {
	predecessorDataSource = whereToPullFrom;
    }


    /**
     * Returns the Timer.
     *
     * @return the Timer
     */
    public final Timer getTimer() {
        return timer;
    }


    /**
     * Sets the Timer.
     *
     * @param timer the Timer
     */
    public void setTimer(Timer timer) {
        this.timer = timer;
    }


    /**
     * Determine whether to dump the output.
     *
     * @return true to dump, false to not dump
     */
    public final boolean getDump() {
	return this.dump;
    }


    /**
     * Set whether we should dump the output.
     *
     * @param dump true to dump the output; false otherwise
     */
    public void setDump(boolean dump) {
	this.dump = dump;
    }
}
