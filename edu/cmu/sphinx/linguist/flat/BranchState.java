package edu.cmu.sphinx.linguist.flat;


/**
 * Represents a branching node in a grammar
 *
 */
public class BranchState extends SentenceHMMState {

    /**
     * Creates a branch state
     *
     * @param nodeID the grammar node id
     */
    public BranchState(String leftContext, String rightContext, int nodeID) {
	super("B" + "[" + leftContext +"," +
                rightContext + "]", null, nodeID);
    }


    /**
     * Retrieves a short label describing the type of this state.
     * Typically, subclasses of SentenceHMMState will implement this
     * method and return a short (5 chars or less) label
     *
     * @return the short label.
     */
    public String getTypeLabel() {
	return "Brnch";
    }
}
