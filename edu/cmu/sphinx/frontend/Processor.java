/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

/**
 * A front-end processor. A Processor can do one of a few things given
 * a Data object. It can either pass it along unchanged, in which case
 * the Processor just returns it. In this case the Processor might
 * change state according to the information given by the Data object.
 * Alternatively, it can filter the object, in which case the Processor
 * modifies it, and returns the same object or another object of the same
 * type. Finally, it can transform it, in which case it will return an
 * object of another type.
 */
public interface Processor {

    /**
     * Processes the given Data object.
     *
     * @param input the input Data object
     *
     * @return the transformed or filtered object
     */
    public Data process(Data input);
}
