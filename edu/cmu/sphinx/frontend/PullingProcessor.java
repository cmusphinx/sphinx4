/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import java.io.IOException;


/**
 * A PullingProcessor reads a Data object from a DataSource, and processes
 * it. This way, it produces another Data object that can be read
 * by another object. Therefore, it is both a <code>DataSource</code>
 * and a <code>Processor</code>.
 *
 * <p>PullingProcessor implements the methods that allow you
 * to get/set the source to "pull" from, but leaves it to the subclass
 * to implement the <code>process()</code> method of the <code>Process</code>
 * interface.
 *
 * <p>The <code>read()</code> method reads a single Data object from the
 * source, executes the <code>process()</code> method on it, and returns
 * the resulting Data object. A subclass can override this method for a
 * different reading behavior (e.g. read more than one Data object from
 * the source).
 */
public abstract class PullingProcessor implements DataSource, Processor {


    /**
     * the predecessor DataSource to pull Data objects from
     */
    private DataSource predecessorDataSource;


    /**
     * Returns the DataSource to pull Data objects from
     *
     * @return the DataSource to pull Data objects from, or null if no source
     */
    public DataSource getSource() {
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
     * Reads a single Data object from this PullingProcessor (which is also
     * a DataSource). This method obtains a <code>Data</code> object
     * from its source by calling the <code>read()</code> method of its
     * source, execute the <code>process()</code> method on the
     * <code>Data</code> object, and return the resulting <code>Data</code>
     * object. To alter the source reading behavior, e.g., to read more than
     * one Data object from its source, subclasses should override this
     * method. 
     *
     * @return a Data object
     *
     * @throws java.io.IOException
     */
    public Data read() throws IOException {
	Data input = getSource().read();
	if (input instanceof SegmentEndPointSignal) {
	    SegmentEndPointSignal signal = (SegmentEndPointSignal) input;
	    signal.setData(process(signal.getData()));
	    return signal;
	} else {
	    return input;
	}
    }
}
