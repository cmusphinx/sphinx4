
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

package edu.cmu.sphinx.decoder.linguist.lextree;

import edu.cmu.sphinx.knowledge.dictionary.Dictionary;
import edu.cmu.sphinx.knowledge.dictionary.Word;

import edu.cmu.sphinx.knowledge.acoustic.HMM;
import edu.cmu.sphinx.knowledge.acoustic.HMMPosition;
import edu.cmu.sphinx.knowledge.acoustic.Unit;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.knowledge.dictionary.Pronunciation;
import edu.cmu.sphinx.knowledge.dictionary.Dictionary;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.knowledge.language.WordSequence;
import edu.cmu.sphinx.decoder.linguist.util.HMMPool;


import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


/**
 * Represents the vocabulary as a lex tree
 */
class LexTree {

    private NonLeafLexNode initialNode;
    private HMMPool hmmPool;
    private Dictionary dictionary;
    private Map nextNodesMap = new HashMap();
    private boolean addSilenceWord = true;

    private Word sentenceStartWord;
    private Word sentenceEndWord;
    private Word silenceWord;
    private LanguageModel lm;

    /**
     * Creates the lextree
     *
     * @param pool the pool of HMMs and units
     * @param dictionary the dictionary containing the pronunciations
     * @param words the set of words to add to the lex tree
     */
    LexTree(HMMPool pool, Dictionary dictionary, LanguageModel lm) {
        hmmPool = pool;
        this.dictionary = dictionary;
        this.lm = lm;
        this.sentenceStartWord = dictionary.getSentenceStartWord();
        this.sentenceEndWord = dictionary.getSentenceEndWord();
        this.silenceWord = dictionary.getSilenceWord();

        Utilities.dumpMemoryInfo("lextree before");
        Timer.start("Create lextree");
        initialNode = new InitialLexNode();
        addWords(lm);
        freezeNodes();
        Timer.stop("Create lextree");
        Utilities.dumpMemoryInfo("lextree after");
        dumpStats();
        // dumpTree(0, initialNode);
    }


    /**
     * Returns the initial node for this lex tree
     *
     * @return the initial lex node
     */
    NonLeafLexNode getInitialNode() {
        return initialNode;
    }

    /**
     * Adds the given collection of words to the lex tree
     *
     * @param words the collection of words
     */
    private void addWords(LanguageModel lm) {
        Collection words = lm.getVocabulary();
        boolean addedSilence = false;
        for (Iterator i = words.iterator(); i.hasNext(); ) {
            String word = (String) i.next();

            // don't add the 'start' tag
            if (word.equals(sentenceStartWord.getSpelling())) {
                continue;
            }

            if (word.equals(silenceWord.getSpelling())) {
                addedSilence = true;
            }
            addWord(word);
        }
        if (addSilenceWord && !addedSilence) {
            addWord(Dictionary.SILENCE_SPELLING);
        }
    }


    /**
     * Adds a single word to the lex tree
     *
     * @param spelling the word to add
     */
    private void addWord(String spelling) {
        Word word = dictionary.getWord(spelling);
        if (word == null) {
            System.out.println("LexTree: Can't find word in dictionary: " 
                               + spelling + " (skipping it)");
        } else {
            Pronunciation[] pronunciations =
                word.getPronunciations(null);
            if (pronunciations == null) {
                System.out.println("LexTree: Can't find pronunciation for '" +
                                   word + " (skipping it)");
            } else {
                float prob = getProbability(word);
                for (int i = 0; i < pronunciations.length; i++) {
                    addPronunciation(pronunciations[i], prob);
                }
            }
        }
    }

    /**
     * Gets the unigram probability for the given word
     *
     * @param word the word
     *
     * @return the unigram probability for the word.
     */
    private float getProbability(Word word) {
        Word[] wordArray = new Word[1];
        wordArray[0] = word;
        return lm.getProbability(WordSequence.getWordSequence(wordArray));
    }

    /**
     * Adds the given pronunciation to the lex tree
     *
     * @param pronunciation the pronunciation
     */
    private void addPronunciation(Pronunciation pronunciation, float prob) {
        NonLeafLexNode node = initialNode;
        Unit[] units = pronunciation.getUnits();

        for (int i = 0; i < units.length; i++) {
            node = node.addUnit(units[i], getPosition(i, units), prob);
        }
        node.addWord(pronunciation, prob);
    }

