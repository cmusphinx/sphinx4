/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.decoder.linguist;


import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.Collection;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.knowledge.acoustic.LeftRightContext;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.decoder.linguist.WordState;

/**
 * Represents a single state in an SentenceHMM
 */
public class  SentenceHMMState implements Serializable {
    private final static int MASK_IS_FINAL 		= 0x1;
    private final static int MASK_COLOR_RED 		= 0x2;
    private final static int MASK_PROCESSED 		= 0x4;
    private final static int MASK_FAN_IN 		= 0x8;
    private final static int MASK_IS_WORD_START		= 0x10;
    private final static int MASK_IS_SHARED_STATE	= 0x20;
    private final static int MASK_WHICH 	= 0xffff;
    private final static int SHIFT_WHICH 	= 0x8;
    private final static int BINARY_VERSION_NUMBER = 0x10000;

    private static int globalStateNumber = -1000;

    private int stateNumber;

    // a number of separate variables are maintained in 'fields'
    // inorder to reduce the size of the SentenceHMM

    private int fields;  
    private String name;

    private transient Map arcs;
    private transient SentenceHMMState parent;
    private transient String signature;
    private transient String cachedName;
    private transient String fullName;
    private transient SentenceHMMStateArc[] successorArray;
    private transient Token bestToken;


    /**
     * Creates a SentenceHMMState
     *
     * @param arcs the arcs to the next set of states
     *
     */
    protected SentenceHMMState(String name, SentenceHMMState parent, int which) {
	this.name = name + which;
	this.parent = parent;
	setWhich(which);
	this.arcs = new LinkedHashMap();
	stateNumber = globalStateNumber--;
	setProcessed(false);
	setColor(Color.RED);
    }


    /**
     * Empty contructor
     */
    protected SentenceHMMState() {
	stateNumber = globalStateNumber--;
    }



    /**
     * Validates this SentenceHMM
     *
     * @return true if the SentenceHMM is valid, otherwise, returns
     * false.
     */
    public boolean validate() {
	return true;
    }



    /**
     * Determines if this state has been 'processed'. The meaning of
     * 'processed' is not defined here, but is up to the higher levels
     *
     * @return true if the state has been processed.
     */
    public boolean isProcessed() {
	return (fields & MASK_PROCESSED) == MASK_PROCESSED;
    }

    /**
     * Determines if this state marks the beginning of a word
     *
     * @return true if the state marks the beginning of a word
     */
    public boolean isWordStart() {
	return (fields & MASK_IS_WORD_START) == MASK_IS_WORD_START;
    }

    /**
     * Sets the 'wordStart' flag
     *
     * @param wordStart <code>true</code> if this state marks the
     * beginning of a word.
     */
    public void setWordStart(boolean wordStart) {
	if (wordStart) {
	    this.fields |= MASK_IS_WORD_START;
	} else {
	    this.fields &= ~MASK_IS_WORD_START;
	}
    }

    /**
     * Determines if this state is a shard state
     *
     * @return true if the state marks the beginning of a word
     */
    public boolean isSharedState() {
	return (fields & MASK_IS_SHARED_STATE) == MASK_IS_SHARED_STATE;
    }

    /**
     * Sets the shared state flag
     *
     * @param shared <code>true</code> if this state is shared
     */
    public void setSharedState(boolean shared) {
	if (shared) {
	    this.fields |= MASK_IS_SHARED_STATE;
	} else {
	    this.fields &= ~MASK_IS_SHARED_STATE;
	}
    }

    /**
     * Returns the word associated with the particular unit
     *
     * @return the word associated with this state, or null if there
     * is no word associated with this state.
     */
    public edu.cmu.sphinx.decoder.linguist.GrammarWord getAssociatedWord() {
	edu.cmu.sphinx.decoder.linguist.GrammarWord word = null;
	SentenceHMMState state = this;

	while ( state != null && !(state instanceof WordState)) {
	    state = state.getParent();
	}

	if (state != null) {
	    WordState wordState = (WordState) state;
	    word = wordState.getWord();
	}
	return word;
    }

    /**
     * Retrieves a short label describing the type of this state.
     * Typically, subclasses of SentenceHMMState will implement this
     * method and return a short (5 chars or less) label
     *
     * @return the short label.
     */
    public String getTypeLabel() {
	return "state";
    }



    /**
     * Determines if this state is a fan-in state. The search may
     * need to adjust the pruning for states that fan in since they
     * are shared by multiple paths
     */
    public boolean isFanIn() {
	return (fields & MASK_FAN_IN) == MASK_FAN_IN;
    }

