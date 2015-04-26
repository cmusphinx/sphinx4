package edu.cmu.sphinx.frontend.databranch;

import edu.cmu.sphinx.util.props.Configurable;

/** Some API-elements which are shared by components which can generate {@link edu.cmu.sphinx.frontend.Data}s. */
public interface DataProducer extends Configurable {

    /** Registers a new listener for <code>Data</code>s.
     * @param l listener to add
     */
    void addDataListener(DataListener l);


    /** Unregisters a listener for <code>Data</code>s.
     * @param l listener to remove
     */
    void removeDataListener(DataListener l);
}
