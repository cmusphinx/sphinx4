
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

package edu.cmu.sphinx.decoder.linguist.clextree;

import edu.cmu.sphinx.knowledge.dictionary.Word;
import edu.cmu.sphinx.knowledge.language.WordSequence;
import edu.cmu.sphinx.knowledge.acoustic.HMM;
import edu.cmu.sphinx.knowledge.acoustic.HMMPosition;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.knowledge.dictionary.Pronunciation;
import edu.cmu.sphinx.knowledge.dictionary.Dictionary;
import edu.cmu.sphinx.decoder.linguist.util.HMMPool;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;


/**
 * Represents the vocabulary as a lex tree with nodes in the tree
 * representing either words (WordNode) or units (HMMNode). HMMNodes
 * may be shared.
 */
class HMMTree {
    private HMMPool hmmPool;
    private WordNode initialNode;
    private Dictionary dictionary;

    private LanguageModel lm;
    private boolean addFillerWords = false;
    private boolean addSilenceWord = true; // TODO: Property for this?
    private Set entryPoints = new HashSet();
    private Set exitPoints = new HashSet();
    private Set allWords = null;
    private EntryPointTable entryPointTable;
    private boolean debug = false;

    /**
     * Creates the HMMTree
     *
     * @param pool the pool of HMMs and units
     * @param dictionary the dictionary containing the pronunciations
     * @param words the set of words to add to the lex tree
     * @param addFillerWords if <code>false</code> add filler words
     */
    HMMTree(HMMPool pool, Dictionary dictionary, LanguageModel lm,
            boolean addFillerWords) {
        this.hmmPool = pool;
        this.dictionary = dictionary;
        this.lm = lm;
        this.addFillerWords = addFillerWords;

        // Utilities.dumpMemoryInfo("lextree before");
        Timer.start("Create HMMTree");
        compile();
        Timer.stop("Create HMMTree");
        // Utilities.dumpMemoryInfo("lextree after");
        // dumpTree();
    }



    /**
     * Given a base unit and a left context, return the
     * set of entry points into the lex tree
     *
     * @param lc the left context
     * @param base the center unit
     *
     * @return the set of entry points
     */
    public Collection getEntryPoint(Unit lc, Unit base) {
        EntryPoint ep = entryPointTable.getEntryPoint(base);
        return ep.getEntryPointsFromLeftContext(lc).getSuccessors();
    }

    /**
     * Compiles the vocabulary into an HMM Tree
     */
    private void compile() {
        collectEntryAndExitUnits();
        entryPointTable = new EntryPointTable(entryPoints);
        addWords();
	entryPointTable.createEntryPointMaps();
        freeze();
    }

    /**
     * Dumps the tree
     *
     */
    void dumpTree() {
        System.out.println("Dumping Tree ...");
        Map dupNode = new HashMap();
        dumpTree(0, getInitialNode(), dupNode);
        System.out.println("... done Dumping Tree");
    }

    /**
     * Dumps the tree
     *
     * @param level the level of the dump
     * @param node the root of the tree to dump
     * @param map map of visited nodes
     */
    private void dumpTree(int level, Node node, Map dupNode) {
        if (dupNode.get(node) == null) {
            dupNode.put(node, node);
            System.out.println(Utilities.pad(level * 1) + node);
            Collection next = node.getSuccessors();
            for (Iterator i= next.iterator(); i.hasNext(); ) {
                Node nextNode = (Node) i.next();
                dumpTree(level + 1, nextNode, dupNode);
            }
        }
    }

    /**
     * Collects all of the entry and exit points for the vocabulary.
     */
    private void collectEntryAndExitUnits() {
        Collection words = getAllWords();
        for (Iterator i = words.iterator(); i.hasNext(); ) {
            Word word = (Word) i.next();
            for (int j = 0; j < word.getPronunciations().length; j++) {
                Pronunciation p = word.getPronunciations()[j];
                Unit first = p.getUnits()[0];
                Unit last = p.getUnits()[p.getUnits().length - 1];
                entryPoints.add(first);
                exitPoints.add(last);
            }
        }

        if (debug) {
            System.out.println("Entry Points: " + entryPoints.size());
            System.out.println("Exit Points: " + exitPoints.size());
        }
    }


    /**
     * Called after the lex tree is built. Frees all temporary
     * structures. After this is called, no more words can be added to
     * the lex tree.
     */
    private void freeze() {
        entryPointTable.freeze();
        Node.freezeAllNodes();
        hmmPool = null;
        dictionary = null;
        lm = null;
        entryPoints = null;
        exitPoints = null;
        allWords = null;
    }