    /**
     * Searches through the lex tree for the node that represents the
     * given word.
     *
     * @param word the word of intereset
     */
    WordLexNode findWordNode(String word) {
        WordLexNode node = findWordNode(initialNode, word);
        if (node == null) {
            System.out.println
                ("LexTree.findWordNode(): cannot find WordLexNode for "+word);
        }
        return node;
    }

    /**
     * Searches through the lex tree for the node that represents the
     * given word.
     *
     * @param node the node to start the search at
     * @param word the word to search for
     *
     * @return the first found node representing the word or null if
     * it is not found
     */
    private WordLexNode findWordNode(LexNode node, String word) {

        if (node instanceof WordLexNode) {
            WordLexNode wordNode = (WordLexNode) node;
            if (wordNode.getPronunciation().getWord().getSpelling().equals
                (word)) {
                return wordNode;
            } else {
                return null;
            }
        } else if (node instanceof NonLeafLexNode) {
            NonLeafLexNode nlNode = (NonLeafLexNode) node;
            LexNode[] nextNodes = nlNode.getNextNodes();
            for (int i = 0; i < nextNodes.length; i++) {
                LexNode nextNode = nextNodes[i];
                WordLexNode result = findWordNode(nextNode, word);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Given an index into a set of units representing a
     * pronunciation, determine the HMMPosition of the index within
     * the word.
     *
     * @param index the unit index
     * @param units the set of units
     *
     * @return the position of the unit
     *
     */
    private HMMPosition getPosition(int index, Unit[] units) {
        HMMPosition position = HMMPosition.INTERNAL;
        if (units.length == 1) {
            position = HMMPosition.SINGLE;
        } else if (index == 0) {
            position = HMMPosition.BEGIN;
        } else if (index == units.length - 1) {
            position = HMMPosition.END;
        }
        return position;
    }

    /**
     * Freeze the nodes in the tree.  After we are done building the
     * tree we can convert our successor hashmaps into simple arrays,
     * conserving space and making access faster.
     */
    private void freezeNodes() {
        for (Iterator i = nextNodesMap.keySet().iterator(); i.hasNext(); ) {
            NonLeafLexNode node = (NonLeafLexNode) i.next();
            node.freeze();
        }
        nextNodesMap = null;
        lm = null;
        dictionary = null;
    }

    /**
     * Dumps the words that are in the lex tree
     *
     * @param node the root of the tree to dump
     */
    void dumpWords(NonLeafLexNode node) {
        LexNode[] nextNodes = node.getNextNodes();
        for (int i = 0; i < nextNodes.length; i++) {
            LexNode nextNode = nextNodes[i];
            if (nextNode instanceof WordLexNode) {
                WordLexNode wln = (WordLexNode) nextNode;
                System.out.println("Word: " +
                        wln.getPronunciation().getWord());
            } else {
                dumpWords((NonLeafLexNode) nextNode);
            }
        }
    }

    /**
     * Dumps the tree
     *
     * @param level the level of the dump
     * @param node the root of the tree to dump
     */
    void dumpTree(int level, NonLeafLexNode node) {
        LexNode[] nextNodes = node.getNextNodes();
        for (int i = 0; i < nextNodes.length; i++) {
            LexNode nextNode = nextNodes[i];
            if (nextNode instanceof WordLexNode) {
                WordLexNode wln = (WordLexNode) nextNode;
                System.out.println(Utilities.pad(level * 1) 
                   + "'" + wln.getPronunciation().getWord() + "'"
                   + " " + wln.getProbability());
            } else if (nextNode instanceof UnitLexNode) {
                UnitLexNode uln = (UnitLexNode) nextNode;
                System.out.println(Utilities.pad(level * 1) +
                        uln.getUnit().getName() + 
                        " " + uln.getProbability());
                dumpTree(level + 1,  uln);
            }
        }
    }

    /**
     * Collects stats for a subtree
     *
     * @param int depth the current depth in the tree
     * @param stats the stats are accumulated here
     * @param node the root of the tree to collect the stats
     */
    void collectStats(int depth, TreeStats stats, NonLeafLexNode node) {
        if (depth > stats.maxDepth) {
            stats.maxDepth = depth;
        }
        LexNode[] nextNodes = node.getNextNodes();

        if (nextNodes.length > stats.maxBranches) {
            stats.maxBranches = nextNodes.length;
        }
        for (int i = 0; i < nextNodes.length; i++) {
            stats.numBranches++;
            LexNode nextNode = nextNodes[i];
            if (nextNode instanceof WordLexNode) {
                stats.numWords++;
            } else if (nextNode instanceof UnitLexNode) {
                stats.numUnits++;
                collectStats(depth + 1, stats, (NonLeafLexNode) nextNode);
            }
        }
    }

    /**
     * Dumps the stats for this tree
     */
    void dumpStats() {
        TreeStats stats = new TreeStats();
        collectStats(0, stats, initialNode);
        stats.dump();

    }

    /**
     * Little class to track tree statistics
     */
    class TreeStats {
        int numBranches = 0;
        int numWords = 0;
        int numUnits = 0;
        int maxBranches = 0;
        int maxDepth = 0;

        /**
         * Dumps the tree stats
         */
        void dump() {
            System.out.println(" =========== lex tree stats ======= ");
            System.out.println(" Units       : " + numUnits);
            System.out.println(" Words       : " + numWords);
            System.out.println(" MaxDepth    : " + maxDepth);
            System.out.println(" NumBranches : " + numBranches);
            System.out.println(" MaxBranches : " + maxBranches);
            System.out.println(" AvgBranches : " 
                    + numBranches / (float) numUnits);
        }
    }


    /**
     * A LexTree is made up of lex nodes. The simplest kind of lex
     * node is a leaf lex node.  A leaf lex node has no successors
     */
    class LexNode {
        private float probability; // probability of reaching this node

        /**
         * Creates a lex node with the given probability
         *
         * @param probability the probability of transitioning to this
         * node
         */
        LexNode(float probability) {
            this.probability = probability;
        }

        /**
         * Sets the node probability
         *
         * @param probability the node probability
         */
        protected void setProbability(float probability) {
            this.probability = probability;
        }

        /**
         * Gets the node probability
         *
         * @param the node probability
         */
        public float getProbability() {
            // BUG: disable this feature
            return 0.0f;
        }
    }



    /**
     * A lexNode is a non-leaf lex node. It has successors. When
     * building the lex tree, it is convenient and efficient to use a
     * hashmap to manage the set of successors, but once the entire
     * lextree is built we'd rather have the successors managed as a
     * simple array for speed and space reduction.  Therefore, when
     * building the lex tree, the successors are maintained in a
     * lexTree.nextNodesMap that is keyed with the lex node.  Once the
     * lex tree is built, the nodes in the tree are 'frozen' and the
     * hashmap successors are converted to simple arrays.
     */
    class NonLeafLexNode extends LexNode {
        private LexNode[] nextNodes;

        /**
         * creates a lex node.
         */
        NonLeafLexNode(float probability) {
            super(probability);
            nextNodesMap.put(this, new HashMap());
        }



        /**
         * Get the set of successor lex nodes
         *
         * @return the collection of successors
         */
        public LexNode[] getNextNodes() {
            return nextNodes;
        }

        /**
         * Freezes the node
         */
        void freeze() {
            Map map = getNextNodesMap();
            nextNodes = (LexNode[])
                map.values().toArray(new LexNode[map.size()]);
        }

        /**
         * Gets the successor map for this node
         *
         * @return the successor map
         */
        private Map getNextNodesMap() {
            return (Map) nextNodesMap.get(this);
        }

        /**
         * Adds a unit to the set of succesors to this node. If a
         * child representing this unit already exists then that child
         * is returned, otherwise a new child node is created,
         * added and returned.
         *
         * @param unit the unit to add
         *
         * @return the node representing the unit added
         */
        NonLeafLexNode addUnit(Unit unit, HMMPosition hmmPosition, float prob) {
            assert !unit.isContextDependent();
            UnitInPosition uip = new UnitInPosition(unit, hmmPosition);
            UnitLexNode next = (UnitLexNode) getNextNodesMap().get(uip);
            if (next != null) {
                assert next.getID() == hmmPool.getID(unit);
                if (next.getProbability() < prob) {
                    next.setProbability(prob);
                }
                return next;
            } else {
                next = new UnitLexNode(unit, hmmPosition, prob);
                getNextNodesMap().put(uip, next);
            }
            return next;
        }

        /**
         * Adds a word (represented as a pronunciation) to this node
         *
         * @param p the pronunciation to add
         *
         * @return the node representing the word
         */
        void addWord(Pronunciation p, float prob) {
            LexNode next = (LexNode) getNextNodesMap().get(p);
            if (next != null) {
                System.out.println("Duplicate pronunciation for " + p
                        + " dropped");
            } else {
                getNextNodesMap().put(p, new WordLexNode(p, prob));
            }
        }
    }


    /**
     * Represents a unit in a lex tree
     */
    class UnitLexNode extends NonLeafLexNode {
        private int id;
        private HMMPosition position;

        /**
         * Creates a UnitLexNode for the given unit
         *
         * @param unit the unit held by this node
         * @param position the position of the unit
         * word
         */
        UnitLexNode(Unit unit, HMMPosition position, float prob) {
            super(prob);
            id = hmmPool.getID(unit);
            this.position = position;
        }

        /**
         * Gets the unit for this node
         *
         * @return the unit
         */
        Unit getUnit() {
            return hmmPool.getUnit(id);
        }

        /**
         * Gets the unit for this node
         *
         * @return the unit
         */
        HMMPosition getPosition() {
            return position;
        }

        /**
         * Gets the unit ID 
         *
         * @return the unit id
         */
        int getID() {
            return id;
        }

        /**
         * Determines if this unit marks a word end
         *
         * @return <code>true</code> if this unit marks a word end.
         */
        boolean isWordEnd() {
            return position == HMMPosition.SINGLE || 
                   position == HMMPosition.END;
        }

        /**
         * Determines if this unit marks a word beginning
         *
         * @return <code>true</code> if this unit marks a word end.
         */
        boolean isWordBeginning() {
            return position == HMMPosition.BEGIN || 
                   position == HMMPosition.SINGLE;
        }

        public String toString() {
            return getUnit().getName();
        }
    }



    /**
     * An initial node (the head of the tree)
     */
    class InitialLexNode extends NonLeafLexNode {
        InitialLexNode() {
            super(0.0f);
        }
    }



    /**
     * Represents a word in a lex tree. Words are always leaf nodes
     */
    class WordLexNode extends LexNode {
        private Pronunciation pronunciation;

        /**
         * Creates a node with the given pronunciation
         *
         * @param p the pronunciation
         */
        WordLexNode(Pronunciation p, float probability) {
            super(probability);
            this.pronunciation = p;
        }


        /**
         * Returns the pronunciation for this node
         *
         * @param the pronunciation
         */
        Pronunciation getPronunciation() {
            return pronunciation;
        }

        /**
         * Returns a string representation of this object
         *
         * @param the string representation
         */
        public String toString() {
            return "WordLexNode: " + pronunciation.toString();
        }

        /**
         * Determines if this node marks the beginning of a sentence
         *
         * @return true if this node markes the  beginning of a sentence
         *
         */
        public boolean isSentenceStart() {
            return pronunciation.getWord().equals(sentenceStartWord);
        }

        /**
         * Determines if this node marks the end of a sentence
         *
         * @return true if this node marks the end of a sentence
         *
         */
        public boolean isSentenceEnd() {
            return pronunciation.getWord().equals(sentenceEndWord);
        }

        /**
         * Determines if this node marks a silence word
         *
         * @return true if this node marks a silence word
         *
         */
        public boolean isSilence() {
            return pronunciation.getWord().equals(silenceWord);
        }
    }

}


/**
 * Represents a unit in a particular position. This object is designed
 * to server has a hash key, and implements hashCode and equals
 * properly for that task.
 */
class UnitInPosition {
    private Unit unit;
    private HMMPosition position;

    /**
     * Creates a UnitInPosition object
     *
     * @param unit the unit
     * @param position the position
     */
    UnitInPosition(Unit unit, HMMPosition position) {
        this.unit = unit;
        this.position = position;
    }

    /**
     * Returns the hashCode for this object
     *
     * @return the hashCode
     */
    public int hashCode() {
        return unit.getName().hashCode() * 307 + position.hashCode();
    }

    
    /**
     * Determines if the given object is equal to this object
     * 
     * @param o the object to test
     *
     * @return <code>true</code> if the object is equal to this
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof UnitInPosition) {
            UnitInPosition other = (UnitInPosition) o;
            return other.getUnit().equals(unit) && 
                   other.getPosition() == position;
        } else {
            return false;
        }
    }

    /**
     * Gets the unit
     *
     * @return the unit
     */
    public Unit getUnit() {
        return unit;
    }

    /**
     * Gets the position
     *
     * @return the position
     */
    public HMMPosition getPosition() {
        return position;
    }
}
