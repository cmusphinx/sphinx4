
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

package edu.cmu.sphinx.decoder.search;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.decoder.linguist.simple.*;
import edu.cmu.sphinx.decoder.linguist.lextree.LexTreeLinguist;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A list of ActiveLists.  Different token types are placed in different lists..
 *
 * This class is not thread safe and should only be used by  a single
 * thread.
 *
 */
public class SimpleActiveListManager implements ActiveListManager  {

    private Class[] searchStateOrder;
    private Class[] nonEmittingClasses;
    private Class emittingClass;

    private AbstractMap listMap = new HashMap();
    private ActiveList emittingActiveList;

    private SphinxProperties props;
    private int absoluteWordBeamWidth;
    private double relativeWordBeamWidth;


    /**
     * Creates active lists with properties
     *
     * @param props the sphinx properties
     * @param searchStateOrder an array of classes that represents the order 
     *     in which the states will be returned
     */
    public SimpleActiveListManager(SphinxProperties props, 
                                   Class[] searchStateOrder) {
        this.props = props;
        this.searchStateOrder = searchStateOrder;

        this.absoluteWordBeamWidth = props.getInt
            (PROP_ABSOLUTE_WORD_BEAM_WIDTH,
             PROP_ABSOLUTE_WORD_BEAM_WIDTH_DEFAULT);

        this.relativeWordBeamWidth = 
            props.getDouble(PROP_RELATIVE_WORD_BEAM_WIDTH,
                            PROP_RELATIVE_WORD_BEAM_WIDTH_DEFAULT);

        String activeListClass = props.getString
            (SimpleBreadthFirstSearchManager.PROP_ACTIVE_LIST_TYPE,
             SimpleBreadthFirstSearchManager.PROP_ACTIVE_LIST_TYPE_DEFAULT);

        nonEmittingClasses = new Class[searchStateOrder.length - 1];

        for (int i = 0; i < nonEmittingClasses.length; i++) {
            nonEmittingClasses[i] = searchStateOrder[i];
            try {
                ActiveList list = (ActiveList)
                    Class.forName(activeListClass).newInstance();
                list.setProperties(props);
                list.setAbsoluteBeamWidth(absoluteWordBeamWidth);
                list.setRelativeBeamWidth(relativeWordBeamWidth);
                listMap.put(nonEmittingClasses[i], list);
            } catch (ClassNotFoundException fe) {
                throw new Error("Can't create active list", fe);
            } catch (InstantiationException ie) {
                throw new Error("Can't create active list", ie);
            } catch (IllegalAccessException iea) {
                throw new Error("Can't create active list", iea);
            }
        }

        emittingClass = searchStateOrder[searchStateOrder.length - 1];
        emittingActiveList = new PartitionActiveList(props);
    }


    /**
     * Creates a new version of this active list with
     * the same general properties as this list
     *
     * @return the new active list
     */
    public ActiveListManager createNew() {
        return new SimpleActiveListManager(props, searchStateOrder);
    }


    /**
     * Adds the given token to the list
     *
     * @param token the token to add
     */
    public void add(Token token) {
        ActiveList activeList;
        if (token.isEmitting()) {
            activeList = emittingActiveList;
        } else {
            activeList = findListFor(token.getSearchState().getClass());
        }
        activeList.add(token);
    }

    private ActiveList findListFor(Token token) {
        if (token.isEmitting()) {
            return emittingActiveList;
        } else {
            return findListFor(token.getSearchState().getClass());
        }
    }

    private ActiveList findListFor(Class type) {
        return (ActiveList)listMap.get(type);
    }


    /**
     * Replaces an old token with a new token
     *
     * @param oldToken the token to replace (or null in which case,
     * replace works like add).
     *
     * @param newToken the new token to be placed in the list.
     *
     */
    public void replace(Token oldToken, Token newToken) {
        ActiveList activeList = findListFor(oldToken);
        assert activeList != null;
        activeList.replace(oldToken, newToken);
    }


    /**
     * Returns the emitting ActiveList, and removes it from this manager.
     *
     * @return the emitting ActiveList
     */
    public ActiveList getEmittingList() {
        ActiveList list = emittingActiveList;
        emittingActiveList = list.createNew();
        return list;
    }


    /**
     * Returns an Iterator of all the non-emitting ActiveLists. The
     * iteration order is the same as the search state order.
     *
     * @return an Iterator of non-emitting ActiveLists
     */
    public Iterator getNonEmittingListIterator() {
        /*
        Class stateClass = nonEmittingClasses[listPtr];

	ActiveList list = (ActiveList)listMap.remove(stateClass);
        listMap.put(stateClass, list.createNew());

	if ((++listPtr) >= nonEmittingClasses.length) {
	    listPtr = 0;
	}
	return list;
        */
        return (new NonEmittingListIterator());
    }


    private class NonEmittingListIterator implements Iterator {
        private int listPtr;
        private Class stateClass;
        private ActiveList list;

        public NonEmittingListIterator() {
            listPtr = 0;
        }

        public boolean hasNext() {
            return (listPtr < nonEmittingClasses.length);
        }

        public Object next() {
            checkPriorLists();
            stateClass = nonEmittingClasses[listPtr++];
            list = (ActiveList) listMap.get(stateClass);
            return list;
        }

        /**
         * Check that all lists prior to listPtr is empty.
         */
        private void checkPriorLists() {
            for (int i = 0; i < listPtr; i++) {
                ActiveList activeList = findListFor(nonEmittingClasses[i]);
                if (activeList.size() > 0) {
                    throw new Error("At " + nonEmittingClasses[listPtr] +
                                    ". List for " + nonEmittingClasses[i] + 
                                    " should not have tokens.");
                }
            }
        }

        public void remove() {
            listMap.put(stateClass, list.createNew());
        }
    }
}