    /**
     * Adds the given collection of words to the lex tree
     *
     */
    private void addWords() {
        Set words = getAllWords();
        for (Iterator i = words.iterator(); i.hasNext(); ) {
            Word word = (Word) i.next();
            addWord(word);
        }
    }


    /**
     * Returns the entire set of words, including filler words
     *
     * @return the set of all words (as Word objects)
     */
    private Set getAllWords() {
        if (allWords == null) {
            allWords = new HashSet();
            Collection words = lm.getVocabulary();
            for (Iterator i = words.iterator(); i.hasNext(); ) {
                String spelling = (String) i.next();
                Word word = dictionary.getWord(spelling);
                if (word != null) {
                    allWords.add(word);
                }
            }

            if (addFillerWords) {
                Word[] fillerWords = dictionary.getFillerWords();
                for (int i = 0; i < fillerWords.length; i++) {
                    allWords.add(fillerWords[i]);
                }
            } else if (addSilenceWord) {
                allWords.add(dictionary.getSilenceWord());
            }
        }
        return allWords;
    }

    /**
     * Adds a single word to the lex tree
     *
     * @param word the word to add
     */
    private void addWord(Word word) {
        Pronunciation[] pronunciations = word.getPronunciations();
        for (int i = 0; i < pronunciations.length; i++) {
            addPronunciation(pronunciations[i]);
        }
    }

    /**
     * Adds the given pronunciation to the lex tree
     *
     * @param pronunciation the pronunciation
     */
    private void addPronunciation(Pronunciation pronunciation ) {
        Unit baseUnit;
        Unit lc;
        Unit rc;
        Node curNode;

        Unit[] units = pronunciation.getUnits();
	baseUnit = units[0];
	EntryPoint ep = entryPointTable.getEntryPoint(baseUnit);

        if  (units.length > 1) {
            curNode = ep.getNode();
            lc = baseUnit;
            for (int i = 1; i < units.length - 1; i++) {
                baseUnit = units[i];
                rc = units[i + 1];
                HMM hmm = getHMM(baseUnit, lc, rc, HMMPosition.INTERNAL);
                curNode = curNode.addSuccessor(hmm);
                lc = baseUnit;          // next lc is this baseUnit
            }

            // now add the last unit with all possible right contexts
            Node penultimateNode = curNode;
            baseUnit = units[units.length - 1];

            for (Iterator i = entryPoints.iterator(); i.hasNext(); ) {
                rc = (Unit) i.next();
                HMM hmm = getHMM(baseUnit, lc, rc, HMMPosition.END);
                HMMNode actualTailNode = 
		    (HMMNode) penultimateNode.addSuccessor(hmm);
                actualTailNode.addRC(rc);
                actualTailNode.addSuccessor(pronunciation);
            }
        } else {
	    ep.addSingleUnitWord(pronunciation);
	}
    }



    /**
     * Retrieves an HMM for a unit in context. If there is no direct
     * match, the nearest match will be used.  Note that we are
     * currently only dealing with, at most, single unit left 
     * and right contexts.
     *
     * @param base the base CI unit
     * @param lc the left context
     * @param rc the right context
     * @param pos the position of the base unit within the word
     *
     * @return the HMM. (This should never return null)
     */
    private HMM getHMM(Unit base, Unit lc, Unit rc, HMMPosition pos) {
        int id = hmmPool.buildID(hmmPool.getID(base), hmmPool.getID(lc), 
                                 hmmPool.getID(rc));
        HMM hmm = hmmPool.getHMM(id, pos);
        if (hmm == null) {
            System.out.println(
                    "base ID " + hmmPool.getID(base)  +
                    "left ID " + hmmPool.getID(lc)  +
                    "right ID " + hmmPool.getID(rc));
            System.out.println("Unit " + base + " lc " + lc + " rc " +
                    rc + " pos " + pos);
            System.out.println("ID " + id + " hmm " + hmm);
        }
        assert hmm != null;
        return hmm;
    }


    /**
     * Returns the initial node for this lex tree
     *
     * @return the initial lex node
     */
    WordNode getInitialNode() {
        return initialNode;
    }

    /**
     * The EntryPoint table is used to manage the set of entry points
     * into the lex tree. 
     */
    class EntryPointTable {
	private Map entryPoints;


        /**
         * Create the entry point table give the set of all possible
         * entry point units
         *
         * @param entryPointCollection the set of possible entry
         * points
         */
	EntryPointTable(Collection entryPointCollection) {
	    entryPoints = new HashMap();
	    for (Iterator i = entryPointCollection.iterator(); i.hasNext(); ) {
		Unit unit = (Unit) i.next();
		entryPoints.put(unit, new EntryPoint(unit));
	    }
	}

