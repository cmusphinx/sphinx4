/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * Notifies the Preemphasizer to change its "prior" value.
 */
public class PreemphasisPriorSignal implements Signal {

    private short prior;

    
    /**
     * Constructs a PreemphasisPriorSignal with the given prior value.
     *
     * @param priorValue the new prior value that the Preemphasizer should use
     */
    public PreemphasisPriorSignal(short priorValue) {
	this.prior = priorValue;
    }


    /**
     * Returns the prior value
     *
     * @return the prior value
     */
    public short getPrior() {
	return prior;
    }
}
