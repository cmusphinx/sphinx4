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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;

import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * A list of ActiveLists. Different token types are placed in different lists.
 * 
 * This class is not thread safe and should only be used by a single thread.
 *  
 */
public class SimpleActiveListManager implements ActiveListManager {

    /**
     * This property is used in the Iterator returned by the
     * getNonEmittingListIterator() method. When the Iterator.next() method is
     * called, this property determines whether the lists prior to that
     * returned by next() are empty (they should be empty). If they are not
     * empty, an Error will be thrown.
     */
    public static final String PROP_CHECK_PRIOR_LISTS_EMPTY = "checkPriorListsEmpty";

    /**
     * The default value of PROP_CHECK_PRIOR_LISTS_EMPTY.
     */
    public static final boolean PROP_CHECK_PRIOR_LISTS_EMPTY_DEFAULT = false;

    /**
     * Sphinx property that defines the name of the active list factory to be
     * used by this search manager.
     */
    public final static String PROP_ACTIVE_LIST_FACTORY = "activeListFactory";

    /**
     * Sphinx property that defines the name of the word active list factory to
     * be used by this search manager. The Word active list typically has
     * narrower relative and absolute beams than the standard factory.
     */
    public final static String PROP_WORD_ACTIVE_LIST_FACTORY = "wordActiveListFactory";
    // --------------------------------------
    // Configuration data
    // --------------------------------------
    private String name;
    private boolean checkPriorLists;
    private ActiveListFactory activeListFactory;
    private ActiveListFactory wordActiveListFactory;
    private int absoluteWordBeam;
    private double relativeWordBeam;

    private Class[] searchStateOrder;
    private Class[] nonEmittingClasses;
    private Class emittingClass;
    private AbstractMap listMap = new HashMap();
    private ActiveList emittingActiveList;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        this.name = name;
        registry.register(PROP_ACTIVE_LIST_FACTORY, PropertyType.COMPONENT);
        registry
                .register(PROP_WORD_ACTIVE_LIST_FACTORY, PropertyType.COMPONENT);
        registry.register(PROP_CHECK_PRIOR_LISTS_EMPTY, PropertyType.BOOLEAN);
        
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        activeListFactory = (ActiveListFactory) ps.getComponent(
                PROP_ACTIVE_LIST_FACTORY, ActiveListFactory.class);
        wordActiveListFactory = (ActiveListFactory) ps.getComponent(
                PROP_WORD_ACTIVE_LIST_FACTORY, ActiveListFactory.class);
        checkPriorLists = ps.getBoolean(PROP_CHECK_PRIOR_LISTS_EMPTY,
                PROP_CHECK_PRIOR_LISTS_EMPTY_DEFAULT);
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
     * @see edu.cmu.sphinx.decoder.search.ActiveListManager#setStateOrder(java.lang.Class[])
     */
    public void setStateOrder(Class[] searchStateOrder) {
        this.searchStateOrder = searchStateOrder;
        emittingClass = searchStateOrder[searchStateOrder.length - 1];
        nonEmittingClasses = new Class[searchStateOrder.length - 1];

        // assign the non-emitting classes
        for (int i = 0; i < nonEmittingClasses.length; i++) {
            nonEmittingClasses[i] = searchStateOrder[i];
        }

        // create the emitting and non-emitting active lists
        createActiveLists();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.decoder.search.ActiveListManager#getStateOrder()
     */
    public Class[] getStateOrder() {
        return searchStateOrder;
    }

    /**
     * Creates the emitting and non-emitting active lists. When creating the
     * non-emitting active lists, we will look at their respective beam widths
     * (eg, word beam, unit beam, state beam).
     */
    private void createActiveLists() {
        emittingActiveList = activeListFactory.newInstance();
        for (int i = 0; i < nonEmittingClasses.length; i++) {
            ActiveList list;

            if (WordSearchState.class.isAssignableFrom(nonEmittingClasses[i])) {
                list = wordActiveListFactory.newInstance();
            } else {
                list = activeListFactory.newInstance();
            }
            listMap.put(nonEmittingClasses[i], list);
        }
    }


    /**
     * Adds the given token to the list
     * 
     * @param token
     *                the token to add
     */
    public void add(Token token) {
        ActiveList activeList = findListFor(token);
        if (activeList == null) {
            throw new Error("Cannot find ActiveList for "
                    + token.getSearchState().getClass());
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
        return (ActiveList) listMap.get(type);
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
        emittingActiveList = list.newInstance();
        return list;
    }

    /**
     * Returns an Iterator of all the non-emitting ActiveLists. The iteration
     * order is the same as the search state order.
     * 
     * @return an Iterator of non-emitting ActiveLists
     */
    public Iterator getNonEmittingListIterator() {
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
            if (checkPriorLists) {
                checkPriorLists();
            }
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
                    throw new Error("At " + nonEmittingClasses[listPtr]
                            + ". List for " + nonEmittingClasses[i]
                            + " should not have tokens.");
                }
            }
        }

        public void remove() {
            listMap.put(stateClass, list.newInstance());
        }
    }

    /**
     * Outputs debugging info for this list manager
     */
    public void dump() {
        dumpList(emittingActiveList);
        for (Iterator i = listMap.values().iterator(); i.hasNext();) {
            ActiveList al = (ActiveList) i.next();
            dumpList(al);
        }
    }

    /**
     * Dumps out debugging info for the given active list
     * 
     * @param al
     *                the active list to dump
     */
    private void dumpList(ActiveList al) {
        System.out.println("GBT " + al.getBestToken() + " size: " + al.size());
    }

}
