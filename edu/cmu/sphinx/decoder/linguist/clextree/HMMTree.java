
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
    private Node initialNode;
    private Dictionary dictionary;

    private Word silenceWord;
    private LanguageModel lm;
    private boolean addSilenceWord = true;
    private Set entryPoints = new HashSet();
    private Set exitPoints = new HashSet();
    private Set wordSet = new HashSet();
    private EntryPointTable entryPointTable;
    private boolean debug = false;

    /**
     * Creates the HMMTree
     *
     * @param pool the pool of HMMs and units
     * @param dictionary the dictionary containing the pronunciations
     * @param words the set of words to add to the lex tree
     */
    HMMTree(HMMPool pool, Dictionary dictionary, LanguageModel lm) {
        this.hmmPool = pool;
        this.dictionary = dictionary;
        this.lm = lm;
        this.silenceWord = dictionary.getSilenceWord();

        Utilities.dumpMemoryInfo("lextree before");
        Timer.start("Create HMMTree");
        compile();
        Timer.stop("Create HMMTree");
        Utilities.dumpMemoryInfo("lextree after");
        // dumpTree();
    }


    private void compile() {
        collectEntryAndExitUnits();
        entryPointTable = new EntryPointTable(entryPoints);
        addWords();

	entryPointTable.createEntryPointMaps();
	connectWords();
        freeze();
        hmmPool = null;
        dictionary = null;
        lm = null;
    }

    /**
     * Dumps the tree
     *
     * @param level the level of the dump
     * @param node the root of the tree to dump
     */


    void dumpTree() {
        System.out.println("Dumping Tree ...");
        Map dupNode = new HashMap();
        dumpTree(0, getInitialNode(), dupNode);
        System.out.println("... done Dumping Tree");
    }

    void dumpTree(int level, Node node, Map dupNode) {
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

    private void collectEntryAndExitUnits() {
        Collection words = lm.getVocabulary();
        for (Iterator i = words.iterator(); i.hasNext(); ) {
            String sword = (String) i.next();
            // BUG: why doesn't th getVocabulary call return a
            // collection of Words instead of strings, to eliminate
            // this redundant lookup
            Word word = dictionary.getWord(sword);
            if (word != null) {
                for (int j = 0; j < word.getPronunciations().length; j++) {
                    Pronunciation p = word.getPronunciations()[j];
                    Unit first = p.getUnits()[0];
                    Unit last = p.getUnits()[p.getUnits().length - 1];
                    entryPoints.add(first);
                    exitPoints.add(last);
                }
            } else {
                System.out.println("Dropped missing " + sword);
            }
        }

        if (debug) {
            System.out.println("Entry Points: " + entryPoints.size());
            System.out.println("Exit Points: " + exitPoints.size());
        }
    }

    private void buildEntryPointTable() {
        // create the entry point table and add an entry for each
        // entry point.

    }

    private void connectWords() {
	for (Iterator i = wordSet.iterator(); i.hasNext(); ) {
	    WordNode wordNode = (WordNode) i.next();
            // don't connect the sentence ending word
            if (wordNode.getWord() != dictionary.getSentenceEndWord()) {
                connectWordToStart(wordNode);
            } 
	}
    }

    // we have a word represented by the given tail node. This word 
    // needs to connect up with all of the possible entry points, 
    // while respecting the left and base context.

    // NOTE: there is likely some large opportunity for sharing here
    // that will greatly reduce memory requirements. Coming soon
    private void connectWordToStart(WordNode tail) {
	int count = 0;
	Unit lc = tail.getLastUnit();
	for (Iterator i = tail.getRC().iterator(); i.hasNext(); ) {
	    Unit nextBase = (Unit) i.next();
	    EntryPoint ep = entryPointTable.getEntryPoint(nextBase);
	    Node entryPointNode = ep.getEntryPointsFromLeftContext(lc);

	    for (Iterator j = entryPointNode.getSuccessors().iterator(); 
		    j.hasNext(); ) {
		HMMNode epNode = (HMMNode) j.next();
		tail.addSuccessor(epNode);
		count++;
	    }
	}
	if (false) {
	    System.out.println("    Added " + count + " tails to " 
		+ tail.getPronunciation());
	}
    }



    private void freeze() {
    }

    /**
     * Adds the given collection of words to the lex tree
     *
     * @param words the collection of words
     */
    private void addWords() {
        Collection words = lm.getVocabulary();
        boolean addedSilence = false;
        for (Iterator i = words.iterator(); i.hasNext(); ) {
            String sword = (String) i.next();
            Word word = dictionary.getWord(sword);

            if (word == null) {
                continue;
            }

            if (word == silenceWord) {
                addedSilence = true;
            }
            addWord(word);
        }
        if (addSilenceWord && !addedSilence) {
            addWord(silenceWord);
        }
    }

    /**
     * Adds a single word to the lex tree
     *
     * @param spelling the word to add
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
                wordSet.add(actualTailNode.addSuccessor(pronunciation));
            }
        } else {
	    ep.addSingleUnitWord(pronunciation);
	}
    }



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
    Node getInitialNode() {
        EntryPoint ep = entryPointTable.getEntryPoint(Unit.SILENCE);
        // return ep.getEntryPointsFromLeftContext(Unit.SILENCE);
        return initialNode;
    }

    class EntryPointTable {
	private Map entryPoints;
	EntryPointTable(Collection eps) {
	    entryPoints = new HashMap();
	    for (Iterator i = eps.iterator(); i.hasNext(); ) {
		Unit unit = (Unit) i.next();
		entryPoints.put(unit, new EntryPoint(unit));
	    }
	}
	EntryPoint getEntryPoint(Unit baseUnit) {
	    return (EntryPoint) entryPoints.get(baseUnit);
	}

	void createEntryPointMaps() {
	    for (Iterator i = entryPoints.values().iterator(); i.hasNext(); ) {
		EntryPoint ep = (EntryPoint) i.next();
		ep.createEntryPointMap();
	    }
	}

	List collectWordNodes() {
	    List list = new ArrayList();
	    for (Iterator i = entryPoints.values().iterator(); i.hasNext(); ) {
		EntryPoint ep = (EntryPoint) i.next();
		ep.collectWordNodes(list);
	    }
	    return list;
	}

	void dump() {
	    for (Iterator i = entryPoints.values().iterator(); i.hasNext(); ) {
		EntryPoint ep = (EntryPoint) i.next();
		ep.dump();
	    }
	}
    }


    class EntryPoint {
	Unit baseUnit;
	Node baseNode;      // second units and beyond start here
	Map unitToEntryPointMap;
	List singleUnitWords;
	int nodeCount = 0;
	Set rcSet;

	EntryPoint(Unit baseUnit) {
	    this.baseUnit = baseUnit;
	    this.baseNode = new Node();
	    this.unitToEntryPointMap = new HashMap();
	    this.singleUnitWords = new ArrayList();
	}

	Node getEntryPointsFromLeftContext(Unit leftContext) {
	    return (Node) unitToEntryPointMap.get(leftContext);
	}

	Node getNode() {
	    return baseNode;
	}


	void addSingleUnitWord(Pronunciation p) {
	    singleUnitWords.add(p);
	}

	Collection getRC() {
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
                        wordSet.add(wordNode);
		    }
		}
	    }
	}

	private void connectEntryPointNode(Node epNode, Unit rc) {
	    for (Iterator i = baseNode.getSuccessors().iterator(); 
					    i.hasNext(); ) {
		HMMNode successor = (HMMNode) i.next();
		if (successor.getBaseUnit() == rc) {
		    epNode.addSuccessor(successor);
		}
	    }
	}

	void collectWordNodes(List list ) {
	    collectWordNodes(list, baseNode);
	}

	private void collectWordNodes(List list, Node node) {
	    if (node instanceof WordNode) {
		list.add(node);
	    } else {
		for (Iterator i = node.getSuccessors().iterator();
			i.hasNext(); ) {
		    Node nextNode = (Node) i.next();
		    collectWordNodes(list, nextNode);
		}
	    }
	}


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
class Node {
    static int nodeCount = 0;
    Map successors; // the set of successors

    Node() {
        successors = new HashMap();
    }


    /**
     * Adds a child node to the successor.  If a node similar to
     * the child has already been added, we use the previously
     * added node, otherwise we add this. Also, we record the base
     * unit of the child in the set of right context
     */
    Node addSuccessor(HMM hmm) {
        Node child = null;
        Node matchingChild = (Node) successors.get(hmm);
        if (matchingChild == null) {
            child = new HMMNode(hmm);
            successors.put(hmm, child);
        } else {
            child = matchingChild;
        }
        return child;
    }

    /**
     * Adds a child node to the successor.  If a node similar to
     * the child has already been added, we use the previously
     * added node, otherwise we add this. Also, we record the base
     * unit of the child in the set of right context
     */
    WordNode addSuccessor(Pronunciation pronunciation) {
        WordNode child = null;
        WordNode matchingChild = (WordNode) successors.get(pronunciation);
        if (matchingChild == null) {
            child = new WordNode(pronunciation, (HMMNode) this);
            successors.put(pronunciation, child);
        } else {
            child = matchingChild;
        }
        return child;
    }


    /**
     * Adds a child node to the successor.  If a node similar to
     * the child has already been added, we use the previously
     * added node, otherwise we add this. Also, we record the base
     * unit of the child in the set of right context
     */
    HMMNode addSuccessor(HMMNode child) {
        HMMNode matchingChild = (HMMNode) successors.get(child.getHMM());
        if (matchingChild == null) {
            successors.put(child.getHMM(), child);
        } else {
            child = matchingChild;
        }
        return child;
    }


    void freeze() {
    }

    Collection getSuccessors() {
        return successors.values();
    }

    public String toString() {
        return "Node ";
    }

}


class WordNode extends Node {
    Pronunciation pronunciation;
    HMMNode parent;

    WordNode(Pronunciation pronunciation, HMMNode parent) {
        this.pronunciation = pronunciation;
        this.parent = parent;
    }


    Word getWord() {
        return pronunciation.getWord();
    }

    Pronunciation getPronunciation() {
        return pronunciation;
    }

    Collection getRC() {
        return parent.getRC();
    }

    Unit getLastUnit() {
	Unit[] units = pronunciation.getUnits();
	return units[units.length - 1];
    }


    public String toString() {
        return "WordNode " + pronunciation;
    }
}

class HMMNode extends Node {
    private HMM hmm;
    private Set rcSet;

    HMMNode(HMM hmm) {
        this.hmm = hmm;
    }

    Unit getBaseUnit() {
        return hmm.getBaseUnit();
    }

    HMM getHMM() {
        return hmm;
    }

    public String toString() {
        return "HMMNode " + hmm;
    }

    void addRC(Unit rc) {
	if (rcSet == null) {
	    rcSet = new HashSet();
	}
	rcSet.add(rc);
    }

    Collection getRC() {
	return rcSet;
    }
}