        /**
         * Given a CI unit, return the EntryPoint object that manages
         * the entry point for the unit
         *
         * @param baseUnit the unit of interest (A ci unit)
         *
         * @return the object that manages the entry point for the
         * unit
         */
	EntryPoint getEntryPoint(Unit baseUnit) {
	    return (EntryPoint) entryPoints.get(baseUnit);
	}

        /**
         * Creates the entry point maps for all entry points. 
         */
	void createEntryPointMaps() {
	    for (Iterator i = entryPoints.values().iterator(); i.hasNext(); ) {
		EntryPoint ep = (EntryPoint) i.next();
		ep.createEntryPointMap();
	    }
	}

        /**
         * Freezes the entry point table
         */
	void freeze() {
	    for (Iterator i = entryPoints.values().iterator(); i.hasNext(); ) {
		EntryPoint ep = (EntryPoint) i.next();
		ep.freeze();
	    }
	}

        /**
         * Dumps the entry point table
         */
	void dump() {
	    for (Iterator i = entryPoints.values().iterator(); i.hasNext(); ) {
		EntryPoint ep = (EntryPoint) i.next();
		ep.dump();
	    }
	}
    }


    /**
     * Manages a single entry point.
     */
    class EntryPoint {
	Unit baseUnit;
	Node baseNode;      // second units and beyond start here
	Map unitToEntryPointMap;
	List singleUnitWords;
	int nodeCount = 0;
	Set rcSet;

        /**
         * Creates an entry point for the given usnit
         *
         * @param baseUnit the EntryPoint is created for this unit
         */
	EntryPoint(Unit baseUnit) {
	    this.baseUnit = baseUnit;
	    this.baseNode = new Node();
	    this.unitToEntryPointMap = new HashMap();
	    this.singleUnitWords = new ArrayList();
	}

        /**
         * Given a left context get a node that represents a single
         * set of entry points into this unit
         *
         * @param leftContext the left context of interest
         *
         * @return the node representing the entry point
         */
	Node getEntryPointsFromLeftContext(Unit leftContext) {
	    return (Node) unitToEntryPointMap.get(leftContext);
	}


        /**
         * Once we have built the full entry point we can
         * eliminate some fields
         */
        void freeze() {
            singleUnitWords = null;
            rcSet = null;
        }

        /**
         * Gets the base node for this entry point
         *
         * @return the base node
         */
	Node getNode() {
	    return baseNode;
	}


        /**
         * Adds a one-unit word to this entry point. Such single unit
         * words need to be dealt with specially.
         *
         * @param p the pronunciation of the single unit word
         */
	void addSingleUnitWord(Pronunciation p) {
	    singleUnitWords.add(p);
	}

        /**
         * Gets the set of possible right contexts that we can
         * transition to from this entry point
         *
         * @return the set of possible transition points.
         */
	private Collection getRC() {
	    if (rcSet == null) {
		rcSet = new HashSet();
	    }

	    for (Iterator i = baseNode.getSuccessors().iterator();
		    i.hasNext(); ) {
		HMMNode node = (HMMNode) i.next();
		rcSet.add(node.getBaseUnit());
	    }
	    return  rcSet;
	}

        /**
         * Creates the entry point map for this entry point.  The
         * entry point map is represented by the unitToEntryPointMap.
         * It contains a node for each possible left context. The node
         * successors point to the following hmm nodes (usually
         * associated with the next units that can follow from this
         * entry point.
         */
	void createEntryPointMap() {
	    for (Iterator i = exitPoints.iterator(); i.hasNext(); ) {
		Unit lc = (Unit) i.next();
		Node epNode = new Node();
		for (Iterator j = getRC().iterator(); j.hasNext(); ) {
		    Unit rc = (Unit) j.next();
		    HMM hmm = getHMM(baseUnit, lc, rc, HMMPosition.BEGIN);
		    Node addedNode = epNode.addSuccessor(hmm);
		    nodeCount++;
		    connectEntryPointNode(addedNode, rc);
		}
		connectSingleUnitWords(lc, epNode);
	        unitToEntryPointMap.put(lc, epNode);
	    }
	}


