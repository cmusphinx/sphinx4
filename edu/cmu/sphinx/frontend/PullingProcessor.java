/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.Timer;


/**
 * A PullingProcessor reads a Data object from a DataSource, processes
 * it and returns either the same Data object (in which case the
 * PullingProcessor is called a <i>filter</i>), or another Data object (in
 * which case the PullingProcessor is called a <i>translator</i>).
 * This Data object is then read by another object via the
 * <code>read()</code> method. Therefore, it is a <code>DataSource</code>.
 *
 * <p>A series of PullingProcessors chained together produces a processing
 * pipeline, with each PullingProcessor having a different processing
 * function. PullingProcessors are chained together by the <pre>
 * PullingProcesor.setSource(DataSource) </pre>
 * method. This is possible because each PullingProcessor is also
 * a <code>DataSource</code>. Therefore, to chain three PullingProcessors,
 * A, B and C, with B reading the product of A, and C reading the product
 * of B, one would do: <pre>
 * PullingProcessor A = new PullingProcessor();
 * PullingProcessor B = new PullingProcessor();
 * PullingProcessor C = new PullingProcessor();
 * B.setSource(A);
 * C.setSource(B);</pre>
 *
 * Continuing this example, to trigger the chain of processing, one would
 * call: <pre>
 * Data result;
 * do {
 *     result = C.read();
 * } while (result != null); </pre>
 * The <code>read()</code> method of <code>C</code> should call the 
 * <code>read()</code> method of <code>B</code>, which should call the
 * <code>read()</code> method of <code>A</code>. The filtering/translating
 * is also done in the <code>read()</code> method. For example, the
 * <code>read()</code> method of <code>B</code> might look like: <pre>
 * public Data read() {
 *
 *     // getSource() returns A
 *     Data input = getSource().read();
 *
 *     // process() is the filter/translation method
 *     // which returns a Data object
 *     return process(input); 
 * } </pre>
 *
 * Note that a PullingProcessor might be reading different types of
 * <code>Data</code> objects from its source. As a result, the
 * <code>process()</code> method should handle each Data type differently.
 * For more detailed examples, please look at the code of the different
 * subclasses of PullingProcessors.
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