    public void setFanIn(boolean fanIn) {
	if (fanIn) {
	    this.fields |= MASK_FAN_IN;
	} else {
	    this.fields &= ~MASK_FAN_IN;
	}
    }

    /**
     * Sets the processed flag for this state
     *
     * @param processed the new setting for the processed flag
     */
    public void setProcessed(boolean processed) {
	if (processed) {
	    this.fields |= MASK_PROCESSED;
	} else {
	    this.fields &= ~MASK_PROCESSED;
	}
    }

    /**
     * Reset process flags for this state and all successor states
     */
    public void resetAllProcessed() {
	visitStates(new SentenceHMMStateVisitor() {
	    public boolean visit(SentenceHMMState state) {
	    	state.setProcessed(false);
		return false;
	    }
	}, this, false);
    }
    
    /**
     * Reports an invalid message
     *
     * @param msg the message to display
     */
    private void report(String msg) {
	System.out.println("Invalid state " + getTitle() + "-" + msg);
    }

    /**
     * Get the next successor states and their probababilities
     *
     * @return the arcs to the successor states
     */
    // FIXME
     Collection getSuccessors() {
	return arcs.values();
    }

    /**
     * Gets the number of successors
     *
     * @return the number of successors
     */
    public int getNumSuccessors() {
	return arcs.size();
    }


    /**
     * Gets an array of successors. This allocates the array as
     * necessary. Any future adds of successors will invalidate this
     * array (and set it to null). IN general we build up the tree
     * first before we start getting successors, so this gives us
     * better performance in the decoder.
     *
     * @return the array of successor arcs
     */
    public SentenceHMMStateArc[] getSuccessorArray() {
	if (successorArray == null) {
	    successorArray = new SentenceHMMStateArc[arcs.size()];
	    arcs.values().toArray(successorArray);
	}
	return successorArray;
    }


    /**
     * remove the given arc from the set of succors
     *
     * @param arc the arc to remove
     */
    public void deleteSuccessor(SentenceHMMStateArc arc) {
	getSuccessors().remove(arc);
    }


    /**
     * Returns the best token associated with this sentence state
     *
     * @return the token
     */
    public final Token getBestToken() {
	return bestToken;
    }

    /**
     * Sets the best token associated with this sentence state
     *
     * @param token  the token
     *
     * @return the previous best token
     */
    public final Token setBestToken(Token token) {
	Token oldBestToken = bestToken;
	bestToken = token;
        if (oldBestToken != null && oldBestToken.getFrameNumber() ==
                token.getFrameNumber()) {
            return oldBestToken;
        } else {
            return null;
        }
    }


    /**
     * Connects the arc to this sentence hmm.  If the node at the end
     * of the arc is already pointing to some other node as its
     * predecessor, don't change that relationship, since its probably
     * a result of the nodes being reused'
     *
     * @param arc the path to the next state
     */
    public void connect(SentenceHMMStateArc arc) {
	if (successorArray != null) {
	    successorArray = null;
	}
	rawConnect(arc);
    }


    /**
     * Connects the arc to this sentence hmm, but don't affect the
     * predecessor relation ship
     *
     * @param arc the arc to the next state
     */
    private void rawConnect(SentenceHMMStateArc arc) {
 	arcs.put(arc.getNextState().getValueSignature(), arc);
    }


    /**
     * Determines if this state is an emitting state
     *
     * @return true if the state is an emitting state
     */
    public boolean isEmitting() {
	return false;
    }

    /**
     * Determines if this is a final state
     * 
     * @return true if this is a final state
     */
    public boolean isFinalState() {
	return (fields & MASK_IS_FINAL) == MASK_IS_FINAL;
    }

    /**
     * Sets this is to be final state
     * 
     * @param  state true if this is a final state
     */
    public void setFinalState(boolean state) {
	if (state) {
	    this.fields |= MASK_IS_FINAL;
	} else {
	    this.fields &= ~MASK_IS_FINAL;
	}
    }

    public boolean isUnit() {
	return false;
    }


    /**
     * Clears/resets any accumulated state or history
     */
    public void clear() {
	bestToken = null;
    }


    /**
     * Dumps this SentenceHMMState and all its successors. Just for debugging.
     */
    public void dumpAll() {
	visitStates(new SentenceHMMStateVisitor() {
	    public boolean visit(SentenceHMMState state) {
		state.dump();
		return false;
	    }
	}, this, true);
    }


    /**
     * Returns any annotation for this state
     *
     * @return the annotation
     */
    protected String getAnnotation() {
	return "";
    }