        /**
         * Connects the single unit words associated with this entry
         * point.   The singleUnitWords list contains all single unit
         * pronunciations that have as their sole unit, the unit
         * associated with this entry point. Entry points for these
         * words are added to the epNode for all possible left (exit)
         * and right (entry) contexts.
         *
         * @param lc the left context
         * @param epNode the entry point node
         */
	private void connectSingleUnitWords(Unit lc, Node epNode) {
	    if (singleUnitWords.size() > 0) {
		for (Iterator i = entryPoints.iterator(); i.hasNext(); ) {
		    Unit rc = (Unit) i.next();
		    HMM hmm = getHMM(baseUnit, lc, rc, HMMPosition.SINGLE);
		    HMMNode tailNode = (HMMNode) epNode.addSuccessor(hmm);
                    WordNode wordNode;
                    tailNode.addRC(rc);
		    nodeCount++;

		    for (int j = 0; j < singleUnitWords.size(); j++) {
			Pronunciation p = (Pronunciation) 
			    singleUnitWords.get(j);

			if (p.getWord() == dictionary.getSentenceStartWord()) {
                            wordNode = new WordNode(p, tailNode);
			    initialNode = wordNode;
			} else {
                            wordNode = (WordNode) tailNode.addSuccessor(p);
                        }
                        nodeCount++;
		    }
		}
	    }
	}

        /**
         * Connect the entry points that match the given rc to the
         * given epNode
         *
         * @param epNode add matching successors here
         * @param rc the next unit
         *
         */ 
	private void connectEntryPointNode(Node epNode, Unit rc) {
	    for (Iterator i = baseNode.getSuccessors().iterator(); 
					    i.hasNext(); ) {
		HMMNode successor = (HMMNode) i.next();
		if (successor.getBaseUnit() == rc) {
		    epNode.addSuccessor(successor);
		}
	    }
	}

        /**
         * Dumps the entry point
         */
	void dump() {
	    System.out.println("EntryPoint " + baseUnit + " RC Followers: " 
			    + getRC().size());
	    int count = 0;
	    Collection rcs = getRC();
	    System.out.print("    ");
	    for (Iterator i = rcs.iterator(); i.hasNext(); ) {
		Unit rc = (Unit) i.next();
		System.out.print(Utilities.pad(rc.getName(), 4));
		if (count++ >= 12 ) {
		    count = 0;
		    System.out.println();
		    System.out.print("    ");
		}
	    }
	    System.out.println();
	}
    }
}

/**
 * Represents a node in the HMM Tree
 */

class Node {
    private static int nodeCount = 0;
    // private Map successors; // the set of successors
    private static Map successorsPerNode = new HashMap();
    private Collection frozenSuccessors = null;

    /**
     * Creates a node
     */
    Node() {
        nodeCount++;
        if (false) {
            if ((nodeCount % 10000) == 0) {
                System.out.println("NC " + nodeCount);
            }
        }
    }


    static void freezeAllNodes() {
        // Utilities.dumpMemoryInfo("Before Freeze");
        System.out.println("Freezing " +
                successorsPerNode.keySet().size() + " nodes");
        for (Iterator i = successorsPerNode.keySet().iterator(); i.hasNext();) {
            Node node = (Node) i.next();
            node.freeze();
        }
        successorsPerNode = null;
        // Utilities.dumpMemoryInfo("After Freeze");
    }



    /**
     * Given an object get the set of successors for this object
     *
     * @param key the object key
     *
     * @return the node containing the successors
     */
    private Node getSuccessor(Object key) {
        Map successors = getSuccessorMap();
        return (Node) successors.get(key);
    }

    /**
     * Add the child to the set of successors
     *
     * @param key the object key
     * @param child the child to add
     */
    private void putSuccessor(Object key, Node child) {
        Map successors = getSuccessorMap();
        successors.put(key, child);
    }


    /**
     * Gets the successor map for this node
     *
     * @return the successor map
     */
    private Map getSuccessorMap() {
        Map successorMap = (Map) successorsPerNode.get(this);
        if (successorMap == null) {
            successorMap = new HashMap();
            successorsPerNode.put(this, successorMap);
        }
        return successorMap;
    }


    /**
     * Freeze the node. Convert the successor map into an array list
     */
    private void freeze() {
        frozenSuccessors = new ArrayList();
        for (Iterator i = getSuccessorMap().values().iterator();
                i.hasNext(); ) {
            frozenSuccessors.add(i.next());
        }
    }

    /**
     * Adds a child node holding an hmm to the successor.  If a node similar to
     * the child has already been added, we use the previously
     * added node, otherwise we add this. Also, we record the base
     * unit of the child in the set of right context
     *
     * @param hmm the hmm to add
     * @return the node that holds the hmm (new or old)
     */
    Node addSuccessor(HMM hmm) {
        Node child = null;
        Node matchingChild = getSuccessor(hmm);
        if (matchingChild == null) {
            child = new HMMNode(hmm);
            putSuccessor(hmm, child);
        } else {
            child = matchingChild;
        }
        assert frozenSuccessors == null;
        return child;
    }

