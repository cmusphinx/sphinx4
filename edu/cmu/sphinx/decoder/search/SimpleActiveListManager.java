
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

    Class listOrder[] = {
        // Todo: Find an alternative to hardcoding the state class order.
        // TODO:  This is a horrible hack.  How should we really do this
        // TODO: it is tied to the question of what information should
        // TODO: be exported by the linguist
        LexTreeLinguist.LexTreeInitialState.class,
        HMMStateState.class,
        LexTreeLinguist.LexTreeHMMState.class,
        HMMStateStateNE.class,
        LexTreeHMMStateNE.class,
        BranchState.class,
        LexTreeLinguist.LexTreeUnitState.class,
        PronunciationState.class,
        LexTreeLinguist.LexTreeWordState.class,
        ExtendedUnitState.class,
        GrammarState.class
    };
    AbstractMap listMap = new HashMap();
    SphinxProperties props;
    int listPtr = 0;
    private class HMMStateStateNE {}
    private class LexTreeHMMStateNE {}
    int absoluteWordBeamWidth;
    float relativeWordBeamWidth;

    /**
     * Creates active lists with properties
     *
     * @param props the sphinx properties
     */
    public SimpleActiveListManager(SphinxProperties props) {
        this.props = props;
        absoluteWordBeamWidth = props.getInt(PROP_ABSOLUTE_WORD_BEAM_WIDTH,
                PROP_ABSOLUTE_WORD_BEAM_WIDTH_DEFAULT);
        double linearRelativeWordBeamWidth = props.getFloat(PROP_RELATIVE_WORD_BEAM_WIDTH,
                PROP_RELATIVE_WORD_BEAM_WIDTH_DEFAULT);

        LogMath logMath = LogMath.getLogMath(props.getContext());
        relativeWordBeamWidth = logMath.linearToLog(linearRelativeWordBeamWidth);


    }

    /**
     * Creates a new version of this active list with
     * the same general properties as this list
     *
     * @return the new active list
     */
    public ActiveListManager createNew() {
        return new SimpleActiveListManager(props);
    }




    /**
     * Adds the given token to the list
     *
     * @param token the token to add
     */
    public void add(Token token) {
        assert isKnownType(token);

        Class type;
        // TODO:  This is a horrible hack.  How should we really do this
        // TODO: it is tied to the question of what information should
        // TODO: be exported by the linguist
        if (token.getSearchState() instanceof HMMStateState && !token.isEmitting()) {
            type = HMMStateStateNE.class;
        } else if( token.getSearchState() instanceof LexTreeLinguist.LexTreeHMMState && !token.isEmitting() ) {
            type = LexTreeHMMStateNE.class;
        } else {
            type = token.getSearchState().getClass();
        }
        ActiveList activeList = findListFor(type);
        if (activeList == null) {
            SimpleActiveList simpleActiveList = new SimpleActiveList(props);
            if (!token.isEmitting()) {
                simpleActiveList.absoluteBeamWidth = absoluteWordBeamWidth;
                simpleActiveList.relativeBeamWidth = relativeWordBeamWidth;
            }
            activeList = simpleActiveList;
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

