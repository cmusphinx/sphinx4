/*
 * 
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.decoder.search;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.ListIterator;

import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.dictionary.Word;

/**
 * A factory for WordActiveList. The word active list is active list
 * designed to hold word tokens only. In addition to the usual active
 * list properties such as absolute and relative beams, the word
 * active list allows restricting the number of copies of any
 * particular word in the word beam.  Also the word active list can
 * restrict the number of fillers in the beam.
 */
public class WordActiveListFactory implements ActiveListFactory {
    /**
     * Sphinx property that defines the name of the logmath to be used by this
     * search manager.
     */
    public final static String PROP_LOG_MATH = "logMath";

    /**
     * property that sets the max paths for a single word. (zero
     * disables this feature)
     */
    public final static String PROP_MAX_PATHS_PER_WORD
        ="maxPathsPerWord";

    /**
     * The default value for the PROP_MAX_PATHS_PER_WORD property
     */
    public final static int PROP_MAX_PATHS_PER_WORD_DEFAULT = 0;

    /**
     * property that sets the max filler words allowed in the beam.
     * (zero disables this feature)
     */
    public final static String PROP_MAX_FILLER_WORDS ="maxFillerWords";

    /**
     * The default value for the PROP_MAX_FILLER_WORDS property
     */
    public final static int PROP_MAX_FILLER_WORDS_DEFAULT = 1;


    private String name;
    private int absoluteBeamWidth = 2000;
    private float logRelativeBeamWidth;
    private LogMath logMath;
    private int maxPathsPerWord;
    private int maxFiller;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        this.name = name;
        registry.register(PROP_ABSOLUTE_BEAM_WIDTH, PropertyType.INT);
        registry.register(PROP_RELATIVE_BEAM_WIDTH, PropertyType.DOUBLE);
        registry.register(PROP_LOG_MATH, PropertyType.COMPONENT);
        registry.register(PROP_MAX_PATHS_PER_WORD, PropertyType.INT);
        registry.register(PROP_MAX_FILLER_WORDS, PropertyType.INT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        absoluteBeamWidth = ps.getInt(PROP_ABSOLUTE_BEAM_WIDTH,
                PROP_ABSOLUTE_BEAM_WIDTH_DEFAULT);
        double relativeBeamWidth = ps.getDouble(PROP_RELATIVE_BEAM_WIDTH,
                PROP_RELATIVE_BEAM_WIDTH_DEFAULT);
        logMath = (LogMath) ps.getComponent(PROP_LOG_MATH, LogMath.class);
        logRelativeBeamWidth = logMath.linearToLog(relativeBeamWidth);
        maxPathsPerWord = ps.getInt(PROP_MAX_PATHS_PER_WORD,
                PROP_MAX_PATHS_PER_WORD_DEFAULT);
        maxFiller = ps.getInt(PROP_MAX_FILLER_WORDS,
                PROP_MAX_FILLER_WORDS_DEFAULT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.decoder.search.ActiveListFactory#newInstance()
     */
    public ActiveList newInstance() {
        return new WordActiveList();
    }

    /**
     * An active list that manages words. Guarantees only one version
     * of a word.
     * 
     * 
     * Note that all scores are maintained in the LogMath log domain
     */
    class WordActiveList implements ActiveList {
        private Token bestToken;
        private List tokenList = new LinkedList();


        /**
         * Adds the given token to the list
         * 
         * @param token
         *                the token to add
         */
        public void add(Token token) {
            tokenList.add(token);
            if (bestToken == null || token.getScore() > bestToken.getScore()) {
                bestToken = token;
            }
        }

        /**
         * Replaces an old token with a new token
         * 
         * @param oldToken
         *                the token to replace (or null in which case, replace
         *                works like add).
         * 
         * @param newToken
         *                the new token to be placed in the list.
         *  
         */
        public void replace(Token oldToken, Token newToken) {
            add(newToken);
            if (oldToken != null) {
                tokenList.remove(oldToken);
            }
        }

        /**
         * Purges excess members. Remove all nodes that fall below the
         * relativeBeamWidth
         * 
         * @return a (possible new) active list
         */

        public ActiveList purge() {
            int fillerCount = 0;
            Map countMap = new HashMap();
            Collections.sort(tokenList, Token.COMPARATOR);
            // remove word duplicates
            for (ListIterator i = tokenList.listIterator(); i.hasNext(); ) {
                Token token = (Token) i.next();
                WordSearchState wordState = 
                    (WordSearchState) token.getSearchState();

                Word word = wordState.getPronunciation().getWord();

                // only allow  maxFiller words
                if (maxFiller > 0) {
                    if (word.isFiller()) {
                        if (fillerCount < maxFiller) {
                            fillerCount++;
                        } else {
                            i.remove();
                            continue;
                        }
                    }
                }

                if (maxPathsPerWord > 0) {
                    Integer count = (Integer) countMap.get(word);
                    int c = count == null ? 0 : count.intValue();

                    // Since the tokens are sorted by score we only
                    // keep the n tokens for a particular word

                    if (c < maxPathsPerWord - 1) {
                        countMap.put(word, new Integer(c + 1));
                    } else {
                        i.remove();
                    }
                }
            }

            if (tokenList.size() > absoluteBeamWidth) {
                tokenList = tokenList.subList(0, absoluteBeamWidth);
            }

            return this;
        }

        /**
         * Retrieves the iterator for this tree.
         * 
         * @return the iterator for this token list
         */
        public Iterator iterator() {
            return tokenList.iterator();
        }

        /**
         * Gets the set of all tokens
         * 
         * @return the set of tokens
         */
        public List getTokens() {
            return tokenList;
        }

        /**
         * Returns the number of tokens on this active list
         * 
         * @return the size of the active list
         */
        public final int size() {
            return tokenList.size();
        }

        /**
         * gets the beam threshold best upon the best scoring token
         * 
         * @return the beam threshold
         */
        public float getBeamThreshold() {
            return getBestScore() + logRelativeBeamWidth;
        }

        /**
         * gets the best score in the list
         * 
         * @return the best score
         */
        public float getBestScore() {
            float bestScore = -Float.MAX_VALUE;
            if (bestToken != null) {
                bestScore = bestToken.getScore();
            }
            return bestScore;
        }

        /**
         * Sets the best scoring token for this active list
         * 
         * @param token
         *                the best scoring token
         */
        public void setBestToken(Token token) {
            bestToken = token;
        }

        /**
         * Gets the best scoring token for this active list
         * 
         * @return the best scoring token
         */
        public Token getBestToken() {
            return bestToken;
        }

        /* (non-Javadoc)
         * @see edu.cmu.sphinx.decoder.search.ActiveList#createNew()
         */
        public ActiveList newInstance() {
            return WordActiveListFactory.this.newInstance();
        }
    }
}