    /**
     * Adds a child node holding a pronunciation to the successor.  
     * If a node similar to
     * the child has already been added, we use the previously
     * added node, otherwise we add this. Also, we record the base
     * unit of the child in the set of right context
     *
     * @param pronunciation the pronunciation to add
     *
     * @return the node that holds the pronunciation (new or old)
     */
    WordNode addSuccessor(Pronunciation pronunciation) {
        WordNode child = null;
        WordNode matchingChild = (WordNode) getSuccessor(pronunciation);
        if (matchingChild == null) {
            child = new WordNode(pronunciation, (HMMNode) this);
            putSuccessor(pronunciation, child);
        } else {
            child = matchingChild;
        }
        assert frozenSuccessors == null;
        return child;
    }


    /**
     * Adds a child node to the successor.  If a node similar to
     * the child has already been added, we use the previously
     * added node, otherwise we add this. Also, we record the base
     * unit of the child in the set of right context
     *
     * @param child the child to add
     * @return the node (may be different than child if there was
     * already a node attached holding the hmm held by child)
     *
     */
    HMMNode addSuccessor(HMMNode child) {
        HMMNode matchingChild = (HMMNode) getSuccessor(child.getHMM());
        if (matchingChild == null) {
            putSuccessor(child.getHMM(), child);
        } else {
            child = matchingChild;
        }

        assert frozenSuccessors == null;
        return child;
    }


    /**
     * Returns the successors for this node
     *
     * @return the set of successor nodes
     */
    Collection getSuccessors() {
        if (frozenSuccessors != null) {
            return frozenSuccessors;
        } else {
            return getSuccessorMap().values();
        }
    }


    /**
     * Returns the string representation fo this object
     *
     * @return the string representation of the object
     */
    public String toString() {
        return "Node ";
    }

}


/**
 * A node representing a word in the hmm tree
 *
 */
class WordNode extends Node {
    private Pronunciation pronunciation;
    private HMMNode parent;

    /**
     * Creates a word node
     *
     * @param pronunciation the pronunciation to wrap in this node
     * @param parent the parent node
     */
    WordNode(Pronunciation pronunciation, HMMNode parent) {
        this.pronunciation = pronunciation;
        this.parent = parent;
    }


    /**
     * Gets the word associated with this node
     *
     * @return the word
     */
    Word getWord() {
        return pronunciation.getWord();
    }

    /**
     * Gets the pronunciation associated with this node
     *
     * @return the pronunciation
     */
    Pronunciation getPronunciation() {
        return pronunciation;
    }

    /**
     * Gets the set of possible next units for this node
     *
     * @return the set of next units
     */
    Collection getRC() {
        return parent.getRC();
    }

    /**
     * Gets the last unit for this word
     *
     * @return the last unit
     */
    Unit getLastUnit() {
	Unit[] units = pronunciation.getUnits();
	return units[units.length - 1];
    }

    /**
     * Returns the successors for this node
     *
     * @return the set of successor nodes
     */
    Collection getSuccessors() {
        throw new Error("Not supported");
    }


    /**
     * Gets the parent of this word node
     *
     * @return the parent
     */
    HMMNode getParent() {
        return parent;
    }


    /**
     * Returns a string representation for this object
     *
     * @return a string representation
     */
    public String toString() {
        return "WordNode " + pronunciation;
    }
}

/**
 * A node that represents an HMM in the hmm tree
 */
class HMMNode extends Node {
    private HMM hmm;
    private Set rcSet;

    /**
     * Creates the node, wrapping the given hmm
     *
     * @param hmm the hmm to hold
     */
    HMMNode(HMM hmm) {
        this.hmm = hmm;
    }

    /**
     * Returns the base unit for this hmm node
     *
     * @return the base unit
     */
    Unit getBaseUnit() {
        return hmm.getBaseUnit();
    }

    /**
     * Returns the hmm for this node
     *
     * @return the hmm
     */
    HMM getHMM() {
        return hmm;
    }

    /**
     * Returns a string representation for this object
     *
     * @return a string representation
     */
    public String toString() {
        return "HMMNode " + hmm;
    }

    /**
     * Adds a right context to the set of possible right contexts for
     * this node. This is typically only needed for hmms at the ends
     * of words.
     *
     * @param rc the right context.
     */
    void addRC(Unit rc) {
	if (rcSet == null) {
	    rcSet = new HashSet();
	}
	rcSet.add(rc);
    }

    /**
     * returns the set of right contexts for this node
     *
     * @return the set of right contexts
     */
    Collection getRC() {
	return rcSet;
    }
}

