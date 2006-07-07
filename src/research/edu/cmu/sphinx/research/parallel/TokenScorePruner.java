
/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.research.parallel;

import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.search.ActiveListFactory;
import edu.cmu.sphinx.decoder.search.Token;

import edu.cmu.sphinx.decoder.pruner.Pruner;

import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.SphinxProperties;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Prunes an ActiveList of ParallelTokens based on their scores.
 */
public abstract class TokenScorePruner implements Pruner {

    public static final String PROP_ABSOLUTE_BEAM_WIDTH = "absoluteBeamWidth";
    
    public static final int PROP_ABSOLUTE_BEAM_WIDTH_DEFAULT = 2000;

    public static final String PROP_RELATIVE_BEAM_WIDTH = "relativeBeamWidth";

    public static final double PROP_RELATIVE_BEAM_WIDTH_DEFAULT = 0;

    /**
     * Sphinx property that defines the name of the logmath to be used by this
     * search manager.
     */
    public final static String PROP_LOG_MATH = "logMath";

    /**
     * Sphinx property that defines the name of the active list factory to be
     * used by this search manager.
     */
    public final static String PROP_ACTIVE_LIST_FACTORY = "activeListFactory";

    private String name;
    private int absoluteBeamWidth;
    private float relativeBeamWidth;
    private boolean doRelativePruning = false;
    private LogMath logMath;
    private ActiveListFactory activeListFactory;

    private static Comparator tokenComparator = null;


    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String, edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
        throws PropertyException {
        this.name = name;
        registry.register(PROP_ABSOLUTE_BEAM_WIDTH, PropertyType.INT);
        registry.register(PROP_RELATIVE_BEAM_WIDTH, PropertyType.DOUBLE);
        registry.register(PROP_LOG_MATH, PropertyType.COMPONENT);
        registry.register(PROP_ACTIVE_LIST_FACTORY, PropertyType.COMPONENT);
    }


    /* (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        logMath = (LogMath) ps.getComponent(PROP_LOG_MATH, LogMath.class);
        setAbsoluteBeamWidth(ps.getInt(PROP_ABSOLUTE_BEAM_WIDTH,
                                       PROP_ABSOLUTE_BEAM_WIDTH_DEFAULT));
        double linearRelativeBeamWidth =
            ps.getDouble(PROP_RELATIVE_BEAM_WIDTH,
                         PROP_RELATIVE_BEAM_WIDTH_DEFAULT);
        setRelativeBeamWidth
            ((float) logMath.linearToLog(linearRelativeBeamWidth));
        activeListFactory = (ActiveListFactory) ps.getComponent
            (PROP_ACTIVE_LIST_FACTORY, ActiveListFactory.class);
    }
    

    /**
     * Returns the score that we use to compare this Token with
     * other Tokens.
     *
     * @param token the Token to compare
     */
    protected abstract float getTokenScore(Token token);


    /**
     * Returns a token comparator that is used to order ParallelTokens in
     * descending order of their combined score.
     *
     * @return a ParallelTokenComparator
     */
    protected Comparator getTokenComparator() {
        if (tokenComparator == null) {
            tokenComparator = new Comparator() {
                public int compare(Object o1, Object o2) {
                    Token t1 = (Token) o1;
		    Token t2 = (Token) o2;
                    
                    if (getTokenScore(t1) > getTokenScore(t2)) {
                        return -1;
                    } else if (getTokenScore(t1) == getTokenScore(t2)) {
                        return 0;
                    } else {
                        return 1;
                    }
                }
            };
        }
        return tokenComparator;
    }


    /**
     * Prunes the set of tokens in the given ActiveList. It is 
     * assumed that all token in the ActiveList are ParallelTokens.
     * Pruning is performed according to the absolute and relative
     * beam widths of the given ActiveList.
     *
     * @param activeList a activeList of tokens
     */
    public ActiveList prune(ActiveList activeList) {

	ActiveList newList = activeListFactory.newInstance();

        List tokenList = activeList.getTokens();
        Collections.sort(tokenList, getTokenComparator());

        if (tokenList.size() > 0) {
            Token bestToken = (Token) tokenList.get(0);
            float highestScore = getTokenScore(bestToken);
            float pruneScore = highestScore + relativeBeamWidth;

            int count = 0;  // the number of tokens included so far
            float lastScore = highestScore;
            float thisScore = highestScore;
            
            Iterator i = tokenList.iterator();
           
            // do the pruning
            while (i.hasNext() && newList.size() < absoluteBeamWidth) {

                Token token = (Token) i.next();
                thisScore = getTokenScore(token);

		if (doRelativePruning) {
		    if (thisScore > pruneScore) {
			newList.add(token);
			lastScore = thisScore;
		    } else {
			break;
		    }
		} else {
		    newList.add(token);
		    lastScore = thisScore;
		}
            }
        }

	return newList;
    }


    /**
     * Returns the absolute beam width of this TokenScorePruner.
     *
     * @return the absolute beam width (in the linear domain)
     */
    public int getAbsoluteBeamWidth() {
        return this.absoluteBeamWidth;
    }


    /**
     * Sets the absolute beam width of this TokenScorePruner
     *
     * @param absoluteBeamWidth the absolute beam width
     */
    public void setAbsoluteBeamWidth(int absoluteBeamWidth) {
        this.absoluteBeamWidth = absoluteBeamWidth;
    }


    /**
     * Returns the relative beam width (in the linear domain) of this
     * TokenScorePruner
     *
     * @return the relative beam width (in the linear domain)
     */
    public float getRelativeBeamWidth() {
        return relativeBeamWidth;
    }


    /**
     * Sets the relative beam width (log domain) of this TokenScorePruner.
     *
     * @param relativeBeamWidth the relative beam width in log domain
     */
    public void setRelativeBeamWidth(float relativeBeamWidth) {
        this.relativeBeamWidth = relativeBeamWidth;
    }


    /**
     * Starts the pruner
     */
    public void startRecognition() {}


    /**
     * Performs post-recognition cleanup.
     */
    public void stopRecognition() {}
    

    /**
     * Allocates resources necessary for this pruner
     */
    public void allocate() {}


    /**
     * Deallocates resources necessary for this pruner
     */
    public void deallocate() {}

    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }
}


