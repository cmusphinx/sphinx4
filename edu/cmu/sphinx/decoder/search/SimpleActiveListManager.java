
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

/**
 * A list of ActiveLists.  Different token types are placed in different lists..
 *
 * This class is not thread safe and should only be used by  a single
 * thread.
 *
 */
public class SimpleActiveListManager implements ActiveListManager  {

    private Class[] listOrder;
    private AbstractMap listMap = new HashMap();
    private SphinxProperties props;
    private int listPtr = 0;
    private int absoluteWordBeamWidth;
    private float relativeWordBeamWidth;


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
        this.listOrder = searchStateOrder;

        absoluteWordBeamWidth = props.getInt
            (PROP_ABSOLUTE_WORD_BEAM_WIDTH,
             PROP_ABSOLUTE_WORD_BEAM_WIDTH_DEFAULT);
        relativeWordBeamWidth = 
            props.getFloat(PROP_RELATIVE_WORD_BEAM_WIDTH,
                           PROP_RELATIVE_WORD_BEAM_WIDTH_DEFAULT);
    }


    /**
     * Creates a new version of this active list with
     * the same general properties as this list
     *
     * @return the new active list
     */
    public ActiveListManager createNew() {
        return new SimpleActiveListManager(props, listOrder);
    }


    /**
     * Adds the given token to the list
     *
     * @param token the token to add
     */
    public void add(Token token) {
        assert isKnownType(token);

        Class type = token.getSearchState().getClass();
        
        ActiveList activeList = findListFor(type);
        if (activeList == null) {
            FastActiveList newActiveList;
            if (token.isEmitting()) {
                newActiveList = new FastActiveList(props);
            } else { 
                newActiveList = new FastActiveList
                    (props, absoluteWordBeamWidth, relativeWordBeamWidth);
            }
            activeList = newActiveList;
            listMap.put(type, activeList);
        }
        activeList.add(token);
    }

    private boolean isKnownType(Token token) {
        for (int i = 0; i<listOrder.length;i++) {
            if (token.getSearchState().getClass() == listOrder[i]) {
                return true;
            }
        }
        return false;
    }

    private ActiveList findListFor(Token token) {
        return findListFor(token.getSearchState().getClass());
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
     * Returns the next ActiveList according to the order of SearchStates.
     *
     * @return the next ActiveList
     */
    public ActiveList getNextList() {
        if (listMap.isEmpty()) {
            return null;
        }
        if (listPtr == listOrder.length) {
            listPtr = 0;
        }

        ActiveList activeList = findListFor(listOrder[listPtr++]);
        if (activeList == null) {
            return getNextList();
        }
        listMap.remove(listOrder[listPtr-1]);
        return activeList;
    }
}