    /**
     * Dumps this state
     */
    private void dump() {
	System.out.println(" ----- " + getTitle() + " ---- ");
	for (Iterator i = getSuccessors().iterator(); i.hasNext(); ) {
	    SentenceHMMStateArc arc = (SentenceHMMStateArc) i.next();
	    System.out.println("   -> " + arc.getNextState().getTitle());
	}
    }


    /**
     * Remove the best token from this SentenceHMMState and all its
     * succeeding SentenceHMMStates.
     */
    public void resetAll() {
	visitStates(new SentenceHMMStateVisitor() {
		public boolean visit(SentenceHMMState state) {
		    state.clear();
		    return false;
		}
	    }, this, false);
    }


    /**
     * Validates this SentenceHMMState and all successors
     *
     */
    public void validateAll() {
	ValidatorVisitor vv = new ValidatorVisitor();
	visitStates(vv, this, false);
	if (vv.isOK()) {
	    System.out.println(" *** SentenceHMM is valid ***");
	}
    }


    /**
     * Gets the name for this state
     *
     * @return the name
     */
    public String getName() {
	return name;
    }

    /**
     * Returns a pretty name for this HMM
     *
     * @return a pretty name
     */
    public String getPrettyName() {
	return getName();
    }


    /**
     * Returns the string representation of this object
     */
    public String toString() {
	if (cachedName == null) {
	    StringBuffer sb = new StringBuffer();
	    if (isEmitting()) {
		sb.append("*");
	    }
	    sb.append(getName());

	    String base =  (isEmitting() ? "*" : "") + getName()
		+ getWhich() + (isFinalState() ? "!" : "");

	    if (parent != null) {
		sb.append("_");
		sb.append(parent.toString());
	    }

	    if (isFinalState()) {
		sb.append("!");
	    }
	    cachedName = sb.toString();
	}
	return cachedName;
    }


    /**
     * Gets the fullName for this state
     *
     * @return the full name for this state
     */
    public String getFullName() {
	if (fullName == null) {
	    if (parent == null) {
		fullName =  getName();
	    } else {
		fullName =  getName() + "." + parent.getFullName();
	    }
	}

	return fullName;
    }


    /**
     * Gets the signature for this state
     *
     * @return the signature
     */
    public String getSignature() {
	/*
	System.out.println("getsig : " + signature + " name " +
		getName());
	*/
	if (signature == null) {
	    signature = getFullName();
	}
	return signature;
    }




    /**
     *  gets the title (fullname + stateNumber) 
     * for this state
     *
     * @return the title
     */
    public String getTitle() {
	// return getFullName() + ":" + stateNumber;
	return getSignature() + ":" + stateNumber;
    }


    /**
     * Retrieves the index for this state
     * words
     *
     * @return the index
     */
    public int getWhich() {
	return (fields >> SHIFT_WHICH) & MASK_WHICH;
    }


    /**
     * Sets the index for this state
     * 
     * @param which the index for this state
     */
    public void setWhich(int which) {
	assert which >= 0 && which <= MASK_WHICH;
	fields |= (which & MASK_WHICH) << SHIFT_WHICH;
    }


    /**
     * Retrieves the parent sate
     *
     * @return the parent state (or null if this state does not have a
     * parent state).
     */
    public SentenceHMMState getParent() {
	return parent;
    }

    /**
     * Sets the parent state for this state
     *
     * @param parent the parent state
     */
    private void setParent(SentenceHMMState parent) {
	this.parent = parent;
    }


    /**
     * Searches the set of arcs for an arc that points to a state with
     * an identical value
     *
     * @param state the state to search for
     *
     * @return the arc or null if none could be found.
     */
    public SentenceHMMStateArc findArc(SentenceHMMState state) {
	SentenceHMMStateArc arc =
 	    (SentenceHMMStateArc) arcs.get(state.getValueSignature());
	return arc;
    }

     /**
      * Returns the value signature of this unit
      *
      * @return the value signature
      */
     public String getValueSignature() {
 	return getSignature();
     }


    /**
     * Visit all of the states starting at start with the given vistor
     *
     * @param vistor the state visitor
     * @param start the place to start the search
     * @param sorted if true, states are sorted before visited
     *
     * @return  true if the visiting was terminated before all nodes
     * were visited
     */
    public static boolean visitStates(SentenceHMMStateVisitor visitor,
	    SentenceHMMState start, boolean sorted) {
	Set states = collectStates(start);

	if (sorted) {
	    // sort the states by stateNumber

	    TreeSet sortedStates = new TreeSet(new Comparator() {
			public int compare(Object o1, Object o2) {
			    SentenceHMMState so1 = (SentenceHMMState) o1;
			    SentenceHMMState so2 = (SentenceHMMState) o2;
			    return so1.stateNumber - so2.stateNumber;
			}
		    });
	    sortedStates.addAll(states);
	    states = sortedStates;
	}

	for (Iterator i = states.iterator(); i.hasNext();) {
	    SentenceHMMState state = (SentenceHMMState) i.next();
	    if (visitor.visit(state)) {
		return true;
	    }
	} 
	return false;
    }


    /**
     * Writes out a binary representation of the SentenceHMM graph that
     * is headed by the given state.
     *
     * @param path the name of the file to be written
     *
     * @param initialState the head of the graph
     */
    public static void exportBinaryRepresentation(String path,
	    SentenceHMMState initialState) {

    // we handle the object references manually otherwise we'd have to
    // deal with a stack overflow as even very small sentence hmms are
    // dumped out.

	try {
	    FileOutputStream out = new FileOutputStream(path);
	    ObjectOutputStream s = new ObjectOutputStream(out);

	   // collect up all the state into a list
	    Set states = collectStates(initialState);

	    Map stateMap = new HashMap();
	    for (Iterator i = states.iterator(); i.hasNext(); ) {
		SentenceHMMState state = (SentenceHMMState) i.next();
		stateMap.put(state.getSignature(), state);
	    }

	    System.out.println("Export " + states.size() + " states");

	   // set all the state numbers 
	    int count = 0;

	    for (Iterator i = states.iterator(); i.hasNext(); ) {
		SentenceHMMState state = (SentenceHMMState) i.next();
		state.setStateNumber(count++);
	    }

	    s.writeInt(BINARY_VERSION_NUMBER);
	    // write out the initial state number
	    s.writeInt(initialState.getStateNumber());

	    // write the number of states
	    s.writeInt(states.size());

	   // write all the states out 

	    for (Iterator i = states.iterator(); i.hasNext(); ) {
		SentenceHMMState state = (SentenceHMMState) i.next();
		s.writeObject(state);
	    }

	    // write all the parts of the sentence hmm that refer
	    // to other sentence hmms

	    // a state needs to have its parent set before we hook up
	    // the arcs (to get a valid signature) so we do this in
	    // two passes.

	    for (Iterator i = states.iterator(); i.hasNext(); ) {
		SentenceHMMState state = (SentenceHMMState) i.next();
		s.writeInt(state.getStateNumber());

		if (state.getParent() == null) {
		    s.writeInt(-1);
		} else {
		    SentenceHMMState parent = state.getParent();

		    // due to path folding and such, it is possible
		    // for a states parent to not actually be in the
		    // sentence hmm graph. If this is the case, then
		    // the parents statenumber will be negative (since
		    // we've just assigned positive state numbers to
		    // the in-graph states.  If we find such a state,
		    // we are guaranteed to find an identical in-graph
		    // state so we substitute. This has a nice affect
		    // of making loaded binary graphs a bit cleaner.

		    if (parent.getStateNumber() < 0) {
			parent = (SentenceHMMState)
			    stateMap.get(parent.getSignature());
		    }
		    s.writeInt(parent.getStateNumber());
		}
	    }

	    for (Iterator i = states.iterator(); i.hasNext(); ) {
		SentenceHMMState state = (SentenceHMMState) i.next();
		s.writeInt(state.getStateNumber());

		SentenceHMMStateArc[] arcs = state.getSuccessorArray();
		s.writeInt(arcs.length);
		for (int j = 0; j < arcs.length; j++) {
		    s.writeInt(arcs[j].getNextState().getStateNumber());
		    s.writeFloat(arcs[j].getAcousticProbability());
		    s.writeFloat(arcs[j].getLanguageProbability());
		    s.writeFloat(arcs[j].getInsertionProbability());
		}
	    }
	    s.flush();
	    s.close();
	} catch (IOException ioe) {
	    System.out.println("IOE " + ioe);
	}
    }

    // we handle the object references manually otherwise we'd have to
    // deal with a stack overflow as even very small sentence hmms are
    // dumped out.

    /**
     * Reads in a binary representation of the SentenceHMM graph
     * previous written by exportBinaryRepresentation
     *
     * @param path the name of the file to be written
     *
     * @return the state that forms the head of the graph
     */
    public static SentenceHMMState importBinaryRepresentation(String path) {
	SentenceHMMState initialState = null;

	try {
	    FileInputStream in = new FileInputStream(path);
	    ObjectInputStream s = new ObjectInputStream(in);

	    int binaryVersionNumber = s.readInt();

	    if (binaryVersionNumber  != BINARY_VERSION_NUMBER) {
		throw new StreamCorruptedException("bad version number");
	    }
	    int initialStateNumber = s.readInt();
	    int numStates = s.readInt();

	    System.out.println("Importing " + numStates + " states");

	    SentenceHMMState[] states = new SentenceHMMState[numStates];

	    // read the states

	    for (int i = 0; i < numStates; i++) {
		SentenceHMMState state = (SentenceHMMState) s.readObject();
		states[state.getStateNumber()] = state;
	    }

	    // Read the parents 

	    for (int i = 0; i < numStates; i++) {
		int stateNumber = s.readInt();

		SentenceHMMState state = states[i];

		int parentState = s.readInt();
		if (parentState != -1) {
		    SentenceHMMState parent = states[parentState];
		    if (parent == null) {
			System.out.println("NULL parent for " + state);
		    }
		    state.setParent(parent);
		}
	    }

	    // read in all of the arcs

	    for (int i = 0; i < numStates; i++) {
		int stateNumber = s.readInt();

		SentenceHMMState state = states[i];

		int arcsLength = s.readInt();

		for (int j = 0; j < arcsLength; j++) {
		    int arcStateNumber = s.readInt();
		    float acousticProb = s.readFloat();
		    float languageProb = s.readFloat();
		    float insertionProb = s.readFloat();
		    SentenceHMMState arcState = states[arcStateNumber];
		    SentenceHMMStateArc arc = new SentenceHMMStateArc(arcState,
			    acousticProb, languageProb, insertionProb);
		    state.rawConnect(arc);
		}
	    }
	    s.close();

	    initialState = states[initialStateNumber];

	} catch (IOException ioe) {
	    System.out.println("IOE " + ioe);
	} catch (ClassNotFoundException cnf) {
	    System.out.println("CNF " + cnf);
	}
	return initialState;
    }



    /**
     * Sets the color for this node
     *
     * @param color the color of this node
     */
    public void setColor(Color color) {
	if (color == Color.RED) {
	    this.fields |= MASK_COLOR_RED;
	} else {
	    this.fields &= ~MASK_COLOR_RED;
	}
    }

    /**
     * Gets the color for this node
     *
     * @return the color of this node
     */
    public Color getColor() {
	if ((fields & MASK_COLOR_RED) == MASK_COLOR_RED) {
	    return Color.RED;
	} else {
	    return Color.GREEN;
	}
    }


    /**
     * Sets the state number for this state
     *
     * @param stateNumber the state number
     */
    private void setStateNumber(int stateNumber) {
	this.stateNumber = stateNumber;
    }

    /**
     * Gets the state number for this state
     *
     * @param stateNumber the state number
     */
    private int getStateNumber() {
	return stateNumber;
    }

    /**
     * Collect all states starting from the given start state
     *
     * @param start the state to start the search from
     *
     * @return the set of collected state
     */
    static public Set collectStates(SentenceHMMState start) {
	Set visitedStates = new HashSet();
	List queue = new LinkedList();

	queue.add(start);

	while (queue.size() > 0) {
	    SentenceHMMState state = (SentenceHMMState) queue.remove(0);
	    visitedStates.add(state);
	    SentenceHMMStateArc[] successors = state.getSuccessorArray();
	    for (int i = 0; i < successors.length; i++) {
		SentenceHMMStateArc arc = successors[i];
		SentenceHMMState nextState = arc.getNextState();
		if (!visitedStates.contains(nextState)) {
		    queue.add(nextState);
		}
	    }
	}
	return visitedStates;
    }

    /**
     * Serializes the non-transient fields to the given stream
     *
     * @param s the stream to write the object to
     *
     * @throws java.io.IOException if an error occurs during the write.
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
	s.defaultWriteObject();
    }

    /**
     * De-serializes the non-transient fields to the given stream
     *
     * @param s the stream to read the object from
     *
     * @throws java.io.IOException if an error occurs during the read.
     */
    private void readObject(ObjectInputStream s)
	throws IOException, ClassNotFoundException {
	s.defaultReadObject();
	this.arcs = new LinkedHashMap();
    }

}


/**
 * A state visitor that validates states
 */
class ValidatorVisitor implements SentenceHMMStateVisitor {
    private boolean allOK = true;


    public boolean visit(SentenceHMMState state) {
	if (!state.validate()) {
	    allOK = false;
	}
	return false;
    }

    /**
     * Determines if all states validated properly
     *
     * @return true if all states were valid
     */
    public boolean isOK() {
	return allOK;
    }
}


